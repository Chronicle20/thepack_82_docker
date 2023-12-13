package net.sf.odinms.net.world;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.sf.odinms.tools.Pair;

/**
 *
 * @author Danny
 */
public class PlayerBuffStorage implements Serializable {
	private List<Pair<Integer, List<PlayerBuffValueHolder>>> buffs = new ArrayList<>();
	private List<Pair<Integer, List<PlayerCoolDownValueHolder>>> coolDowns = new ArrayList<>();
	private int id = (int) (Math.random()*100);
	
	public PlayerBuffStorage() {
	}
	
	public void addBuffsToStorage(int chrid, List<PlayerBuffValueHolder> toStore) {
        buffs.removeIf(stored -> Objects.equals(stored.left(), chrid));
		buffs.add(new Pair<>(chrid, toStore));
	}
	
	public void addCooldownsToStorage(int chrid, List<PlayerCoolDownValueHolder> toStore) {
        coolDowns.removeIf(stored -> Objects.equals(stored.left(), chrid));
		coolDowns.add(new Pair<>(chrid, toStore));
	}
	
	public List<PlayerBuffValueHolder> getBuffsFromStorage(int chrid) {
		List<PlayerBuffValueHolder> ret = null;
		Pair<Integer, List<PlayerBuffValueHolder>> stored;
		for (int i = 0; i < buffs.size(); i++) {
			stored = buffs.get(i);
			if (stored.left().equals(chrid)) {
				ret = stored.right();
				buffs.remove(stored);
			}
		}
		return ret;
	}
	
	public List<PlayerCoolDownValueHolder> getCooldownsFromStorage(int chrid) {
		List<PlayerCoolDownValueHolder> ret = null;
		Pair<Integer, List<PlayerCoolDownValueHolder>> stored;
		for (int i = 0; i < coolDowns.size(); i++) {
			stored = coolDowns.get(i);
			if (stored.left().equals(chrid)) {
				ret = stored.right();
				coolDowns.remove(stored);
			}
		}
		return ret;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final PlayerBuffStorage other = (PlayerBuffStorage) obj;
        return id == other.id;
    }
}
