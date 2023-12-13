package net.sf.odinms.client;

public class BuddylistEntry {
	private final String name;
	private final int characterId;
	private int channelId;
	private final boolean visible;

	/**
	 * 
	 * @param name
	 * @param characterId
	 * @param channel should be -1 if the buddy is offline
	 * @param visible
	 */
	public BuddylistEntry(String name, int characterId, int channel, boolean visible) {
		super();
		this.name = name;
		this.characterId = characterId;
		this.channelId = channel;
		this.visible = visible;
	}

	/**
	 * @return the channel the character is on. If the character is offline returns -1.
	 */
	public int getChannelId() {
		return channelId;
	}

	public void setChannelId(int channelId) {
		this.channelId = channelId;
	}

	public boolean isOnline() {
		return channelId >= 0;
	}

	public String getName() {
		return name;
	}

	public int getCharacterId() {
		return characterId;
	}

	public boolean isVisible() {
		return visible;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + characterId;
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
		final BuddylistEntry other = (BuddylistEntry) obj;
        return characterId == other.characterId;
    }
}