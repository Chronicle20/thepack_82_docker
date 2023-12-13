package net.sf.odinms.client;

import java.util.Arrays;
import java.util.Optional;

public enum MapleInventoryType {

    UNDEFINED(0),
    EQUIP(1),
    USE(2),
    SETUP(3),
    ETC(4),
    CASH(5),
    EQUIPPED(-1);

    final byte type;

    MapleInventoryType(int type) {
        this.type = (byte) type;
    }

    public static Optional<MapleInventoryType> getByType(byte type) {
		return Arrays.stream(MapleInventoryType.values())
				.filter(it -> it.getType() == type)
				.findFirst();
    }

    public static MapleInventoryType getByWZName(String name) {
        return switch (name) {
            case "Install" -> SETUP;
            case "Consume" -> USE;
            case "Etc" -> ETC;
            case "Cash", "Pet" -> CASH;
            default -> UNDEFINED;
        };
    }

    public byte getType() {
        return type;
    }

    public short getBitfieldEncoding() {
        return (short) (2 << type);
    }

}
