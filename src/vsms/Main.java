package vsms;

import vsms.ui.Menu;
import vsms.web.WebServer;

/**
 * Entry point for the Vehicle Service Management System.
 * Starts Web Server on http://localhost:8080 and launches interactive console menu.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC driver not found on the classpath.");
            System.err.println("Place mysql-connector-j-*.jar in the lib/ folder.");
        }

        // Launch Embedded Web Server on http://localhost:8080
        try {
            WebServer webServer = new WebServer(8080);
            webServer.start();
        } catch (Exception e) {
            System.err.println("[Web] Failed to launch web server on port 8080: " + e.getMessage());
        }

        // Launch Console Menu (or keep Web Server alive if non-interactive)
        try {
            new Menu().run();
        } catch (Exception e) {
            System.out.println("\n[VSMS] Web Server active on http://localhost:8080");
            try {
                Thread.currentThread().join();
            } catch (InterruptedException ignored) {
            }
        }
    }
}