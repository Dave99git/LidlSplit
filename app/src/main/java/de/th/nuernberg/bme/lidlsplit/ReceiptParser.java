package de.th.nuernberg.bme.lidlsplit;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReceiptParser {

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
            Pattern.compile("^(.+?)\\s+(\\d+[.,]\\d{2})\\s*(?:€|EUR)?\\s*[A-Z]?$");

    /** Pattern for lines that should be ignored when parsing items */
    private static final Pattern IGNORE_PATTERN =
            Pattern.compile("(?i)(stra\\u00dfe|signaturzähler|ta-?nr)");
    private static final Pattern TOTAL_PATTERN =
            Pattern.compile("(?i)zu\\s+zahlen.*?(\\d+[.,]?\\d*)");
    private static final Pattern DATE_TIME_PATTERN =
            Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4})\\s+(\\d{2}:\\d{2})");
    private static final Pattern CITY_PATTERN =
            Pattern.compile("(\\d{5})\\s+(.+)");

    public ReceiptData parse(String text) {
        List<PurchaseItem> items = new ArrayList<>();
        double total = 0.0;
        String street = null;
        String city = null;
        LocalDateTime dateTime = null;

        String[] lines = text.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            if (IGNORE_PATTERN.matcher(line).find()) continue;

            if (total == 0.0) {
                Matcher totalMatcher = TOTAL_PATTERN.matcher(line);
                if (totalMatcher.find()) {
                    total = parseDouble(totalMatcher.group(1));
                    continue;
                }
            }

            Matcher itemMatcher = ITEM_PATTERN.matcher(line);
            if (itemMatcher.matches()) {
                String name = itemMatcher.group(1).trim();
                double price = parseDouble(itemMatcher.group(2));
                items.add(new PurchaseItem(name, price));
                continue;
            }

            if (dateTime == null) {
                Matcher dtMatcher = DATE_TIME_PATTERN.matcher(line);
                if (dtMatcher.find()) {
                    DateTimeFormatter df = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                    dateTime = LocalDateTime.parse(dtMatcher.group(1) + " " + dtMatcher.group(2), df);
                }
            }

            if (i < 5) {
                if (street == null && line.matches(".*\\d+.*")) {
                    street = line;
                    continue;
                }
                if (street != null && city == null) {
                    Matcher cityMatcher = CITY_PATTERN.matcher(line);
                    if (cityMatcher.matches()) {
                        city = cityMatcher.group(2).trim();
                    }
                }
            }
        }

        return new ReceiptData(items, total, street, city, dateTime);
    }

    private double parseDouble(String value) {
        return Double.parseDouble(value.replace(",", "."));
    }
}
