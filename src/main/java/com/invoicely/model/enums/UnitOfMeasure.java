package com.invoicely.model.enums;

public enum UnitOfMeasure {
    PCS("Pieces"),
    KG("Kilograms"),
    MTR("Meters"),
    LTR("Litres"),
    BOX("Boxes");

    private final String displayName;

    UnitOfMeasure(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
