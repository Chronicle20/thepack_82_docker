package net.sf.odinms.client.messages;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.Ring;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.server.life.MapleLifeFactory;
import net.sf.odinms.server.life.MapleNPC;
import net.sf.odinms.server.maps.AbstractMapleMapObject;
import net.sf.odinms.server.maps.MapleMapObjectType;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.StringUtil;

import java.util.List;
import java.util.Optional;

public class SuperCommand {

    @SuppressWarnings("unchecked")
    public static boolean executeSuperCommand(MapleClient c, MessageCallback mc, String line) {
        MapleCharacter player = c.getPlayer();
        ChannelServer cserv = c.getChannelServer();
        String[] splitted = line.split(" ");
        if (splitted[0].equals("!checkkarma")) {
            checkKarma(mc, cserv, splitted);
        } else if (splitted[0].equals("!dcall")) {
            dcAll(cserv, player);
        } else if (splitted[0].equals("!givedonatorpoint")) {
            giveDonatorPoint(mc, cserv, splitted);
        } else if (splitted[0].equals("!horntail")) {
            spawnHorntail(player);
        } else if (splitted[0].equals("!npc")) {
            spawnNpc(c, mc, splitted);
        } else if (splitted[0].equals("!removenpcs")) {
            removeNpcs(c);
        } else if (splitted[0].equals("!ringme")) {
            ringMe(c, mc, splitted);
        } else if (splitted[0].equals("!speak")) {
            speak(c, mc, cserv, splitted);
        } else if (splitted[0].equals("!unban")) {
            unban(mc, splitted);
        } else if (splitted[0].equals("!zakum")) {
            zakum(player);
        } else {
            if (c.getPlayer().gmLevel() == 4) {
                mc.dropMessage("SuperGM Command " + splitted[0] + " does not exist");
            }
            return false;
        }
        return true;
    }

    private static void zakum(MapleCharacter player) {
        player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(8800000), player.getPosition());
        for (int x = 8800003; x <= 8800010; x++) {
            player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(x), player.getPosition());
        }
    }

    private static void unban(MessageCallback mc, String[] splitted) {
        if (splitted.length != 2) {
            mc.dropMessage("Syntax: !unban [userid]");
            return;
        }

        MapleCharacter.unban(splitted[1], false);
        MapleCharacter.unbanIP(splitted[1]);
        mc.dropMessage("Unbanned " + splitted[1]);
    }

    private static void speak(MapleClient c, MessageCallback mc, ChannelServer cserv, String[] splitted) {
        if (splitted.length != 3) {
            mc.dropMessage("Syntax: !speak [user] [message]");
            return;
        }

        cserv.getPlayerStorage().getCharacterByName(splitted[1])
                .ifPresent(victim -> victim.getMap().broadcastMessage(MaplePacketCreator.getChatText(victim.getId(), StringUtil.joinStringFrom(splitted, 2), victim.isGM() && c.getChannelServer().allowGmWhiteText(), 0)));
    }

    private static void ringMe(MapleClient c, MessageCallback mc, String[] splitted) {
        if (splitted.length != 3) {
            mc.dropMessage("Syntax: !ringme [ringid] [user]");
            return;
        }

        int itemId = Integer.parseInt(splitted[1]);
        if (itemId < 111200 || itemId > 1120000 || (itemId > 1112006 && itemId < 1112800) || itemId == 1112808) {
            mc.dropMessage("Invalid itemID.");
            return;
        }

        int[] ret = Ring.createRing(c, itemId, c.getPlayer().getId(), c.getPlayer().getName(), MapleCharacter.getIdByName(splitted[2], c.getPlayer().getWorld()), splitted[2]);
        if (ret[0] == -1 || ret[1] == -1) {
            mc.dropMessage("Make sure the person you are attempting to create a ring with is online.");
        }
    }

    private static void removeNpcs(MapleClient c) {
        c.getPlayer().getMap().getMapObjectsInRange(c.getPlayer().getPosition(), Double.POSITIVE_INFINITY, List.of(MapleMapObjectType.NPC)).stream()
                .map(mo -> (MapleNPC) mo)
                .filter(MapleNPC::isCustom)
                .map(AbstractMapleMapObject::getObjectId)
                .forEach(id -> c.getPlayer().getMap().removeMapObject(id));
    }

    private static void spawnNpc(MapleClient c, MessageCallback mc, String[] splitted) {
        MapleCharacter player = c.getPlayer();
        int npcId = Integer.parseInt(splitted[1]);
        MapleNPC npc = MapleLifeFactory.getNPC(npcId);
        if (npc.getName().equals("MISSINGNO")) {
            mc.dropMessage("You have entered an invalid Npc-Id");
            return;
        }

        npc.setPosition(player.getPosition());
        npc.setCy(player.getPosition().y);
        npc.setRx0(player.getPosition().x + 50);
        npc.setRx1(player.getPosition().x - 50);
        npc.setFh(player.getMap().getFootholds().findBelow(c.getPlayer().getPosition()).getId());
        npc.setCustom(true);
        player.getMap().addMapObject(npc);
        player.getMap().broadcastMessage(MaplePacketCreator.spawnNPC(npc));
    }

    private static void spawnHorntail(MapleCharacter player) {
        for (int i = 8810002; i < 8810010; i++) {
            player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(i), player.getPosition());
        }
    }

    private static void giveDonatorPoint(MessageCallback mc, ChannelServer cserv, String[] splitted) {
        if (splitted.length != 3) {
            mc.dropMessage("Syntax: !givedonatorpoint [user] [num]");
            return;
        }

        cserv.getPlayerStorage().getCharacterByName(splitted[1]).ifPresent(target -> {
            target.gainDonatorPoints(Integer.parseInt(splitted[2]));
            mc.dropMessage(String.format("You have given %s %s donator points.", splitted[1], splitted[2]));
        });
    }

    private static void dcAll(ChannelServer cserv, MapleCharacter player) {
        for (MapleCharacter everyone : cserv.getPlayerStorage().getAllCharacters()) {
            if (everyone != player) {
                everyone.getClient().getSession().close();

            }
            everyone.saveToDB(true);
            cserv.removePlayer(everyone);
        }
    }

    private static void checkKarma(MessageCallback mc, ChannelServer cserv, String[] splitted) {
        if (splitted.length <= 1) {
            mc.dropMessage("Syntax: !checkkarma [user]");
            return;
        }

        Optional<MapleCharacter> victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
        if (victim.isEmpty()) {
            mc.dropMessage(String.format("User [%s] cannot be found.", splitted[1]));
            return;
        }

        if (splitted.length == 2) {
            mc.dropMessage(victim + "'s Karma level is at: " + victim.get().getKarma());
            if (victim.get().getKarma() <= -50) {
                mc.dropMessage("You may want to ban him/her for low karma.");
                return;
            } else if (victim.get().getKarma() >= 50) {
                mc.dropMessage("You may want to set him/her as an intern for high karma.");
                return;
            }
            return;
        }

        if (splitted[2].equals("ban")) {
            if (victim.get().getKarma() < -49) {
                victim.get().ban("Low Karma: " + victim.get().getKarma());
                return;
            }

            mc.dropMessage("Too much karma");
            return;
        }

        if (splitted[2].equals("intern")) {
            if (victim.get().getKarma() <= 49) {
                mc.dropMessage("Not enough karma");
                return;
            }

            victim.get().setGMLevel(2);
            mc.dropMessage("You have set " + victim + " as an intern.");
            return;
        }

        mc.dropMessage("Syntax: !checkkarma [user] [ban/intern]");
    }
}