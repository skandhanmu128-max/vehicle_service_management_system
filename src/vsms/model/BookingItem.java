package vsms.model;

import java.math.BigDecimal;

public class BookingItem {
    private int id;
    private int bookingId;
    private int serviceTypeId;
    private String serviceName;
    private BigDecimal cost;

    public BookingItem() {
    }

    public BookingItem(int id, int bookingId, int serviceTypeId, String serviceName, BigDecimal cost) {
        this.id = id;
        this.bookingId = bookingId;
        this.serviceTypeId = serviceTypeId;
        this.serviceName = serviceName;
        this.cost = cost;
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

    public int getServiceTypeId() {
        return serviceTypeId;
    }

    public void setServiceTypeId(int serviceTypeId) {
        this.serviceTypeId = serviceTypeId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }
}