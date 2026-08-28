package vsms.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Booking {
    public static final String STATUS_BOOKED = "BOOKED";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private int id;
    private int vehicleId;
    private LocalDate scheduledDate;
    private String status;
    private Integer odometer;
    private String technician;
    private String notes;
    private List<BookingItem> items = new ArrayList<>();

    public Booking() {
        this.status = STATUS_BOOKED;
    }

    public Booking(int id, int vehicleId, LocalDate scheduledDate,
                   String status, Integer odometer, String technician, String notes) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.scheduledDate = scheduledDate;
        this.status = status;
        this.odometer = odometer;
        this.technician = technician;
        this.notes = notes;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(int vehicleId) {
        this.vehicleId = vehicleId;
    }

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(LocalDate scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getOdometer() {
        return odometer;
    }

    public void setOdometer(Integer odometer) {
        this.odometer = odometer;
    }

    public String getTechnician() {
        return technician;
    }

    public void setTechnician(String technician) {
        this.technician = technician;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<BookingItem> getItems() {
        return items;
    }

    public void setItems(List<BookingItem> items) {
        this.items = items;
    }

    public boolean isCompleted() {
        return STATUS_COMPLETED.equalsIgnoreCase(status);
    }

    @Override
    public String toString() {
        return String.format("Booking[id=%d, vehicleId=%d, date=%s, status=%s]",
                id, vehicleId, scheduledDate, status);
    }
}