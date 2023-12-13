package net.sf.odinms.server.fourthjobquests;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleJob;
import net.sf.odinms.client.messages.ServernoticeMapleClientMessageCallback;
import net.sf.odinms.net.StringValueHolder;
import net.sf.odinms.net.world.MaplePartyCharacter;
import net.sf.odinms.tools.MaplePacketCreator;

/**
 * @author AngelSL
 */

public class FourthJobQuestsPortalHandler {

    private FourthJobQuestsPortalHandler() {
    }

    public static boolean handlePortal(String name, MapleCharacter c) {
        ServernoticeMapleClientMessageCallback snmcmc = new ServernoticeMapleClientMessageCallback(5, c.getClient());
        if (name.equals(FourthJobQuests.RUSH.getValue())) {
            if (!checkPartyLeader(c) && !checkRush(c)) {
                snmcmc.dropMessage("You step into the portal, but it swiftly kicks you out.");
                c.getClient().getSession().write(MaplePacketCreator.enableActions());
                return true;
            }
            if (!checkPartyLeader(c) && checkRush(c)) {
                snmcmc.dropMessage("You're not the party leader.");
                c.getClient().getSession().write(MaplePacketCreator.enableActions());
                return true;
            }
            if (!checkRush(c)) {
                snmcmc.dropMessage("Someone in your party is not a 4th Job warrior.");
                c.getClient().getSession().write(MaplePacketCreator.enableActions());
                return true;
            }
            c.getClient().getChannelServer().getEventSM().getEventManager("4jrush").startInstance(c.getParty(), c.getMap());
            return true;
        }

        if (!name.equals(FourthJobQuests.BERSERK.getValue())) {
            return false;
        }

        if (!checkBerserk(c)) {
            snmcmc.dropMessage("The portal to the Forgotten Shrine is locked");
            c.getClient().getSession().write(MaplePacketCreator.enableActions());
            return true;
        }

        c.getClient().getChannelServer().getEventSM().getEventManager("4jberserk").startInstance(c.getParty(), c.getMap());
        return true;
    }

    private static boolean checkRush(MapleCharacter c) {
        for (MaplePartyCharacter mpc : c.getParty().getMembers()) {
            if (!MapleJob.getById(mpc.getJobId()).orElse(MapleJob.BEGINNER).isA(MapleJob.WARRIOR)) {
                return false;
            }
            if (!(MapleJob.getById(mpc.getJobId()).orElse(MapleJob.BEGINNER).isA(MapleJob.HERO) || MapleJob.getById(mpc.getJobId()).orElse(MapleJob.BEGINNER).isA(MapleJob.PALADIN) || MapleJob.getById(mpc.getJobId()).orElse(MapleJob.BEGINNER).isA(MapleJob.DARKKNIGHT))) {
                return false;
            }
        }
        return true;
    }

    private static boolean checkPartyLeader(MapleCharacter c) {
        return c.getParty().getLeader().getId() == c.getId();
    }

    private static boolean checkBerserk(MapleCharacter c) {
        return c.haveItem(4031475, 1, false, true);
    }

    public enum FourthJobQuests implements StringValueHolder {
        RUSH("s4rush"),
        BERSERK("s4berserk");
        private final String name;

        FourthJobQuests(String Newname) {
            this.name = Newname;
        }

        @Override
        public String getValue() {
            return name;
        }
    }
}
