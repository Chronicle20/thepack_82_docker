package net.sf.odinms.provider;

import java.util.List;
import java.util.stream.Stream;

import net.sf.odinms.provider.wz.MapleDataType;

public interface MapleData extends MapleDataEntity, Iterable<MapleData> {
	String getName();
	MapleDataType getType();
	List<MapleData> getChildren();
	MapleData getChildByPath(String path);
	Object getData();
	Stream<MapleData> stream();
}
