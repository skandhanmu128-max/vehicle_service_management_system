package vsms.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import vsms.db.DBConnection;
import vsms.model.Booking;
import vsms.model.BookingItem;

public class BookingDAO {

    /**
     * Inserts a booking and all of its line items atomically.
     * The returned booking has its generated id populated.
     */
    public int insertWithItems(Booking booking) throws SQLException {
        String bookingSql = "INSERT INTO `booking` (`vehicle_id`, `scheduled_date`, `status`, `odometer`, `technician`, `notes`)"
                + " VALUES (?, ?, ?, ?, ?, ?)";
        String itemSql = "INSERT INTO `booking_item` (`booking_id`, `service_type_id`, `cost`) VALUES (?, ?, ?)";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int bookingId;
                try (PreparedStatement ps = conn.prepareStatement(bookingSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, booking.getVehicleId());
                    ps.setDate(2, Date.valueOf(booking.getScheduledDate()));
                    ps.setString(3, booking.getStatus());
                    if (booking.getOdometer() != null) {
                        ps.setInt(4, booking.getOdometer());
                    } else {
                        ps.setNull(4, java.sql.Types.INTEGER);
                    }
                    ps.setString(5, booking.getTechnician());
                    ps.setString(6, booking.getNotes());
                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Failed to obtain generated booking id.");
                        }
                        bookingId = keys.getInt(1);
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(itemSql)) {
                    for (BookingItem item : booking.getItems()) {
                        ps.setInt(1, bookingId);
                        ps.setInt(2, item.getServiceTypeId());
                        ps.setBigDecimal(3, item.getCost());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                conn.commit();
                return bookingId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public Booking findById(int id) throws SQLException {
        Booking booking = null;
        String sql = "SELECT * FROM `booking` WHERE `id` = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    booking = map(rs);
                }
            }
        }
        if (booking != null) {
            loadItems(booking);
        }
        return booking;
    }

    public List<Booking> findByStatus(String status) throws SQLException {
        String sql = "SELECT * FROM `booking` WHERE `status` = ? ORDER BY `scheduled_date` DESC, `id` DESC";
        return queryList(sql, status);
    }

    public List<Booking> findByVehicleId(int vehicleId) throws SQLException {
        String sql = "SELECT * FROM `booking` WHERE `vehicle_id` = ? ORDER BY `scheduled_date` DESC, `id` DESC";
        return queryList(sql, Integer.toString(vehicleId));
    }

    public List<Booking> findAll() throws SQLException {
        String sql = "SELECT * FROM `booking` ORDER BY `scheduled_date` DESC, `id` DESC";
        List<Booking> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        for (Booking b : list) {
            loadItems(b);
        }
        return list;
    }

    public boolean updateStatus(int id, String status) throws SQLException {
        String sql = "UPDATE `booking` SET `status` = ? WHERE `id` = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean completeBooking(int id, int odometer, String technician) throws SQLException {
        String sql = "UPDATE `booking` SET `status` = 'COMPLETED', `odometer` = ?, `technician` = ? WHERE `id` = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, odometer);
            ps.setString(2, technician);
            ps.setInt(3, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean hasInvoice(int bookingId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM `invoice` WHERE `booking_id` = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    private void loadItems(Booking booking) throws SQLException {
        String sql = "SELECT bi.`id`, bi.`booking_id`, bi.`service_type_id`, bi.`cost`, st.`name`"
                + " FROM `booking_item` bi"
                + " JOIN `service_type` st ON st.`id` = bi.`service_type_id`"
                + " WHERE bi.`booking_id` = ? ORDER BY bi.`id`";
        List<BookingItem> items = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, booking.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new BookingItem(
                            rs.getInt("id"),
                            rs.getInt("booking_id"),
                            rs.getInt("service_type_id"),
                            rs.getString("name"),
                            rs.getBigDecimal("cost")));
                }
            }
        }
        booking.setItems(items);
    }

    private List<Booking> queryList(String sql, String param) throws SQLException {
        List<Booking> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        for (Booking b : list) {
            loadItems(b);
        }
        return list;
    }

    private Booking map(ResultSet rs) throws SQLException {
        Booking b = new Booking(
                rs.getInt("id"),
                rs.getInt("vehicle_id"),
                rs.getDate("scheduled_date").toLocalDate(),
                rs.getString("status"),
                (Integer) rs.getObject("odometer"),
                rs.getString("technician"),
                rs.getString("notes"));
        b.setItems(new ArrayList<>());
        return b;
    }
}