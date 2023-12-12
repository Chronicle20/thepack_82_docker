package net.sf.odinms.scripting.portal;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.scripting.AbstractScriptManager;
import net.sf.odinms.server.MaplePortal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortalScriptManager extends AbstractScriptManager {
	private static final Logger log = LoggerFactory.getLogger(PortalScriptManager.class);
	private static PortalScriptManager instance = new PortalScriptManager();
	private Map<String, PortalScript> scripts = new HashMap<String, PortalScript>();
	private ScriptEngineFactory sef;

	private PortalScriptManager() {
		ScriptEngineManager sem = new ScriptEngineManager();
		sef = sem.getEngineByName("javascript").getFactory();
	}
	
	public static PortalScriptManager getInstance() {
		return instance;
	}

	private PortalScript getPortalScript(String scriptName) throws ScriptException {
		String scriptPath = "portal/" + scriptName + ".js";
		PortalScript script = scripts.get(scriptPath);
		if (script != null) {
			return script;
		}

		ScriptEngine engine = getInvocableScriptEngine(scriptPath);
		if (!(engine instanceof Invocable iv)) {
			return null;
		}

		script = iv.getInterface(PortalScript.class);
		if (script == null) {
			throw new ScriptException(String.format("Portal script \"%s\" fails to implement the PortalScript interface", scriptName));
		}

		scripts.put(scriptPath, script);
		return script;
	}

    public boolean executePortalScript(MaplePortal portal, MapleClient c) {
		try {
			PortalScript script = getPortalScript(portal.getScriptName());
			if (script != null) {
				return script.enter(new PortalPlayerInteraction(c, portal));
			}
		} catch (Exception e) {
			log.warn("Portal script error in: {}", portal.getScriptName(), e);
		}
		return false;
	}
	
	public void clearScripts() {
		scripts.clear();
	}
}
