package net.sf.odinms.server.life;

public enum ElementalEffectiveness {
	NORMAL, IMMUNE, STRONG, WEAK;
	
	public static ElementalEffectiveness getByNumber(int num) {
        return switch (num) {
            case 1 -> IMMUNE;
            case 2 -> STRONG;
            case 3 -> WEAK;
            default -> throw new IllegalArgumentException("Unkown effectiveness: " + num);
        };
	}
}
