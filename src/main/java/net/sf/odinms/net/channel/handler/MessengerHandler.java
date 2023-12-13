package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.world.MapleMessenger;
import net.sf.odinms.net.world.MapleMessengerCharacter;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.rmi.RemoteException;
import java.util.function.Consumer;

public class MessengerHandler extends AbstractMaplePacketHandler {

    private static void declineMessenger(MapleClient c, String targeted) {
        c.getChannelServer().getPlayerStorage().getCharacterByName(targeted).ifPresentOrElse(declineMessenger(c), () -> declineMessengerOtherChannel(c, targeted));
    }

    private static void declineMessengerOtherChannel(MapleClient c, String targeted) {
        try {
            ChannelServer.getInstance(c.getChannel()).getWorldInterface().declineChat(targeted, c.getPlayer().getName());
        } catch (RemoteException e) {
            c.getChannelServer().reconnectWorld();
        }
    }

    private static Consumer<MapleCharacter> declineMessenger(MapleClient c) {
        return (target) -> {
            if (target.getMessenger() != null) {
                target.getClient().getSession().write(MaplePacketCreator.messengerNote(c.getPlayer().getName(), 5, 0));
            }
        };
    }

    private static void leaveMessenger(MapleClient c) {
        if (c.getPlayer().getMessenger() != null) {
            try {
                ChannelServer.getInstance(c.getChannel()).getWorldInterface().leaveMessenger(c.getPlayer().getMessenger().getId(), new MapleMessengerCharacter(c.getPlayer()));
            } catch (RemoteException e) {
                c.getChannelServer().reconnectWorld();
            }
            c.getPlayer().setMessenger(null);
            c.getPlayer().setMessengerPosition(4);
        }
    }

    private static void inviteToMessenger(MapleClient c, String input) {
        if (c.getPlayer().getMessenger().getMembers().size() >= 3) {
            c.getSession().write(MaplePacketCreator.messengerChat(c.getPlayer().getName() + " : You cannot have more than 3 people in the Maple Messenger"));
            return;
        }

        c.getChannelServer().getPlayerStorage().getCharacterByName(input).ifPresentOrElse(inviteToMessenger(c), () -> inviteToMessengerOtherChannel(c, input));
    }

    private static void inviteToMessengerOtherChannel(MapleClient c, String input) {
        try {
            if (ChannelServer.getInstance(c.getChannel()).getWorldInterface().isConnected(input)) {
                ChannelServer.getInstance(c.getChannel()).getWorldInterface().messengerInvite(c.getPlayer().getName(), c.getPlayer().getMessenger().getId(), input, c.getChannel());
            } else {
                c.getSession().write(MaplePacketCreator.messengerNote(input, 4, 0));
            }
        } catch (RemoteException e) {
            c.getChannelServer().reconnectWorld();
        }
    }

    private static Consumer<MapleCharacter> inviteToMessenger(MapleClient c) {
        return (target) -> {
            if (target.getMessenger() != null) {
                c.getSession().write(MaplePacketCreator.messengerChat(c.getPlayer().getName() + " : " + target.getName() + " is already using Maple Messenger"));
                return;
            }

            target.getClient().getSession().write(MaplePacketCreator.messengerInvite(c.getPlayer().getName(), c.getPlayer().getMessenger().getId()));
            c.getSession().write(MaplePacketCreator.messengerNote(target.getName(), 4, 1));
        };
    }

    private static void answerInvite(MapleClient c, int messengerId) {
        MapleCharacter player = c.getPlayer();
        MapleMessenger messenger = player.getMessenger();
        if (messenger == null) {
            if (messengerId == 0) {
                try {
                    MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(player);
                    messenger = ChannelServer.getInstance(c.getChannel()).getWorldInterface().createMessenger(messengerplayer);
                    player.setMessenger(messenger);
                    player.setMessengerPosition(0);
                } catch (RemoteException e) {
                    c.getChannelServer().reconnectWorld();
                }
                return;
            }

            try {
                messenger = ChannelServer.getInstance(c.getChannel()).getWorldInterface().getMessenger(messengerId);
                int position = messenger.getLowestPosition();
                MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(player, position);
                if (messenger != null) {
                    if (messenger.getMembers().size() < 3) {
                        player.setMessenger(messenger);
                        player.setMessengerPosition(position);
                        ChannelServer.getInstance(c.getChannel()).getWorldInterface().joinMessenger(messenger.getId(), messengerplayer, player.getName(), messengerplayer.getChannel());
                    }
                }
            } catch (RemoteException e) {
                c.getChannelServer().reconnectWorld();
            }
        }
    }

    private static void chat(MapleClient c, String input) {
        if (c.getPlayer().getMessenger() != null) {
            try {
                ChannelServer.getInstance(c.getChannel()).getWorldInterface().messengerChat(c.getPlayer().getMessenger().getId(), input, c.getPlayer().getName());
            } catch (RemoteException e) {
                c.getChannelServer().reconnectWorld();
            }
        }
    }

    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        String input;
        byte mode = slea.readByte();
        switch (mode) {
            case 0x00:
                int messengerid = slea.readInt();
                answerInvite(c, messengerid);
                break;
            case 0x02:
                leaveMessenger(c);
                break;
            case 0x03:
                input = slea.readMapleAsciiString();
                inviteToMessenger(c, input);
                break;
            case 0x05:
                String targeted = slea.readMapleAsciiString();
                declineMessenger(c, targeted);
                break;
            case 0x06:
                input = slea.readMapleAsciiString();
                chat(c, input);
                break;
        }
    }
}