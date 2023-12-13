package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.scripting.npc.Marriage;
import net.sf.odinms.scripting.npc.NPCScriptManager;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

import java.util.function.Consumer;

public class RingActionHandler extends AbstractMaplePacketHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RingActionHandler.class);

    private static void handleSendRing(MapleClient c, String partnerName) {
        if (partnerName.equalsIgnoreCase(c.getPlayer().getName())) {
            c.getSession().write(net.sf.odinms.tools.MaplePacketCreator.serverNotice(1, "You cannot put your own name in it."));
            return;
        }

        c.getChannelServer().getPlayerStorage()
                .getCharacterByName(partnerName)
                .ifPresentOrElse(handleSendRing(c), () -> handleSendRingPartnerNotFound(c, partnerName));
    }

    private static Consumer<MapleCharacter> handleSendRing(MapleClient c) {
        return (partner) -> {
            if (partner.getGender() == c.getPlayer().getGender()) {
                c.getSession().write(net.sf.odinms.tools.MaplePacketCreator.serverNotice(1, "Your partner is the same gender as you."));
                return;
            }
            if (c.getPlayer().isMarried() == 0 && partner.isMarried() == 0) {
                NPCScriptManager.getInstance().start(partner.getClient(), 9201002, "marriagequestion", c.getPlayer());
            }
        };
    }

    private static void handleSendRingPartnerNotFound(MapleClient c, String partnerName) {
        c.getSession().write(net.sf.odinms.tools.MaplePacketCreator.serverNotice(1, partnerName + " was not found on this channel. If you are both logged in, please make sure you are in the same channel."));
    }

    private static void handleBrokenEngagement(MapleClient c) {
        c.getPlayer().getPartner().ifPresent(breakEngagement(c));
    }

    private static Consumer<MapleCharacter> breakEngagement(MapleClient c) {
        return (partner) -> {
            Marriage.divorceEngagement(c.getPlayer(), partner);
            c.getSession().write(net.sf.odinms.tools.MaplePacketCreator.serverNotice(1, "Your engagement has been broken up."));
        };
    }

    private static void handleCancel(MapleClient c) {
        c.getSession().write(net.sf.odinms.tools.MaplePacketCreator.serverNotice(1, "You've cancelled the request."));
    }

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        byte mode = slea.readByte();
        switch (mode) {
            case 0x00:
                String partnerName = slea.readMapleAsciiString();
                handleSendRing(c, partnerName);
                break;
            case 0x01:
                handleCancel(c);
                break;
            case 0x03:
                handleBrokenEngagement(c);
                break;
            default:
                log.info("Unhandled Ring Packet : " + slea);
                break;
        }
    }
}
