/*
Kerning PQ: 3rd stage to 4th stage portal
*/
const MaplePacketCreator = Java.type('net.sf.odinms.tools.MaplePacketCreator');

function enter(pi) {
	var nextMap = 103000803;
	var eim = pi.getPlayer().getEventInstance()
	var target = eim.getMapInstance(nextMap);
	var targetPortal = target.getPortal("st00");
	// only let people through if the eim is ready
	var avail = eim.getProperty("3stageclear");
	if (avail == null || pi.getPlayer().isGM()) {
		// do nothing; send message to player
		pi.getPlayer().getClient().getSession().write(MaplePacketCreator.serverNotice(6, "The warp is currently unavailable."));
		return false;
	}
	else {
		pi.getPlayer().changeMap(target, targetPortal);
		return true;
	}
}