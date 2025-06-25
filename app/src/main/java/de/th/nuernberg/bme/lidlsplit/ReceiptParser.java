package de.th.nuernberg.bme.lidlsplit;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReceiptParser {

    private static final Pattern ITEM_PATTERN =
            Pattern.compile("^(.+?)\s+(-?[0-9]+,[0-9]{2})\s*A?$");
    private static final Pattern TOTAL_PATTERN =
            Pattern.compile("(?i)zu\\s+zahlen.*?(-?[0-9]+,[0-9]{2})");
    private static final Pattern DATE_TIME_PATTERN =
            Pattern.compile("(\d{2}\.\d{2}\.\d{4})\\s+(\d{2}:\d{2})");
    private static final Pattern CITY_PATTERN =
            Pattern.compile("(\d{5})\\s+(.+)");

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

            Matcher itemMatcher = ITEM_PATTERN.matcher(line);
            if (itemMatcher.matches()) {
                String name = itemMatcher.group(1).trim();
                double price = parseDouble(itemMatcher.group(2));
                items.add(new PurchaseItem(name, price));
                continue;
            }

            if (total == 0.0) {
                Matcher totalMatcher = TOTAL_PATTERN.matcher(line);
                if (totalMatcher.find()) {
                    total = parseDouble(totalMatcher.group(1));
                }
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
