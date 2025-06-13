package de.th.nuernberg.bme.lidlsplit;

public class Purchase {
    private final String date;
    private final String amount;
    private final boolean paid;

    public Purchase(String date, String amount, boolean paid) {
        this.date = date;
        this.amount = amount;
        this.paid = paid;
    }

    public String getDate() {
        return date;
    }

    public String getAmount() {
        return amount;
    }

    public boolean isPaid() {
        return paid;
    }
}
