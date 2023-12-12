
const SavedLocationType = Java.type('net.sf.odinms.client.SavedLocationType');

function enter(pi) {
	pi.getPlayer().saveLocation(SavedLocationType.FREE_MARKET);
	pi.warp(910000000, "out00");
	return true;
}