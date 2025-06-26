package de.th.nuernberg.bme.lidlsplit;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.util.Log;

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
            Pattern.compile("(?i)(?:gesamtsumme|summe|gesamt|zu\\s+zahlen).*?(\\d+[.,]?\\d*)");
    private static final Pattern DATE_TIME_PATTERN =
            Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4})\\s+(\\d{2}:\\d{2})");
    private static final Pattern DATE_ONLY_PATTERN =
            Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4})");
    private static final Pattern ADVANTAGE_PATTERN =
            Pattern.compile("(?i)preisvorteil\\s+(-?\\d+[.,]?\\d*)");
    private static final Pattern PRICE_ONLY_PATTERN =
            Pattern.compile("(-?\\d+[.,]\\d{2})\\s*(?:€|EUR)?\\s*[A-Z]?$");
    private static final Pattern IGNORE_LINE_PATTERN =
            Pattern.compile("(?i)(TA-?Nr|TSE|Bonkopie|Seriennummer)");

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
        String pendingName = null;
        for (int i = 2; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }

            if (IGNORE_LINE_PATTERN.matcher(line).find()) {
                continue;
            }

            if (total == 0.0) {
                Matcher totalMatcher = TOTAL_PATTERN.matcher(line);
                if (totalMatcher.find()) {
                    total = parseDouble(totalMatcher.group(1));
                    continue;
                }
                Matcher euroMatcher = PRICE_ONLY_PATTERN.matcher(line);
                if (total == 0.0 && euroMatcher.matches() && line.toLowerCase().contains("eur")) {
                    total = parseDouble(euroMatcher.group(1));
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

            Matcher priceMatcher = PRICE_ONLY_PATTERN.matcher(line);
            if (priceMatcher.matches() && pendingName != null) {
                double price = parseDouble(priceMatcher.group(1));
                lastItem = new PurchaseItem(pendingName, price);
                items.add(lastItem);
                pendingName = null;
                continue;
            }

            Matcher itemMatcher = ITEM_PATTERN.matcher(line);
            if (itemMatcher.matches()) {
                String name = itemMatcher.group(1).trim();
                double price = parseDouble(itemMatcher.group(2));
                lastItem = new PurchaseItem(name, price);
                items.add(lastItem);
                pendingName = null;
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
                    continue;
                }
            }

            // treat as item name if nothing else matched
            pendingName = line;
        }

        for (PurchaseItem item : items) {
            Log.d("ReceiptParser", "Artikel: " + item.getName() + " / " + item.getPrice());
        }
        if (street != null || city != null) {
            StringBuilder addr = new StringBuilder();
            if (street != null) addr.append(street);
            if (city != null) {
                if (addr.length() > 0) addr.append(", ");
                addr.append(city);
            }
            Log.d("ReceiptParser", "Adresse: " + addr.toString());
        }
        if (dateTime != null) {
            Log.d("ReceiptParser", "Datum: " + dateTime.toString());
        }
        Log.d("ReceiptParser", "Gesamtpreis: " + total);

        return new ReceiptData(items, total, street, city, dateTime);
    }

    /**
     * Parses the provided receipt text and extracts all relevant information
     * using regular expressions. The returned {@link ParsedReceipt} contains a
     * list of articles, the address, the purchase date and the total price. The
     * implementation mirrors the requirements of the kata and does not rely on
     * the {@link #parse(String)} method above so that the logic can be followed
     * more easily.
     */
    public static ParsedReceipt parseReceipt(String text) {
        List<Artikel> artikelListe = new ArrayList<>();
        String adresse = null;
        String datum = null;
        double gesamtpreis = 0.0;

        String[] lines = text.split("\n");

        if (lines.length > 0) {
            adresse = lines[0].trim();
        }
        if (lines.length > 1) {
            String city = lines[1].trim();
            if (adresse == null || adresse.isEmpty()) {
                adresse = city;
            } else {
                adresse = adresse + ", " + city;
            }
        }

        Pattern itemLine = Pattern.compile("^(.+?)\\s+(\\d+[.,]\\d{2})\\s*(?:€|EUR)?\\s*[A-Z]?$");
        Pattern priceOnly = Pattern.compile("(-?\\d+[.,]\\d{2})\\s*(?:€|EUR)?\\s*[A-Z]?$");
        Pattern totalPattern = Pattern.compile("(?i)(?:gesamtsumme|summe|gesamt|zu\\s+zahlen).*?(\\d+[.,]\\d{2})");
        Pattern advantagePattern = Pattern.compile("(?i)preisvorteil\\s+(-?\\d+[.,]\\d{2})");
        Pattern datePattern = Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4})");

        Artikel lastItem = null;
        String pendingName = null;

        for (int i = 2; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }

            if (gesamtpreis == 0.0) {
                Matcher tot = totalPattern.matcher(line);
                if (tot.find()) {
                    gesamtpreis = parseDouble(tot.group(1));
                    continue;
                }
            }

            Matcher adv = advantagePattern.matcher(line);
            if (adv.matches() && lastItem != null) {
                double diff = parseDouble(adv.group(1));
                lastItem.preis += diff;
                if (lastItem.preis < 0) {
                    artikelListe.remove(artikelListe.size() - 1);
                    lastItem = null;
                }
                continue;
            }

            Matcher priceM = priceOnly.matcher(line);
            if (priceM.matches() && pendingName != null) {
                double price = parseDouble(priceM.group(1));
                lastItem = new Artikel(pendingName, price);
                artikelListe.add(lastItem);
                pendingName = null;
                continue;
            }

            Matcher itemM = itemLine.matcher(line);
            if (itemM.matches()) {
                String name = itemM.group(1).trim();
                double price = parseDouble(itemM.group(2));
                lastItem = new Artikel(name, price);
                artikelListe.add(lastItem);
                pendingName = null;
                continue;
            }

            if (datum == null) {
                Matcher dateM = datePattern.matcher(line);
                if (dateM.find()) {
                    datum = dateM.group(1);
                    continue;
                }
            }

            pendingName = line;
        }

        return new ParsedReceipt(artikelListe, datum, gesamtpreis, adresse);
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

        ReceiptParser parser = new ReceiptParser();
        ReceiptData data = parser.parse(text);

        if (data.getStreet() != null) {
            adresse = data.getStreet();
        }
        if (data.getCity() != null) {
            adresse = adresse.isEmpty() ? data.getCity() : adresse + ", " + data.getCity();
        }
        if (data.getDateTime() != null) {
            DateTimeFormatter df = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            datum = data.getDateTime().toLocalDate().format(df);
        }
        gesamtpreis = data.getTotal();

        List<Artikel> artikelListe = new ArrayList<>();
        for (PurchaseItem pi : data.getItems()) {
            artikelListe.add(new Artikel(pi.getName(), pi.getPrice()));
        }

        return artikelListe;
    }

    private static double parseDouble(String value) {
        return Double.parseDouble(value.replace(",", "."));
    }
}
