package vsms.service;

import java.sql.SQLException;
import java.util.List;

import vsms.dao.CustomerDAO;
import vsms.dao.VehicleDAO;
import vsms.model.Customer;
import vsms.model.Vehicle;

public class VehicleService {

    private final VehicleDAO vehicleDAO = new VehicleDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();

    public Vehicle register(int customerId, String make, String model, int year,
                            String licensePlate, String vin)
            throws SQLException, IllegalArgumentException {
        if (customerDAO.findById(customerId) == null) {
            throw new IllegalArgumentException("Customer id " + customerId + " does not exist.");
        }
        if (make == null || make.isBlank() || model == null || model.isBlank()) {
            throw new IllegalArgumentException("Vehicle make and model are required.");
        }
        if (licensePlate == null || licensePlate.isBlank()) {
            throw new IllegalArgumentException("License plate is required.");
        }
        Vehicle v = new Vehicle(0, customerId, make.trim(), model.trim(),
                year, licensePlate.trim(), vin);
        int id = vehicleDAO.insert(v);
        v.setId(id);
        return v;
    }

    public Vehicle findById(int id) throws SQLException {
        return vehicleDAO.findById(id);
    }

    public List<Vehicle> findByCustomer(int customerId) throws SQLException {
        return vehicleDAO.findByCustomerId(customerId);
    }

    public List<Vehicle> findAll() throws SQLException {
        return vehicleDAO.findAll();
    }

    public Customer getOwner(int customerId) throws SQLException {
        return customerDAO.findById(customerId);
    }

    public void update(Vehicle v) throws SQLException, IllegalArgumentException {
        if (v == null || v.getId() <= 0) {
            throw new IllegalArgumentException("Invalid vehicle.");
        }
        vehicleDAO.update(v);
    }

    public boolean delete(int id) throws SQLException {
        return vehicleDAO.delete(id);
    }
}