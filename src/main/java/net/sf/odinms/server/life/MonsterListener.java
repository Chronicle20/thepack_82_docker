package net.sf.odinms.server.life;

import net.sf.odinms.client.MapleCharacter;

public interface MonsterListener {
	void monsterKilled(MapleMonster monster, MapleCharacter highestDamageChar);
}
