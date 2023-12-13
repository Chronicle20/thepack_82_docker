package net.sf.odinms.scripting.quest;

/**
 * @author RMZero213
 */

public interface QuestScript {
	void start(byte mode, byte type, int selection);
	void end(byte mode, byte type, int selection);
}