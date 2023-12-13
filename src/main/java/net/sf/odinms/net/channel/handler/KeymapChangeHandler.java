package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.KeyBinding;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class KeymapChangeHandler extends AbstractMaplePacketHandler {

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		slea.readInt(); //who knows
		int numChanges = slea.readInt();
		for (int i = 0; i < numChanges; i++) {
			int key = slea.readInt();
			int type = slea.readByte();
			int action = slea.readInt();
			KeyBinding newbinding = new KeyBinding(type, action);
			c.getPlayer().changeKeybinding(key, newbinding);
		}
	}
}