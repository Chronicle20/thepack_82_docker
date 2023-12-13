package net.sf.odinms.client;

import java.util.Arrays;
import java.util.Optional;

public enum SkinColor {
    NORMAL(0), DARK(1), BLACK(2), PALE(3), BLUE(4), WHITE(9);

    final int id;

    SkinColor(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static Optional<SkinColor> getById(int id) {
        return Arrays.stream(SkinColor.values())
                .filter(sc -> sc.getId() == id)
                .findFirst();
    }
}