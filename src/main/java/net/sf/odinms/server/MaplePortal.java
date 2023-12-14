package net.sf.odinms.server;

import java.awt.Point;

import net.sf.odinms.client.MapleClient;

public interface MaplePortal {

    int MAP_PORTAL = 2;
    int DOOR_PORTAL = 6;
    boolean OPEN = true;
    boolean CLOSED = false;

    int getType();
    int getId();
    Point getPosition();
    String getName();
    String getTarget();
    String getScriptName();
    void setScriptName(String newName);
    void setPortalStatus(boolean newStatus);
    boolean getPortalStatus();
    int getTargetMapId();
    void enterPortal(MapleClient c);
    void setPortalState(boolean state);
    boolean getPortalState();
}
