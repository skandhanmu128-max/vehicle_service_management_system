package vsms.db;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Provides JDBC connections to MySQL, with automatic fallback to an
 * embedded H2 database if MySQL server is not running locally.
 */
public final class DBConnection {

    private static final Properties PROPS = new Properties();
    private static boolean useEmbedded = false;

    static {
        loadProperties();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ignored) {
        }
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException ignored) {
        }
    }

    private static void loadProperties() {
        boolean loaded = false;
        InputStream in = DBConnection.class.getResourceAsStream("/db.properties");
        if (in != null) {
            try {
                PROPS.load(in);
                loaded = true;
            } catch (IOException e) {
            } finally {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
        }

        if (!loaded && Files.exists(Paths.get("db.properties"))) {
            try (InputStream fs = Files.newInputStream(Paths.get("db.properties"))) {
                PROPS.load(fs);
                loaded = true;
            } catch (IOException ignored) {
            }
        }
    }

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        if (useEmbedded) {
            return getEmbeddedConnection();
        }

        try {
            return getMySqlConnection();
        } catch (SQLException e) {
            // MySQL unavailable - attempt embedded H2 fallback
            try {
                Connection h2Conn = getEmbeddedConnection();
                if (!useEmbedded) {
                    useEmbedded = true;
                    System.out.println("[DB] MySQL unavailable. Using embedded H2 database fallback.");
                    DatabaseSetup.setupDatabase();
                }
                return h2Conn;
            } catch (SQLException ex) {
                throw e; // throw original MySQL exception if H2 also fails
            }
        }
    }

    public static Connection getMySqlConnection() throws SQLException {
        String url = PROPS.getProperty("db.url");
        if (url == null || url.isBlank()) {
            String host = PROPS.getProperty("db.host", "localhost");
            String port = PROPS.getProperty("db.port", "3306");
            String name = PROPS.getProperty("db.name", "vehicle_service_mgmt");
            url = "jdbc:mysql://" + host + ":" + port + "/" + name
                    + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        }
        String user = PROPS.getProperty("db.user", "root");
        String password = PROPS.getProperty("db.password", "");
        return DriverManager.getConnection(url, user, password);
    }

    public static Connection getEmbeddedConnection() throws SQLException {
        File dir = new File("data");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String url = "jdbc:h2:./data/vehicle_service_mgmt;MODE=MySQL;CASE_INSENSITIVE_IDENTIFIERS=TRUE;AUTO_SERVER=TRUE";
        return DriverManager.getConnection(url, "sa", "");
    }

    public static boolean isEmbeddedMode() {
        return useEmbedded;
    }

    public static String databaseName() {
        return useEmbedded ? "vehicle_service_mgmt (Embedded H2)" : PROPS.getProperty("db.name", "vehicle_service_mgmt") + " (MySQL)";
    }

    public static String getProperty(String key, String defaultValue) {
        String value = PROPS.getProperty(key);
        return (value == null) ? defaultValue : value;
    }
}