package vsms.service;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import vsms.dao.BookingDAO;
import vsms.dao.ServiceTypeDAO;
import vsms.dao.VehicleDAO;
import vsms.model.Booking;
import vsms.model.BookingItem;
import vsms.model.ServiceType;
import vsms.model.Vehicle;

public class BookingService {

    private final BookingDAO bookingDAO = new BookingDAO();
    private final ServiceTypeDAO serviceTypeDAO = new ServiceTypeDAO();
    private final VehicleDAO vehicleDAO = new VehicleDAO();

    public Booking createBooking(int vehicleId, LocalDate scheduledDate, String notes,
                                 List<Integer> serviceTypeIds)
            throws SQLException, IllegalArgumentException {
        Vehicle vehicle = vehicleDAO.findById(vehicleId);
        if (vehicle == null) {
            throw new IllegalArgumentException("Vehicle id " + vehicleId + " does not exist.");
        }
        if (scheduledDate == null || scheduledDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Scheduled date must be today or later.");
        }
        if (serviceTypeIds == null || serviceTypeIds.isEmpty()) {
            throw new IllegalArgumentException("Select at least one service.");
        }

        Set<Integer> unique = new LinkedHashSet<>(serviceTypeIds);
        Booking booking = new Booking();
        booking.setVehicleId(vehicleId);
        booking.setScheduledDate(scheduledDate);
        booking.setNotes(notes);

        List<BookingItem> items = new ArrayList<>();
        for (Integer typeId : unique) {
            ServiceType st = serviceTypeDAO.findById(typeId);
            if (st == null) {
                throw new IllegalArgumentException("Service id " + typeId + " does not exist.");
            }
            BookingItem item = new BookingItem();
            item.setServiceTypeId(st.getId());
            item.setServiceName(st.getName());
            item.setCost(st.getBaseCost());
            items.add(item);
        }
        booking.setItems(items);

        int id = bookingDAO.insertWithItems(booking);
        booking.setId(id);
        return booking;
    }

    public Booking findById(int id) throws SQLException {
        return bookingDAO.findById(id);
    }

    public List<Booking> findAll() throws SQLException {
        return bookingDAO.findAll();
    }

    public List<Booking> findByStatus(String status) throws SQLException {
        return bookingDAO.findByStatus(status);
    }

    public List<Booking> findByVehicle(int vehicleId) throws SQLException {
        return bookingDAO.findByVehicleId(vehicleId);
    }

    public void startService(int bookingId) throws SQLException, IllegalStateException {
        Booking b = bookingDAO.findById(bookingId);
        if (b == null) {
            throw new IllegalStateException("Booking not found.");
        }
        if (!Booking.STATUS_BOOKED.equals(b.getStatus())) {
            throw new IllegalStateException("Only BOOKED bookings can be started (current: " + b.getStatus() + ").");
        }
        bookingDAO.updateStatus(bookingId, Booking.STATUS_IN_PROGRESS);
    }

    public void complete(int bookingId, int odometer, String technician)
            throws SQLException, IllegalStateException {
        Booking b = bookingDAO.findById(bookingId);
        if (b == null) {
            throw new IllegalStateException("Booking not found.");
        }
        if (Booking.STATUS_COMPLETED.equals(b.getStatus())) {
            throw new IllegalStateException("Booking is already completed.");
        }
        if (Booking.STATUS_CANCELLED.equals(b.getStatus())) {
            throw new IllegalStateException("A cancelled booking cannot be completed.");
        }
        bookingDAO.completeBooking(bookingId, odometer, technician);
    }

    public void cancel(int bookingId) throws SQLException, IllegalStateException {
        Booking b = bookingDAO.findById(bookingId);
        if (b == null) {
            throw new IllegalStateException("Booking not found.");
        }
        if (Booking.STATUS_COMPLETED.equals(b.getStatus())
                || Booking.STATUS_CANCELLED.equals(b.getStatus())) {
            throw new IllegalStateException("Only BOOKED / IN_PROGRESS bookings can be cancelled.");
        }
        bookingDAO.updateStatus(bookingId, Booking.STATUS_CANCELLED);
    }

    public boolean hasInvoice(int bookingId) throws SQLException {
        return bookingDAO.hasInvoice(bookingId);
    }

    public BigDecimal calculateTotal(Booking booking) {
        BigDecimal total = BigDecimal.ZERO;
        for (BookingItem item : booking.getItems()) {
            total = total.add(item.getCost());
        }
        return total;
    }

    public List<ServiceType> serviceCatalog() throws SQLException {
        return serviceTypeDAO.findAll();
    }
}