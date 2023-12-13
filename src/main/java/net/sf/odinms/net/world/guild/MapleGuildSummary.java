package net.sf.odinms.net.world.guild;

public record MapleGuildSummary(String name, short logoBG, byte logoBGColor, short logo,
                                byte logoColor) implements java.io.Serializable {

    public MapleGuildSummary(MapleGuild g) {
        this(g.getName(), (short) g.getLogoBG(), (byte) g.getLogoBGColor(), (short) g.getLogo(), (byte) g.getLogoColor());
    }
}
