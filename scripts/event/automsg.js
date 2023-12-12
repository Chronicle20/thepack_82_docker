const MaplePacketCreator = Java.type('net.sf.odinms.tools.MaplePacketCreator');
const Calendar = Java.type('java.util.Calendar');
const System = Java.type('java.lang.System');

var setupTask;

function init() {
    scheduleNew();
}

function scheduleNew() {
    var cal = Calendar.getInstance();
    cal.set(Calendar.HOUR, 3);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    var nextTime = cal.getTimeInMillis();
    while (nextTime <= System.currentTimeMillis())
        nextTime += 1000*600; //10 min
    setupTask = em.scheduleAtTimestamp("start", nextTime);
}

function cancelSchedule() {
    setupTask.cancel(true);
}

function start() {
    scheduleNew();
    var Message = new Array("The sum of the base-10 logarithms of the divisors of 10^n is 792. What is n?");
    em.getChannelServer().broadcastPacket(MaplePacketCreator.serverNotice(5, "[Tip] : " + Message[Math.floor(Math.random() * Message.length)]));
    var iter = em.getInstances().iterator();
    while (iter.hasNext())
        var eim = iter.next();
}