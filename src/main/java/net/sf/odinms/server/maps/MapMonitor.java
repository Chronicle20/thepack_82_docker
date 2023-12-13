package net.sf.odinms.server.maps;

import net.sf.odinms.server.MaplePortal;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.tools.MaplePacketCreator;

import java.util.concurrent.ScheduledFuture;

public class MapMonitor {

    private final ScheduledFuture<?> monitorSchedule;
    private final MapleMap map;
    private final MaplePortal portal;
    private final MapleReactor reactor;

    public MapMonitor(final MapleMap map, MaplePortal portal, MapleReactor reactor) {
        this.map = map;
        this.portal = portal;
        this.reactor = reactor;
        this.monitorSchedule = TimerManager.getInstance().register(
                () -> {
                    if (map.getCharacters().isEmpty()) {
                        cancelAction();
                    }
                }, 5000);
    }

    public void cancelAction() {
        monitorSchedule.cancel(false);
        map.killAllMonsters();
        if (portal != null) {
            portal.setPortalStatus(MaplePortal.OPEN);
        }
        if (reactor != null) {
            reactor.setState((byte) 0);
            reactor.getMap().broadcastMessage(MaplePacketCreator.triggerReactor(reactor, 0));
        }
        map.resetReactors();
    }
}