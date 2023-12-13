package net.sf.odinms.net.world;

import java.io.Serializable;

/**
 *
 * @author Danny
 */
public record PlayerCoolDownValueHolder(int skillId, long startTime, long length, int id) implements Serializable {
    public PlayerCoolDownValueHolder(int skillId, long startTime, long length) {
        this(skillId, startTime, length, (int) (Math.random() * 100));
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
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PlayerCoolDownValueHolder other = (PlayerCoolDownValueHolder) obj;
        return id == other.id;
    }
}