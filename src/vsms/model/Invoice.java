package vsms.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Invoice {
    private int id;
    private int bookingId;
    private String invoiceNumber;
    private BigDecimal total;
    private String pdfPath;
    private LocalDateTime generatedAt;

    public Invoice() {
    }

    public Invoice(int id, int bookingId, String invoiceNumber,
                   BigDecimal total, String pdfPath, LocalDateTime generatedAt) {
        this.id = id;
        this.bookingId = bookingId;
        this.invoiceNumber = invoiceNumber;
        this.total = total;
        this.pdfPath = pdfPath;
        this.generatedAt = generatedAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getPdfPath() {
        return pdfPath;
    }

    public void setPdfPath(String pdfPath) {
        this.pdfPath = pdfPath;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    @Override
    public String toString() {
        return String.format("Invoice[number=%s, bookingId=%d, total=Rs. %s, path=%s]",
                invoiceNumber, bookingId, total, pdfPath);
    }
}