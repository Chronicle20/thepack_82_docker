package net.sf.odinms.scripting.reactor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.scripting.AbstractScriptManager;
import net.sf.odinms.server.life.MapleMonsterInformationProvider.DropEntry;
import net.sf.odinms.server.maps.MapleReactor;

/**
 * @author Lerk
 */

public class ReactorScriptManager extends AbstractScriptManager {
	private static ReactorScriptManager instance = new ReactorScriptManager();
	private Map<Integer, List<DropEntry>> drops = new HashMap<>();

	public synchronized static ReactorScriptManager getInstance() {
		return instance;
	}

	private Invocable initializeInvocable(MapleClient c, MapleReactor reactor) {
		ScriptEngine engine = getInvocableScriptEngine("reactor/" + reactor.getId() + ".js", c);
		if (engine == null) {
			return null;
		}

		Invocable iv = (Invocable) engine;
		ReactorActionManager rm = new ReactorActionManager(c, reactor, iv);
		engine.put("rm", rm);

		return iv;
	}

	public void act(MapleClient c, MapleReactor reactor) {
		try {
			Invocable iv = initializeInvocable(c, reactor);
			if (iv == null) {
				return;
			}

			iv.invokeFunction("act");
		} catch (final ScriptException | NoSuchMethodException | NullPointerException e) {
			log.error("Error during act script for reactor: {}", reactor.getId(), e);
		}
	}

	public List<DropEntry> getDrops(int rid) {
		List<DropEntry> ret = drops.get(rid);
		if (ret == null) {
			ret = new LinkedList<>();
			try {
				Connection con = DatabaseConnection.getConnection();
				PreparedStatement ps = con.prepareStatement("SELECT itemid, chance FROM reactordrops WHERE reactorid = ? AND chance >= 0");
				ps.setInt(1, rid);
				ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					ret.add(new DropEntry(rs.getInt("itemid"), rs.getInt("chance")));
				}
				rs.close();
				ps.close();
			} catch (Exception e) {
				log.error("Could not retrieve drops for reactor " + rid, e);
			}
			drops.put(rid, ret);
		}
		return ret;
	}
	
	public void clearDrops() {
		drops.clear();
	}
}
