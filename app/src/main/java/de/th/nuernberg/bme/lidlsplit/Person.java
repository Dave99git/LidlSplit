package de.th.nuernberg.bme.lidlsplit;

public class Person {
    private final long id;
    private final String name;

    public Person(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Person(String name) {
        this(-1, name);
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
