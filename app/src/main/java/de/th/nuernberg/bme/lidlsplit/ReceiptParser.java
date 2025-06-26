package de.th.nuernberg.bme.lidlsplit;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReceiptParser {

    /**
     * Simple parsing API used by some older components of the app. The fields
     * below are populated by {@link #parseBon(String)}. They do not interfere
     * with the newer {@link #parse(String)} method which returns a
     * {@link ReceiptData} object.
     */
    public static String adresse = "";
    public static String datum = "";
    public static double gesamtpreis = 0.0;

    /**
     * Matches a single receipt line consisting of the item name followed by a price. The
     * recognised receipts occasionally contain additional trailing characters such as an euro
     * sign or the letter "A" used for deposits. These extras should be ignored when parsing.
     *
     * Example lines that should match:
     * <pre>
     * Laugenbrezel 10er      1,99
     * LAUGENBREZEL 10ER 1,99 €
     * MILCH 0,89A
     * </pre>
     */
    private static final Pattern ITEM_PATTERN =
            Pattern.compile("^(.+?)\\s+(\\d+[.,]?\\d*)\\s*(?:€|EUR)?\\s*[A-Z]?$");
    private static final Pattern TOTAL_PATTERN =
            Pattern.compile("(?i)zu\\s+zahlen.*?(\\d+[.,]?\\d*)");
    private static final Pattern DATE_TIME_PATTERN =
            Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4})\\s+(\\d{2}:\\d{2})");
    private static final Pattern DATE_ONLY_PATTERN =
            Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4})");
    private static final Pattern ADVANTAGE_PATTERN =
            Pattern.compile("(?i)preisvorteil\\s+(-?\\d+[.,]?\\d*)");

    public ReceiptData parse(String text) {
        List<PurchaseItem> items = new ArrayList<>();
        double total = 0.0;
        String street = null;
        String city = null;
        LocalDateTime dateTime = null;

        String[] lines = text.split("\n");
        if (lines.length > 0) street = lines[0].trim();
        if (lines.length > 1) city = lines[1].trim();

        PurchaseItem lastItem = null;
        for (int i = 2; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            if (total == 0.0) {
                Matcher totalMatcher = TOTAL_PATTERN.matcher(line);
                if (totalMatcher.find()) {
                    total = parseDouble(totalMatcher.group(1));
                    continue;
                }
            }

            Matcher advMatcher = ADVANTAGE_PATTERN.matcher(line);
            if (advMatcher.matches() && lastItem != null) {
                double adv = parseDouble(advMatcher.group(1));
                double newPrice = lastItem.getPrice() + adv;
                if (newPrice >= 0) {
                    lastItem = new PurchaseItem(lastItem.getName(), newPrice);
                    items.set(items.size() - 1, lastItem);
                } else {
                    items.remove(items.size() - 1);
                    lastItem = null;
                }
                continue;
            }

            Matcher itemMatcher = ITEM_PATTERN.matcher(line);
            if (itemMatcher.matches()) {
                String name = itemMatcher.group(1).trim();
                double price = parseDouble(itemMatcher.group(2));
                lastItem = new PurchaseItem(name, price);
                items.add(lastItem);
                continue;
            }

            if (dateTime == null) {
                Matcher dtMatcher = DATE_TIME_PATTERN.matcher(line);
                if (dtMatcher.find()) {
                    DateTimeFormatter df = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                    dateTime = LocalDateTime.parse(dtMatcher.group(1) + " " + dtMatcher.group(2), df);
                    continue;
                }
                Matcher dMatcher = DATE_ONLY_PATTERN.matcher(line);
                if (dMatcher.find()) {
                    DateTimeFormatter df = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                    LocalDate d = LocalDate.parse(dMatcher.group(1), df);
                    dateTime = LocalDateTime.of(d, LocalTime.MIDNIGHT);
                }
            }
        }

        return new ReceiptData(items, total, street, city, dateTime);
    }

    /**
     * Parses a receipt into a simple list of {@link Artikel} objects and
     * populates the static fields {@link #adresse}, {@link #datum} and
     * {@link #gesamtpreis}. This method is intentionally simple and primarily
     * used for unit tests in this kata.
     */
    public static List<Artikel> parseBon(String text) {
        adresse = "";
        datum = "";
        gesamtpreis = 0.0;

        List<Artikel> artikelListe = new ArrayList<>();
        String[] lines = text.split("\n");
        if (lines.length > 0) {
            adresse = lines[0].trim();
        }
        if (lines.length > 1) {
            adresse = adresse.isEmpty() ? lines[1].trim()
                    : adresse + ", " + lines[1].trim();
        }

        Artikel lastArtikel = null;
        Pattern itemPattern = Pattern.compile("^(.+?)\\s+(\\d+[.,]?\\d*)\\s*(?:€|EUR)?\\s*[A-Z]?$");
        Pattern advPattern = Pattern.compile("(?i)preisvorteil\\s+(-?\\d+[.,]?\\d*)");
        Pattern totalPattern = Pattern.compile("(?i)(gesamtsumme|summe|gesamt|zu\\s+zahlen).*?(\\d+[.,]?\\d*)");
        Pattern datePattern = Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4})");
        Pattern ignorePattern = Pattern.compile("(?i)(Allersberger\\s+Stra\\w*|Signaturzähler|TA-?Nr\.)");

        for (int i = 2; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }

            if (ignorePattern.matcher(line).find()) {
                continue;
            }

            Matcher advMatcher = advPattern.matcher(line);
            if (advMatcher.matches() && lastArtikel != null) {
                double diff = parseDouble(advMatcher.group(1));
                lastArtikel.preis += diff;
                if (lastArtikel.preis < 0) {
                    artikelListe.remove(artikelListe.size() - 1);
                    lastArtikel = null;
                }
                continue;
            }

            Matcher itemMatcher = itemPattern.matcher(line);
            if (itemMatcher.matches()) {
                String name = itemMatcher.group(1).trim();
                double preis = parseDouble(itemMatcher.group(2));
                lastArtikel = new Artikel(name, preis);
                artikelListe.add(lastArtikel);
                continue;
            }

            Matcher totalMatcher = totalPattern.matcher(line);
            if (totalMatcher.find()) {
                gesamtpreis = parseDouble(totalMatcher.group(2));
                continue;
            }

            Matcher dateMatcher = datePattern.matcher(line);
            if (dateMatcher.find()) {
                datum = dateMatcher.group(1);
            }
        }

        return artikelListe;
    }

    private static double parseDouble(String value) {
        return Double.parseDouble(value.replace(",", "."));
    }
}
