package com.unza.clinic.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "drugs")
public class Drug {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String drugId;
    private String name;
    private String category;
    private String drugType;
    private String batchNumber;
    private Integer stock;
    private Integer reorderLevel;
    private String unit;
    private String expiry;
    private String storageLocation;
    private String status;

    public Drug() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDrugId() { return drugId; }
    public void setDrugId(String drugId) { this.drugId = drugId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDrugType() { return drugType; }
    public void setDrugType(String drugType) { this.drugType = drugType; }
    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Integer getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(Integer reorderLevel) { this.reorderLevel = reorderLevel; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getExpiry() { return expiry; }
    public void setExpiry(String expiry) { this.expiry = expiry; }
    public String getStorageLocation() { return storageLocation; }
    public void setStorageLocation(String storageLocation) { this.storageLocation = storageLocation; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
