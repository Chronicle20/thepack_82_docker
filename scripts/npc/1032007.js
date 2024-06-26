/**
-- Odin JavaScript --------------------------------------------------------------------------------
	Joel  - Ellinia Station(101000300)
-- By ---------------------------------------------------------------------------------------------
	Information
-- Version Info -----------------------------------------------------------------------------------
	1.2 - Price as GMS [Sadiq]
	1.1 - Changed Price as GMS by Shogi
	1.0 - First Version by Information
---------------------------------------------------------------------------------------------------
**/

var cost = 5000;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if(mode == -1)
        cm.dispose();
    else {
        if(mode == 1)
            status++;
        if(mode == 0) {
            cm.sendNext("You must have some business to take care of here, right?");
            cm.dispose();
            return;
        }
        if(status == 0) 
            cm.sendYesNo("Hello, I'm in charge of selling tickets for the ship ride to Orbis Station of Ossyria. The ride to Orbis takes off every 15 minutes, beginning on the hour, and it'll cost you #b"+cost+" mesos#k. Are you sure you want to purchase #b#t4031045##k?");
        else if(status == 1) {
            if(cm.getMeso() >= cost && cm.canHold(4031045)) {
                cm.gainItem(4031045,1);
                cm.gainMeso(-cost);
                cm.dispose();
            } else {
                cm.sendOk("Are you sure you have #b"+cost+" mesos#k? If so, then I urge you to check your etc. inventory, and see if it's full or not.");
                cm.dispose();
            }
        }
    }
}
