package net.sf.odinms.client.messages;

import net.sf.odinms.client.CharacterUtil;
import net.sf.odinms.client.Equip;
import net.sf.odinms.client.ExpTable;
import net.sf.odinms.client.IItem;
import net.sf.odinms.client.Item;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.client.MapleJob;
import net.sf.odinms.client.Pet;
import net.sf.odinms.client.Statistic;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.world.PlayerCoolDownValueHolder;
import net.sf.odinms.net.world.remote.WorldLocation;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.MapleShopFactory;
import net.sf.odinms.server.MapleTrade;
import net.sf.odinms.server.life.MapleLifeFactory;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.maps.FakeCharacter;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.server.maps.MapleMapObject;
import net.sf.odinms.server.maps.MapleMapObjectType;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.StringUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class GMCommand {

    public static boolean executeGMCommand(MapleClient c, MessageCallback mc, String line) {
        MapleCharacter player = c.getPlayer();
        ChannelServer cserv = c.getChannelServer();
        String[] splitted = line.split(" ");
        if (splitted[0].equals("!ban")) {
            ban(c, mc, splitted, player, cserv);
        } else if (splitted[0].equals("!cancelbuffs")) {
            cserv.getPlayerStorage().getCharacterByName(splitted[1]).ifPresent(MapleCharacter::cancelAllBuffs);
        } else if (splitted[0].equals("!charinfo")) {
            characterInfo(mc, cserv, splitted);
        } else if (splitted[0].equals("!chattype")) {
            player.setGMChat(!player.getGMChat());
            mc.dropMessage("Done.");
        } else if (splitted[0].equals("!cleardrops")) {
            MapleMap map = player.getMap();
            List<MapleMapObject> items = map.getMapObjectsInRange(player.getPosition(), Double.POSITIVE_INFINITY, List.of(MapleMapObjectType.ITEM));
            for (MapleMapObject i : items) {
                map.removeMapObject(i);
                map.broadcastMessage(MaplePacketCreator.removeItemFromMap(i.getObjectId(), 0, player.getId()));
            }
            mc.dropMessage("You have destroyed " + items.size() + " items on the ground.");
        } else if (splitted[0].equals("!clock")) {
            player.getMap().broadcastMessage(MaplePacketCreator.getClock(Integer.parseInt(splitted[1])));
        } else if (splitted[0].equals("!connected")) {
            try {
                Map<Integer, Integer> connected = cserv.getWorldInterface().getConnected();
                StringBuilder conStr = new StringBuilder("Connected Clients: ");
                boolean first = true;
                for (int i : connected.keySet()) {
                    if (!first) {
                        conStr.append(", ");
                    } else {
                        first = false;
                    }
                    if (i == 0) {
                        conStr.append("Total: ").append(connected.get(i));
                    } else {
                        conStr.append("Ch").append(i).append(": ").append(connected.get(i));
                    }
                }
                mc.dropMessage(conStr.toString());
            } catch (RemoteException e) {
                cserv.reconnectWorld();
            }
        } else if (splitted[0].equals("!dc")) {
            disconnect(cserv, splitted);
        } else if (splitted[0].equals("!drop") || splitted[0].equals("!droprandomstatitem") || splitted[0].equals("!item")) {
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            int itemId = Integer.parseInt(splitted[1]);
            short quantity = (short) getOptionalIntArg(splitted, 2, 1);
            IItem toDrop;
            if (splitted[0].equals("!drop")) {
                if (ii.getInventoryType(itemId) == MapleInventoryType.EQUIP) {
                    toDrop = ii.getEquipById(itemId);
                } else {
                    toDrop = new Item(itemId, (byte) 0, quantity);
                }
                toDrop.setOwner(player.getName());
                player.getMap().spawnItemDrop(player, player, toDrop, player.getPosition(), true, true);
            } else if (splitted[0].equals("!item")) {
                if (itemId >= 5000000 && itemId <= 5000100) {
                    Pet.createPet(itemId);
                } else {
                    MapleInventoryManipulator.addById(c, itemId, quantity, "", player.getName());
                }
            } else {
                if (!MapleItemInformationProvider.getInstance().getInventoryType(itemId).equals(MapleInventoryType.EQUIP)) {
                    mc.dropMessage("Command can only be used for equips.");
                } else {
                    toDrop = MapleItemInformationProvider.getInstance().randomizeStats((Equip) MapleItemInformationProvider.getInstance().getEquipById(itemId));
                    player.getMap().spawnItemDrop(player, player, toDrop, player.getPosition(), true, true);
                }
            }
        } else if (splitted[0].equals("!event")) {
            if (player.getClient().getChannelServer().eventOn == false) {
                int mapid = getOptionalIntArg(splitted, 1, c.getPlayer().getMapId());
                player.getClient().getChannelServer().eventOn = true;
                player.getClient().getChannelServer().eventMap = mapid;
                try {
                    cserv.getWorldInterface().broadcastMessage(null, MaplePacketCreator.serverNotice(6, c.getChannel(), "[Event] The event has started in Channel " + c.getChannel() + " in " + player.getMapId() + "!").getBytes());
                } catch (RemoteException e) {
                    cserv.reconnectWorld();
                }
            } else {
                player.getClient().getChannelServer().eventOn = false;
                try {
                    cserv.getWorldInterface().broadcastMessage(null, MaplePacketCreator.serverNotice(6, c.getChannel(), "[Event] The event has ended. Thanks to all of those who participated.").getBytes());
                } catch (RemoteException e) {
                    cserv.reconnectWorld();
                }
            }
        } else if (splitted[0].equals("!fakechar")) {
            for (int i = 0; i < getOptionalIntArg(splitted, 1, 1); i++) {
                FakeCharacter fc = new FakeCharacter(player, player.getId() + player.getFakeChars().size() + 1);
                player.addFakeChar(fc);
            }
            mc.dropMessage("Please move around for it to take effect.");
        } else if (splitted[0].equals("!fakerelog")) {
            c.getSession().write(MaplePacketCreator.getCharInfo(player));
            player.getMap().removePlayer(player);
            player.getMap().addPlayer(player);
        } else if (splitted[0].equals("!fame")) {
            fame(cserv, splitted);
        } else if (splitted[0].equals("!giftnx")) {
            giftNx(mc, cserv, splitted);
        } else if (splitted[0].equals("!god")) {
            player.setGodMode(!player.isGodMode());
            mc.dropMessage("Done.");
        } else if (splitted[0].equals("!heal")) {
            player.setHp(player.getMaxHp());
            player.updateSingleStat(Statistic.HP, player.getMaxHp());
            player.setMp(player.getMaxMp());
            player.updateSingleStat(Statistic.MP, player.getMaxMp());
        } else if (splitted[0].equals("!healmap")) {
            for (MapleCharacter mch : player.getMap().getCharacters()) {
                if (mch != null) {
                    mch.setHp(mch.getMaxHp());
                    mch.updateSingleStat(Statistic.HP, mch.getMaxHp());
                    mch.setMp(mch.getMaxMp());
                    mch.updateSingleStat(Statistic.MP, mch.getMaxMp());
                }
            }
        } else if (splitted[0].equals("!healperson")) {
            heal(cserv, splitted);
        } else if (splitted[0].equals("!hurt")) {
            hurt(cserv, splitted);
        } else if (splitted[0].equals("!id") || splitted[0].equals("!search")) {
            try {
                URL url = new URL("http://www.mapletip.com/search_java.php?search_value=" + splitted[1] + "&check=true");
                URLConnection urlConn = url.openConnection();
                urlConn.setDoInput(true);
                urlConn.setUseCaches(false);
                BufferedReader dis = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
                String s;
                while ((s = dis.readLine()) != null) {
                    mc.dropMessage(s);
                }
                dis.close();
            } catch (IOException ioe) {
            }
        } else if (splitted[0].equals("!jail")) {
            jail(mc, cserv, splitted);
        } else if (splitted[0].equals("!job")) {
            player.changeJob(MapleJob.getById(Integer.parseInt(splitted[1])).orElse(MapleJob.BEGINNER));
        } else if (splitted[0].equals("!jobperson")) {
            changeTargetJob(cserv, splitted);
        } else if (splitted[0].equals("!karma")) {
            karma(mc, cserv, splitted, player);
        } else if (splitted[0].equals("!kill")) {
            kill(cserv, splitted);
        } else if (splitted[0].equals("!killall") || splitted[0].equals("!monsterdebug")) {
            MapleMap map = player.getMap();
            List<MapleMapObject> monsters = map.getMapObjectsInRange(player.getPosition(), Double.POSITIVE_INFINITY, List.of(MapleMapObjectType.MONSTER));
            boolean kill = splitted[0].equals("!killall");
            for (MapleMapObject monstermo : monsters) {
                MapleMonster monster = (MapleMonster) monstermo;
                if (kill) {
                    map.killMonster(monster, player, true);
                    monster.giveExpToCharacter(player, monster.getExp(), true, 1);
                } else {
                    mc.dropMessage("Monster " + monster.toString());
                }
            }
            if (kill) {
                mc.dropMessage("Killed " + monsters.size() + " monsters.");
            }
        } else if (splitted[0].equals("!killallmany")) {
            int size = 0;
            for (MapleCharacter mch : cserv.getPlayerStorage().getAllCharacters()) {
                MapleMap map = mch.getMap();
                List<MapleMapObject> monsters = map.getMapObjectsInRange(mch.getPosition(), Double.POSITIVE_INFINITY, List.of(MapleMapObjectType.MONSTER));
                for (MapleMapObject monstermo : monsters) {
                    MapleMonster monster = (MapleMonster) monstermo;
                    map.killMonster(monster, player, true);
                    monster.giveExpToCharacter(player, monster.getExp(), true, 1);
                }
                size += monsters.size();
            }
            mc.dropMessage("Killed " + size + " monsters.");
        } else if (splitted[0].equals("!killeveryone")) {
            for (MapleCharacter mch : cserv.getPlayerStorage().getAllCharacters()) {
                mch.setHp(0);
                mch.updateSingleStat(Statistic.HP, 0);
            }
        } else if (splitted[0].equals("!killmap")) {
            for (MapleCharacter mch : player.getMap().getCharacters()) {
                mch.setHp(0);
                mch.updateSingleStat(Statistic.HP, 0);
            }
        } else if (splitted[0].equals("!levelperson")) {
            levelTarget(cserv, splitted);
        } else if (splitted[0].equals("!levelup")) {
            player.gainExp(ExpTable.getExpNeededForLevel(player.getLevel() + 1) - player.getExp(), false, false);
        } else if (splitted[0].equals("!mesoperson")) {
            giveMesoToTarget(cserv, splitted);
        } else if (splitted[0].equals("!mesos")) {
            player.gainMeso(Integer.parseInt(splitted[1]), true);
        } else if (splitted[0].equals("!mute")) {
            mute(mc, cserv, splitted);
        } else if (splitted[0].equals("!notice") || (splitted[0].equals("!say"))) {
            String type = "[Notice] ";
            if (splitted[0].equals("!say")) {
                type = "[" + player.getName() + "] ";
            }
            try {
                ChannelServer.getInstance(c.getChannel()).getWorldInterface().broadcastMessage(player.getName(), MaplePacketCreator.serverNotice(6, type + StringUtil.joinStringFrom(splitted, 1)).getBytes());
            } catch (RemoteException e) {
                cserv.reconnectWorld();
            }
        } else if (splitted[0].equals("!openshop")) {
            MapleShopFactory.getInstance().getShop(Integer.parseInt(splitted[1])).sendShop(c);
        } else if (splitted[0].equals("!pos")) {
            mc.dropMessage("Your Pos: x = " + player.getPosition().x + ", y = " + player.getPosition().y + ", fh = " + player.getMap().getFootholds().findBelow(player.getPosition()).getId());
        } else if (splitted[0].equals("!resetcooldowns")) {
            for (PlayerCoolDownValueHolder i : player.getAllCooldowns()) {
                player.removeCooldown(i.skillId());
            }
            mc.dropMessage("Success.");
        } else if (splitted[0].equals("!saveall")) {
            for (ChannelServer chan : ChannelServer.getAllInstances()) {
                for (MapleCharacter chr : chan.getPlayerStorage().getAllCharacters()) {
                    chr.saveToDB(true);
                }
            }
            mc.dropMessage("Done.");
        } else if (splitted[0].equals("!servermessage")) {
            for (int i = 1; i <= ChannelServer.getAllInstances().size(); i++) {
                ChannelServer.getInstance(i).setServerMessage(StringUtil.joinStringFrom(splitted, 1));
            }
        } else if (splitted[0].equalsIgnoreCase("!showMonsterID")) {
            MapleMap map = player.getMap();
            double range = Double.POSITIVE_INFINITY;
            List<MapleMapObject> monsters = map.getMapObjectsInRange(player.getPosition(), range, List.of(MapleMapObjectType.MONSTER));
            for (MapleMapObject monstermo : monsters) {
                MapleMonster monster = (MapleMonster) monstermo;
                mc.dropMessage("name=" + monster.getName() + " ID=" + monster.getId() + " isAlive=" + monster.isAlive());
            }
        } else if (splitted[0].equals("!skill")) {
            player.maxSkillLevel(Integer.parseInt(splitted[1]));
        } else if (splitted[0].equals("!slap")) {
            int loss = Integer.parseInt(splitted[2]);
            slap(mc, cserv, splitted, loss);
        } else if (splitted[0].equals("!spawn")) {
            for (int i = 0; i < Math.min(getOptionalIntArg(splitted, 2, 1), 500); i++) {
                player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(Integer.parseInt(splitted[1])), player.getPosition());
            }
        } else if (splitted[0].equals("!unjail")) {
            unjail(cserv, splitted);
        } else if (splitted[0].equals("!warpallhere")) {
            for (MapleCharacter mch : cserv.getPlayerStorage().getAllCharacters()) {
                if (mch.getMapId() != player.getMapId()) {
                    mch.changeMap(player.getMap(), player.getPosition());
                }
            }
        } else if (splitted[0].equals("!warp")) {
            warp(c, mc, cserv, splitted, player);
        } else if (splitted[0].equals("!warpmap")) {
            for (MapleCharacter mch : player.getMap().getCharacters()) {
                if (mch != null) {
                    MapleMap target = ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(Integer.parseInt(splitted[1]));
                    mch.changeMap(target, target.getPortal(0));
                }
            }
        } else if (splitted[0].equals("!warphere")) {
            warpTargetHere(cserv, splitted, player);
        } else if (splitted[0].equals("!whereami")) {
            mc.dropMessage("You are on map " + player.getMap().getId());
        } else if (splitted[0].equals("!whosthere")) {
            StringBuilder builder = new StringBuilder("Players on Map: ");
            for (MapleCharacter chr : player.getMap().getCharacters()) {
                if (builder.length() > 150) {
                    builder.setLength(builder.length() - 2);
                    mc.dropMessage(builder.toString());
                }
                builder.append(CharacterUtil.makeMapleReadable(chr.getName())).append(", ");
            }
            builder.setLength(builder.length() - 2);
            c.getSession().write(MaplePacketCreator.serverNotice(6, builder.toString()));
        } else if (splitted[0].equals("!exprate")) {//RATE COMMANDS
            int exp = Integer.parseInt(splitted[1]);
            cserv.setExpRate(exp);
            ChannelServer.getInstance(c.getChannel()).broadcastPacket(MaplePacketCreator.serverNotice(6, "Exp Rate has been changed to " + exp + "x."));
        } else if (splitted[0].equals("!petexprate")) {
            int exp = Integer.parseInt(splitted[1]);
            cserv.setPetExpRate(exp);
            ChannelServer.getInstance(c.getChannel()).broadcastPacket(MaplePacketCreator.serverNotice(6, "Pet Exp Rate has been changed to " + exp + "x."));
        } else if (splitted[0].equals("!mountexprate")) {
            int exp = Integer.parseInt(splitted[1]);
            cserv.setMountRate(exp);
            ChannelServer.getInstance(c.getChannel()).broadcastPacket(MaplePacketCreator.serverNotice(6, "Mount Exp Rate has been changed to " + exp + "x."));
        } else if (splitted[0].equals("!mesorate")) {
            int meso = Integer.parseInt(splitted[1]);
            cserv.setMesoRate(meso);
            ChannelServer.getInstance(c.getChannel()).broadcastPacket(MaplePacketCreator.serverNotice(6, "Meso Rate has been changed to " + meso + "x."));
        } else if (splitted[0].equals("!droprate")) {
            int drop = Integer.parseInt(splitted[1]);
            cserv.setDropRate(drop);
            ChannelServer.getInstance(c.getChannel()).broadcastPacket(MaplePacketCreator.serverNotice(6, "Drop Rate has been changed to " + drop + "x."));
        } else if (splitted[0].equals("!bossdroprate")) {
            int bossdrop = Integer.parseInt(splitted[1]);
            cserv.setBossDropRate(bossdrop);
            ChannelServer.getInstance(c.getChannel()).broadcastPacket(MaplePacketCreator.serverNotice(6, "Boss Drop Rate has been changed to " + bossdrop + "x."));
        } else if (splitted[0].equals("!shopmesorate")) {
            int rate = Integer.parseInt(splitted[1]);
            cserv.setShopMesoRate(rate);
            ChannelServer.getInstance(c.getChannel()).broadcastPacket(MaplePacketCreator.serverNotice(6, "Shop Meso Rate has been changed to " + rate + "x."));
        } else if (splitted[0].equals("!anego")) {//MONSTER COMMANDS
            player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9400121), player.getPosition());
        } else if (splitted[0].equals("!balrog")) {
            int[] ids = {8130100, 8150000, 9400536};
            for (int a : ids) {
                player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(a), player.getPosition());
            }
        } else if (splitted[0].equals("!bird")) {
            player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9300090), player.getPosition());
            player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9300089), player.getPosition());
        } else if (splitted[0].equals("!blackcrow")) {
            player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9400014), player.getPosition());
        } else if (splitted[0].equals("!bob")) {
            player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9400551), player.getPosition());
        } else if (splitted[0].equals("!centipede")) {
            player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9500177), player.getPosition());
        } else if (splitted[0].equals("!clone")) {
            int[] ids = {9001002, 9001000, 9001003, 9001001};
            for (int a : ids) {
                player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(a), player.getPosition());
            }
        } else if (splitted[0].equals("!coke")) {
            int[] ids = {9500144, 9500151, 9500152, 9500153, 9500154, 9500143, 9500145, 9500149, 9500147};
            for (int a : ids) {
                player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(a), player.getPosition());
            }
        } else if (splitted[0].equals("!ergoth")) {
            player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9300028), player.getPosition());
        } else if (splitted[0].equals("!franken")) {
            player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9300139), player.getPosition());
            player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9300140), player.getPosition());
        } else if (splitted[0].equals("!horseman")) {
            player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9400549), player.getPosition());
        } else if (splitted[0].equals("!leafreboss")) {
            player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(8180000), player.getPosition());
            player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(8180001), player.getPosition());
        } else if (splitted[0].equals("!loki")) {
            player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9400567), player.getPosition());
        } else if (splitted[0].equals("!ludimini")) {
            player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(8160000), player.getPosition());
            player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(8170000), player.getPosition());
        } else if (splitted[0].equals("!mushmom")) {
            int[] ids = {6130101, 6300005, 9400205};
            for (int a : ids) {
                player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(a), player.getPosition());
            }
        } else if (splitted[0].equals("!nx")) {
            for (int x = 0; x < 10; x++) {
                player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9400202), player.getPosition());
            }
        } else if (splitted[0].equals("!pap")) {
            player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(8500001), player.getPosition());
        } else if (splitted[0].equals("!papapixie")) {
            player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9300039), player.getPosition());
        } else if (splitted[0].equals("!pianus")) {
            player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(8510000), player.getPosition());
        } else if (splitted[0].equals("!pirate")) {
            int[] ids = {9300119, 9300107, 9300105, 9300106};
            for (int a : ids) {
                player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(a), player.getPosition());
            }
        } else if (splitted[0].equals("!snackbar")) {
            player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9500179), player.getPosition());
        } else if (splitted[0].equals("!theboss")) {
            player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(9400300), player.getPosition());
        } else if (splitted[0].equals("!str") || splitted[0].equals("!dex") || splitted[0].equals("!int") || splitted[0].equals("!luk")) {
            giveMainStat(cserv, splitted);
        } else if (splitted[0].equals("!ap")) {
            player.setRemainingAp(Integer.parseInt(splitted[1]));
            player.updateSingleStat(Statistic.AVAILABLEAP, player.getRemainingAp());
        } else if (splitted[0].equals("!sp")) {
            player.setRemainingSp(Integer.parseInt(splitted[1]));
            player.updateSingleStat(Statistic.AVAILABLESP, player.getRemainingSp());
        } else if (splitted[0].equals("!allocate")) {
            int up = Integer.parseInt(splitted[2]);
            if (splitted[1].equals("str")) {
                player.setStr(player.getStr() + up);
                player.updateSingleStat(Statistic.STR, player.getStr());
            } else if (splitted[1].equals("dex")) {
                player.setDex(player.getDex() + up);
                player.updateSingleStat(Statistic.DEX, player.getDex());
            } else if (splitted[1].equals("int")) {
                player.setInt(player.getInt() + up);
                player.updateSingleStat(Statistic.INT, player.getInt());
            } else if (splitted[1].equals("luk")) {
                player.setLuk(player.getLuk() + up);
                player.updateSingleStat(Statistic.LUK, player.getLuk());
            } else if (splitted[1].equals("hp")) {
                player.setMaxHp(player.getMaxHp() + up);
                player.updateSingleStat(Statistic.MAXHP, player.getMaxHp());
            } else if (splitted[1].equals("mp")) {
                player.setMaxMp(player.getMaxMp() + up);
                player.updateSingleStat(Statistic.MAXMP, player.getMaxMp());
            } else {
                mc.dropMessage(splitted[1] + " is not a valid stat.");
            }
        } else if (splitted[0].equals("!exp")) {
            int exp = Integer.parseInt(splitted[1]);
            player.setExp(exp);
            player.updateSingleStat(Statistic.EXP, exp);
        } else if (splitted[0].equals("!level")) {
            player.setLevel(Integer.parseInt(splitted[1]));
            player.gainExp(-player.getExp(), false, false);
            player.updateSingleStat(Statistic.LEVEL, player.getLevel());
        } else if (splitted[0].equals("!maxall")) {
            player.setStr(32767);
            player.setDex(32767);
            player.setInt(32767);
            player.setLuk(32767);
            player.setLevel(255);
            player.setFame(13337);
            player.setMaxHp(30000);
            player.setMaxMp(30000);
            player.updateSingleStat(Statistic.STR, 32767);
            player.updateSingleStat(Statistic.DEX, 32767);
            player.updateSingleStat(Statistic.INT, 32767);
            player.updateSingleStat(Statistic.LUK, 32767);
            player.updateSingleStat(Statistic.LEVEL, 255);
            player.updateSingleStat(Statistic.FAME, 13337);
            player.updateSingleStat(Statistic.MAXHP, 30000);
            player.updateSingleStat(Statistic.MAXMP, 30000);
        } else if (splitted[0].equals("!setall")) {
            int x = Integer.parseInt(splitted[1]);
            player.setStr(x);
            player.setDex(x);
            player.setInt(x);
            player.setLuk(x);
            player.updateSingleStat(Statistic.STR, player.getStr());
            player.updateSingleStat(Statistic.DEX, player.getStr());
            player.updateSingleStat(Statistic.INT, player.getStr());
            player.updateSingleStat(Statistic.LUK, player.getStr());
        } else {
            if (player.gmLevel() == 3) {
                mc.dropMessage("GM Command " + splitted[0] + " does not exist");
            }
            return false;
        }
        return true;
    }

    private static void giveMainStat(ChannelServer cserv, String[] splitted) {
        cserv.getPlayerStorage().getCharacterByName(splitted[1]).ifPresent(victim -> {
            int up = Integer.parseInt(splitted[2]);
            switch (splitted[0]) {
                case "!str" -> {
                    victim.setStr(up);
                    victim.updateSingleStat(Statistic.STR, victim.getStr());
                }
                case "!dex" -> {
                    victim.setDex(up);
                    victim.updateSingleStat(Statistic.DEX, victim.getDex());
                }
                case "!luk" -> {
                    victim.setLuk(up);
                    victim.updateSingleStat(Statistic.LUK, victim.getLuk());
                }
                default -> {
                    victim.setInt(up);
                    victim.updateSingleStat(Statistic.INT, victim.getInt());
                }
            }
        });
    }

    private static void warpTargetHere(ChannelServer cserv, String[] splitted, MapleCharacter player) {
        cserv.getPlayerStorage().getCharacterByName(splitted[1]).ifPresent(c -> c.changeMap(player.getMap(), player.getMap().findClosestSpawnpoint(player.getPosition())));
    }

    private static void warp(MapleClient c, MessageCallback mc, ChannelServer cserv, String[] splitted, MapleCharacter player) {
        Optional<MapleCharacter> victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
        if (victim.isPresent()) {
            if (splitted.length == 2) {
                MapleMap target = victim.get().getMap();
                player.changeMap(target, target.findClosestSpawnpoint(victim.get().getPosition()));
            } else {
                MapleMap target = ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(Integer.parseInt(splitted[2]));
                victim.get().changeMap(target, target.getPortal(0));
            }
        } else {
            try {
                victim = Optional.of(player);
                WorldLocation loc = cserv.getWorldInterface().getLocation(splitted[1]);
                if (loc != null) {
                    mc.dropMessage("You will be cross-channel warped. This may take a few seconds.");
                    MapleMap target = cserv.getMapFactory().getMap(loc.map);
                    victim.get().cancelAllBuffs();
                    String ip = cserv.getIP(loc.channel);
                    victim.get().getMap().removePlayer(victim.get());
                    victim.get().setMap(target);
                    String[] socket = ip.split(":");
                    if (victim.get().getTrade() != null) {
                        MapleTrade.cancelTrade(player);
                    }
                    victim.get().saveToDB(true);
                    if (victim.get().getCheatTracker() != null) {
                        victim.get().getCheatTracker().dispose();
                    }
                    ChannelServer.getInstance(c.getChannel()).removePlayer(player);
                    c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION);
                    try {
                        c.getSession().write(MaplePacketCreator.getChannelChange(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1])));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    MapleMap target = cserv.getMapFactory().getMap(Integer.parseInt(splitted[1]));
                    player.changeMap(target, target.getPortal(0));
                }
            } catch (Exception e) {
            }
        }
    }

    private static void unjail(ChannelServer cserv, String[] splitted) {
        MapleMap target = cserv.getMapFactory().getMap(100000000);
        cserv.getPlayerStorage().getCharacterByName(splitted[1]).ifPresent(c -> c.changeMap(target, target.getPortal(0)));
    }

    private static void slap(MessageCallback mc, ChannelServer cserv, String[] splitted, int loss) {
        cserv.getPlayerStorage().getCharacterByName(splitted[1]).ifPresent(victim -> {
            victim.setHp(victim.getHp() - loss);
            victim.updateSingleStat(Statistic.HP, victim.getHp() - loss);
            mc.dropMessage("You slapped " + victim.getName() + ".");
        });
    }

    private static void mute(MessageCallback mc, ChannelServer cserv, String[] splitted) {
        cserv.getPlayerStorage().getCharacterByName(splitted[1]).ifPresent(victim -> {
            victim.setCanTalk(victim.getCanTalk() ? 2 : 0);
            mc.dropMessage(victim.getName() + " has been muted or unmuted!");
        });
    }

    private static void giveMesoToTarget(ChannelServer cserv, String[] splitted) {
        cserv.getPlayerStorage().getCharacterByName(splitted[1]).ifPresent(c -> c.gainMeso(Integer.parseInt(splitted[2]), true));
    }

    private static void levelTarget(ChannelServer cserv, String[] splitted) {
        cserv.getPlayerStorage().getCharacterByName(splitted[1]).ifPresent(victim -> {
            victim.setLevel(Integer.parseInt(splitted[2]));
            victim.gainExp(-victim.getExp(), false, false);
            victim.updateSingleStat(Statistic.LEVEL, victim.getLevel());
        });
    }

    private static void kill(ChannelServer cserv, String[] splitted) {
        cserv.getPlayerStorage().getCharacterByName(splitted[1]).ifPresent(victim -> {
            victim.setHp(0);
            victim.updateSingleStat(Statistic.HP, 0);
        });
    }

    private static void karma(MessageCallback mc, ChannelServer cserv, String[] splitted, MapleCharacter player) {
        Optional<MapleCharacter> victim = cserv.getPlayerStorage().getCharacterByName(splitted[2]);
        if (victim.isEmpty()) {
            return;
        }

        if (splitted[1].equals("up")) {
            if (victim.get().getKarma() < 25 || player.getGMLevel() > 3) {
                victim.get().upKarma();
                mc.dropMessage("You have raised " + victim + "'s karma. It is currently at " + victim.get().getKarma() + ".");
            } else {
                mc.dropMessage("You are unable raise " + victim + "'s karma level.");
            }
        } else if (splitted[1].equals("down")) {
            if (victim.get().getKarma() > -25 || player.getGMLevel() > 3) {
                victim.get().downKarma();
                mc.dropMessage("You have dropped " + victim + "'s karma. It is currently at " + victim.get().getKarma() + ".");
            } else {
                mc.dropMessage("You cannot drop " + victim + "'s karma level anymore.");
            }
        } else {
            mc.dropMessage("Syntax: !karma [up/down] [user]");
        }
    }

    private static void changeTargetJob(ChannelServer cserv, String[] splitted) {
        cserv.getPlayerStorage().getCharacterByName(splitted[1]).ifPresent(changeTargetJob(Integer.parseInt(splitted[2])));
    }

    private static Consumer<MapleCharacter> changeTargetJob(int jobId) {
        return (target) -> MapleJob.getById(jobId).ifPresentOrElse(changeTargetJob(target), unknownJob(jobId));
    }

    private static Consumer<MapleJob> changeTargetJob(MapleCharacter target) {
        return target::changeJob;
    }

    private static Runnable unknownJob(int jobId) {
        return () -> {
        };
    }

    private static void jail(MessageCallback mc, ChannelServer cserv, String[] splitted) {
        Optional<MapleCharacter> victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
        int mapid = 200090300;
        if (splitted.length > 2 && splitted[1].equals("2")) {
            mapid = 980000404;
            victim = cserv.getPlayerStorage().getCharacterByName(splitted[2]);
        }
        if (victim.isEmpty()) {
            mc.dropMessage(splitted[1] + " not found!");
            return;
        }

        MapleMap target = cserv.getMapFactory().getMap(mapid);
        victim.get().changeMap(target, target.getPortal(0));
        mc.dropMessage(victim.get().getName() + " was jailed!");
    }

    private static void hurt(ChannelServer cserv, String[] splitted) {
        cserv.getPlayerStorage().getCharacterByName(splitted[1]).ifPresent(victim -> {
            victim.setHp(1);
            victim.updateSingleStat(Statistic.HP, 1);
        });
    }

    private static void heal(ChannelServer cserv, String[] splitted) {
        cserv.getPlayerStorage().getCharacterByName(splitted[1]).ifPresent(victim -> {
            victim.setHp(victim.getMaxHp());
            victim.updateSingleStat(Statistic.HP, victim.getMaxHp());
            victim.setMp(victim.getMaxMp());
            victim.updateSingleStat(Statistic.MP, victim.getMaxMp());
        });
    }

    private static void giftNx(MessageCallback mc, ChannelServer cserv, String[] splitted) {
        Optional<MapleCharacter> victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
        if (victim.isEmpty()) {
            return;
        }

        for (int i = 1; i < 5; i *= 2) {
            int finalI = i;
            victim.ifPresent(c -> c.modifyCSPoints(finalI, Integer.parseInt(splitted[2])));
        }
        mc.dropMessage("Done");
    }

    private static void fame(ChannelServer cserv, String[] splitted) {
        cserv.getPlayerStorage().getCharacterByName(splitted[1]).ifPresent(victim -> {
            int fame = Integer.parseInt(splitted[2]);
            victim.setFame(fame);
            victim.updateSingleStat(Statistic.FAME, fame);
        });
    }

    private static void disconnect(ChannelServer cserv, String[] splitted) {
        cserv.getPlayerStorage().getCharacterByName(splitted[1]).ifPresent(victim -> {
            victim.getClient().getSession().close();
            victim.getClient().disconnect();
            victim.saveToDB(true);
            cserv.removePlayer(victim);
        });
    }

    private static void characterInfo(MessageCallback mc, ChannelServer cserv, String[] splitted) {
        Optional<MapleCharacter> other = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
        if (other.isEmpty()) {
            return;
        }

        String builder = MapleClient.getLogMessage(other.get(), "") +
                " at " +
                other.get().getPosition().x +
                "/" +
                other.get().getPosition().y +
                "/" +
                other.get().getMap().getFootholds().findBelow(other.get().getPosition()).getId() +
                " " +
                other.get().getHp() +
                "/" +
                other.get().getCurrentMaxHp() +
                "hp " +
                other.get().getMp() +
                "/" +
                other.get().getCurrentMaxMp() +
                "mp " +
                other.get().getExp() +
                "exp" +
                " remoteAddress: " +
                other.get().getClient().getSession().getRemoteAddress();
        mc.dropMessage(builder);
        other.get().getClient().dropDebugMessage(mc);
    }

    private static void ban(MapleClient c, MessageCallback mc, String[] splitted, MapleCharacter player, ChannelServer cserv) {
        try {
            String originalReason = StringUtil.joinStringFrom(splitted, 2);
            String reason = player.getName() + " banned " + splitted[1] + ": " + originalReason;
            Optional<MapleCharacter> target = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            if (target.isPresent()) {
                if (target.get().gmLevel() < 3 || player.gmLevel() > 4) {
                    String readableTargetName = CharacterUtil.makeMapleReadable(target.get().getName());
                    String ip = target.get().getClient().getSession().getRemoteAddress().toString().split(":")[0];
                    reason += " (IP: " + ip + ")";
                    target.get().ban(reason);
                    mc.dropMessage("Banned " + readableTargetName + " ipban for " + ip + " reason: " + originalReason);
                    try {
                        ChannelServer.getInstance(c.getChannel()).getWorldInterface().broadcastMessage(player.getName(), MaplePacketCreator.serverNotice(0, readableTargetName + " has been banned for " + originalReason + ".").getBytes());
                    } catch (RemoteException e) {
                        cserv.reconnectWorld();
                    }
                } else {
                    mc.dropMessage("You may not ban GMs.");
                }
            } else {
                if (MapleCharacter.ban(splitted[1], reason, false)) {
                    mc.dropMessage("Offline Banned " + splitted[1]);
                } else {
                    mc.dropMessage("Failed to ban " + splitted[1]);
                }
            }
        } catch (NullPointerException e) {
            mc.dropMessage(splitted[1] + " could not be banned.");
        }
    }

    public static int getOptionalIntArg(String[] splitted, int position, int def) {
        if (splitted.length > position) {
            try {
                return Integer.parseInt(splitted[position]);
            } catch (NumberFormatException nfe) {
                return def;
            }
        }
        return def;
    }
}