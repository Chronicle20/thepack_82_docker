package net.sf.odinms.net.channel;

import java.util.Collection;
import java.util.Optional;

import net.sf.odinms.client.MapleCharacter;

public interface IPlayerStorage {
	Optional<MapleCharacter> getCharacterByName(String name);
	Optional<MapleCharacter> getCharacterById(int id);
	Collection<MapleCharacter> getAllCharacters();
}
