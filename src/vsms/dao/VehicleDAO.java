package vsms.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import vsms.db.DBConnection;
import vsms.model.Vehicle;

public class VehicleDAO {

    public int insert(Vehicle v) throws SQLException {
        String sql = "INSERT INTO vehicle (customer_id, make, model, year, license_plate, vin)"
                + " VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, v.getCustomerId());
            ps.setString(2, v.getMake());
            ps.setString(3, v.getModel());
            ps.setInt(4, v.getYear());
            ps.setString(5, v.getLicensePlate());
            ps.setString(6, v.getVin());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
                throw new SQLException("Failed to obtain generated vehicle id.");
            }
        }
    }

    public Vehicle findById(int id) throws SQLException {
        String sql = "SELECT * FROM vehicle WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
                return null;
            }
        }
    }

    public List<Vehicle> findByCustomerId(int customerId) throws SQLException {
        String sql = "SELECT * FROM vehicle WHERE customer_id = ? ORDER BY id";
        List<Vehicle> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public List<Vehicle> findAll() throws SQLException {
        String sql = "SELECT * FROM vehicle ORDER BY id";
        List<Vehicle> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public boolean update(Vehicle v) throws SQLException {
        String sql = "UPDATE vehicle SET make = ?, model = ?, year = ?, license_plate = ?, vin = ?"
                + " WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, v.getMake());
            ps.setString(2, v.getModel());
            ps.setInt(3, v.getYear());
            ps.setString(4, v.getLicensePlate());
            ps.setString(5, v.getVin());
            ps.setInt(6, v.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM vehicle WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Vehicle map(ResultSet rs) throws SQLException {
        return new Vehicle(
                rs.getInt("id"),
                rs.getInt("customer_id"),
                rs.getString("make"),
                rs.getString("model"),
                rs.getInt("year"),
                rs.getString("license_plate"),
                rs.getString("vin"));
    }
}