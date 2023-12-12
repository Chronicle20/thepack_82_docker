const MaplePacketCreator = Java.type('net.sf.odinms.tools.MaplePacketCreator');

/*
Kerning PQ: 4th stage to final stage portal
*/

function enter(pi) {
	var nextMap = 103000804;
	var eim = pi.getPlayer().getEventInstance()
	var target = eim.getMapInstance(nextMap);
	var targetPortal = target.getPortal("st00");
	// only let people through if the eim is ready
	var avail = eim.getProperty("4stageclear");
	if (avail == null || pi.getPlayer().isGM()) {
		// do nothing; send message to player
		pi.getPlayer().getClient().getSession().write(MaplePacketCreator.serverNotice(6, "The warp is currently unavailable."));
		return false;	}
	else {
		pi.getPlayer().changeMap(target, targetPortal);
		return true;
	}
}