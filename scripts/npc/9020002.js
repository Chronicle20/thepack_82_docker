/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc> 
                       Matthias Butz <matze@odinms.de>
                       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License version 3
    as published by the Free Software Foundation. You may not use, modify
    or distribute this program under any other version of the
    GNU Affero General Public License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

/* Nella
 * 
 * Hidden Street : 1st Accompaniment <1st Stage> (103000800)
 ~ 2nd, 3rd, 4th (801, 802, 803)
 * Hidden Street : 1st Accompaniment <Last Stage> (103000804)
 * Hidden Street : 1st Accompaniment <Bonus> (103000805)
 * Hidden Street : 1st Accompaniment <Exit> (103000890)
 * 
 * Kerning City Party Quest NPC 
*/

var status;

function start() {
    status = -1
    action(1,0,0);
}

function action(mode, type, selection){
    if (mode == -1) {
        cm.dispose();
    }
    if (mode == 0 && status == 0) {
        cm.dispose();
        return;
    }
    else {
        if (mode == 1) {
            status++;
        }
        else
            status--;
        var mapId = cm.getChar().getMapId();
        if (mapId == 103000890) {
            if (status == 0) {
                cm.sendNext("See you next time.");
            }
            else {
                cm.warp(103000000,"mid00");
                cm.removeAll(4001007);
                cm.removeAll(4001008);
                cm.dispose();
            }
        }
        else {
            var outText = "Once you leave the map, you'll have to restart the whole quest if you want to try it again.  Do you still want to leave this map?";
            if (mapId == 103000805) {
                outText = "Are you ready to leave this map?";
            }
            if (status == 0) {
                cm.sendYesNo(outText);
            }
            else if (mode == 1) {
                // Remove them from the PQ!
                var eim = cm.getChar().getEventInstance();
                if (eim == null)
                    // warp player
                    cm.warp(103000890,"st00");
                else if (cm.isLeader())
                    eim.disbandParty();
                else
                    eim.leftParty(cm.getChar());
                cm.dispose();
            }
            else {
                cm.dispose();
            }
        }
    }
}
