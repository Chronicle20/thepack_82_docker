package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class ItemMoveHandler extends AbstractMaplePacketHandler {

    public ItemMoveHandler() {
    }

    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.readInt(); //?
        MapleInventoryType type = MapleInventoryType.getByType(slea.readByte()).orElseThrow();
        byte source = (byte) slea.readShort();
        byte destination = (byte) slea.readShort();
        short quantity = slea.readShort();
        if (source < 0 && destination > 0) {
            MapleInventoryManipulator.unequip(c, source, destination);
        } else if (destination < 0) {
            MapleInventoryManipulator.equip(c, source, destination);
        } else if (destination == 0) {
            MapleInventoryManipulator.drop(c, type, source, quantity);
        } else {
            MapleInventoryManipulator.move(c, type, source, destination);
        }
    }
}
