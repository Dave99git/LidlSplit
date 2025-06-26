package de.th.nuernberg.bme.lidlsplit;

public class Article {
    private String name;
    private double price;

    public Article(String name, double price) {
        this.name = name;
        this.price = price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }
}
