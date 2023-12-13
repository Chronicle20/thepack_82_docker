package net.sf.odinms.scripting.npc;

import net.sf.odinms.client.MapleCharacter;

public interface NPCScript {
    void start();
    void start(MapleCharacter chr);
    void action(byte mode, byte type, int selection);
}
