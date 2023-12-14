package net.sf.odinms.net.world;

import java.io.Serializable;
import net.sf.odinms.server.MapleStatEffect;

/**
 *
 * @author Danny
 */
public record PlayerBuffValueHolder (long startTime, MapleStatEffect effect, int id) implements Serializable {
	public PlayerBuffValueHolder(long startTime, MapleStatEffect effect) {
		this(startTime, effect,  (int) (Math.random()*100));
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
		final PlayerBuffValueHolder other = (PlayerBuffValueHolder) obj;
        return id == other.id;
    }
}
