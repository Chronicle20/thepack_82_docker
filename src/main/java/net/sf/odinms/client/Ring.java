package net.sf.odinms.client;

import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.tools.MaplePacketCreator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Ring implements Comparable<Ring> {

    private final int ringId;
    private final int ringId2;
    private final int partnerId;
    private final int itemId;
    private final String partnerName;
    private boolean equipped;

    private Ring(int id, int id2, int partnerId, int itemId, String partnerName) {
        this.ringId = id;
        this.ringId2 = id2;
        this.partnerId = partnerId;
        this.itemId = itemId;
        this.partnerName = partnerName;
    }

    public static Ring loadFromDb(int ringId) {
        try {
            Connection con = DatabaseConnection.getConnection(); // Get a connection to the database
            PreparedStatement ps = con.prepareStatement("SELECT * FROM rings WHERE id = ?"); // Get ring details..
            ps.setInt(1, ringId);
            ResultSet rs = ps.executeQuery();

            rs.next();
            Ring ret = new Ring(ringId, rs.getInt("partnerRingId"), rs.getInt("partnerChrId"), rs.getInt("itemid"), rs.getString("partnerName"));

            rs.close();
            ps.close();

            return ret;
        } catch (SQLException ex) {
            Logger.getLogger(Pet.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public static int[] createRing(MapleClient c, int itemid, int chrId, String chrName, int partnerId, String partnername) {
        try {
            Optional<MapleCharacter> chr = c.getChannelServer().getPlayerStorage().getCharacterById(partnerId);
            if (chr.isEmpty()) {
                int[] ret_ = new int[2];
                ret_[0] = -1;
                ret_[1] = -1;
                return ret_;
            }
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("INSERT INTO rings (itemid, partnerChrId, partnername) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, itemid);
            ps.setInt(2, partnerId);
            ps.setString(3, partnername);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            int[] ret = new int[2];
            ret[0] = rs.getInt(1);
            rs.close();
            ps.close();

            ps = con.prepareStatement("INSERT INTO rings (itemid, partnerRingId, partnerChrId, partnername) VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, itemid);
            ps.setInt(2, ret[0]);
            ps.setInt(3, chrId);
            ps.setString(4, chrName);
            ps.executeUpdate();
            rs = ps.getGeneratedKeys();
            rs.next();
            ret[1] = rs.getInt(1);
            rs.close();
            ps.close();

            ps = con.prepareStatement("UPDATE rings SET partnerRingId = ? WHERE id = ?");
            ps.setInt(1, ret[1]);
            ps.setInt(2, ret[0]);
            ps.executeUpdate();
            ps.close();

            MapleCharacter player = c.getPlayer();

            MapleInventoryManipulator.addRing(player, itemid, ret[0]);

            MapleInventoryManipulator.addRing(chr.get(), itemid, ret[1]);

            c.getSession().write(MaplePacketCreator.getCharInfo(player));
            player.getMap().removePlayer(player);
            player.getMap().addPlayer(player);

            chr.get().getClient().getSession().write(MaplePacketCreator.getCharInfo(chr.get()));
            chr.get().getMap().removePlayer(chr.get());
            chr.get().getMap().addPlayer(chr.get());
            chr.get().getClient().getSession().write(MaplePacketCreator.serverNotice(5, "You have received a ring from " + player.getName() + ". Please log out and log back in again if it does not work correctly."));

            return ret;
        } catch (SQLException ex) {
            Logger.getLogger(Pet.class.getName()).log(Level.SEVERE, null, ex);
            int[] ret = new int[2];
            ret[0] = -1;
            ret[1] = -1;
            return ret;
        }

    }

    public int getRingId() {
        return ringId;
    }

    public int getPartnerRingId() {
        return ringId2;
    }

    public int getPartnerChrId() {
        return partnerId;
    }

    public int getItemId() {
        return itemId;
    }

    public String getPartnerName() {
        return partnerName;
    }

    public boolean isEquipped() {
        return equipped;
    }

    public void setEquipped(boolean equipped) {
        this.equipped = equipped;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Ring) {
            return ((Ring) o).getRingId() == getRingId();
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + this.ringId;
        return hash;
    }

    @Override
    public int compareTo(Ring other) {
        return Integer.compare(ringId, other.getRingId());
    }
}