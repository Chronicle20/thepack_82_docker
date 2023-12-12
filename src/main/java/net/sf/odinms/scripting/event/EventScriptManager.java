package net.sf.odinms.scripting.event;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.scripting.AbstractScriptManager;
import net.sf.odinms.scripting.SynchronizedInvocable;

/**
 *
 * @author Matze
 */
public class EventScriptManager extends AbstractScriptManager {
	private static final String INJECTED_VARIABLE_NAME = "em";

	private class EventEntry {
		public EventEntry(Invocable iv, EventManager em) {
			this.iv = iv;
			this.em = em;
		}
		public Invocable iv;
		public EventManager em;
	}
	
	private Map<String,EventEntry> events = new LinkedHashMap<String,EventEntry>();
	
	public EventScriptManager(ChannelServer cserv, String[] scripts) {
		super();
		for (String script : scripts) {
			if (!script.isEmpty()) {
				events.put(script, initializeEventEntry(script, cserv));
			}
		}
	}
	
	public EventManager getEventManager(String event) {
		EventEntry entry = events.get(event);
		if (entry == null) {
			return null;
		}
		return entry.em;
	}
	
	public void init() {
		for (EventEntry entry : events.values()) {
			try {
				entry.iv.invokeFunction("init", (Object) null);
			} catch (Exception ex) {
				log.error("Error on script: {}", entry.em.getName(), ex);
			}
		}
	}
	
	public void cancel() {
		for (EventEntry entry : events.values()) {
			entry.em.cancel();
		}
	}

	private EventEntry initializeEventEntry(String script, ChannelServer channel) {
		ScriptEngine engine = getInvocableScriptEngine("event/" + script + ".js");
		Invocable iv = SynchronizedInvocable.of((Invocable) engine);
		EventManager eventManager = new EventManager(channel, iv, script);
		engine.put(INJECTED_VARIABLE_NAME, eventManager);
		return new EventEntry(iv, eventManager);
	}
}
