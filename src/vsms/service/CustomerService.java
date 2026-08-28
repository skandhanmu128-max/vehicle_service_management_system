package vsms.service;

import java.sql.SQLException;
import java.util.List;

import vsms.dao.CustomerDAO;
import vsms.dao.VehicleDAO;
import vsms.model.Customer;
import vsms.model.Vehicle;

public class CustomerService {

    private final CustomerDAO customerDAO = new CustomerDAO();
    private final VehicleDAO vehicleDAO = new VehicleDAO();

    public Customer register(String name, String phone, String email, String address)
            throws SQLException, IllegalArgumentException {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Customer name is required.");
        }
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Phone number is required.");
        }
        if (customerDAO.findByPhone(phone) != null) {
            throw new IllegalArgumentException("A customer with phone " + phone + " already exists.");
        }
        Customer c = new Customer(0, name.trim(), phone.trim(), email, address);
        int id = customerDAO.insert(c);
        c.setId(id);
        return c;
    }

    public Customer findById(int id) throws SQLException {
        return customerDAO.findById(id);
    }

    public Customer findByPhone(String phone) throws SQLException {
        return customerDAO.findByPhone(phone);
    }

    public List<Customer> findAll() throws SQLException {
        return customerDAO.findAll();
    }

    public void update(Customer c) throws SQLException, IllegalArgumentException {
        if (c == null || c.getId() <= 0) {
            throw new IllegalArgumentException("Invalid customer.");
        }
        if (c.getName() == null || c.getName().isBlank()) {
            throw new IllegalArgumentException("Customer name is required.");
        }
        Customer existing = customerDAO.findByPhone(c.getPhone());
        if (existing != null && existing.getId() != c.getId()) {
            throw new IllegalArgumentException("Another customer already uses phone " + c.getPhone());
        }
        customerDAO.update(c);
    }

    public boolean delete(int id) throws SQLException {
        if (customerDAO.countVehicles(id) > 0) {
            throw new IllegalStateException(
                    "Cannot delete: customer has registered vehicles (they are deleted with it). "
                    + "Remove the vehicles first if you want to keep the customer.");
        }
        return customerDAO.delete(id);
    }

    public List<Vehicle> getVehicles(int customerId) throws SQLException {
        return vehicleDAO.findByCustomerId(customerId);
    }

    public int countVehicles(int customerId) throws SQLException {
        return customerDAO.countVehicles(customerId);
    }
}