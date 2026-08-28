package vsms.model;

import java.math.BigDecimal;

public class ServiceType {
    private int id;
    private String name;
    private String description;
    private BigDecimal baseCost;

    public ServiceType() {
    }

    public ServiceType(int id, String name, String description, BigDecimal baseCost) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.baseCost = baseCost;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getBaseCost() {
        return baseCost;
    }

    public void setBaseCost(BigDecimal baseCost) {
        this.baseCost = baseCost;
    }

    @Override
    public String toString() {
        return String.format("%-3d %-22s %-40s Rs. %s", id, name, description, baseCost);
    }
}