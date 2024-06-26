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

/* Shumi JQ Chest #1
*/
const MapleQuestStatus = Java.type('net.sf.odinms.client.MapleQuestStatus');

var status = 0;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    var prizes = Array(1040045, 1040055, 1040129, 1040137, 1041109, 1041009, 1041134, 1041132, 1041005, 1041138, 1042018, 1042035, 1042038, 1042024, 1042002, 1702000, 1702027);
    var chances = Array(10, 10, 10, 5, 10, 10, 10, 10, 10, 5, 10, 10, 10, 10, 10, 5, 3);
    var totalodds = 0;
    var choice = 0;
    for (var i = 0; i < chances.length; i++) {
        var itemGender = (Math.floor(prizes[i]/1000)%10);
        if ((cm.getChar().getGender() != itemGender) && (itemGender != 2))
            chances[i] = 0;
    }
    for (var i = 0; i < chances.length; i++)
        totalodds += chances[i];
    var randomPick = Math.floor(Math.random()*totalodds)+1;
    for (var i = 0; i < chances.length; i++) {
        randomPick -= chances[i];
        if (randomPick <= 0) {
            choice = i;
            randomPick = totalodds + 100;
        }
    }
    if (cm.getQuestStatus(2055).equals(MapleQuestStatus.Status.STARTED))
        cm.gainItem(4031039,1);
    cm.gainItem(prizes[choice],1);
    cm.warp(103000100, 0);
    cm.dispose();
}