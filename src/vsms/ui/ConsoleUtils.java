package vsms.ui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public final class ConsoleUtils {

    private ConsoleUtils() {
    }

    public static String readLine(Scanner sc, String prompt, boolean required) {
        while (true) {
            System.out.print(prompt);
            String value = sc.nextLine().trim();
            if (required && value.isEmpty()) {
                System.out.println("  >> This field is required.");
                continue;
            }
            return value;
        }
    }

    public static String readOptional(Scanner sc, String prompt) {
        System.out.print(prompt);
        String value = sc.nextLine().trim();
        return value.isEmpty() ? null : value;
    }

    public static Integer readInt(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = sc.nextLine().trim();
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.out.println("  >> Please enter a valid number.");
            }
        }
    }

    public static Integer readOptionalInt(Scanner sc, String prompt) {
        System.out.print(prompt);
        String value = sc.nextLine().trim();
        if (value.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static LocalDate readDate(Scanner sc, String prompt) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        while (true) {
            System.out.print(prompt + " (yyyy-MM-dd) ");
            String value = sc.nextLine().trim();
            try {
                return LocalDate.parse(value, fmt);
            } catch (DateTimeParseException e) {
                System.out.println("  >> Invalid date. Use yyyy-MM-dd, e.g. 2026-09-15.");
            }
        }
    }

    public static void pause(Scanner sc) {
        System.out.print("\nPress ENTER to continue ...");
        sc.nextLine();
        System.out.println();
    }
}