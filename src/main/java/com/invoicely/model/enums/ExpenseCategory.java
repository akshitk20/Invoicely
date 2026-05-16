package com.invoicely.model.enums;

public enum ExpenseCategory {
    INTERNET_PHONE("Internet & Phone"),
    SOFTWARE_SUBSCRIPTIONS("Software & Subscriptions"),
    HARDWARE_EQUIPMENT("Hardware & Equipment"),
    TRAVEL_TRANSPORT("Travel & Transport"),
    OFFICE_RENT("Office Rent"),
    PROFESSIONAL_SERVICES("Professional Services"),
    MARKETING_ADS("Marketing & Ads"),
    OTHERS("Others");

    private final String displayName;

    ExpenseCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
