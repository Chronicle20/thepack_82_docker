package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.messages.CommandProcessor;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.rmi.RemoteException;
import java.util.Optional;

/**
 * @author Matze
 */
public class WhisperHandler extends AbstractMaplePacketHandler {

    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        byte mode = slea.readByte();
        if (mode == 6) {
            String recipient = slea.readMapleAsciiString();
            String text = slea.readMapleAsciiString();
            if (!CommandProcessor.processCommand(c, text)) {
                Optional<MapleCharacter> player = c.getChannelServer().getPlayerStorage().getCharacterByName(recipient);
                if (player.isPresent()) {
                    player.get().getClient().getSession().write(MaplePacketCreator.getWhisper(c.getPlayer().getName(), c.getChannel(), text));
                    c.getSession().write(MaplePacketCreator.getWhisperReply(recipient, (byte) 1));
                    return;
                }

                try {
                    if (ChannelServer.getInstance(c.getChannel()).getWorldInterface().isConnected(recipient)) {
                        ChannelServer.getInstance(c.getChannel()).getWorldInterface().whisper(
                                c.getPlayer().getName(), recipient, c.getChannel(), text);
                        c.getSession().write(MaplePacketCreator.getWhisperReply(recipient, (byte) 1));
                    } else {
                        c.getSession().write(MaplePacketCreator.getWhisperReply(recipient, (byte) 0));
                    }
                } catch (RemoteException e) {
                    c.getSession().write(MaplePacketCreator.getWhisperReply(recipient, (byte) 0));
                    c.getChannelServer().reconnectWorld();
                }
            }
            return;
        }

        if (mode == 5) {
            String recipient = slea.readMapleAsciiString();
            Optional<MapleCharacter> player = c.getChannelServer().getPlayerStorage().getCharacterByName(recipient);
            if (player.isPresent() && (c.getPlayer().isGM() || !player.get().isHidden())) {
                if (player.get().inCS()) {
                    c.getSession().write(MaplePacketCreator.getFindReplyWithCS(player.get().getName()));
                    return;
                }
                c.getSession().write(MaplePacketCreator.getFindReplyWithMap(player.get().getName(), player.get().getMap().getId()));
                return;
            }

            player = ChannelServer.getAllInstances().stream()
                    .map(channel -> channel.getPlayerStorage().getCharacterByName(recipient))
                    .flatMap(Optional::stream)
                    .findFirst();
            if (player.isPresent()) {
                c.getSession().write(MaplePacketCreator.getFindReply(player.get().getName(), (byte) player.get().getClient().getChannel()));
                return;
            }
            c.getSession().write(MaplePacketCreator.getWhisperReply(recipient, (byte) 0));
        }
    }
}
