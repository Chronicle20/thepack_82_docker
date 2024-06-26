/**
-- Odin JavaScript --------------------------------------------------------------------------------
	Hotel Receptionist - Sleepywood Hotel(105040400)
-- By ---------------------------------------------------------------------------------------------
	Unknown
-- Version Info -----------------------------------------------------------------------------------
        1.2 - Cleanup and Statement fix by Moogra
	1.1 - Statement fix [Information]
	1.0 - First Version by Unknown
---------------------------------------------------------------------------------------------------
**/

var status = 0;
var regcost = 499;
var vipcost = 999;
var iwantreg = 0;
var iwantvip = 0;
var tempvar;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1)
        cm.dispose();
    else {
        if (mode == 1)
            status++; if (mode == 0 && status == 1) {
            cm.dispose();
            return;
        } if (mode == 0 && status == 2) {
            cm.sendNext("We offer other kinds of services, too, so please think carefully and then make your decision.");
            cm.dispose();
            return;
        }
        if (status == 0)
            cm.sendNext("Welcome. We're the Sleepywood Hotel. Our hotel works hard to serve you the best at all times. If you are tired and worn out from hunting, how about a relaxing stay at our hotel?");
        else if (status == 1)
{            cm.sendSimple("We offer two kinds of rooms for our service. Please choose the one of your liking.\r\n#b#L0#Regular sauna (" + regcost + " mesos per use)#l\r\n#L1#VIP sauna (" + vipcost + " mesos per use)#l");
	        iwantreg = 1; }
        else if (status == 2) {
            if (selection == 0)
                cm.sendYesNo("You have chosen the regular sauna. Your HP and MP will recover fast and you can even purchase some items there. Are you sure you want to go in?");
            else if (selection == 1)
{                cm.sendYesNo("You've chosen the VIP sauna. Your HP and MP will recover even faster than that of the regular sauna and you can even find a special item in there. Are you sure you want to go in?");
	        iwantvip = 1; }
		        }
        else if (status == 3) {
            if (iwantreg == 1) {
                if (cm.getMeso() >= regcost) {
                    cm.warp(105040401);
                    cm.gainMeso(-regcost);
                } else
                    cm.sendNext("I'm sorry. It looks like you don't have enough mesos. It will cost you at least " + regcost + "mesos to stay at our hotel.");
            } else if (iwantvip == 1) {
                if (cm.getMeso() >= vipcost) {
                    cm.warp(105040402);
                    cm.gainMeso(-vipcost);
                } else
                    cm.sendNext("I'm sorry. It looks like you don't have enough mesos. It will cost you at least " + vipcost + "mesos to stay at our hotel.");
            }
            cm.dispose();
        }
    }
}