package de.th.nuernberg.bme.lidlsplit;

public class Purchase {
    private final long id;
    private final String date;
    private final double amount;
    private final boolean paid;

    private long payerId;
    private java.util.List<Person> persons;
    private java.util.List<Article> articles;
    private java.util.Map<Article, java.util.List<Person>> assignments;
    private java.util.Map<DebtPair, Boolean> debtStatus;

    public Purchase(long id, String date, double amount, boolean paid) {
        this(id, date, amount, paid, -1, new java.util.ArrayList<>(), new java.util.ArrayList<>(), new java.util.HashMap<>(), new java.util.HashMap<>());
    }

    public Purchase(long id, String date, double amount, boolean paid,
                    long payerId,
                    java.util.List<Person> persons,
                    java.util.List<Article> articles,
                    java.util.Map<Article, java.util.List<Person>> assignments,
                    java.util.Map<DebtPair, Boolean> debtStatus) {
        this.id = id;
        this.date = date;
        this.amount = amount;
        this.paid = paid;
        this.payerId = payerId;
        this.persons = persons;
        this.articles = articles;
        this.assignments = assignments;
        this.debtStatus = debtStatus;
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

    public long getPayerId() {
        return payerId;
    }

    public java.util.List<Person> getPersons() {
        return persons;
    }

    public java.util.List<Article> getArticles() {
        return articles;
    }

    public java.util.Map<Article, java.util.List<Person>> getAssignments() {
        return assignments;
    }

    public java.util.Map<DebtPair, Boolean> getDebtStatus() {
        return debtStatus;
    }
}
