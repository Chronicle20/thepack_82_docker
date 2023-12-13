package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.BuddyList;
import net.sf.odinms.client.BuddyList.BuddyAddResult;
import net.sf.odinms.client.BuddyList.BuddyOperation;
import net.sf.odinms.client.BuddylistEntry;
import net.sf.odinms.client.CharacterIdNameBuddyCapacity;
import net.sf.odinms.client.CharacterNameAndId;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.channel.remote.ChannelWorldInterface;
import net.sf.odinms.net.world.remote.WorldChannelInterface;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static net.sf.odinms.client.BuddyList.BuddyOperation.ADDED;
import static net.sf.odinms.client.BuddyList.BuddyOperation.DELETED;

public class BuddylistModifyHandler extends AbstractMaplePacketHandler {

    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BuddylistModifyHandler.class);

    private void nextPendingRequest(MapleClient c) {
        CharacterNameAndId pendingBuddyRequest = c.getPlayer().getBuddylist().pollPendingRequest();
        if (pendingBuddyRequest != null) {
            c.getSession().write(MaplePacketCreator.requestBuddylistAdd(pendingBuddyRequest.id(), pendingBuddyRequest.name()));
        }
    }

    private CharacterIdNameBuddyCapacity getCharacterIdAndNameFromDatabase(String name) throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = con.prepareStatement("SELECT id, name, buddyCapacity FROM characters WHERE name LIKE ?");
        ps.setString(1, name);
        ResultSet rs = ps.executeQuery();
        CharacterIdNameBuddyCapacity ret = null;
        if (rs.next()) {
            ret = new CharacterIdNameBuddyCapacity(rs.getInt("id"), rs.getString("name"), rs.getInt("buddyCapacity"));
        }
        rs.close();
        ps.close();
        return ret;
    }

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int mode = slea.readByte();
        MapleCharacter player = c.getPlayer();
        WorldChannelInterface worldInterface = c.getChannelServer().getWorldInterface();
        BuddyList buddylist = player.getBuddylist();
        if (mode == 1) { // add
            String nameToAdd = slea.readMapleAsciiString();
            addBuddy(c, nameToAdd);
        } else if (mode == 2) { // accept buddy
            int otherCid = slea.readInt();
            if (!buddylist.isFull()) {
                try {
                    int channel = worldInterface.find(otherCid);
                    String otherName = c.getChannelServer().getPlayerStorage().getCharacterById(otherCid).map(MapleCharacter::getName).orElseGet(lookupName(otherCid));
                    if (otherName != null) {
                        buddylist.put(new BuddylistEntry(otherName, otherCid, channel, true));
                        c.getSession().write(MaplePacketCreator.updateBuddylist(buddylist.getBuddies()));
                        notifyRemoteChannel(c, channel, otherCid, ADDED);
                    }
                } catch (RemoteException e) {
                    log.error("REMOTE THROW", e);
                }
            }
            nextPendingRequest(c);
        } else if (mode == 3) { // delete
            int otherCid = slea.readInt();
            if (buddylist.containsVisible(otherCid)) {
                try {
                    notifyRemoteChannel(c, worldInterface.find(otherCid), otherCid, DELETED);
                } catch (RemoteException e) {
                    log.error("REMOTE THROW", e);
                }
            }
            buddylist.remove(otherCid);
            c.getSession().write(MaplePacketCreator.updateBuddylist(player.getBuddylist().getBuddies()));
            nextPendingRequest(c);
        }
    }

    private Supplier<String> lookupName(int otherCid) {
        return () -> {
            String otherName = null;
            try {
                PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT name FROM characters WHERE id = ?");
                ps.setInt(1, otherCid);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    otherName = rs.getString("name");
                }
                rs.close();
                ps.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return otherName;
        };
    }

    private void addBuddy(MapleClient client, String nameToAdd) {
        Optional<BuddylistEntry> ble = client.getPlayer().getBuddylist().get(nameToAdd);
        if (ble.isPresent() && !ble.get().isVisible()) {
            client.getSession().write(MaplePacketCreator.buddylistMessage((byte) 13));
            return;
        }

        if (client.getPlayer().getBuddylist().isFull()) {
            client.getSession().write(MaplePacketCreator.buddylistMessage((byte) 11));
            return;
        }


        Optional<BuddyInformation> buddyInformation = client.getChannelServer().getPlayerStorage().getCharacterByName(nameToAdd)
                .map(localBuddyInformation())
                .or(remoteBuddyInformation(client.getChannelServer().getWorldInterface(), nameToAdd));

        if (buddyInformation.isEmpty()) {
            client.getSession().write(MaplePacketCreator.buddylistMessage((byte) 15));
            return;
        }
        buddyInformation.ifPresent(attemptAddBuddy(client));
    }

    private Consumer<? super BuddyInformation> attemptAddBuddy(MapleClient client) {
        return buddyInformation -> {
            BuddyAddResult buddyAddResult = null;
            if (buddyInformation.channel() != -1) {
                try {
                    buddyAddResult = client.getChannelServer().getWorldInterface().getChannelInterface(buddyInformation.channel()).requestBuddyAdd(buddyInformation.name(), client.getChannel(), client.getPlayer().getId(), client.getPlayer().getName());
                } catch (RemoteException e) {
                    log.error("Unable to attempt adding buddy due to a remote exception.", e);
                    return;
                }
            } else {
                try {
                    Connection con = DatabaseConnection.getConnection();
                    PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) as buddyCount FROM buddies WHERE characterid = ? AND pending = 0");
                    ps.setInt(1, buddyInformation.id());
                    ResultSet rs = ps.executeQuery();
                    if (!rs.next()) {
                        throw new RuntimeException("Result set expected");
                    } else {
                        int count = rs.getInt("buddyCount");
                        if (count >= buddyInformation.buddyCapacity()) {
                            buddyAddResult = BuddyAddResult.BUDDYLIST_FULL;
                        }
                    }
                    rs.close();
                    ps.close();
                    ps = con.prepareStatement("SELECT pending FROM buddies WHERE characterid = ? AND buddyid = ?");
                    ps.setInt(1, buddyInformation.id());
                    ps.setInt(2, client.getPlayer().getId());
                    rs = ps.executeQuery();
                    if (rs.next()) {
                        buddyAddResult = BuddyAddResult.ALREADY_ON_LIST;
                    }
                    rs.close();
                    ps.close();
                } catch (SQLException e) {
                    log.error("Unable to attempt adding buddy due to a sql exception.", e);
                    return;
                }
            }

            if (buddyAddResult == BuddyAddResult.BUDDYLIST_FULL) {
                client.getSession().write(MaplePacketCreator.buddylistMessage((byte) 12));
                return;
            }

            int displayChannel = -1;
            int otherCid = buddyInformation.id();
            if (buddyAddResult == BuddyAddResult.ALREADY_ON_LIST && buddyInformation.channel() != -1) {
                displayChannel = buddyInformation.channel();
                try {
                    notifyRemoteChannel(client, buddyInformation.channel(), otherCid, ADDED);
                } catch (RemoteException e) {
                    log.error("Unable to notify remote channel that the buddy has been added due to an exception.", e);
                }
            } else if (buddyAddResult != BuddyAddResult.ALREADY_ON_LIST && buddyInformation.channel() == -1) {
                try {
                    Connection con = DatabaseConnection.getConnection();
                    PreparedStatement ps = con.prepareStatement("INSERT INTO buddies (characterid, `buddyid`, `pending`) VALUES (?, ?, 1)");
                    ps.setInt(1, buddyInformation.id());
                    ps.setInt(2, client.getPlayer().getId());
                    ps.executeUpdate();
                    ps.close();
                } catch (SQLException e) {
                    log.error("Unable to persist buddy request.", e);
                    return;
                }
            }
            client.getPlayer().getBuddylist().put(new BuddylistEntry(buddyInformation.name(), otherCid, displayChannel, true));
            client.getSession().write(MaplePacketCreator.updateBuddylist(client.getPlayer().getBuddylist().getBuddies()));
        };
    }

    private Supplier<Optional<? extends BuddyInformation>> remoteBuddyInformation(WorldChannelInterface worldInterface, String nameToAdd) {
        return () -> {
            try {
                int channel = worldInterface.find(nameToAdd);
                CharacterIdNameBuddyCapacity charWithId = getCharacterIdAndNameFromDatabase(nameToAdd);
                return Optional.of(new BuddyInformation(charWithId.id(), charWithId.name(), charWithId.buddyCapacity(), channel));
            } catch (RemoteException | SQLException e) {
                return Optional.empty();
            }
        };

    }

    private Function<? super MapleCharacter, BuddyInformation> localBuddyInformation() {
        return (target) -> new BuddyInformation(target.getId(), target.getName(), target.getBuddyCapacity(), target.getClient().getChannel());
    }

    private void notifyRemoteChannel(MapleClient c, int remoteChannel, int otherCid, BuddyOperation operation)
            throws RemoteException {
        WorldChannelInterface worldInterface = c.getChannelServer().getWorldInterface();
        MapleCharacter player = c.getPlayer();

        if (remoteChannel != -1) {
            ChannelWorldInterface channelInterface = worldInterface.getChannelInterface(remoteChannel);
            channelInterface.buddyChanged(otherCid, player.getId(), player.getName(), c.getChannel(), operation);
        }
    }

    private record BuddyInformation(int id, String name, int buddyCapacity, int channel) {
    }
}
