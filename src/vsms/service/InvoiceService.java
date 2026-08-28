package vsms.service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import vsms.dao.BookingDAO;
import vsms.dao.InvoiceDAO;
import vsms.dao.VehicleDAO;
import vsms.model.Booking;
import vsms.model.Customer;
import vsms.model.Invoice;
import vsms.model.Vehicle;
import vsms.pdf.PdfInvoiceGenerator;

public class InvoiceService {

    private final BookingDAO bookingDAO = new BookingDAO();
    private final InvoiceDAO invoiceDAO = new InvoiceDAO();
    private final VehicleDAO vehicleDAO = new VehicleDAO();
    private final CustomerService customerService = new CustomerService();

    public Invoice generateInvoice(int bookingId)
            throws SQLException, IllegalStateException, IOException {
        Booking booking = bookingDAO.findById(bookingId);
        if (booking == null) {
            throw new IllegalStateException("Booking not found.");
        }
        if (!booking.isCompleted()) {
            throw new IllegalStateException(
                    "Invoices can only be generated for COMPLETED bookings. Current status: " + booking.getStatus());
        }
        if (invoiceDAO.findByBookingId(bookingId) != null) {
            throw new IllegalStateException("An invoice already exists for this booking.");
        }

        Vehicle vehicle = vehicleDAO.findById(booking.getVehicleId());
        Customer customer = customerService.findById(vehicle.getCustomerId());

        BigDecimal total = booking.getItems().stream()
                .map(item -> item.getCost())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String invoiceNumber = invoiceDAO.nextInvoiceNumber();
        String fileName = invoiceNumber + ".pdf";
        String pdfPath = PdfInvoiceGenerator.generate(booking, customer, vehicle,
                invoiceNumber, total, fileName);

        Invoice invoice = new Invoice();
        invoice.setBookingId(bookingId);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setTotal(total);
        invoice.setPdfPath(pdfPath);
        int id = invoiceDAO.insert(invoice);
        invoice.setId(id);
        return invoice;
    }

    public Invoice findByBookingId(int bookingId) throws SQLException {
        return invoiceDAO.findByBookingId(bookingId);
    }

    public Invoice findByNumber(String invoiceNumber) throws SQLException {
        return invoiceDAO.findByNumber(invoiceNumber);
    }

    public List<Invoice> findAll() throws SQLException {
        return invoiceDAO.findAll();
    }

    public boolean openPdf(Invoice invoice) {
        if (invoice == null || invoice.getPdfPath() == null) {
            return false;
        }
        File pdf = new File(invoice.getPdfPath());
        if (!pdf.exists()) {
            return false;
        }
        try {
            String osSafePath = pdf.getAbsolutePath().replace('/', '\\');
            new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", osSafePath).start();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public String formatDateTime(java.time.LocalDateTime dt) {
        if (dt == null) {
            return "";
        }
        return new SimpleDateFormat("dd-MMM-yyyy HH:mm").format(
                Date.from(dt.atZone(java.time.ZoneId.systemDefault()).toInstant()));
    }
}