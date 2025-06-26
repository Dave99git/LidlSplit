package de.th.nuernberg.bme.lidlsplit;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.th.nuernberg.bme.lidlsplit.Article;
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
        Log.d("ReceiptParser", "OCR-Rohtext:\n" + text);

        List<PurchaseItem> items = new ArrayList<>();
        double total = 0.0;
        String street = null;
        String city = null;
        LocalDateTime dateTime = null;

        String[] lines = text.split("\n");
        if (lines.length > 0) street = lines[0].trim();
        if (lines.length > 1) city = lines[1].trim();

        if (street != null || city != null) {
            Log.d("ReceiptParser", "Adresse erkannt: " + street + ", " + city);
        }

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
                    Log.d("ReceiptParser", "Gesamtbetrag erkannt: " + total);
                    continue;
                }
                Matcher euroMatcher = PRICE_ONLY_PATTERN.matcher(line);
                if (total == 0.0 && euroMatcher.matches() && line.toLowerCase().contains("eur")) {
                    total = parseDouble(euroMatcher.group(1));
                    Log.d("ReceiptParser", "Gesamtbetrag erkannt: " + total);
                    continue;
                }
            }

            Matcher advMatcher = ADVANTAGE_PATTERN.matcher(line);
            if (advMatcher.matches() && lastItem != null) {
                double adv = parseDouble(advMatcher.group(1));
                String oldName = lastItem.getName();
                double newPrice = lastItem.getPrice() + adv;
                if (newPrice >= 0) {
                    lastItem = new PurchaseItem(oldName, newPrice);
                    items.set(items.size() - 1, lastItem);
                } else {
                    items.remove(items.size() - 1);
                    lastItem = null;
                }
                Log.d("ReceiptParser", "Preisvorteil erkannt f\u00fcr: " + oldName + " -> Neuer Preis: " + newPrice);
                continue;
            }

            Matcher priceMatcher = PRICE_ONLY_PATTERN.matcher(line);
            if (priceMatcher.matches() && pendingName != null) {
                double price = parseDouble(priceMatcher.group(1));
                lastItem = new PurchaseItem(pendingName, price);
                Log.d("ReceiptParser", "Erkannt: Artikel: " + pendingName + " / Preis: " + price);
                items.add(lastItem);
                pendingName = null;
                continue;
            }

            Matcher itemMatcher = ITEM_PATTERN.matcher(line);
            if (itemMatcher.matches()) {
                String name = itemMatcher.group(1).trim();
                double price = parseDouble(itemMatcher.group(2));
                lastItem = new PurchaseItem(name, price);
                Log.d("ReceiptParser", "Erkannt: Artikel: " + name + " / Preis: " + price);
                items.add(lastItem);
                pendingName = null;
                continue;
            }

            if (dateTime == null) {
                Matcher dtMatcher = DATE_TIME_PATTERN.matcher(line);
                if (dtMatcher.find()) {
                    DateTimeFormatter df = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                    dateTime = LocalDateTime.parse(dtMatcher.group(1) + " " + dtMatcher.group(2), df);
                    Log.d("ReceiptParser", "Datum erkannt: " + dateTime);
                    continue;
                }
                Matcher dMatcher = DATE_ONLY_PATTERN.matcher(line);
                if (dMatcher.find()) {
                    DateTimeFormatter df = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                    LocalDate d = LocalDate.parse(dMatcher.group(1), df);
                    dateTime = LocalDateTime.of(d, LocalTime.MIDNIGHT);
                    Log.d("ReceiptParser", "Datum erkannt: " + dateTime);
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
    public static ParsedReceipt parseReceipt(String ocrText) {
        List<Article> articleList = new ArrayList<>();
        String[] lines = ocrText.split("\n");

        String address = "";
        String date = "";
        double totalPrice = 0.0;

        Pattern pricePattern = Pattern.compile("^-?[0-9]{1,3},[0-9]{2}$");
        Pattern datePattern = Pattern.compile("\\b(\\d{2}\\.\\d{2}\\.\\d{2,4})\\b");

        if (lines.length > 0) {
            address = lines[0].trim();
        }
        if (lines.length > 1 && lines[1].trim().matches(".*\\d{5}.*")) {
            address += (address.isEmpty() ? "" : ", ") + lines[1].trim();
        }

        for (int i = 0; i < lines.length; i++) {
            String currentLine = lines[i].trim();
            Log.d("ReceiptParser", "Zeile[" + i + "]: " + currentLine);

            Matcher dateMatcher = datePattern.matcher(currentLine);
            if (dateMatcher.find()) {
                date = dateMatcher.group(1);
            }

            String lower = currentLine.toLowerCase();
            if (lower.contains("gesamt") || lower.contains("summe")) {
                Matcher m = Pattern.compile("(-?[0-9]{1,3},[0-9]{2})").matcher(currentLine);
                if (m.find()) {
                    totalPrice = parseGermanPrice(m.group(1));
                }
                continue;
            }

            if (lower.contains("pfand") || lower.contains("girocard") || lower.contains("kartenzahlung") ||
                    lower.contains("ust-id") || lower.contains("beleg-nr") || lower.contains("seriennr")) {
                continue;
            }

            if (lower.contains("preisvorteil") && !articleList.isEmpty()) {
                Matcher m = Pattern.compile("(-?[0-9]{1,3},[0-9]{2})").matcher(currentLine);
                if (m.find()) {
                    double adv = parseGermanPrice(m.group(1));
                    Article last = articleList.get(articleList.size() - 1);
                    last.setPrice(last.getPrice() + adv);
                }
                continue;
            }

            if (i + 1 < lines.length) {
                String nextLine = lines[i + 1].trim();
                if (pricePattern.matcher(nextLine).matches() && !pricePattern.matcher(currentLine).matches()) {
                    articleList.add(new Article(currentLine, parseGermanPrice(nextLine)));
                    i++; // skip price line
                    continue;
                }
            }

            Matcher inlineItem = Pattern.compile("^(.+?)\\s+(-?[0-9]{1,3},[0-9]{2})\\s*[A-Z]?$").matcher(currentLine);
            if (inlineItem.matches()) {
                String name = inlineItem.group(1).trim();
                if (name.toLowerCase().contains("pfand") || name.toLowerCase().contains("girocard") ||
                        name.toLowerCase().contains("kartenzahlung") || name.toLowerCase().contains("ust-id") ||
                        name.toLowerCase().contains("beleg-nr") || name.toLowerCase().contains("seriennr")) {
                    continue;
                }
                double price = parseGermanPrice(inlineItem.group(2));
                articleList.add(new Article(name, price));
            }
        }

        Log.d("ReceiptParser", "Adresse: " + address);
        Log.d("ReceiptParser", "Datum: " + date);
        Log.d("ReceiptParser", "Gesamtpreis: " + totalPrice);

        for (Article a : articleList) {
            Log.d("ReceiptParser", "Artikel: " + a.getName() + " / " + a.getPrice());
        }

        return new ParsedReceipt(articleList, date, totalPrice, address);
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

    private static double parseGermanPrice(String priceText) {
        return Double.parseDouble(priceText.replace(".", "").replace(",", "."));
    }
}
