package de.th.nuernberg.bme.lidlsplit;

public class Debt {
    private final Person debtor;
    private final Person creditor;
    private final double amount;
    private boolean settled;

    public Debt(Person debtor, Person creditor, double amount) {
        this.debtor = debtor;
        this.creditor = creditor;
        this.amount = amount;
        this.settled = false;
    }

    public Person getDebtor() {
        return debtor;
    }

    public Person getCreditor() {
        return creditor;
    }

    public double getAmount() {
        return amount;
    }

    public boolean isSettled() {
        return settled;
    }

    public void setSettled(boolean settled) {
        this.settled = settled;
    }
}
