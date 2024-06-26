const MaplePacketCreator = Java.type('net.sf.odinms.tools.MaplePacketCreator');

function enter(pi) {
	var map = pi.getPlayer().getMap();
	var reactor = map.getReactorByName("gate03");
	var state = reactor.getState();
	if (state >= 4) {
		pi.warp(670010600, 8);
		return true;
	} else {
		pi.getClient().getSession().write(MaplePacketCreator.serverNotice(5, "The gate is closed."));
		return false;
	}
}