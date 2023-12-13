package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.messages.CommandProcessor;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.rmi.RemoteException;
import java.util.function.Consumer;

public class SpouseChatHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        String recipient = slea.readMapleAsciiString();
        String message = slea.readMapleAsciiString();
        if (CommandProcessor.processCommand(c, message)) {
            return;
        }

        if (c.getPlayer().isMarried() == 1) {
            c.getChannelServer().getPlayerStorage().getCharacterById(c.getPlayer().getPartnerId()).ifPresentOrElse(spouseChatInChannel(c, message), spouseChatInWorld(c, recipient, message));
        }
    }

    private Runnable spouseChatInWorld(MapleClient c, String recipient, String message) {
        return () -> {
            try {
                if (c.getChannelServer().getWorldInterface().isConnected(recipient)) {
                    c.getChannelServer().getWorldInterface().sendSpouseChat(c.getPlayer().getName(), recipient, message);
                    c.getSession().write(MaplePacketCreator.sendSpouseChat(c.getPlayer(), message));
                } else {
                    c.getSession().write(MaplePacketCreator.serverNotice(6, "You are not married or your spouse is currently offline."));
                }
            } catch (RemoteException e) {
                c.getSession().write(MaplePacketCreator.serverNotice(6, "You are not married or your spouse is currently offline."));
                c.getChannelServer().reconnectWorld();
            }
        };
    }

    private Consumer<MapleCharacter> spouseChatInChannel(MapleClient client, String message) {
        return (spouse) -> {
            spouse.getClient().getSession().write(MaplePacketCreator.sendSpouseChat(client.getPlayer(), message));
            client.getSession().write(MaplePacketCreator.sendSpouseChat(client.getPlayer(), message));
        };
    }
}