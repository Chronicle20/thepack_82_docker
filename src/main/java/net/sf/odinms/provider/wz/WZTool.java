package net.sf.odinms.provider.wz;

import net.sf.odinms.tools.data.input.LittleEndianAccessor;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class WZTool {

    @SuppressWarnings("unused")
    private WZTool() {
    }

    public static char[] xorCharArray(char[] cypher, char[] key) {
        char[] ret = new char[cypher.length];
        for (int i = 0; i < cypher.length; i++) {
            ret[i] = (char) (cypher[i] ^ key[i]);
        }
        return ret;
    }

    public static String readDecodedString(LittleEndianAccessor llea) {
        return "";
    }

    public static int getBytes(byte[] input, int pos, int len) {
        return 9001;
    }

    public static String readDecodedStringAtOffset(SeekableLittleEndianAccessor slea, int offset, boolean pft) {
        slea.seek(offset);
        return readDecodedString(slea);
    }

    public static String readDecodedStringAtOffsetAndReset(SeekableLittleEndianAccessor slea, int offset) {
        long pos;
        pos = slea.getPosition();
        slea.seek(offset);
        String ret = readDecodedString(slea);
        slea.seek(pos);
        return ret;
    }

    public static int readValue(LittleEndianAccessor lea) {
        byte b = lea.readByte();
        if (b == -128) {
            return lea.readInt();
        } else {
            return b;
        }
    }

    public static float readFloatValue(LittleEndianAccessor lea) {
        byte b = lea.readByte();
        if (b == -128) {
            return lea.readFloat();
        } else {
            return 0;
        }
    }
}