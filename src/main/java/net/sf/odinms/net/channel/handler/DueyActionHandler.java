package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleDueyActions;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class DueyActionHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        byte type = slea.readByte();
        if (type == MapleDueyActions.C_SEND_ITEM.getCode()) { //send item
            byte inventoryTypeId = slea.readByte();
            MapleInventoryType inventoryType = MapleInventoryType.getByType(inventoryTypeId).orElseThrow();


            byte slot = slea.readByte();
            slea.readByte();
            short quantity = slea.readShort();
            boolean senditem = slot == 0 && inventoryTypeId == 0 && quantity == 0;
            int mesos = slea.readInt();
            boolean sendmesos = mesos == 0;
            String name = slea.readMapleAsciiString();
            slea.readByte();
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            if (!c.getChannelServer().characterNameExists(name)) {
                c.getSession().write(MaplePacketCreator.sendDueyAction(MapleDueyActions.S_ERROR_SENDING.getCode()));
                return;
            }

            if (sendmesos && !ii.isDropRestricted(inventoryTypeId)) {
                c.getPlayer().gainMeso(-mesos, false);
            }

            if (senditem && !ii.isDropRestricted(inventoryTypeId)) {
                MapleInventoryManipulator.removeFromSlot(c, inventoryType, slot, quantity, false);
            }
            c.getSession().write(MaplePacketCreator.sendDueyAction(MapleDueyActions.S_SUCCESSFULLY_SENT.getCode()));
            return;
        }

        if (type == MapleDueyActions.C_CLOSE_DUEY.getCode()) {
            //TODO
        }
    }
}
