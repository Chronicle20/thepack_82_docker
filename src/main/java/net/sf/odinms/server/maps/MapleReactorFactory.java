/* 
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc> 
                       Matthias Butz <matze@odinms.de>
                       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License version 3
    as published by the Free Software Foundation. You may not use, modify
    or distribute this program under any other version of the
    GNU Affero General Public License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.odinms.server.maps;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import net.sf.odinms.provider.MapleData;
import net.sf.odinms.provider.MapleDataProvider;
import net.sf.odinms.provider.MapleDataProviderFactory;
import net.sf.odinms.provider.MapleDataTool;
import net.sf.odinms.tools.Pair;
import net.sf.odinms.tools.StringUtil;

public class MapleReactorFactory {
	//private static Logger log = LoggerFactory.getLogger(MapleReactorFactory.class);
	private static MapleDataProvider data = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/Reactor.wz"));
	private static Map<Integer, MapleReactorStats> reactorStats = new HashMap<>();
	
	public static MapleReactorStats getReactor(int rid) {
		MapleReactorStats stats = reactorStats.get(rid);
		if (stats == null) {
			int infoId = rid;
			MapleData reactorData = data.getData(StringUtil.getLeftPaddedStr(infoId + ".img", '0', 11));
			MapleData link = reactorData.getChildByPath("info/link");
			if (link != null) {
				infoId = MapleDataTool.getIntConvert("info/link",reactorData);
				stats = reactorStats.get(infoId);
			}
			if (stats == null) {
				reactorData = data.getData(StringUtil.getLeftPaddedStr(infoId + ".img", '0', 11));
				MapleData reactorInfoData = reactorData.getChildByPath("0/event/0");
				stats = new MapleReactorStats();
				
				if (reactorInfoData != null) {
					boolean areaSet = false;
					int i = 0;
					while (reactorInfoData != null) {
						Pair<Integer,Integer> reactItem = null;
						int type = MapleDataTool.getIntConvert("type",reactorInfoData);
						if (type == 100) { //reactor waits for item
							reactItem = new Pair<>(MapleDataTool.getIntConvert("0", reactorInfoData), MapleDataTool.getIntConvert("1", reactorInfoData));
							if (!areaSet) { //only set area of effect for item-triggered reactors once
								stats.setTL(MapleDataTool.getPoint("lt",reactorInfoData));
								stats.setBR(MapleDataTool.getPoint("rb",reactorInfoData));
								areaSet = true;
							}
						}
						byte nextState = (byte)MapleDataTool.getIntConvert("state",reactorInfoData);
						stats.addState((byte) i, type, reactItem, nextState);
						i++;
						reactorInfoData = reactorData.getChildByPath(i + "/event/0");
					}
				} else { //sit there and look pretty; likely a reactor such as Zakum/Papulatus doors that shows if player can enter
					stats.addState((byte) 0, 999, null, (byte) 0);
				}

				reactorStats.put(infoId, stats);
				if (rid != infoId) {
					reactorStats.put(rid, stats);
				}
			} else { // stats exist at infoId but not rid; add to map
				reactorStats.put(rid, stats);
			}
		}
		return stats;
	}
}
