package net.sf.odinms.server.maps;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.tools.MaplePacketCreator;

import java.util.Calendar;
import java.util.concurrent.ScheduledFuture;

public class MapleMapTimer {
    private final Calendar predictedStopTime;
    private final int mapToWarpTo;
    private final int minLevelToWarp;
    private final int maxLevelToWarp;
    private final ScheduledFuture<?> sf0F;

    public MapleMapTimer(ScheduledFuture<?> sfO, int newDuration, int mapToWarpToP, int minLevelToWarpP, int maxLevelToWarpP) {
        this.predictedStopTime = Calendar.getInstance();
        this.predictedStopTime.add(Calendar.SECOND, newDuration);
        this.mapToWarpTo = mapToWarpToP;
        this.minLevelToWarp = minLevelToWarpP;
        this.maxLevelToWarp = maxLevelToWarpP;
        this.sf0F = sfO;
    }

    public MaplePacket makeSpawnData() {
        int timeLeft;
        long StopTimeStamp = this.predictedStopTime.getTimeInMillis();
        long CurrentTimeStamp = Calendar.getInstance().getTimeInMillis();
        timeLeft = (int) (StopTimeStamp - CurrentTimeStamp) / 1000;
        return MaplePacketCreator.getClock(timeLeft);
    }

    public void sendSpawnData(MapleClient c) {
        c.getSession().write(makeSpawnData());
    }

    public ScheduledFuture<?> getSF0F() {
        return sf0F;
    }

    public int warpToMap() {
        return this.mapToWarpTo;
    }

    public int minLevelToWarp() {
        return this.minLevelToWarp;
    }

    public int maxLevelToWarp() {
        return this.maxLevelToWarp;
    }
}
