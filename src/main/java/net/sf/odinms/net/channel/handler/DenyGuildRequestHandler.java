package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.util.function.Consumer;

/**
 * @author Xterminator
 */

public class DenyGuildRequestHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.readByte();
        String from = slea.readMapleAsciiString();
        c.getChannelServer().getPlayerStorage().getCharacterByName(from)
                .map(MapleCharacter::getClient)
                .ifPresent(denyGuildRequest(c.getPlayer().getName()));
    }

    private Consumer<MapleClient> denyGuildRequest(String name) {
        return (target) -> target.getSession().write(MaplePacketCreator.denyGuildInvitation(name));
    }
}