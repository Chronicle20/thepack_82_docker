package net.sf.odinms.net.channel;

import net.sf.odinms.client.BuddyList;
import net.sf.odinms.client.BuddyList.BuddyAddResult;
import net.sf.odinms.client.BuddyList.BuddyOperation;
import net.sf.odinms.client.BuddylistEntry;
import net.sf.odinms.client.CharacterUtil;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.Pet;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.ByteArrayMaplePacket;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.net.channel.remote.ChannelWorldInterface;
import net.sf.odinms.net.world.MapleMessenger;
import net.sf.odinms.net.world.MapleMessengerCharacter;
import net.sf.odinms.net.world.MapleParty;
import net.sf.odinms.net.world.MaplePartyCharacter;
import net.sf.odinms.net.world.PartyOperation;
import net.sf.odinms.net.world.guild.MapleGuildSummary;
import net.sf.odinms.net.world.remote.CheaterData;
import net.sf.odinms.server.ShutdownServer;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.tools.CollectionUtil;
import net.sf.odinms.tools.MaplePacketCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Matze
 */
public class ChannelWorldInterfaceImpl extends UnicastRemoteObject implements ChannelWorldInterface {

    private static final long serialVersionUID = 7815256899088644192L;
    private ChannelServer server;

    public ChannelWorldInterfaceImpl() throws RemoteException {
        super(0, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory());
    }

    public ChannelWorldInterfaceImpl(ChannelServer server) throws RemoteException {
        super(0, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory());
        this.server = server;
    }

    public int getChannelId() throws RemoteException {
        return server.getChannel();
    }

    public void setChannelId(int id) throws RemoteException {
        server.setChannel(id);
    }

    public String getIP() throws RemoteException {
        return server.getIP();
    }

    public void broadcastMessage(String sender, byte[] message) throws RemoteException {
        MaplePacket packet = new ByteArrayMaplePacket(message);
        server.broadcastPacket(packet);
    }

    public void whisper(String sender, String target, int channel, String message) throws RemoteException {
        if (isConnected(target)) {
            server.getPlayerStorage().getCharacterByName(target)
                    .map(MapleCharacter::getClient)
                    .map(MapleClient::getSession)
                    .ifPresent(s -> s.write(MaplePacketCreator.getWhisper(sender, channel, message)));
        }
    }

    public boolean isConnected(String charName) throws RemoteException {
        return server.getPlayerStorage().getCharacterByName(charName).isPresent();
    }

    public void shutdown(int time) throws RemoteException {
        server.broadcastPacket(MaplePacketCreator.serverNotice(0, "The world will be shut down in " + (time / 60000) + " minutes, please log off safely"));
        TimerManager.getInstance().schedule(new ShutdownServer(server.getChannel()), time);
    }

    public int getConnected() throws RemoteException {
        return server.getConnectedClients();
    }

    @Override
    public void loggedOff(String name, int characterId, int channel, int[] buddies) throws RemoteException {
        updateBuddies(characterId, channel, buddies, true);
    }

    @Override
    public void loggedOn(String name, int characterId, int channel, int[] buddies) throws RemoteException {
        updateBuddies(characterId, channel, buddies, false);
    }

    private void updateBuddies(int buddyId, int channel, int[] buddies, boolean offline) {
        Arrays.stream(buddies)
                .mapToObj(id -> server.getPlayerStorage().getCharacterById(id))
                .flatMap(Optional::stream)
                .filter(c -> c.getBuddylist().get(buddyId).isPresent())
                .forEach(c -> updateBuddy(c, buddyId, channel, offline));
    }

    private void updateBuddy(MapleCharacter character, int buddyId, int channelId, boolean offline) {
        if (offline) {
            updateBuddyOffline(character, character.getBuddylist().get(buddyId).orElseThrow());
        } else {
            updateBuddy(character, character.getBuddylist().get(buddyId).orElseThrow(), channelId);
        }
    }

    private void updateBuddy(MapleCharacter character, BuddylistEntry entry, int channel) {
        entry.setChannelId(channel);
        character.getBuddylist().put(entry);
        character.getClient().getSession().write(MaplePacketCreator.updateBuddyChannel(entry.getCharacterId(), channel));
    }

    private void updateBuddyOffline(MapleCharacter character, BuddylistEntry entry) {
        updateBuddy(character, entry, -1);
    }

    @Override
    public void updateParty(MapleParty party, PartyOperation operation, MaplePartyCharacter target) throws RemoteException {
        for (MaplePartyCharacter partychar : party.getMembers()) {
            if (partychar.getChannel() == server.getChannel()) {
                server.getPlayerStorage().getCharacterByName(partychar.getName()).ifPresent(chr -> {
                    if (operation == PartyOperation.DISBAND) {
                        chr.setParty(null);
                    } else {
                        chr.setParty(party);
                    }
                    chr.getClient().getSession().write(MaplePacketCreator.updateParty(chr.getClient().getChannel(), party, operation, target));
                });
            }
        }
        switch (operation) {
            case LEAVE:
            case EXPEL:
                if (target.getChannel() == server.getChannel()) {
                    server.getPlayerStorage().getCharacterByName(target.getName()).ifPresent(chr -> {
                        chr.getClient().getSession().write(MaplePacketCreator.updateParty(chr.getClient().getChannel(), party, operation, target));
                        chr.setParty(null);
                    });
                }
        }
    }

    @Override
    public void partyChat(MapleParty party, String text, String nameFrom) throws RemoteException {
        party.getMembers().stream()
                .filter(p -> p.getChannel() == server.getChannel())
                .map(MaplePartyCharacter::getName)
                .filter(n -> !(n.equals(nameFrom)))
                .map(n -> server.getPlayerStorage().getCharacterByName(n))
                .flatMap(Optional::stream)
                .map(MapleCharacter::getClient)
                .map(MapleClient::getSession)
                .forEach(s -> s.write(MaplePacketCreator.multiChat(nameFrom, text, 1)));
    }

    public boolean isAvailable() throws RemoteException {
        return true;
    }

    public int getLocation(String name) throws RemoteException {
        return server.getPlayerStorage().getCharacterByName(name).map(MapleCharacter::getMapId).orElse(-1);
    }

    public List<CheaterData> getCheaters() throws RemoteException {
        List<CheaterData> cheaters = new ArrayList<>();
        List<MapleCharacter> allplayers = new ArrayList<>(server.getPlayerStorage().getAllCharacters());
        for (int x = allplayers.size() - 1; x >= 0; x--) {
            MapleCharacter cheater = allplayers.get(x);
            if (cheater.getCheatTracker().getPoints() > 0) {
                cheaters.add(new CheaterData(cheater.getCheatTracker().getPoints(), CharacterUtil.makeMapleReadable(cheater.getName()) + " (" + cheater.getCheatTracker().getPoints() + ") " + cheater.getCheatTracker().getSummary()));
            }
        }
        Collections.sort(cheaters);
        return CollectionUtil.copyFirst(cheaters, 10);
    }

    @Override
    public BuddyAddResult requestBuddyAdd(String addName, int channelFrom, int cidFrom, String nameFrom) {
        Optional<MapleCharacter> addChar = server.getPlayerStorage().getCharacterByName(addName);
        if (addChar.isEmpty()) {
            return BuddyAddResult.OK;
        }

        BuddyList buddylist = addChar.get().getBuddylist();
        if (buddylist.isFull()) {
            return BuddyAddResult.BUDDYLIST_FULL;
        }

        if (!buddylist.contains(cidFrom)) {
            buddylist.addBuddyRequest(addChar.get().getClient(), cidFrom, nameFrom, channelFrom);
            return BuddyAddResult.OK;
        }

        if (buddylist.containsVisible(cidFrom)) {
            return BuddyAddResult.ALREADY_ON_LIST;
        }
        return BuddyAddResult.OK;
    }

    @Override
    public boolean isConnected(int characterId) throws RemoteException {
        return server.getPlayerStorage().getCharacterById(characterId).isPresent();
    }

    @Override
    public void buddyChanged(int cid, int cidFrom, String name, int channel, BuddyOperation operation) {
        server.getPlayerStorage().getCharacterById(cid).ifPresent(buddyChanged(cidFrom, name, channel, operation));
    }

    private Consumer<MapleCharacter> buddyChanged(int cidFrom, String name, int channel, BuddyOperation operation) {
        return (addChar) -> {
            BuddyList buddylist = addChar.getBuddylist();
            switch (operation) {
                case ADDED:
                    if (buddylist.contains(cidFrom)) {
                        buddylist.put(new BuddylistEntry(name, cidFrom, channel, true));
                        addChar.getClient().getSession().write(MaplePacketCreator.updateBuddyChannel(cidFrom, channel - 1));
                    }
                    break;
                case DELETED:
                    if (buddylist.contains(cidFrom)) {
                        buddylist.put(new BuddylistEntry(name, cidFrom, -1, buddylist.get(cidFrom).map(BuddylistEntry::isVisible).orElse(false)));
                        addChar.getClient().getSession().write(MaplePacketCreator.updateBuddyChannel(cidFrom, -1));
                    }
                    break;
            }
        };
    }

    @Override
    public void buddyChat(int[] recipientCharacterIds, int cidFrom, String nameFrom, String chattext) throws RemoteException {
        Arrays.stream(recipientCharacterIds)
                .mapToObj(id -> server.getPlayerStorage().getCharacterById(id))
                .flatMap(Optional::stream)
                .filter(c -> c.getBuddylist().containsVisible(cidFrom))
                .forEach(c -> c.getClient().getSession().write(MaplePacketCreator.multiChat(nameFrom, chattext, 0)));
    }

    @Override
    public int[] multiBuddyFind(int charIdFrom, int[] characterIds) throws RemoteException {
        List<Integer> ret = Arrays.stream(characterIds)
                .mapToObj(id -> server.getPlayerStorage().getCharacterById(id))
                .flatMap(Optional::stream)
                .filter(c -> c.getBuddylist().containsVisible(charIdFrom))
                .map(MapleCharacter::getId)
                .toList();
        int[] retArr = new int[ret.size()];
        int pos = 0;
        for (Integer i : ret) {
            retArr[pos++] = i;
        }
        return retArr;
    }

    @Override
    public void sendPacket(List<Integer> targetIds, MaplePacket packet, int exception) throws RemoteException {
        targetIds.stream()
                .filter(i -> i != exception)
                .map(i -> server.getPlayerStorage().getCharacterById(i))
                .flatMap(Optional::stream)
                .forEach(c -> c.getClient().getSession().write(packet));
    }

    @Override
    public void setGuildAndRank(List<Integer> cids, int guildid, int rank, int exception) throws RemoteException {
        for (int cid : cids) {
            if (cid != exception) {
                setGuildAndRank(cid, guildid, rank);
            }
        }
    }

    @Override
    public void setGuildAndRank(int cid, int guildId, int rank) throws RemoteException {
        server.getPlayerStorage().getCharacterById(cid).ifPresent(c -> setGuildAndRank(c, guildId, rank));
    }

    private void setGuildAndRank(MapleCharacter mc, int guildId, int rank) {
        boolean bDifferentGuild;
        if (guildId == -1 && rank == -1) //just need a respawn
        {
            bDifferentGuild = true;
        } else {
            bDifferentGuild = guildId != mc.getGuildId();
            mc.setGuildId(guildId);
            mc.setGuildRank(rank);
            mc.saveGuildStatus();
        }
        if (bDifferentGuild) {
            mc.getMap().broadcastMessage(mc,
                    MaplePacketCreator.removePlayerFromMap(mc.getId()), false);
            mc.getMap().broadcastMessage(mc,
                    MaplePacketCreator.spawnPlayerMapobject(mc), false);
            Pet[] pets = mc.getPets();
            for (int i = 0; i < 3; i++) {
                if (pets[i] != null) {
                    mc.getMap().broadcastMessage(mc, MaplePacketCreator.showPet(mc, pets[i], false, false), false);
                }
            }
        }
    }

    @Override
    public void setOfflineGuildStatus(int guildid, byte guildrank, int cid) throws RemoteException {
        Logger log = LoggerFactory.getLogger(this.getClass());
        try {
            java.sql.Connection con = DatabaseConnection.getConnection();
            java.sql.PreparedStatement ps = con.prepareStatement("UPDATE characters SET guildid = ?, guildrank = ? WHERE id = ?");
            ps.setInt(1, guildid);
            ps.setInt(2, guildrank);
            ps.setInt(3, cid);
            ps.execute();
            ps.close();
        } catch (SQLException se) {
            log.error("SQLException: " + se.getLocalizedMessage(), se);
        }
    }

    @Override
    public void reloadGuildCharacters() throws RemoteException {
        server.getPlayerStorage().getAllCharacters().stream()
                .filter(MapleCharacter::hasGuild)
                .forEach(reloadGuildCharacter());
        ChannelServer.getInstance(this.getChannelId()).reloadGuildSummary();
    }

    private Consumer<? super MapleCharacter> reloadGuildCharacter() {
        return (target) -> {
            try {
                server.getWorldInterface().setGuildMemberOnline(target.getMGC().orElseThrow(), true, server.getChannel());
                server.getWorldInterface().memberLevelJobUpdate(target.getMGC().orElseThrow());
            } catch (RemoteException e) {
                //TODO
            }
        };
    }

    @Override
    public void changeEmblem(int gid, List<Integer> affectedPlayers, MapleGuildSummary mgs) throws RemoteException {
        ChannelServer.getInstance(this.getChannelId()).updateGuildSummary(gid, mgs);
        this.sendPacket(affectedPlayers, MaplePacketCreator.guildEmblemChange(gid, mgs.logoBG(), mgs.logoBGColor(), mgs.logo(), mgs.logoColor()), -1);
        this.setGuildAndRank(affectedPlayers, -1, -1, -1);    //respawn player
    }

    public void messengerInvite(String senderName, int messengerId, String targetName, int fromChannel) throws RemoteException {
        if (isConnected(targetName)) {
            server.getPlayerStorage().getCharacterByName(targetName).ifPresent(messengerInvite(senderName, messengerId, fromChannel));
        }
    }

    private Consumer<MapleCharacter> messengerInvite(String senderName, int messengerId, int fromChannel) {
        return (target) -> ChannelServer.getInstance(fromChannel).getPlayerStorage().getCharacterByName(senderName).ifPresent(sender -> messengerInvite(messengerId).apply(target));

    }

    private Function<MapleCharacter, Consumer<MapleCharacter>> messengerInvite(int messengerId) {
        return (sender) -> (target) -> {
            MapleMessenger messenger = target.getMessenger();
            if (messenger != null) {
                sender.getClient().getSession().write(MaplePacketCreator.messengerChat(sender + " : " + target.getName() + " is already using Maple Messenger"));
                return;
            }

            target.getClient().getSession().write(MaplePacketCreator.messengerInvite(sender.getName(), messengerId));
            sender.getClient().getSession().write(MaplePacketCreator.messengerNote(target.getName(), 4, 1));
        };
    }

    public void addMessengerPlayer(MapleMessenger messenger, String namefrom, int fromchannel, int position) throws RemoteException {
        Optional<MapleCharacter> from = ChannelServer.getInstance(fromchannel).getPlayerStorage().getCharacterByName(namefrom);
        if (from.isEmpty()) {
            return;
        }
        messenger.getMembers().stream()
                .filter(m -> m.getChannel() == server.getChannel())
                .forEach(addMessengerPlayer(from.get(), position));
    }

    private Consumer<MapleMessengerCharacter> addMessengerPlayer(MapleCharacter from, int position) {
        return (existing) -> {
            Consumer<MapleCharacter> joinFunc = existing.getName().equals(from.getName()) ? joinMessenger(existing.getPosition()) : joinMessenger(from, position);
            server.getPlayerStorage().getCharacterByName(existing.getName()).ifPresent(joinFunc);
        };
    }

    private Consumer<MapleCharacter> joinMessenger(MapleCharacter from, int position) {
        return (existing) -> {
            existing.getClient().getSession().write(MaplePacketCreator.addMessengerPlayer(from.getName(), from, position, from.getClient().getChannel() - 1));
            from.getClient().getSession().write(MaplePacketCreator.addMessengerPlayer(existing.getName(), existing, existing.getMessengerPosition(), existing.getClient().getChannel() - 1));
        };
    }

    private Consumer<MapleCharacter> joinMessenger(int position) {
        return (target) -> target.getClient().getSession().write(MaplePacketCreator.joinMessenger(position));
    }

    public void removeMessengerPlayer(MapleMessenger messenger, int position) throws RemoteException {
        messenger.getMembers().stream()
                .filter(c -> c.getChannel() == server.getChannel())
                .map(MapleMessengerCharacter::getName)
                .map(n -> server.getPlayerStorage().getCharacterByName(n))
                .flatMap(Optional::stream)
                .forEach(removeMessengerPlayer(position));
    }

    private Consumer<MapleCharacter> removeMessengerPlayer(int position) {
        return (target) -> target.getClient().getSession().write(MaplePacketCreator.removeMessengerPlayer(position));
    }

    public void messengerChat(MapleMessenger messenger, String text, String nameFrom) throws RemoteException {
        messenger.getMembers().stream()
                .filter(c -> c.getChannel() == server.getChannel())
                .map(MapleMessengerCharacter::getName)
                .filter(n -> !(n.equals(nameFrom)))
                .map(n -> server.getPlayerStorage().getCharacterByName(n))
                .flatMap(Optional::stream)
                .forEach(sendMessage(text));
    }

    private Consumer<MapleCharacter> sendMessage(String text) {
        return (target) -> target.getClient().getSession().write(MaplePacketCreator.messengerChat(text));
    }

    public void declineChat(String target, String nameFrom) throws RemoteException {
        if (isConnected(target)) {
            server.getPlayerStorage().getCharacterByName(target).ifPresent(declineChat(nameFrom));
        }
    }

    private Consumer<MapleCharacter> declineChat(String nameFrom) {
        return (target) -> {
            if (target.getMessenger() != null) {
                target.getClient().getSession().write(MaplePacketCreator.messengerNote(nameFrom, 5, 0));
            }
        };
    }

    public void updateMessenger(MapleMessenger messenger, String nameFrom, int position, int fromChannel) throws RemoteException {
        ChannelServer.getInstance(fromChannel).getPlayerStorage().getCharacterByName(nameFrom).ifPresent(updateMessenger(messenger, position));

    }

    private Consumer<MapleCharacter> updateMessenger(MapleMessenger messenger, int position) {
        return (from) -> messenger.getMembers().stream()
                .filter(m -> m.getChannel() == server.getChannel())
                .map(MapleMessengerCharacter::getName)
                .filter(n -> !n.equals(from.getName()))
                .map(n -> server.getPlayerStorage().getCharacterByName(n))
                .flatMap(Optional::stream)
                .map(MapleCharacter::getClient)
                .map(MapleClient::getSession)
                .forEach(s -> s.write(MaplePacketCreator.updateMessengerPlayer(from.getName(), from, position, from.getClient().getChannel() - 1)));
    }

    public void sendSpouseChat(String sender, String target, String message) throws RemoteException {
        if (isConnected(target)) {
            server.getPlayerStorage().getCharacterByName(target).ifPresent(sendSpouseChat(sender, message));
        }
    }

    private Consumer<MapleCharacter> sendSpouseChat(String sender, String message) {
        return (target) -> server.getPlayerStorage().getCharacterByName(sender).ifPresent(sendSpouseChat(message).apply(target));
    }

    private Function<MapleCharacter, Consumer<MapleCharacter>> sendSpouseChat(String message) {
        return (target) -> (sender) -> target.getClient().getSession().write(MaplePacketCreator.sendSpouseChat(sender, message));
    }
}
