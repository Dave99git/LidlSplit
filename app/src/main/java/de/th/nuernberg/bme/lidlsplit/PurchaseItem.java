package de.th.nuernberg.bme.lidlsplit;

public class PurchaseItem {
    private final String name;
    private final double price;

    public PurchaseItem(String name, double price) {
        this.name = name;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }
}
