//carta
const MapleQuest = Java.type('net.sf.odinms.server.quest.MapleQuest');
const MapleQuestStatus = Java.type('net.sf.odinms.client.MapleQuestStatus');

function start(){
    if(cm.getChar().getQuest(MapleQuest.getInstance(6301)).getStatus().equals(MapleQuestStatus.Status.STARTED)) {
        if (cm.haveItem(4000175))
            cm.warp(923000000)
        else
            cm.sendOk("In order to open the crack of dimension you will have to posess one piece of Miniature Pianus. Those could be gained by defeating a Pianus.");
    } else
        cm.sendOk("I'm #bCarta the sea-witch.#k Don't fool around with me, as I'm known for my habit of turning people into worms.");
    cm.dispose();
}