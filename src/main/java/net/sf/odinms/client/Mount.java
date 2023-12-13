package net.sf.odinms.client;

import net.sf.odinms.server.TimerManager;
import net.sf.odinms.tools.MaplePacketCreator;

import java.util.concurrent.ScheduledFuture;

/**
 * @author Patrick/PurpleMadness
 */
public class Mount {

    private final int skillId;
    private final MapleCharacter owner;
    private int itemId;
    private int tiredness;
    private int experience;
    private int level;
    private ScheduledFuture<?> tirednessSchedule;

    public Mount(MapleCharacter owner, int id, int skillId) {
        this.itemId = id;
        this.skillId = skillId;
        this.tiredness = 0;
        this.level = 1;
        this.experience = 0;
        this.owner = owner;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int newitemid) {
        this.itemId = newitemid;
    }

    public int getSkillId() {
        return skillId;
    }

    public int getId() {
        return switch (this.itemId) {
            case 1902000 -> 1;
            case 1902001 -> 2;
            case 1902002 -> 3;
            case 1932000 -> 4;
            case 1902008, 1902009 -> 5; //NX ones
            default -> 0;
        };
    }

    public int getTiredness() {
        return tiredness;
    }

    public void setTiredness(int newtiredness) {
        this.tiredness = newtiredness;
        if (tiredness < 0) {
            tiredness = 0;
        }
    }

    public int getExperience() {
        return experience;
    }

    public void setExperience(int newexp) {
        this.experience = newexp;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int newlevel) {
        this.level = newlevel;
    }

    public void increaseTiredness() {
        this.tiredness++;
        owner.getMap().broadcastMessage(MaplePacketCreator.updateMount(owner.getId(), this, false));
        if (tiredness > 100) {
            owner.dispelSkill(1004);
        }
    }

    public void startSchedule() {
        this.tirednessSchedule = TimerManager.getInstance().register(this::increaseTiredness, 60000, 60000);
    }

    public void cancelSchedule() {
        this.tirednessSchedule.cancel(false);
    }
}
