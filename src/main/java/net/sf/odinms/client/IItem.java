package net.sf.odinms.client;

import java.util.List;
import java.util.function.Predicate;

public interface IItem extends Comparable<IItem> {
    int EQUIP = 1;

    int ITEM = 2;

    static Predicate<IItem> Is(int itemId) {
        return (i) -> i.getItemId() == itemId;
    }

    byte getType();

    byte getPosition();

    void setPosition(byte position);

    int getItemId();

    short getQuantity();

    void setQuantity(short quantity);

    String getOwner();

    void setOwner(String owner);

    int getPetId();

    IItem copy();

    void log(String msg, boolean fromDB);

    List<String> getLog();
}