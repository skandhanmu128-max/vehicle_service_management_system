package vsms.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import vsms.db.DBConnection;
import vsms.model.Invoice;

public class InvoiceDAO {

    public int insert(Invoice invoice) throws SQLException {
        String sql = "INSERT INTO `invoice` (`booking_id`, `invoice_number`, `total`, `pdf_path`)"
                + " VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, invoice.getBookingId());
            ps.setString(2, invoice.getInvoiceNumber());
            ps.setBigDecimal(3, invoice.getTotal());
            ps.setString(4, invoice.getPdfPath());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
                throw new SQLException("Failed to obtain generated invoice id.");
            }
        }
    }

    public Invoice findByBookingId(int bookingId) throws SQLException {
        String sql = "SELECT * FROM `invoice` WHERE `booking_id` = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
                return null;
            }
        }
    }

    public Invoice findByNumber(String invoiceNumber) throws SQLException {
        String sql = "SELECT * FROM `invoice` WHERE `invoice_number` = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, invoiceNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
                return null;
            }
        }
    }

    public List<Invoice> findAll() throws SQLException {
        String sql = "SELECT * FROM `invoice` ORDER BY `id` DESC";
        List<Invoice> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public String nextInvoiceNumber() throws SQLException {
        String sql = "SELECT COUNT(*) FROM `invoice`";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            int next = rs.getInt(1) + 1;
            return String.format("INV-%04d", next);
        }
    }

    private Invoice map(ResultSet rs) throws SQLException {
        return new Invoice(
                rs.getInt("id"),
                rs.getInt("booking_id"),
                rs.getString("invoice_number"),
                rs.getBigDecimal("total"),
                rs.getString("pdf_path"),
                rs.getTimestamp("generated_at").toLocalDateTime());
    }
}