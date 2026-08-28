package vsms.pdf;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import vsms.model.Booking;
import vsms.model.BookingItem;
import vsms.model.Customer;
import vsms.model.Vehicle;

/**
 * Minimal, dependency-free PDF invoice generator.
 * Produces a single page A4 PDF using only the built-in Helvetica fonts.
 * All text is normalized to Latin-1 so the PDF remains valid.
 */
public final class PdfInvoiceGenerator {

    private static final float PAGE_W = 595f;
    private static final float PAGE_H = 842f;
    private static final float MARGIN = 50f;
    private static final float RIGHT = 545f;

    private PdfInvoiceGenerator() {
    }

    /**
     * Writes the invoice PDF to the invoices/ directory and returns its path.
     */
    public static String generate(Booking booking, Customer customer, Vehicle vehicle,
                                  String invoiceNumber, BigDecimal total, String fileName)
            throws IOException {
        File dir = new File("invoices");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Could not create invoices/ directory.");
        }
        String path = "invoices" + File.separator + fileName;
        byte[] pdf = buildPdf(booking, customer, vehicle, invoiceNumber, total);
        Files.write(Paths.get(path), pdf);
        return path;
    }

    private static byte[] buildPdf(Booking booking, Customer customer, Vehicle vehicle,
                                   String invoiceNumber, BigDecimal total) {
        String content = buildContentStream(booking, customer, vehicle, invoiceNumber, total);
        byte[] contentBytes = content.getBytes(StandardCharsets.ISO_8859_1);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream ps;
        try {
            ps = new PrintStream(out, true, "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Missing ISO-8859-1 encoding", e);
        }

        ps.print("%PDF-1.4\n");
        ps.print("%\u00E2\u00E3\u00CF\u00D3\n");

        int[] offsets = new int[7];
        offsets[1] = out.size();
        ps.print("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

        offsets[2] = out.size();
        ps.print("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");

        offsets[3] = out.size();
        ps.print("3 0 obj\n"
                + "<< /Type /Page /Parent 2 0 R "
                + "/MediaBox [0 0 " + (int) PAGE_W + " " + (int) PAGE_H + "] "
                + "/Resources << /Font << /F1 4 0 R /F2 6 0 R >> >> "
                + "/Contents 5 0 R >>\n"
                + "endobj\n");

        offsets[4] = out.size();
        ps.print("4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n");

        offsets[5] = out.size();
        ps.print("5 0 obj\n<< /Length " + contentBytes.length + " >>\nstream\n");
        ps.write(contentBytes, 0, contentBytes.length);
        ps.print("\nendstream\nendobj\n");

        offsets[6] = out.size();
        ps.print("6 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>\nendobj\n");

        int startxref = out.size();
        ps.print("xref\n0 7\n");
        for (int i = 0; i < offsets.length; i++) {
            ps.printf(Locale.ROOT, "%010d %05d %c \n", offsets[i], i == 0 ? 65535 : 0, i == 0 ? 'f' : 'n');
        }
        ps.print("trailer\n<< /Size 7 /Root 1 0 R >>\nstartxref\n" + startxref + "\n%%EOF\n");
        ps.flush();
        return out.toByteArray();
    }

    // ------------------------------------------------------------
    // Content stream
    // ------------------------------------------------------------

    private static String buildContentStream(Booking booking, Customer customer, Vehicle vehicle,
                                             String invoiceNumber, BigDecimal total) {
        StringBuilder cs = new StringBuilder(4096);

        String shopName = "VEHICLE SERVICE MANAGEMENT SYSTEM";
        String shopAddress = "Domain Street, Car Lane, Auto City - 600 001";
        String shopContact = "Ph: +91 98765 43210  |  support@vsms.in";

        text(cs, centerX(shopName, 20), 812, shopName, F_BOLD, 20);
        text(cs, centerX(shopAddress, 9), 794, shopAddress, F_REG, 9);
        text(cs, centerX(shopContact, 9), 782, shopContact, F_REG, 9);
        rule(cs, 60, 760);

        text(cs, MARGIN, 730, "INVOICE", F_BOLD, 16);
        text(cs, MARGIN, 710, "Invoice No : " + invoiceNumber, F_BOLD, 11);
        text(cs, MARGIN, 694, "Date       : "
                + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm")), F_REG, 11);
        text(cs, RIGHT, 710, "Booking ID : B-" + booking.getId(), F_REG, 10);
        text(cs, RIGHT, 694, "Service    : " + booking.getScheduledDate().format(
                DateTimeFormatter.ofPattern("dd-MMM-yyyy")), F_REG, 10);
        rule(cs, 60, 684);

        float blockTop = 652;
        text(cs, MARGIN, blockTop, "BILLED TO", F_BOLD, 11);
        text(cs, MARGIN, blockTop - 18, nvl(customer.getName()), F_REG, 11);
        text(cs, MARGIN, blockTop - 34, "Phone : " + nvl(customer.getPhone()), F_REG, 10);
        if (notBlank(customer.getEmail())) {
            text(cs, MARGIN, blockTop - 49, "Email : " + nvl(customer.getEmail()), F_REG, 10);
        }
        if (notBlank(customer.getAddress())) {
            text(cs, MARGIN, blockTop - 64, "Addr  : " + nvl(customer.getAddress()), F_REG, 10);
        }

        text(cs, 300, blockTop, "VEHICLE", F_BOLD, 11);
        text(cs, 300, blockTop - 18, nvl(vehicle.getMake()) + " " + nvl(vehicle.getModel()), F_REG, 11);
        text(cs, 300, blockTop - 34, "Reg No   : " + nvl(vehicle.getLicensePlate()), F_REG, 10);
        text(cs, 300, blockTop - 49, "Year     : " + (vehicle.getYear() > 0 ? vehicle.getYear() : "-"), F_REG, 10);
        if (notBlank(vehicle.getVin())) {
            text(cs, 300, blockTop - 64, "VIN      : " + nvl(vehicle.getVin()), F_REG, 10);
        }
        if (booking.getOdometer() != null) {
            text(cs, 300, blockTop - 79, "Odometer : " + booking.getOdometer() + " km", F_REG, 10);
        }
        rule(cs, 60, 538);

        text(cs, MARGIN, 522, "SERVICES RENDERED", F_BOLD, 12);

        float headerY = 500;
        text(cs, MARGIN, headerY, "No.", F_BOLD, 10);
        text(cs, 95, headerY, "Service", F_BOLD, 10);
        textRight(cs, RIGHT, headerY, "Amount (Rs.)", F_BOLD, 10);
        rule(cs, 60, headerY - 6);

        List<BookingItem> items = booking.getItems();
        float y = headerY - 22;
        int no = 1;
        for (BookingItem item : items) {
            text(cs, MARGIN, y, Integer.toString(no++), F_REG, 10);
            text(cs, 95, y, truncate(nvl(item.getServiceName()), 52), F_REG, 10);
            textRight(cs, RIGHT, y, money(item.getCost()), F_REG, 10);
            y -= 20;
        }

        rule(cs, 60, y - 8);

        float totalY = y - 26;
        text(cs, MARGIN, totalY, "TOTAL AMOUNT", F_BOLD, 12);
        textRight(cs, RIGHT, totalY, money(total), F_BOLD, 12);

        if (notBlank(booking.getTechnician())) {
            text(cs, MARGIN, totalY - 18, "Technician : " + nvl(booking.getTechnician()), F_REG, 9);
        }

        text(cs, centerX("THANK YOU FOR YOUR BUSINESS!", 10), 90, "THANK YOU FOR YOUR BUSINESS!", F_BOLD, 10);
        text(cs, centerX("This is a computer generated invoice.", 9), 74,
                "This is a computer generated invoice.", F_REG, 9);
        text(cs, centerX("Vehicle Service Management System", 9), 54,
                "Vehicle Service Management System", F_REG, 9);

        return cs.toString();
    }

    private static final String F_REG = "F1";
    private static final String F_BOLD = "F2";

    private static void text(StringBuilder cs, float x, float y, String s, String font, float size) {
        cs.append("BT\n/")
          .append(font).append(' ').append(num(size)).append(" Tf\n")
          .append(num(x)).append(' ').append(num(y)).append(" Td\n(")
          .append(escape(s)).append(") Tj\nET\n");
    }

    private static void textRight(StringBuilder cs, float right, float y, String s, String font, float size) {
        float approxWidth = s.length() * size * 0.5f;
        text(cs, right - approxWidth, y, s, font, size);
    }

    private static void rule(StringBuilder cs, float x1, float y) {
        cs.append("0 G\n")
          .append(num(x1)).append(' ').append(num(y)).append(" m ")
          .append(num(RIGHT)).append(' ').append(num(y)).append(" l S\n");
    }

    private static float centerX(String s, float size) {
        float approxWidth = s.length() * size * 0.5f;
        return (PAGE_W - approxWidth) / 2f;
    }

    private static String escape(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' || c == '(' || c == ')') {
                sb.append('\\');
            }
            if (c > 255) {
                sb.append('?');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String num(float v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    private static String money(BigDecimal amount) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        return String.format(Locale.ROOT, "%,.2f", amount);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static String nvl(String s) {
        return s == null ? "" : s.trim();
    }
}