package de.th.nuernberg.bme.lidlsplit;

public class Artikel {
    public String name;
    public double preis;

    public Artikel(String name, double preis) {
        this.name = name;
        this.preis = preis;
    }

    @Override
    public String toString() {
        return name + ": " + preis + " €";
    }
}
