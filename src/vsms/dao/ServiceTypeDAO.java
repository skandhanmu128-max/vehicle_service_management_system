package vsms.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import vsms.db.DBConnection;
import vsms.model.ServiceType;

public class ServiceTypeDAO {

    public List<ServiceType> findAll() throws SQLException {
        String sql = "SELECT * FROM `service_type` ORDER BY `id`";
        List<ServiceType> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public ServiceType findById(int id) throws SQLException {
        String sql = "SELECT * FROM `service_type` WHERE `id` = ?";
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

    private ServiceType map(ResultSet rs) throws SQLException {
        return new ServiceType(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getBigDecimal("base_cost"));
    }
}