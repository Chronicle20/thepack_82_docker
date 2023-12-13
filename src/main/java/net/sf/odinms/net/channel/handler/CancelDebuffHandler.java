package net.sf.odinms.net.channel.handler;

import java.util.ArrayList;
import java.util.List;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.Disease;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class CancelDebuffHandler extends AbstractMaplePacketHandler {
	
	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		List<Disease> diseases = c.getPlayer().getDiseases();
		List<Disease> diseases_ = new ArrayList<>();
		for (Disease disease : diseases) {
			List<Disease> disease_ = new ArrayList<>();
			disease_.add(disease);
			diseases_.add(disease);
			c.getSession().write(MaplePacketCreator.cancelDebuff(disease_));
			c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.cancelForeignDebuff(c.getPlayer().getId(), disease_), false);
		}
		for (Disease disease : diseases_) {
			c.getPlayer().removeDisease(disease);
		}
	}
}
