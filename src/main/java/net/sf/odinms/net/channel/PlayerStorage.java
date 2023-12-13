package net.sf.odinms.net.channel;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import net.sf.odinms.client.MapleCharacter;

public class PlayerStorage implements IPlayerStorage {
	Map<String, MapleCharacter> nameToChar = new LinkedHashMap<>();
	Map<Integer, MapleCharacter> idToChar = new LinkedHashMap<>();

	public void registerPlayer(MapleCharacter chr) {
		nameToChar.put(chr.getName().toLowerCase(), chr);
		idToChar.put(chr.getId(), chr);
	}

	public void deregisterPlayer(MapleCharacter chr) {
		nameToChar.remove(chr.getName().toLowerCase());
		idToChar.remove(chr.getId());
	}

	public Optional<MapleCharacter> getCharacterByName(String name) {
		return Optional.ofNullable(nameToChar.get(name.toLowerCase()));
	}

	public Optional<MapleCharacter> getCharacterById(int id) {
		return Optional.ofNullable(idToChar.get(id));
	}

	public Collection<MapleCharacter> getAllCharacters() {
		return nameToChar.values();
	}
}
