package vsms.model;

public class Vehicle {
    private int id;
    private int customerId;
    private String make;
    private String model;
    private int year;
    private String licensePlate;
    private String vin;

    public Vehicle() {
    }

    public Vehicle(int id, int customerId, String make, String model,
                   int year, String licensePlate, String vin) {
        this.id = id;
        this.customerId = customerId;
        this.make = make;
        this.model = model;
        this.year = year;
        this.licensePlate = licensePlate;
        this.vin = vin;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public String displayName() {
        return String.format("%s %s (%s)", make, model, licensePlate);
    }

    @Override
    public String toString() {
        return String.format("Vehicle[id=%d, customerId=%d, make=%s, model=%s, year=%d, plate=%s, vin=%s]",
                id, customerId, make, model, year, licensePlate, vin);
    }
}