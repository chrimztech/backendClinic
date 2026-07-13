package com.unza.clinic.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "lab_reference_ranges")
public class LabReferenceRange {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String rangeId;
    private String testName;
    private String category;
    private String gender;   // MALE, FEMALE, ALL
    private Integer minAge;
    private Integer maxAge;

    @Column(precision = 12, scale = 3)
    private BigDecimal minValue;

    @Column(precision = 12, scale = 3)
    private BigDecimal maxValue;

    @Column(precision = 12, scale = 3)
    private BigDecimal criticalLow;

    @Column(precision = 12, scale = 3)
    private BigDecimal criticalHigh;

    private String unit;

    @Column(columnDefinition = "TEXT")
    private String interpretationLow;

    @Column(columnDefinition = "TEXT")
    private String interpretationHigh;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public LabReferenceRange() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRangeId() { return rangeId; }
    public void setRangeId(String rangeId) { this.rangeId = rangeId; }
    public String getTestName() { return testName; }
    public void setTestName(String testName) { this.testName = testName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public Integer getMinAge() { return minAge; }
    public void setMinAge(Integer minAge) { this.minAge = minAge; }
    public Integer getMaxAge() { return maxAge; }
    public void setMaxAge(Integer maxAge) { this.maxAge = maxAge; }
    public BigDecimal getMinValue() { return minValue; }
    public void setMinValue(BigDecimal minValue) { this.minValue = minValue; }
    public BigDecimal getMaxValue() { return maxValue; }
    public void setMaxValue(BigDecimal maxValue) { this.maxValue = maxValue; }
    public BigDecimal getCriticalLow() { return criticalLow; }
    public void setCriticalLow(BigDecimal criticalLow) { this.criticalLow = criticalLow; }
    public BigDecimal getCriticalHigh() { return criticalHigh; }
    public void setCriticalHigh(BigDecimal criticalHigh) { this.criticalHigh = criticalHigh; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getInterpretationLow() { return interpretationLow; }
    public void setInterpretationLow(String interpretationLow) { this.interpretationLow = interpretationLow; }
    public String getInterpretationHigh() { return interpretationHigh; }
    public void setInterpretationHigh(String interpretationHigh) { this.interpretationHigh = interpretationHigh; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
