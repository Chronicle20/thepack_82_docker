/* Dark Lord
	Thief Job Advancement
	Victoria Road : Thieves' Hideout (103000003)

	Custom Quest 100009, 100011
*/

var status = 0;
var job;


const MapleQuestStatus = Java.type('net.sf.odinms.client.MapleQuestStatus');
const MapleJob = Java.type('net.sf.odinms.client.MapleJob');

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) 
        cm.dispose();
    else {
        if (mode == 0 && status == 2) {
            cm.sendOk("You know there is no other choice...");
            cm.dispose();
            return;
        }
        if (mode == 1)
            status++;
        else
            status--;
        if (status == 0) {
            if (cm.getJob().equals(MapleJob.BEGINNER)) {
                if (cm.getLevel() >= 10 && cm.getChar().getDex() >= 25)
                    cm.sendNext("So you decided to become a #rThief#k?");
                else {
                    cm.sendOk("Train a bit more and I can show you the way of the #rThief#k.")
                    cm.dispose();
                }
            } else {
                if (cm.getLevel() >= 30 && cm.getJob().equals(MapleJob.THIEF)) {
                    if (cm.getQuestStatus(100009).getId() >= MapleQuestStatus.Status.STARTED.getId()) {
                        cm.completeQuest(100011);
                        if (cm.getQuestStatus(100011) == MapleQuestStatus.Status.COMPLETED) {
                            status = 20;
                            cm.sendNext("I see you have done well. I will allow you to take the next step on your long road.");
                        } else {
                            cm.sendOk("Go and see the #rJob Instructor#k.")
                            cm.dispose();
                        }
                    } else {
                        status = 10;
                        cm.sendNext("The progress you have made is astonishing.");
                    }
                } else if (cm.getQuestStatus(100100).equals(MapleQuestStatus.Status.STARTED)) {
                    cm.completeQuest(100101);
                    if (cm.getQuestStatus(100101).equals(MapleQuestStatus.Status.COMPLETED)) {
                        cm.sendOk("Alright, now take this to #bArec#k.");
                    } else {
                        cm.sendOk("Hey, " + cm.getChar().getName() + "! I need a #bBlack Charm#k. Go and find the Door of Dimension.");
                        cm.startQuest(100101);
                    }
                    cm.dispose();
                } else {
                    cm.sendOk("You have chosen wisely.");
                    cm.dispose();
                }
            }
        } else if (status == 1)
            cm.sendNextPrev("It is an important and final choice. You will not be able to turn back.");
        else if (status == 2)
            cm.sendYesNo("Do you want to become a #rThief#k?");
        else if (status == 3) {
            if (cm.getJob().equals(MapleJob.BEGINNER))
                cm.changeJob(MapleJob.THIEF);
            cm.gainItem(1472000,1);
            cm.gainItem(2070015,500);
            cm.sendOk("So be it! Now go, and go with pride.");
            cm.dispose();
        } else if (status == 11)
            cm.sendNextPrev("You may be ready to take the next step as a #rAssassin#k or #rBandit#k.");
        else if (status == 12)
            cm.sendAcceptDecline("But first I must test your skills. Are you ready?");
        else if (status == 13) {
            if (cm.haveItem(4031011))
                cm.sendOk("Please report this bug at http://odinms.de/forum/.\r\nstatus = 13");
            else {
                cm.startQuest(100009);
                cm.sendOk("Go see the #bJob Instructor#k somewhere in the city. He will show you the way.");
            }
        } else if (status == 21)
            cm.sendSimple("What do you want to become?#b\r\n#L0#Assassin#l\r\n#L1#Bandit#l#k");
        else if (status == 22) {
            var jobName;
            if (selection == 0) {
                jobName = "Assassin";
                job = MapleJob.ASSASSIN;
            } else {
                jobName = "Bandit";
                job = MapleJob.BANDIT;
            }
            cm.sendYesNo("Do you want to become a #r" + jobName + "#k?");
        } else if (status == 23) {
            cm.changeJob(job);
            cm.sendOk("So be it! Now go, my servant.");
        }
    }
}	
