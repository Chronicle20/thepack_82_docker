package net.sf.odinms.server.life;

import net.sf.odinms.client.MapleBuffStat;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleJob;
import net.sf.odinms.client.SkillFactory;
import net.sf.odinms.client.status.MonsterStatus;
import net.sf.odinms.client.status.MonsterStatusEffect;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.world.MapleParty;
import net.sf.odinms.net.world.MaplePartyCharacter;
import net.sf.odinms.scripting.event.EventInstanceManager;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.server.life.MapleMonsterInformationProvider.DropEntry;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.server.maps.MapleMapObject;
import net.sf.odinms.server.maps.MapleMapObjectType;
import net.sf.odinms.tools.ArrayMap;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Pair;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;

public class MapleMonster extends AbstractLoadedMapleLife {

    private MapleMonsterStats stats;
    private MapleMonsterStats overrideStats;
    private int hp;
    private int mp;
    private WeakReference<MapleCharacter> controller = new WeakReference<>(null);
    private boolean controllerHasAggro, controllerKnowsAboutAggro;
    private Collection<AttackerEntry> attackers = new LinkedList<>();
    private EventInstanceManager eventInstance = null;
    private Collection<MonsterListener> listeners = new LinkedList<>();
    private MapleCharacter highestDamageChar;
    private Map<MonsterStatus, MonsterStatusEffect> stati = new LinkedHashMap<>();
    private List<MonsterStatusEffect> activeEffects = new ArrayList<>();
    private MapleMap map;
    private int VenomMultiplier = 0;
    private boolean fake = false;
    private boolean dropsDisabled = false;
    private List<Pair<Integer, Integer>> usedSkills = new ArrayList<>();
    private Map<Pair<Integer, Integer>, Integer> skillsUsed = new HashMap<>();
    private List<MonsterStatus> monsterBuffs = new ArrayList<>();

    public MapleMonster(int id, MapleMonsterStats stats) {
        super(id);
        initWithStats(stats);
    }

    public MapleMonster(MapleMonster monster) {
        super(monster);
        initWithStats(monster.stats);
    }

    private void initWithStats(MapleMonsterStats stats) {
        setStance(5);
        this.stats = stats;
        hp = stats.getHp();
        mp = stats.getMp();
    }

    public void disableDrops() {
        this.dropsDisabled = true;
    }

    public boolean dropsDisabled() {
        return dropsDisabled;
    }

    public int getDrop() {
        MapleMonsterInformationProvider mi = MapleMonsterInformationProvider.getInstance();
        int lastAssigned = -1;
        int minChance = 1;
        List<DropEntry> dl = mi.retrieveDropChances(getId());
        for (DropEntry d : dl) {
            if (d.chance > minChance) {
                minChance = d.chance;
            }
        }
        for (DropEntry d : dl) {
            d.assignedRangeStart = lastAssigned + 1;
            d.assignedRangeLength = (int) Math.ceil(((double) 1 / (double) d.chance) * minChance);
            lastAssigned += d.assignedRangeLength;
        }
        Random r = new Random();
        int c = r.nextInt(minChance);
        for (DropEntry d : dl) {
            if (c >= d.assignedRangeStart && c < (d.assignedRangeStart + d.assignedRangeLength)) {
                return d.itemId;
            }
        }
        return -1;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getMaxHp() {
        if (overrideStats != null) {
            return overrideStats.getHp();
        }
        return stats.getHp();
    }

    public int getMp() {
        return mp;
    }

    public void setMp(int mp) {
        if (mp < 0) {
            mp = 0;
        }
        this.mp = mp;
    }

    public int getMaxMp() {
        if (overrideStats != null) {
            return overrideStats.getMp();
        }
        return stats.getMp();
    }

    public int getExp() {
        if (overrideStats != null) {
            return overrideStats.getExp();
        }
        return stats.getExp();
    }

    public int getLevel() {
        return stats.getLevel();
    }

    public int getRemoveAfter() {
        return stats.getRemoveAfter();
    }

    public int getVenomMulti() {
        return this.VenomMultiplier;
    }

    public void setVenomMulti(int multiplier) {
        this.VenomMultiplier = multiplier;
    }

    public boolean isBoss() {
        return stats.isBoss() || getId() == 8810018;
    }

    public boolean isFfaLoot() {
        return stats.isFfaLoot();
    }

    public int getAnimationTime(String name) {
        return stats.getAnimationTime(name);
    }

    public List<Integer> getRevives() {
        return stats.getRevives();
    }

    public void setOverrideStats(MapleMonsterStats overrideStats) {
        this.overrideStats = overrideStats;
    }

    public byte getTagColor() {
        return stats.getTagColor();
    }

    public byte getTagBgColor() {
        return stats.getTagBgColor();
    }

    public boolean getUndead() {
        return stats.getUndead();
    }

    /**
     * @param from   the player that dealt the damage
     * @param damage
     */
    public void damage(MapleCharacter from, int damage, boolean updateAttackTime) {
        AttackerEntry attacker = null;
        if (from.getParty() != null) {
            attacker = new PartyAttackerEntry(from.getParty().getId(), from.getClient().getChannelServer());
        } else {
            attacker = new SingleAttackerEntry(from, from.getClient().getChannelServer());
        }
        boolean replaced = false;
        for (AttackerEntry aentry : attackers) {
            if (aentry.equals(attacker)) {
                attacker = aentry;
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            attackers.add(attacker);
        }
        int rDamage = Math.max(0, Math.min(damage, this.hp));
        attacker.addDamage(from, rDamage, updateAttackTime);
        this.hp -= rDamage;
        int remhppercentage = (int) Math.ceil((this.hp * 100.0) / getMaxHp());
        if (remhppercentage < 1) {
            remhppercentage = 1;
        }
        long okTime = System.currentTimeMillis() - 4000;
        if (hasBossHPBar()) {
            from.getMap().broadcastMessage(makeBossHPBarPacket(), getPosition());
        } else if (!isBoss()) {
            for (AttackerEntry mattacker : attackers) {
                for (AttackingMapleCharacter cattacker : mattacker.getAttackers()) {
                    if (cattacker.getAttacker().getMap() == from.getMap()) {
                        if (cattacker.getLastAttackTime() >= okTime) {
                            cattacker.getAttacker().getClient().getSession().write(MaplePacketCreator.showMonsterHP(getObjectId(), remhppercentage));
                        }
                    }
                }
            }
        }
    }

    public void heal(int hp, int mp) {
        int hp2Heal = getHp() + hp;
        int mp2Heal = getMp() + mp;
        if (hp2Heal >= getMaxHp()) {
            hp2Heal = getMaxHp();
        }
        if (mp2Heal >= getMaxMp()) {
            mp2Heal = getMaxMp();
        }
        setHp(hp2Heal);
        setMp(mp2Heal);
        getMap().broadcastMessage(MaplePacketCreator.healMonster(getObjectId(), hp));
    }

    public boolean isAttackedBy(MapleCharacter chr) {
        for (AttackerEntry aentry : attackers) {
            if (aentry.contains(chr)) {
                return true;
            }
        }
        return false;
    }

    public void giveExpToCharacter(MapleCharacter attacker, int exp, boolean highestDamage, int numExpSharers) {
        if (highestDamage) {
            if (eventInstance != null) {
                eventInstance.monsterKilled(attacker, this);
            }
            highestDamageChar = attacker;
        }
        if (attacker.getHp() > 0) {
            int personalExp = exp;
            if (exp > 0) {
                Integer holySymbol = attacker.getBuffedValue(MapleBuffStat.HOLY_SYMBOL);
                if (holySymbol != null) {
                    if (numExpSharers == 1) {
                        personalExp *= 1.0 + (holySymbol.doubleValue() / 500.0);
                    } else {
                        personalExp *= 1.0 + (holySymbol.doubleValue() / 100.0);
                    }
                }
            }
            if (exp < 0) {
                personalExp = Integer.MAX_VALUE;
            }
            attacker.gainExp(personalExp, true, false, highestDamage);
            attacker.mobKilled(this.getId());
        }
    }

    public MapleCharacter killBy(MapleCharacter killer) {
        long totalBaseExpL = this.getExp() * ChannelServer.getInstance(killer.getClient().getChannel()).getExpRate() * killer.getClient().getPlayer().hasEXPCard();
        int totalBaseExp = (int) (Math.min(Integer.MAX_VALUE, totalBaseExpL));
        AttackerEntry highest = null;
        int highdamage = 0;
        for (AttackerEntry attackEntry : attackers) {
            if (attackEntry.getDamage() > highdamage) {
                highest = attackEntry;
                highdamage = attackEntry.getDamage();
            }
        }
        for (AttackerEntry attackEntry : attackers) {
            int baseExp = (int) Math.ceil(totalBaseExp * ((double) attackEntry.getDamage() / getMaxHp()));
            attackEntry.killedMob(killer.getMap(), baseExp, attackEntry == highest);
        }
        if (this.getController() != null) { // this can/should only happen when a hidden gm attacks the monster
            getController().getClient().getSession().write(
                    MaplePacketCreator.stopControllingMonster(this.getObjectId()));
            getController().stopControllingMonster(this);
        }
        final List<Integer> toSpawn = this.getRevives();
        if (toSpawn != null) {
            final MapleMap reviveMap = killer.getMap();
            TimerManager.getInstance().schedule(() -> {
                for (Integer mid : toSpawn) {
                    MapleMonster mob = MapleLifeFactory.getMonster(mid);
                    if (eventInstance != null) {
                        eventInstance.registerMonster(mob);
                    }
                    mob.setPosition(getPosition());
                    if (dropsDisabled()) {
                        mob.disableDrops();
                    }
                    reviveMap.spawnRevives(mob);
                }
            }, this.getAnimationTime("die1"));
        }
        if (eventInstance != null) {
            eventInstance.unregisterMonster(this);
        }
        for (MonsterListener listener : listeners.toArray(new MonsterListener[0])) {
            listener.monsterKilled(this, highestDamageChar);
        }
        MapleCharacter ret = highestDamageChar;
        highestDamageChar = null; // may not keep hard references to chars outside of PlayerStorage or MapleMap
        return ret;
    }

    public boolean isAlive() {
        return this.hp > 0;
    }

    public MapleCharacter getController() {
        return controller.get();
    }

    public void setController(MapleCharacter controller) {
        this.controller = new WeakReference<>(controller);
    }

    public void switchController(MapleCharacter newController, boolean immediateAggro) {
        MapleCharacter controllers = getController();
        if (controllers == newController) {
            return;
        }
        if (controllers != null) {
            controllers.stopControllingMonster(this);
            controllers.getClient().getSession().write(MaplePacketCreator.stopControllingMonster(getObjectId()));
        }
        newController.controlMonster(this, immediateAggro);
        setController(newController);
        if (immediateAggro) {
            setControllerHasAggro(true);
        }
        setControllerKnowsAboutAggro(false);
    }

    public void addListener(MonsterListener listener) {
        listeners.add(listener);
    }

    public void removeListener(MonsterListener listener) {
        listeners.remove(listener);
    }

    public boolean isControllerHasAggro() {
        if (fake) {
            return false;
        }
        return controllerHasAggro;
    }

    public void setControllerHasAggro(boolean controllerHasAggro) {
        if (fake) {
            return;
        }
        this.controllerHasAggro = controllerHasAggro;
    }

    public boolean isControllerKnowsAboutAggro() {
        if (fake) {
            return false;
        }
        return controllerKnowsAboutAggro;
    }

    public void setControllerKnowsAboutAggro(boolean controllerKnowsAboutAggro) {
        if (fake) {
            return;
        }
        this.controllerKnowsAboutAggro = controllerKnowsAboutAggro;
    }

    public MaplePacket makeBossHPBarPacket() {
        return MaplePacketCreator.showBossHP(getId(), getHp(), getMaxHp(), getTagColor(), getTagBgColor());
    }

    public boolean hasBossHPBar() {
        return (isBoss() && getTagColor() > 0) || isHT();
    }

    private boolean isHT() {
        return this.getId() == 8810018;
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        if (!isAlive()) {
            return;
        }
        if (isFake()) {
            client.getSession().write(MaplePacketCreator.spawnFakeMonster(this, 0));
        } else {
            client.getSession().write(MaplePacketCreator.spawnMonster(this, false));
        }
        if (!stati.isEmpty()) {
            for (MonsterStatusEffect mse : activeEffects) {
                MaplePacket packet = MaplePacketCreator.applyMonsterStatus(getObjectId(), mse.getStati(), mse.getSkill().getId(), false, 0);
                client.getSession().write(packet);
            }
        }
        if (hasBossHPBar()) {
            client.getSession().write(makeBossHPBarPacket());
        }
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        client.getSession().write(MaplePacketCreator.killMonster(getObjectId(), false));
    }

    @Override
    public String toString() {
        return getName() + "(" + getId() + ") at " + getPosition().x + "/" + getPosition().y + " with " + getHp() + "/" + getMaxHp() +
                "hp, " + getMp() + "/" + getMaxMp() + " mp (alive: " + isAlive() + " oid: " + getObjectId() + ")";
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.MONSTER;
    }

    public EventInstanceManager getEventInstance() {
        return eventInstance;
    }

    public void setEventInstance(EventInstanceManager eventInstance) {
        this.eventInstance = eventInstance;
    }

    public boolean isMobile() {
        return stats.isMobile();
    }

    public ElementalEffectiveness getEffectiveness(Element e) {
        if (!activeEffects.isEmpty() && stati.get(MonsterStatus.DOOM) != null) {
            return ElementalEffectiveness.NORMAL; // like blue snails
        }
        return stats.getEffectiveness(e);
    }

    public boolean applyStatus(MapleCharacter from, final MonsterStatusEffect status, boolean poison, long duration) {
        return applyStatus(from, status, poison, duration, false);
    }

    public boolean applyStatus(MapleCharacter from, final MonsterStatusEffect status, boolean poison, long duration, boolean venom) {
        switch (stats.getEffectiveness(status.getSkill().getElement())) {
            case IMMUNE:
            case STRONG:
                return false;
            case NORMAL:
            case WEAK:
                break;
            default:
                throw new RuntimeException("Unknown elemental effectiveness: " + stats.getEffectiveness(status.getSkill().getElement()));
        }
        if (status.getSkill().getId() == 2111006) { // fp compo
            ElementalEffectiveness effectiveness = stats.getEffectiveness(Element.POISON);
            if (effectiveness == ElementalEffectiveness.IMMUNE || effectiveness == ElementalEffectiveness.STRONG) {
                return false;
            }
        } else if (status.getSkill().getId() == 2211006) { // il compo
            ElementalEffectiveness effectiveness = stats.getEffectiveness(Element.ICE);
            if (effectiveness == ElementalEffectiveness.IMMUNE || effectiveness == ElementalEffectiveness.STRONG) {
                return false;
            }
        } else if (status.getSkill().getId() == 4120005 || status.getSkill().getId() == 4220005) { // venom
            ElementalEffectiveness effectiveness = stats.getEffectiveness(Element.POISON);
            if (effectiveness == ElementalEffectiveness.WEAK) {
                return false;
            }
        }
        if (poison && getHp() <= 1) {
            return false;
        }
        if (isBoss() && !(status.getStati().containsKey(MonsterStatus.SPEED))) {
            return false;
        }
        for (MonsterStatus stat : status.getStati().keySet()) {
            MonsterStatusEffect oldEffect = stati.get(stat);
            if (oldEffect != null) {
                oldEffect.removeActiveStatus(stat);
                if (oldEffect.getStati().isEmpty()) {
                    oldEffect.getCancelTask().cancel(false);
                    oldEffect.cancelPoisonSchedule();
                    activeEffects.remove(oldEffect);
                }
            }
        }
        TimerManager timerManager = TimerManager.getInstance();
        final Runnable cancelTask = () -> {
            if (isAlive()) {
                MaplePacket packet = MaplePacketCreator.cancelMonsterStatus(getObjectId(), status.getStati());
                map.broadcastMessage(packet, getPosition());
                if (getController() != null && !getController().isMapObjectVisible(MapleMonster.this)) {
                    getController().getClient().getSession().write(packet);
                }
            }
            activeEffects.remove(status);
            for (MonsterStatus stat : status.getStati().keySet()) {
                stati.remove(stat);
            }
            setVenomMulti(0);
            status.cancelPoisonSchedule();
        };
        if (poison) {
            int poisonLevel = from.getSkillLevel(status.getSkill());
            int poisonDamage = Math.min(Short.MAX_VALUE, (int) (getMaxHp() / (70.0 - poisonLevel) + 0.999));
            status.setValue(MonsterStatus.POISON, poisonDamage);
            status.setPoisonSchedule(timerManager.register(new PoisonTask(poisonDamage, from, status, cancelTask, false), 1000, 1000));
        } else if (venom) {
            if (from.getJob() == MapleJob.NIGHTLORD || from.getJob() == MapleJob.SHADOWER) {
                int poisonLevel = 0;
                int matk = 0;
                if (from.getJob() == MapleJob.NIGHTLORD) {
                    poisonLevel = from.getSkillLevel(SkillFactory.getSkill(4120005));
                    if (poisonLevel <= 0) {
                        return false;
                    }
                    matk = SkillFactory.getSkill(4120005).getEffect(poisonLevel).getMatk();
                } else if (from.getJob() == MapleJob.SHADOWER) {
                    poisonLevel = from.getSkillLevel(SkillFactory.getSkill(4220005));
                    if (poisonLevel <= 0) {
                        return false;
                    }
                    matk = SkillFactory.getSkill(4220005).getEffect(poisonLevel).getMatk();
                } else {
                    return false;
                }
                Random r = new Random();
                int luk = from.getLuk();
                int maxDmg = (int) Math.ceil(Math.min(Short.MAX_VALUE, 0.2 * luk * matk));
                int minDmg = (int) Math.ceil(Math.min(Short.MAX_VALUE, 0.1 * luk * matk));
                int gap = maxDmg - minDmg;
                if (gap == 0) {
                    gap = 1;
                }
                int poisonDamage = 0;
                for (int i = 0; i < getVenomMulti(); i++) {
                    poisonDamage = poisonDamage + (r.nextInt(gap) + minDmg);
                }
                poisonDamage = Math.min(Short.MAX_VALUE, poisonDamage);
                status.setValue(MonsterStatus.POISON, poisonDamage);
                status.setPoisonSchedule(timerManager.register(new PoisonTask(poisonDamage, from, status, cancelTask, false), 1000, 1000));
            } else {
                return false;
            }
        } else if (status.getSkill().getId() == 4111003) { // shadow web
            int webDamage = (int) (getMaxHp() / 50.0 + 0.999);
            status.setPoisonSchedule(timerManager.schedule(new PoisonTask(webDamage, from, status, cancelTask, true), 3500));
        }
        for (MonsterStatus stat : status.getStati().keySet()) {
            stati.put(stat, status);
        }
        activeEffects.add(status);
        int animationTime = status.getSkill().getAnimationTime();
        MaplePacket packet = MaplePacketCreator.applyMonsterStatus(getObjectId(), status.getStati(), status.getSkill().getId(), false, 0);
        map.broadcastMessage(packet, getPosition());
        if (getController() != null && !getController().isMapObjectVisible(this)) {
            getController().getClient().getSession().write(packet);
        }
        ScheduledFuture<?> schedule = timerManager.schedule(cancelTask, duration + animationTime);
        status.setCancelTask(schedule);
        return true;
    }

    public void applyMonsterBuff(final MonsterStatus status, final int x, int skillId, long duration, MobSkill skill) {
        TimerManager timerManager = TimerManager.getInstance();
        final Runnable cancelTask = () -> {
            if (isAlive()) {
                MaplePacket packet = MaplePacketCreator.cancelMonsterStatus(getObjectId(), Collections.singletonMap(status, x));
                map.broadcastMessage(packet, getPosition());
                if (getController() != null && !getController().isMapObjectVisible(MapleMonster.this)) {
                    getController().getClient().getSession().write(packet);
                }
                removeMonsterBuff(status);
            }
        };
        MaplePacket packet = MaplePacketCreator.applyMonsterStatus(getObjectId(), Collections.singletonMap(status, x), skillId, true, 0, skill);
        map.broadcastMessage(packet, getPosition());
        if (getController() != null && !getController().isMapObjectVisible(this)) {
            getController().getClient().getSession().write(packet);
        }
        timerManager.schedule(cancelTask, duration);
        addMonsterBuff(status);
    }

    public void addMonsterBuff(MonsterStatus status) {
        this.monsterBuffs.add(status);
    }

    public void removeMonsterBuff(MonsterStatus status) {
        this.monsterBuffs.remove(status);
    }

    public boolean isBuffed(MonsterStatus status) {
        return this.monsterBuffs.contains(status);
    }

    public boolean isFake() {
        return fake;
    }

    public void setFake(boolean fake) {
        this.fake = fake;
    }

    public MapleMap getMap() {
        return map;
    }

    public void setMap(MapleMap map) {
        this.map = map;
    }

    public List<Pair<Integer, Integer>> getSkills() {
        return this.stats.getSkills();
    }

    public boolean hasSkill(int skillId, int level) {
        return stats.hasSkill(skillId, level);
    }

    public boolean canUseSkill(MobSkill toUse) {
        if (toUse == null) {
            return false;
        }
        for (Pair<Integer, Integer> skill : usedSkills) {
            if (skill.left() == toUse.getSkillId() && skill.right() == toUse.getSkillLevel()) {
                return false;
            }
        }
        if (toUse.getLimit() > 0) {
            if (this.skillsUsed.containsKey(new Pair<>(toUse.getSkillId(), toUse.getSkillLevel()))) {
                int times = this.skillsUsed.get(new Pair<>(toUse.getSkillId(), toUse.getSkillLevel()));
                if (times >= toUse.getLimit()) {
                    return false;
                }
            }
        }
        if (toUse.getSkillId() == 200) {
            Collection<MapleMapObject> mmo = getMap().getMapObjects();
            int i = 0;
            for (MapleMapObject mo : mmo) {
                if (mo.getType() == MapleMapObjectType.MONSTER) {
                    i++;
                }
            }
            return i <= 100;
        }
        return true;
    }

    public void usedSkill(final int skillId, final int level, long cooltime) {
        this.usedSkills.add(new Pair<>(skillId, level));
        if (this.skillsUsed.containsKey(new Pair<>(skillId, level))) {
            int times = this.skillsUsed.get(new Pair<>(skillId, level)) + 1;
            this.skillsUsed.remove(new Pair<>(skillId, level));
            this.skillsUsed.put(new Pair<>(skillId, level), times);
        } else {
            this.skillsUsed.put(new Pair<>(skillId, level), 1);
        }
        final MapleMonster mons = this;
        TimerManager tMan = TimerManager.getInstance();
        tMan.schedule(() -> mons.clearSkill(skillId, level), cooltime);
    }

    public void clearSkill(int skillId, int level) {
        int index = -1;
        for (Pair<Integer, Integer> skill : usedSkills) {
            if (skill.left() == skillId && skill.right() == level) {
                index = usedSkills.indexOf(skill);
                break;
            }
        }
        if (index != -1) {
            usedSkills.remove(index);
        }
    }

    public int getNoSkills() {
        return this.stats.getNoSkills();
    }

    public boolean isFirstAttack() {
        return this.stats.isFirstAttack();
    }

    public int getBuffToGive() {
        return this.stats.getBuffToGive();
    }

    public String getName() {
        return stats.getName();
    }

    private interface AttackerEntry {

        List<AttackingMapleCharacter> getAttackers();

        void addDamage(MapleCharacter from, int damage, boolean updateAttackTime);

        int getDamage();

        boolean contains(MapleCharacter chr);

        void killedMob(MapleMap map, int baseExp, boolean mostDamage);
    }

    private static class OnePartyAttacker {

        public MapleParty lastKnownParty;
        public int damage;
        public long lastAttackTime;

        public OnePartyAttacker(MapleParty lastKnownParty, int damage) {
            super();
            this.lastKnownParty = lastKnownParty;
            this.damage = damage;
            this.lastAttackTime = System.currentTimeMillis();
        }
    }

    private final class PoisonTask implements Runnable {

        private final int poisonDamage;
        private final MapleCharacter chr;
        private final MonsterStatusEffect status;
        private final Runnable cancelTask;
        private final boolean shadowWeb;
        private final MapleMap map;

        private PoisonTask(int poisonDamage, MapleCharacter chr, MonsterStatusEffect status, Runnable cancelTask, boolean shadowWeb) {
            this.poisonDamage = poisonDamage;
            this.chr = chr;
            this.status = status;
            this.cancelTask = cancelTask;
            this.shadowWeb = shadowWeb;
            this.map = chr.getMap();
        }

        @Override
        public void run() {
            int damage = poisonDamage;
            if (damage >= hp) {
                damage = hp - 1;
                if (!shadowWeb) {
                    cancelTask.run();
                    status.getCancelTask().cancel(false);
                }
            }
            if (hp > 1 && damage > 0) {
                damage(chr, damage, false);
                if (shadowWeb) {
                    map.broadcastMessage(MaplePacketCreator.damageMonster(getObjectId(), damage), getPosition());
                }
            }
        }
    }

    private class AttackingMapleCharacter {

        private MapleCharacter attacker;
        private long lastAttackTime;

        public AttackingMapleCharacter(MapleCharacter attacker, long lastAttackTime) {
            super();
            this.attacker = attacker;
            this.lastAttackTime = lastAttackTime;
        }

        public long getLastAttackTime() {
            return lastAttackTime;
        }

        public void setLastAttackTime(long lastAttackTime) {
            this.lastAttackTime = lastAttackTime;
        }

        public MapleCharacter getAttacker() {
            return attacker;
        }
    }

    private class SingleAttackerEntry implements AttackerEntry {

        private int damage;
        private int chrid;
        private long lastAttackTime;
        private ChannelServer cserv;

        public SingleAttackerEntry(MapleCharacter from, ChannelServer cserv) {
            this.chrid = from.getId();
            this.cserv = cserv;
        }

        @Override
        public void addDamage(MapleCharacter from, int damage, boolean updateAttackTime) {
            if (chrid == from.getId()) {
                this.damage += damage;
            } else {
                throw new IllegalArgumentException("Not the attacker of this entry");
            }
            if (updateAttackTime) {
                lastAttackTime = System.currentTimeMillis();
            }
        }

        @Override
        public List<AttackingMapleCharacter> getAttackers() {
            return cserv.getPlayerStorage().getCharacterById(chrid)
                    .map(c -> new AttackingMapleCharacter(c, lastAttackTime))
                    .map(Collections::singletonList)
                    .orElse(Collections.emptyList());
        }

        @Override
        public boolean contains(MapleCharacter chr) {
            return chrid == chr.getId();
        }

        @Override
        public int getDamage() {
            return damage;
        }

        @Override
        public void killedMob(MapleMap map, int baseExp, boolean mostDamage) {
            cserv.getPlayerStorage().getCharacterById(chrid)
                    .filter(MapleCharacter.inMap(map))
                    .ifPresent(c -> giveExpToCharacter(c, baseExp, mostDamage, 1));
        }

        @Override
        public int hashCode() {
            return chrid;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final SingleAttackerEntry other = (SingleAttackerEntry) obj;
            return chrid == other.chrid;
        }
    }

    private class PartyAttackerEntry implements AttackerEntry {

        private int totDamage;
        private final Map<Integer, OnePartyAttacker> attackers;
        private final int partyid;
        private final ChannelServer cserv;

        public PartyAttackerEntry(int partyid, ChannelServer cserv) {
            this.partyid = partyid;
            this.cserv = cserv;
            attackers = new HashMap<>(6);
        }

        public List<AttackingMapleCharacter> getAttackers() {
            List<AttackingMapleCharacter> ret = new ArrayList<>(attackers.size());
            for (Entry<Integer, OnePartyAttacker> entry : attackers.entrySet()) {
                cserv.getPlayerStorage().getCharacterById(entry.getKey()).ifPresent(c -> ret.add(new AttackingMapleCharacter(c, entry.getValue().lastAttackTime)));
            }
            return ret;
        }

        private Map<MapleCharacter, OnePartyAttacker> resolveAttackers() {
            Map<MapleCharacter, OnePartyAttacker> ret = new HashMap<>(attackers.size());
            for (Entry<Integer, OnePartyAttacker> aentry : attackers.entrySet()) {
                cserv.getPlayerStorage().getCharacterById(aentry.getKey()).ifPresent(c -> ret.put(c, aentry.getValue()));
            }
            return ret;
        }

        @Override
        public boolean contains(MapleCharacter chr) {
            return attackers.containsKey(chr.getId());
        }

        @Override
        public int getDamage() {
            return totDamage;
        }

        public void addDamage(MapleCharacter from, int damage, boolean updateAttackTime) {
            OnePartyAttacker oldPartyAttacker = attackers.get(from.getId());
            if (oldPartyAttacker != null) {
                oldPartyAttacker.damage += damage;
                oldPartyAttacker.lastKnownParty = from.getParty();
                if (updateAttackTime) {
                    oldPartyAttacker.lastAttackTime = System.currentTimeMillis();
                }
            } else {
                // TODO actually this causes wrong behaviour when the party changes between attacks
                // only the last setup will get exp - but otherwise we'd have to store the full party
                // constellation for every attack/everytime it changes, might be wanted/needed in the
                // future but not now
                OnePartyAttacker onePartyAttacker = new OnePartyAttacker(from.getParty(), damage);
                attackers.put(from.getId(), onePartyAttacker);
                if (!updateAttackTime) {
                    onePartyAttacker.lastAttackTime = 0;
                }
            }
            totDamage += damage;
        }

        @Override
        public void killedMob(MapleMap map, int baseExp, boolean mostDamage) {
            Map<MapleCharacter, OnePartyAttacker> attackers_ = resolveAttackers();
            MapleCharacter highest = null;
            int highestDamage = 0;
            Map<MapleCharacter, Integer> expMap = new ArrayMap<>(6);
            for (Entry<MapleCharacter, OnePartyAttacker> attacker : attackers_.entrySet()) {
                MapleParty party = attacker.getValue().lastKnownParty;
                double averagePartyLevel = 0;
                List<MapleCharacter> expApplicable = new ArrayList<>();
                for (MaplePartyCharacter partychar : party.getMembers()) {
                    if (attacker.getKey().getLevel() - partychar.getLevel() <= 5 ||
                            getLevel() - partychar.getLevel() <= 5) {
                        Optional<MapleCharacter> pc = cserv.getPlayerStorage().getCharacterByName(partychar.getName())
                                .filter(MapleCharacter::isAlive)
                                .filter(MapleCharacter.inMap(map));
                        if (pc.isPresent()) {
                            expApplicable.add(pc.get());
                            averagePartyLevel += pc.get().getLevel();
                        }
                    }
                }
                double expBonus = 1.0;
                if (expApplicable.size() > 1) {
                    expBonus = 1.10 + 0.05 * expApplicable.size();
                    averagePartyLevel /= expApplicable.size();
                }
                int iDamage = attacker.getValue().damage;
                if (iDamage > highestDamage) {
                    highest = attacker.getKey();
                    highestDamage = iDamage;
                }
                double innerBaseExp = baseExp * ((double) iDamage / totDamage);
                double expFraction = (innerBaseExp * expBonus) / (expApplicable.size() + 1);
                for (MapleCharacter expReceiver : expApplicable) {
                    Integer oexp = expMap.get(expReceiver);
                    int iexp;
                    iexp = Objects.requireNonNullElse(oexp, 0);
                    double expWeight = (expReceiver == attacker.getKey() ? 2.0 : 1.0);
                    double levelMod = expReceiver.getLevel() / averagePartyLevel;
                    if (levelMod > 1.0 || this.attackers.containsKey(expReceiver.getId())) {
                        levelMod = 1.0;
                    }
                    iexp += (int) Math.round(expFraction * expWeight * levelMod);
                    expMap.put(expReceiver, iexp);
                }
            }
            for (Entry<MapleCharacter, Integer> expReceiver : expMap.entrySet()) {
                boolean white = mostDamage ? expReceiver.getKey() == highest : false;
                giveExpToCharacter(expReceiver.getKey(), expReceiver.getValue(), white, expMap.size());
            }
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + partyid;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final PartyAttackerEntry other = (PartyAttackerEntry) obj;
            return partyid == other.partyid;
        }
    }
}