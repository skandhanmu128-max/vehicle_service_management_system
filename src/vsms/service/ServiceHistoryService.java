package vsms.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import vsms.db.DBConnection;
import vsms.model.ServiceHistoryEntry;

/**
 * Reads the service_history view for completed / in-progress work.
 */
public class ServiceHistoryService {

    public List<ServiceHistoryEntry> allHistory() throws SQLException {
        String sql = "SELECT * FROM service_history ORDER BY service_date DESC, booking_id DESC";
        return query(sql);
    }

    public List<ServiceHistoryEntry> historyByCustomer(int customerId) throws SQLException {
        String sql = "SELECT * FROM service_history WHERE customer_id = ?"
                + " ORDER BY service_date DESC, booking_id DESC";
        return query(sql, customerId);
    }

    public List<ServiceHistoryEntry> historyByVehicle(int vehicleId) throws SQLException {
        String sql = "SELECT * FROM service_history WHERE vehicle_id = ?"
                + " ORDER BY service_date DESC, booking_id DESC";
        return query(sql, vehicleId);
    }

    public List<ServiceHistoryEntry> historyByPlate(String licensePlate) throws SQLException {
        String sql = "SELECT * FROM service_history WHERE license_plate = ?"
                + " ORDER BY service_date DESC, booking_id DESC";
        return query(sql, licensePlate);
    }

    private List<ServiceHistoryEntry> query(String sql) throws SQLException {
        List<ServiceHistoryEntry> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    private List<ServiceHistoryEntry> query(String sql, int param) throws SQLException {
        List<ServiceHistoryEntry> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    private List<ServiceHistoryEntry> query(String sql, String param) throws SQLException {
        List<ServiceHistoryEntry> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    private ServiceHistoryEntry map(ResultSet rs) throws SQLException {
        ServiceHistoryEntry e = new ServiceHistoryEntry();
        e.setBookingId(rs.getInt("booking_id"));
        e.setServiceDate(rs.getDate("service_date").toLocalDate());
        e.setStatus(rs.getString("status"));
        e.setOdometer((Integer) rs.getObject("odometer"));
        e.setTechnician(rs.getString("technician"));
        e.setCustomerId(rs.getInt("customer_id"));
        e.setCustomerName(rs.getString("customer_name"));
        e.setVehicleId(rs.getInt("vehicle_id"));
        e.setVehicleName(rs.getString("vehicle_name"));
        e.setLicensePlate(rs.getString("license_plate"));
        e.setServiceName(rs.getString("service_name"));
        e.setCost(rs.getBigDecimal("cost"));
        return e;
    }

    public BigDecimal totalSpent(List<ServiceHistoryEntry> entries) {
        BigDecimal total = BigDecimal.ZERO;
        for (ServiceHistoryEntry e : entries) {
            total = total.add(e.getCost());
        }
        return total;
    }
}