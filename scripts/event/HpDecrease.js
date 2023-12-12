const Calendar = Java.type('java.util.Calendar');

const System = Java.type('java.lang.System');

var setupTask;

function init() {
    hpInterval();
}

function hpInterval() {
    var cal = Calendar.getInstance();
    cal.set(Calendar.SECOND, 5);
    var nextTime = cal.getTimeInMillis();
    while (nextTime <= System.currentTimeMillis()) {
        nextTime += 5000;
    }
    setupTask = em.scheduleAtTimestamp("decrease", nextTime);
}

function cancelSchedule() {
    setupTask.cancel(true);
}

function decrease() {
    hpInterval();
    var iter = em.getChannelServer().getPlayerStorage().getAllCharacters().iterator();

    while (iter.hasNext()) {
            var pl = iter.next();
            if (pl.getHp() > 0 && !pl.inCS() && pl.getMap().getHPdecrease() > 0)
            {
                pl.setHp(pl.getHp()-pl.getMap().getHPdecrease());
                pl.updateSingleStat(MapleStat.HP, pl.getHp());
            }
    }
}