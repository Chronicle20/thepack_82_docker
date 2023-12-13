package net.sf.odinms.net.world.guild;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.channel.remote.ChannelWorldInterface;
import net.sf.odinms.net.world.WorldRegistryImpl;
import net.sf.odinms.tools.MaplePacketCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

public class MapleGuild implements java.io.Serializable {

    public final static int CREATE_GUILD_COST = 1500000;
    public final static int CHANGE_EMBLEM_COST = 5000000;
    public final static int INCREASE_CAPACITY_COST = 500000;
    public final static boolean ENABLE_BBS = true;
    public static final long serialVersionUID = 6322150443228168192L;
    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MapleGuild.class);
    private List<MapleGuildCharacter> members;
    private String[] rankTitles = new String[5]; // 1 = master, 2 = jr, 5 = lowest member
    private String name;
    private int id;
    private int gp;
    private int logo;
    private int logoColor;
    private int leader;
    private int capacity;
    private int logoBG;
    private int logoBGColor;
    private String notice;
    private int signature;
    private Map<Integer, List<Integer>> notifications = new LinkedHashMap<>();
    private boolean bDirty = true;

    public MapleGuild(MapleGuildCharacter initiator) {
        int guildid = initiator.getGuildId();
        members = new ArrayList<>();
        Logger log = LoggerFactory.getLogger(this.getClass());
        Connection con;
        try {
            con = DatabaseConnection.getConnection();
        } catch (Exception e) {
            log.error("unable to connect to database to load guild information.", e);
            return;
        }
        try {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM guilds WHERE guildid=" + guildid);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                id = -1;
                return;
            }
            id = guildid;
            name = rs.getString("name");
            gp = rs.getInt("GP");
            logo = rs.getInt("logo");
            logoColor = rs.getInt("logoColor");
            logoBG = rs.getInt("logoBG");
            logoBGColor = rs.getInt("logoBGColor");
            capacity = rs.getInt("capacity");
            for (int i = 1; i <= 5; i++) {
                rankTitles[i - 1] = rs.getString("rank" + i + "title");
            }
            leader = rs.getInt("leader");
            notice = rs.getString("notice");
            signature = rs.getInt("signature");
            ps.close();
            rs.close();
            ps = con.prepareStatement("SELECT id, name, level, job, guildrank FROM characters WHERE guildid = ? ORDER BY guildrank ASC, name ASC");
            ps.setInt(1, guildid);
            rs = ps.executeQuery();
            if (!rs.next()) {
                log.error("No members in guild.  Impossible...");
                return;
            }
            do {
                members.add(new MapleGuildCharacter(rs.getInt("id"), rs.getInt("level"), rs.getString("name"), -1,
                        rs.getInt("job"), rs.getInt("guildrank"), guildid, false));
            } while (rs.next());
            setOnline(initiator.getId(), true, initiator.getChannel());
            ps.close();
            rs.close();
        } catch (SQLException se) {
            log.error("unable to read guild information from sql", se);
        }
    }

    // function to create guild, returns the guild id if successful, 0 if not
    public static int createGuild(int leaderId, String name) {
        Connection con;
        try {
            Properties dbProp = new Properties();
            InputStreamReader is = new FileReader("db.properties");
            dbProp.load(is);
            con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT guildid FROM guilds WHERE name = ?");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.first()) // name taken
            {
                return 0;
            }
            ps.close();
            rs.close();
            ps = con.prepareStatement("INSERT INTO guilds (`leader`, `name`, `signature`) VALUES (?, ?, ?)");
            ps.setInt(1, leaderId);
            ps.setString(2, name);
            ps.setInt(3, (int) System.currentTimeMillis());
            ps.execute();
            ps.close();
            ps = con.prepareStatement("SELECT guildid FROM guilds WHERE leader = ?");
            ps.setInt(1, leaderId);
            rs = ps.executeQuery();
            rs.next();
            return rs.getInt("guildid");
        } catch (SQLException se) {
            log.error("SQL THROW", se);
            return 0;
        } catch (Exception e) {
            log.error("CREATE GUILD THROW", e);
            return 0;
        }
    }

    public static MapleGuildResponse sendInvite(MapleClient c, String targetName) {
        Optional<MapleCharacter> mc = c.getChannelServer().getPlayerStorage().getCharacterByName(targetName);

        if (mc.isEmpty()) {
            return MapleGuildResponse.NOT_IN_CHANNEL;
        }
        if (mc.get().getGuildId() > 0) {
            return MapleGuildResponse.ALREADY_IN_GUILD;
        }
        mc.get().getClient().getSession().write(MaplePacketCreator.guildInvite(c.getPlayer().getGuildId(), c.getPlayer().getName()));
        return null;
    }

    public static void displayGuildRanks(MapleClient c, int npcid) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(
                    "SELECT `name`, `GP`, `logoBG`, `logoBGColor`, " +
                            "`logo`, `logoColor` FROM guilds ORDER BY `GP` DESC LIMIT 50", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = ps.executeQuery();
            c.getSession().write(MaplePacketCreator.showGuildRanks(npcid, rs));
            ps.close();
            rs.close();
        } catch (Exception e) {
            log.error("failed to display guild ranks.", e);
        }
    }

    public void buildNotifications() {
        if (!bDirty) {
            return;
        }
        Set<Integer> chs = WorldRegistryImpl.getInstance().getChannelServer();
        if (notifications.keySet().size() != chs.size()) {
            notifications.clear();
            chs.forEach(ch -> notifications.put(ch, new LinkedList<>()));
        } else {
            notifications.values().forEach(List::clear);
        }
        synchronized (members) {
            for (MapleGuildCharacter mgc : members) {
                if (!mgc.isOnline()) {
                    continue;
                }

                List<Integer> ch = notifications.get(mgc.getChannel());
                if (ch == null) {
                    log.warn("Unable to connect to channel " + mgc.getChannel());
                } else {
                    ch.add(mgc.getId());
                }
            }
        }
        bDirty = false;
    }

    public void writeToDB() {
        writeToDB(false);
    }

    public void writeToDB(boolean bDisband) {
        Connection con;
        Logger log = LoggerFactory.getLogger(this.getClass());
        try {
            con = DatabaseConnection.getConnection();
        } catch (Exception e) {
            log.error("unable to connect to database to write guild information.", e);
            return;
        }
        try {
            if (!bDisband) {
                StringBuilder sql = new StringBuilder("UPDATE guilds SET " + "GP = ?, " + "logo = ?, " + "logoColor = ?, " + "logoBG = ?, " + "logoBGColor = ?, ");
                for (int i = 0; i < 5; i++) {
                    sql.append("rank").append(i + 1).append("title = ?, ");
                }
                sql.append("capacity = ?, " + "notice = ? WHERE guildid = ?");
                PreparedStatement ps = con.prepareStatement(sql.toString());
                ps.setInt(1, gp);
                ps.setInt(2, logo);
                ps.setInt(3, logoColor);
                ps.setInt(4, logoBG);
                ps.setInt(5, logoBGColor);
                for (int i = 6; i < 11; i++) {
                    ps.setString(i, rankTitles[i - 6]);
                }
                ps.setInt(11, capacity);
                ps.setString(12, notice);
                ps.setInt(13, this.id);
                ps.execute();
                ps.close();
            } else {
                PreparedStatement ps = con.prepareStatement("UPDATE characters SET guildid = 0, guildrank = 5 WHERE guildid = ?");
                ps.setInt(1, this.id);
                ps.execute();
                ps.close();
                ps = con.prepareStatement("DELETE FROM guilds WHERE guildid = ?");
                ps.setInt(1, this.id);
                ps.execute();
                ps.close();
                this.broadcast(MaplePacketCreator.guildDisband(this.id));
            }
        } catch (SQLException se) {
            log.error(se.getLocalizedMessage(), se);
        }
    }

    public int getId() {
        return id;
    }

    public int getLeaderId() {
        return leader;
    }

    public int getGP() {
        return gp;
    }

    public int getLogo() {
        return logo;
    }

    public void setLogo(int l) {
        logo = l;
    }

    public int getLogoColor() {
        return logoColor;
    }

    public void setLogoColor(int c) {
        logoColor = c;
    }

    public int getLogoBG() {
        return logoBG;
    }

    public void setLogoBG(int bg) {
        logoBG = bg;
    }

    public int getLogoBGColor() {
        return logoBGColor;
    }

    public void setLogoBGColor(int c) {
        logoBGColor = c;
    }

    public String getNotice() {
        if (notice == null) {
            return "";
        }
        return notice;
    }

    public String getName() {
        return name;
    }

    public java.util.Collection<MapleGuildCharacter> getMembers() {
        return java.util.Collections.unmodifiableCollection(members);
    }

    public int getCapacity() {
        return capacity;
    }

    public int getSignature() {
        return signature;
    }

    public void broadcast(MaplePacket packet) {
        broadcast(packet, -1, BCOp.NONE);
    }

    public void broadcast(MaplePacket packet, int exception) {
        broadcast(packet, exception, BCOp.NONE);
    }

    // multi-purpose function that reaches every member of guild
    // (except the character with exceptionId)
    // in all channels with as little access to rmi as possible
    public void broadcast(MaplePacket packet, int exceptionId, BCOp bcop) {
        WorldRegistryImpl wr = WorldRegistryImpl.getInstance();
        Set<Integer> chs = wr.getChannelServer();

        synchronized (notifications) {
            if (bDirty) {
                buildNotifications();
            }

            try {
                ChannelWorldInterface cwi;
                for (Integer ch : chs) {
                    cwi = wr.getChannel(ch);
                    if (!notifications.get(ch).isEmpty()) {
                        if (bcop == BCOp.DISBAND) {
                            cwi.setGuildAndRank(notifications.get(ch), 0, 5, exceptionId);
                        } else if (bcop == BCOp.EMBELMCHANGE) {
                            cwi.changeEmblem(this.id, notifications.get(ch), new MapleGuildSummary(this));
                        } else {
                            cwi.sendPacket(notifications.get(ch), packet, exceptionId);
                        }
                    }
                }
            } catch (java.rmi.RemoteException re) {
                Logger log = LoggerFactory.getLogger(this.getClass());
                log.error("Failed to contact channel(s) for broadcast.", re);
            }
        }
    }

    public void guildMessage(MaplePacket serverNotice) {
        for (MapleGuildCharacter mgc : members) {
            ChannelServer.getAllInstances().stream()
                    .map(c -> c.getPlayerStorage().getCharacterById(mgc.getId()))
                    .flatMap(Optional::stream)
                    .findFirst()
                    .ifPresent(c -> c.getClient().getSession().write(serverNotice));
        }
    }

    public void setOnline(int cid, boolean online, int channel) {
        boolean bBroadcast = true;
        for (MapleGuildCharacter mgc : members) {
            if (mgc.getId() == cid) {
                if (mgc.isOnline() && online) {
                    bBroadcast = false;
                }
                mgc.setOnline(online);
                mgc.setChannel(channel);
                break;
            }
        }

        if (bBroadcast) {
            this.broadcast(MaplePacketCreator.guildMemberOnline(id, cid, online), cid);
        }
        bDirty = true; // member formation has changed, update notifications
    }

    public void guildChat(String name, int cid, String msg) {
        this.broadcast(MaplePacketCreator.multiChat(name, msg, 2), cid);
    }

    public String getRankTitle(int rank) {
        return rankTitles[rank - 1];
    }

    public int addGuildMember(MapleGuildCharacter mgc) {
        synchronized (members) {
            if (members.size() >= capacity) {
                return 0;
            }
            for (int i = members.size() - 1; i >= 0; i--) {
                if (members.get(i).getGuildRank() < 5 || members.get(i).getName().compareTo(mgc.getName()) < 0) {
                    members.add(i + 1, mgc);
                    bDirty = true;
                    break;
                }
            }
        }
        this.broadcast(MaplePacketCreator.newGuildMember(mgc));
        return 1;
    }

    public void leaveGuild(MapleGuildCharacter mgc) {
        this.broadcast(MaplePacketCreator.memberLeft(mgc, false));
        synchronized (members) {
            members.remove(mgc);
            bDirty = true;
        }
    }

    public void expelMember(MapleGuildCharacter initiator, String name, int cid) {
        Logger log = LoggerFactory.getLogger(this.getClass());
        synchronized (members) {
            java.util.Iterator<MapleGuildCharacter> itr = members.iterator();
            MapleGuildCharacter mgc;
            while (itr.hasNext()) {
                mgc = itr.next();
                if (mgc.getId() == cid && initiator.getGuildRank() < mgc.getGuildRank()) {
                    this.broadcast(MaplePacketCreator.memberLeft(mgc, true));
                    itr.remove();
                    bDirty = true;
                    // i hate global for not saying who expelled
                    this.broadcast(MaplePacketCreator.serverNotice(5, initiator.getName() + " has expelled " +
                            mgc.getName() + "."));
                    try {
                        if (mgc.isOnline()) {
                            WorldRegistryImpl.getInstance().getChannel(mgc.getChannel()).setGuildAndRank(cid, 0, 5);
                        } else {
                            String sendTo = mgc.getName();
                            String sendFrom = initiator.getName();
                            String msg = "You have been expelled from the guild.";
                            try {
                                initiator.getName();
                                MaplePacketCreator.sendUnkwnNote(sendTo, msg, sendFrom);
                            } catch (SQLException e) {
                                log.error("SAVING NOTE", e);
                            }
                            WorldRegistryImpl.getInstance().getChannel(1).setOfflineGuildStatus((short) 0, (byte) 5, cid);
                        }
                    } catch (RemoteException re) {
                        re.printStackTrace();
                        return;
                    }
                    return;
                }
            }

            log.error("Unable to find member with name " + name + " and id " + cid);
        }
    }

    public void changeRank(int cid, int newRank) {
        for (MapleGuildCharacter mgc : members) {
            if (cid == mgc.getId()) {
                try {
                    if (mgc.isOnline()) {
                        WorldRegistryImpl.getInstance().getChannel(mgc.getChannel()).setGuildAndRank(cid, this.id, newRank);
                    } else {
                        WorldRegistryImpl.getInstance().getChannel(1).setOfflineGuildStatus((short) this.id, (byte) newRank, cid);
                    }
                } catch (RemoteException re) {
                    re.printStackTrace();
                    return;
                }
                mgc.setGuildRank(newRank);
                this.broadcast(MaplePacketCreator.changeRank(mgc));
                return;
            }
        }
        log.info("INFO: unable to find the correct id for changeRank({}, {})", cid, newRank);
    }

    public void setGuildNotice(String notice) {
        this.notice = notice;
        writeToDB();
        this.broadcast(MaplePacketCreator.guildNotice(this.id, notice));
    }

    public void memberLevelJobUpdate(MapleGuildCharacter mgc) {
        for (MapleGuildCharacter member : members) {
            if (mgc.equals(member)) {
                member.setJobId(mgc.getJobId());
                member.setLevel(mgc.getLevel());
                this.broadcast(MaplePacketCreator.guildMemberLevelJobUpdate(mgc));
                break;
            }
        }
    }

    public void changeRankTitle(String[] ranks) {
        for (int i = 0; i < 5; i++) {
            rankTitles[i] = ranks[i];
        }
        this.broadcast(MaplePacketCreator.rankTitleChange(this.id, ranks));
        this.writeToDB();
    }

    public void disbandGuild() {
        // disband the guild
        this.writeToDB(true);
        this.broadcast(null, -1, BCOp.DISBAND);
    }

    public void setGuildEmblem(short bg, byte bgcolor, short logo, byte logocolor) {
        this.logoBG = bg;
        this.logoBGColor = bgcolor;
        this.logo = logo;
        this.logoColor = logocolor;
        this.writeToDB();
        this.broadcast(null, -1, BCOp.EMBELMCHANGE);
    }

    public MapleGuildCharacter getMGC(int cid) {
        for (MapleGuildCharacter mgc : members) {
            if (mgc.getId() == cid) {
                return mgc;
            }
        }
        return null;
    }

    public boolean increaseCapacity() {
        if (capacity >= 100) {
            return false;
        }
        capacity += 5;
        this.writeToDB();
        this.broadcast(MaplePacketCreator.guildCapacityChange(this.id, this.capacity));
        return true;
    }

    public void gainGP(int amount) {
        this.gp += amount;
        this.writeToDB();
        this.guildMessage(MaplePacketCreator.updateGP(this.id, this.gp));
    }

    private enum BCOp {

        NONE, DISBAND, EMBELMCHANGE
    }
}
