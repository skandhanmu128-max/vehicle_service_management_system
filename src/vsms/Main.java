package vsms;

import vsms.ui.Menu;

/**
 * Entry point for the Vehicle Service Management System.
 *
 * Run from the project root:
 *   javac -encoding UTF-8 -cp "lib/*" -d out $(Get-ChildItem src -Recurse -Filter *.java)
 *   java -cp "out;lib/*;." vsms.Main
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC driver not found on the classpath.");
            System.err.println("Place mysql-connector-j-*.jar in the lib/ folder and run with:");
            System.err.println("  java -cp \"out;lib/*;.\" vsms.Main");
            System.exit(1);
        }
        new Menu().run();
    }
}