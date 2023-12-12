package net.sf.odinms.scripting.quest;

import java.util.HashMap;
import java.util.Map;
import javax.script.Invocable;
import javax.script.ScriptEngine;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.scripting.AbstractScriptManager;

/**
 *
 * @author RMZero213
 */

public class QuestScriptManager extends AbstractScriptManager {

	private Map<MapleClient,QuestActionManager> qms = new HashMap<MapleClient,QuestActionManager>();
	private Map<MapleClient,Invocable> scripts = new HashMap<MapleClient,Invocable>();
	private static QuestScriptManager instance = new QuestScriptManager();
	
	public synchronized static QuestScriptManager getInstance() {
		return instance;
	}

	private ScriptEngine getQuestScriptEngine(MapleClient c, int questid) {
        return getInvocableScriptEngine("quest/" + questid + ".js", c);
	}

	public void start(MapleClient c, int npc, int quest) {
		try {
			QuestActionManager qm = new QuestActionManager(c, npc, quest, true);
			if (qms.containsKey(c)) {
				return;
			}
			qms.put(c, qm);

			ScriptEngine engine = getQuestScriptEngine(c, quest);
			if (engine == null) {
				log.warn("START Quest {} is uncoded.", quest);
				qm.dispose();
				return;
			}

			Invocable iv = (Invocable) engine;
			scripts.put(c, iv);
			iv.invokeFunction("start", (byte) 1, (byte) 0, 0);
		} catch (Exception e) {
			log.error("Error executing Quest script. (" + quest + ")", e);
			dispose(c);
		}
	}
	
	public void start(MapleClient c, byte mode, byte type, int selection) {
		Invocable iv = scripts.get(c);
		if (iv != null) {
			try {
				iv.invokeFunction("start", mode, type, selection);
			} catch (final Exception e) {
				log.error("Error starting quest script: {}", getQM(c).getQuest(), e);
				dispose(c);
			}
		}
	}

	public void end(MapleClient c, int npc, int quest) {
		try {
			QuestActionManager qm = new QuestActionManager(c, npc, quest, false);
			if (qms.containsKey(c)) {
				return;
			}
			qms.put(c, qm);

			ScriptEngine engine = getQuestScriptEngine(c, quest);
			if (engine == null) {
				log.warn("END Quest {} is uncoded.", quest);
				qm.dispose();
				return;
			}

			engine.put("qm", qm);

			Invocable iv = (Invocable) engine;
			scripts.put(c, iv);
			iv.invokeFunction("end", (byte) 1, (byte) 0, 0);
		} catch (Exception e) {
			log.error("Error executing Quest script. (" + quest + ")", e);
			dispose(c);
		}
	}

	public void end(MapleClient c, byte mode, byte type, int selection) {
		Invocable iv = scripts.get(c);
		if (iv != null) {
			try {
				iv.invokeFunction("end", mode, type, selection);
			} catch (final Exception e) {
				log.error("Error ending quest script: {}", getQM(c).getQuest(), e);
				dispose(c);
			}
		}
	}
	
	public void dispose(QuestActionManager qm, MapleClient c) {
		qms.remove(c);
		scripts.remove(c);
		resetContext("quest/" + qm.getQuest() + ".js", c);
	}
	
	public void dispose(MapleClient c) {
		QuestActionManager qm = qms.get(c);
		if (qm != null) {
			dispose(qm, c);
		}
	}
	
	public QuestActionManager getQM(MapleClient c) {
		return qms.get(c);
	}
}