package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.world.MapleParty;
import net.sf.odinms.net.world.MaplePartyCharacter;
import net.sf.odinms.net.world.PartyOperation;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.rmi.RemoteException;
import java.util.Optional;

public class PartyOperationHandler extends AbstractMaplePacketHandler {

    private static void promotePartyLeader(MapleClient c, int newLeaderId) {
        MaplePartyCharacter newLeader = c.getPlayer().getParty().getMemberById(newLeaderId);
        try {
            ChannelServer.getInstance(c.getChannel()).getWorldInterface().updateParty(c.getPlayer().getParty().getId(), PartyOperation.CHANGE_LEADER, newLeader);
        } catch (RemoteException f) {
            c.getChannelServer().reconnectWorld();
        }
    }

    private static void expelFromParty(MapleClient c, int cid) {
        if (c.getPlayer().getId() != c.getPlayer().getParty().getLeaderId()) {
            return;
        }

        MaplePartyCharacter expelled = c.getPlayer().getParty().getMemberById(cid);
        if (expelled == null) {
            return;
        }

        try {
            ChannelServer.getInstance(c.getChannel()).getWorldInterface().updateParty(c.getPlayer().getParty().getId(), PartyOperation.EXPEL, expelled);
            if (c.getPlayer().getEventInstance() != null) {
                /*if leader wants to boot someone, then the whole party gets expelled
                TODO: Find an easier way to get the character behind a MaplePartyCharacter
                possibly remove just the expellee.*/
                if (expelled.isOnline()) {
                    c.getPlayer().getEventInstance().disbandParty();
                }
            }

        } catch (RemoteException e) {
            c.getChannelServer().reconnectWorld();
        }
    }

    private static void sendPartyInvite(MapleClient c, String name) {
        Optional<MapleCharacter> invited = c.getChannelServer().getPlayerStorage().getCharacterByName(name);
        if (invited.isEmpty()) {
            c.getSession().write(MaplePacketCreator.partyStatusMessage(18));
            return;
        }

        if (invited.get().getParty() != null) {
            c.getSession().write(MaplePacketCreator.partyStatusMessage(16));
            return;
        }

        if (c.getPlayer().getParty().getMembers().size() < 6) {
            invited.get().getClient().getSession().write(MaplePacketCreator.partyInvite(c.getPlayer()));
        }
    }

    private static void acceptPartyInvite(MapleClient c, int partyId) {
        if (c.getPlayer().getParty() != null) {
            c.getSession().write(MaplePacketCreator.serverNotice(5, "You can't join the party as you are already in one"));
            return;
        }

        try {
            MapleParty party = ChannelServer.getInstance(c.getChannel()).getWorldInterface().getParty(partyId);
            if (party != null) {
                if (party.getMembers().size() < 6) {
                    ChannelServer.getInstance(c.getChannel()).getWorldInterface().updateParty(party.getId(), PartyOperation.JOIN, new MaplePartyCharacter(c.getPlayer()));
                    c.getPlayer().receivePartyMemberHP();
                    c.getPlayer().updatePartyMemberHP();
                } else {
                    c.getSession().write(MaplePacketCreator.partyStatusMessage(17));
                }
            } else {
                c.getSession().write(MaplePacketCreator.serverNotice(5, "The party you are trying to join does not exist"));
            }
        } catch (RemoteException e) {
            c.getChannelServer().reconnectWorld();
        }
    }

    private static void handleLeavingParty(MapleClient c) {
        if (c.getPlayer().getParty() == null) {
            return;
        }

        try {
            if (c.getPlayer().getId() == c.getPlayer().getParty().getLeaderId()) {
                ChannelServer.getInstance(c.getChannel()).getWorldInterface().updateParty(c.getPlayer().getParty().getId(), PartyOperation.DISBAND, new MaplePartyCharacter(c.getPlayer()));
                if (c.getPlayer().getEventInstance() != null) {
                    c.getPlayer().getEventInstance().disbandParty();
                }
            } else {
                ChannelServer.getInstance(c.getChannel()).getWorldInterface().updateParty(c.getPlayer().getParty().getId(), PartyOperation.LEAVE, new MaplePartyCharacter(c.getPlayer()));
                if (c.getPlayer().getEventInstance() != null) {
                    c.getPlayer().getEventInstance().leftParty(c.getPlayer());
                }
            }
        } catch (RemoteException e) {
            c.getChannelServer().reconnectWorld();
        }
        c.getPlayer().setParty(null);
    }

    private static void handleCreateParty(MapleClient c) {
        if (c.getPlayer().getParty() != null) {
            c.getSession().write(MaplePacketCreator.serverNotice(5, "You can't create a party as you are already in one"));
            return;
        }

        try {
            c.getPlayer().setParty(ChannelServer.getInstance(c.getChannel()).getWorldInterface().createParty(new MaplePartyCharacter(c.getPlayer())));
        } catch (RemoteException e) {
            c.getChannelServer().reconnectWorld();
        }
        c.getSession().write(MaplePacketCreator.partyCreated());
    }

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int operation = slea.readByte();
        switch (operation) {
            case 1: {
                handleCreateParty(c);
                break;
            }
            case 2: {
                handleLeavingParty(c);
                break;
            }
            case 3: {
                int partyid = slea.readInt();
                acceptPartyInvite(c, partyid);
                break;
            }
            case 4: {
                String name = slea.readMapleAsciiString();
                sendPartyInvite(c, name);
                break;
            }
            case 5: {
                int cid = slea.readInt();
                expelFromParty(c, cid);
                break;
            }
            case 6: {
                int newLeader = slea.readInt();
                promotePartyLeader(c, newLeader);
                break;
            }
        }
    }
}
