package de.th.nuernberg.bme.lidlsplit;

public class Purchase {
    private final long id;
    private final String date;
    private final double amount;
    private final boolean paid;

    public Purchase(long id, String date, double amount, boolean paid) {
        this.id = id;
        this.date = date;
        this.amount = amount;
        this.paid = paid;
    }

    public long getId() {
        return id;
    }

    public String getDate() {
        return date;
    }

    public double getAmount() {
        return amount;
    }

    public boolean isPaid() {
        return paid;
    }
}
