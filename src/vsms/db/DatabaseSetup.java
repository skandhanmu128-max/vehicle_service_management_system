package vsms.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * One-time setup helper. Creates the database and tables with idempotent
 * DDL (IF NOT EXISTS) and seeds the service catalog when it is empty.
 * Supports both MySQL server and embedded H2 database.
 */
public final class DatabaseSetup {

    private DatabaseSetup() {
    }

    public static boolean setupDatabase() {
        String host = DBConnection.getProperty("db.host", "localhost");
        String port = DBConnection.getProperty("db.port", "3306");
        String name = DBConnection.getProperty("db.name", "vehicle_service_mgmt");
        String user = DBConnection.getProperty("db.user", "root");
        String password = DBConnection.getProperty("db.password", "");

        String serverUrl = "jdbc:mysql://" + host + ":" + port
                + "/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

        // Try MySQL Setup first
        try (Connection conn = DriverManager.getConnection(serverUrl, user, password);
             Statement st = conn.createStatement()) {

            System.out.println("[setup] Creating MySQL database '" + name + "' if it does not exist ...");
            st.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + name + "` DEFAULT CHARACTER SET utf8mb4");

            try (Statement use = conn.createStatement()) {
                use.execute("USE `" + name + "`");
            }
            createTables(conn);
            seedServiceCatalog(conn);
            System.out.println("[setup] MySQL Database setup complete.");
            return true;

        } catch (SQLException e) {
            // MySQL setup failed - fallback to Embedded H2 Setup
            System.out.println("[setup] MySQL unavailable (" + e.getMessage() + "). Initializing embedded H2 database...");
            try (Connection conn = DBConnection.getEmbeddedConnection()) {
                createTables(conn);
                seedServiceCatalog(conn);
                System.out.println("[setup] Embedded H2 Database setup complete.");
                return true;
            } catch (SQLException ex) {
                System.err.println("[setup] FAILED: " + ex.getMessage());
                ex.printStackTrace();
                return false;
            }
        }
    }

    private static void createTables(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS `customer` ("
                    + " `id` INT AUTO_INCREMENT PRIMARY KEY,"
                    + " `name` VARCHAR(100) NOT NULL,"
                    + " `phone` VARCHAR(20) NOT NULL UNIQUE,"
                    + " `email` VARCHAR(100),"
                    + " `address` VARCHAR(255),"
                    + " `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS `vehicle` ("
                    + " `id` INT AUTO_INCREMENT PRIMARY KEY,"
                    + " `customer_id` INT NOT NULL,"
                    + " `make` VARCHAR(50) NOT NULL,"
                    + " `model` VARCHAR(50) NOT NULL,"
                    + " `year` INT,"
                    + " `license_plate` VARCHAR(20) NOT NULL UNIQUE,"
                    + " `vin` VARCHAR(50),"
                    + " `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                    + " CONSTRAINT `fk_vehicle_customer` FOREIGN KEY (`customer_id`)"
                    + " REFERENCES `customer`(`id`) ON DELETE CASCADE)");

            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS `service_type` ("
                    + " `id` INT AUTO_INCREMENT PRIMARY KEY,"
                    + " `name` VARCHAR(100) NOT NULL,"
                    + " `description` VARCHAR(255),"
                    + " `base_cost` DECIMAL(10,2) NOT NULL)");

            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS `booking` ("
                    + " `id` INT AUTO_INCREMENT PRIMARY KEY,"
                    + " `vehicle_id` INT NOT NULL,"
                    + " `scheduled_date` DATE NOT NULL,"
                    + " `status` VARCHAR(20) DEFAULT 'BOOKED',"
                    + " `odometer` INT,"
                    + " `technician` VARCHAR(100),"
                    + " `notes` VARCHAR(255),"
                    + " `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                    + " CONSTRAINT `fk_booking_vehicle` FOREIGN KEY (`vehicle_id`)"
                    + " REFERENCES `vehicle`(`id`) ON DELETE CASCADE)");

            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS `booking_item` ("
                    + " `id` INT AUTO_INCREMENT PRIMARY KEY,"
                    + " `booking_id` INT NOT NULL,"
                    + " `service_type_id` INT NOT NULL,"
                    + " `cost` DECIMAL(10,2) NOT NULL,"
                    + " CONSTRAINT `fk_item_booking` FOREIGN KEY (`booking_id`)"
                    + " REFERENCES `booking`(`id`) ON DELETE CASCADE,"
                    + " CONSTRAINT `fk_item_service` FOREIGN KEY (`service_type_id`)"
                    + " REFERENCES `service_type`(`id`))");

            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS `invoice` ("
                    + " `id` INT AUTO_INCREMENT PRIMARY KEY,"
                    + " `booking_id` INT NOT NULL,"
                    + " `invoice_number` VARCHAR(20) NOT NULL UNIQUE,"
                    + " `total` DECIMAL(10,2) NOT NULL,"
                    + " `pdf_path` VARCHAR(255),"
                    + " `generated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                    + " CONSTRAINT `fk_invoice_booking` FOREIGN KEY (`booking_id`)"
                    + " REFERENCES `booking`(`id`) ON DELETE CASCADE)");

            st.executeUpdate(
                    "CREATE OR REPLACE VIEW `service_history` AS"
                    + " SELECT b.`id` AS booking_id, b.`scheduled_date` AS service_date,"
                    + " b.`status`, b.`odometer`, b.`technician`,"
                    + " c.`id` AS customer_id, c.`name` AS customer_name,"
                    + " v.`id` AS vehicle_id, CONCAT(v.`make`, ' ', v.`model`) AS vehicle_name,"
                    + " v.`license_plate`, st.`name` AS service_name, bi.`cost`"
                    + " FROM `booking` b"
                    + " JOIN `vehicle` v ON v.`id` = b.`vehicle_id`"
                    + " JOIN `customer` c ON c.`id` = v.`customer_id`"
                    + " JOIN `booking_item` bi ON bi.`booking_id` = b.`id`"
                    + " JOIN `service_type` st ON st.`id` = bi.`service_type_id`"
                    + " WHERE b.`status` IN ('COMPLETED','IN_PROGRESS')");
        }
    }

    private static void seedServiceCatalog(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM `service_type`")) {
                rs.next();
                if (rs.getInt(1) > 0) {
                    return;
                }
            }
            st.executeUpdate(
                    "INSERT INTO `service_type` (`name`, `description`, `base_cost`) VALUES"
                    + " ('Oil Change','Engine oil replacement and filter change',1200.00),"
                    + " ('Tire Rotation','Rotate and balance all four tires',400.00),"
                    + " ('Brake Pad Replacement','Replace front and rear brake pads',2500.00),"
                    + " ('Engine Tuning','Spark plugs, filters and engine tuning',1800.00),"
                    + " ('AC Service','AC gas refill and system check',1500.00),"
                    + " ('Wheel Alignment','Computerised wheel alignment',800.00),"
                    + " ('Battery Replacement','Replace car battery and terminals',3500.00),"
                    + " ('Full Body Wash','Full exterior wash and wax polish',600.00)");
            System.out.println("[setup] Seeded service catalog (8 services).");
        }
    }
}