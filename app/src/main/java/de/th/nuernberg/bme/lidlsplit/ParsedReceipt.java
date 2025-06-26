package de.th.nuernberg.bme.lidlsplit;

import java.util.List;

public class ParsedReceipt {
    private final List<Article> items;
    private final String date;
    private final double total;
    private final String address;

    public ParsedReceipt(List<Article> items, String date, double total, String address) {
        this.items = items;
        this.date = date;
        this.total = total;
        this.address = address;
    }

    public List<Article> getItems() {
        return items;
    }

    public String getDate() {
        return date;
    }

    public double getTotal() {
        return total;
    }

    public String getAddress() {
        return address;
    }
}
