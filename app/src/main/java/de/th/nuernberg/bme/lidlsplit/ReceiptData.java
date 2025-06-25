package de.th.nuernberg.bme.lidlsplit;

import java.time.LocalDateTime;
import java.util.List;

public class ReceiptData {
    private final List<PurchaseItem> items;
    private final double total;
    private final String street;
    private final String city;
    private final LocalDateTime dateTime;

    public ReceiptData(List<PurchaseItem> items, double total, String street, String city, LocalDateTime dateTime) {
        this.items = items;
        this.total = total;
        this.street = street;
        this.city = city;
        this.dateTime = dateTime;
    }

    public List<PurchaseItem> getItems() {
        return items;
    }

    public double getTotal() {
        return total;
    }

    public String getStreet() {
        return street;
    }

    public String getCity() {
        return city;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }
}
