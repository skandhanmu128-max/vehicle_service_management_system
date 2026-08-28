package vsms.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Loads connection settings from db.properties (classpath first, then
 * working directory) and provides JDBC connections to MySQL.
 */
public final class DBConnection {

    private static final Properties PROPS = new Properties();

    static {
        loadProperties();
    }

    private static void loadProperties() {
        boolean loaded = false;

        InputStream in = DBConnection.class.getResourceAsStream("/db.properties");
        if (in != null) {
            try {
                PROPS.load(in);
                loaded = true;
            } catch (IOException e) {
                // fall through to file based lookup
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
                // fall through to defaults
            }
        }

        if (!loaded) {
            System.err.println("[DB] db.properties not found - using default localhost/root/root connection.");
        }
    }

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        String url = PROPS.getProperty("db.url");
        if (url == null || url.isBlank()) {
            String host = PROPS.getProperty("db.host", "localhost");
            String port = PROPS.getProperty("db.port", "3306");
            String name = PROPS.getProperty("db.name", "vehicle_service_mgmt");
            url = "jdbc:mysql://" + host + ":" + port + "/" + name
                    + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        }
        String user = PROPS.getProperty("db.user", "root");
        String password = PROPS.getProperty("db.password", "root");
        return DriverManager.getConnection(url, user, password);
    }

    public static String databaseName() {
        return PROPS.getProperty("db.name", "vehicle_service_mgmt");
    }

public static String getProperty(String key, String defaultValue) {
    String value = PROPS.getProperty(key);
    return (value == null) ? defaultValue : value;
}
}