package net.sf.odinms.scripting;

import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;
import net.sf.odinms.client.MapleClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Matze
 */
public abstract class AbstractScriptManager {

    protected ScriptEngine engine;
    private final ScriptEngineFactory sef;

    protected static final Logger log = LoggerFactory.getLogger(AbstractScriptManager.class);

    protected AbstractScriptManager() {
        sef = new ScriptEngineManager().getEngineByName("graal.js").getFactory();
    }

    protected ScriptEngine getInvocableScriptEngine(String path) {
        Path scriptFile = Path.of("scripts", path);
        if (!Files.exists(scriptFile)) {
            return null;
        }

        ScriptEngine engine = sef.getScriptEngine();
        if (!(engine instanceof GraalJSScriptEngine graalScriptEngine)) {
            throw new IllegalStateException("ScriptEngineFactory did not provide a GraalJSScriptEngine");
        }

        enableScriptHostAccess(graalScriptEngine);

        try (BufferedReader br = Files.newBufferedReader(scriptFile, StandardCharsets.UTF_8)) {
            engine.eval(br);
        } catch (final ScriptException | IOException t) {
            log.warn("Exception during script eval for file: {}", path, t);
            return null;
        }

        return graalScriptEngine;
    }

    protected ScriptEngine getInvocableScriptEngine(String path, MapleClient c) {
        ScriptEngine engine = c.getScriptEngine("scripts/" + path);
        if (engine == null) {
            engine = getInvocableScriptEngine(path);
            c.setScriptEngine(path, engine);
        }

        return engine;
    }


    /**
     * Allow usage of "Java.type()" in script to look up host class
     */
    private void enableScriptHostAccess(GraalJSScriptEngine engine) {
        Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.put("polyglot.js.allowHostAccess", true);
        bindings.put("polyglot.js.allowHostClassLookup", true);
    }

    protected void resetContext(String path, MapleClient c) {
        path = "scripts/" + path;
        c.removeScriptEngine(path);
    }
}
