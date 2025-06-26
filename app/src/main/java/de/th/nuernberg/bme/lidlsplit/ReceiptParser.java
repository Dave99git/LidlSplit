package de.th.nuernberg.bme.lidlsplit;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.th.nuernberg.bme.lidlsplit.Article;
import android.util.Log;
import android.graphics.Rect;
import com.google.mlkit.vision.text.Text;

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
    // Item line: name followed by a price. The name must contain at least one
    // alphabetic character so that lines consisting only of numbers or symbols
    // are ignored. Digits are nevertheless allowed within the name (e.g. "10er")
    // which frequently occurs on Lidl receipts.
    private static final Pattern ITEM_PATTERN = Pattern.compile(
            "^([A-Za-zÄÖÜäöüß][A-Za-zÄÖÜäöüß0-9\\s\\-.]*?)\\s+(\\d+[.,]\\d{2})\\s*(?:€|EUR|A)?$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TOTAL_PATTERN =
            Pattern.compile("(?i)(?:gesamtsumme|summe|gesamt|zu\\s+zahlen|betrag).*?(\\d+[.,]?\\d*)");
    private static final Pattern DATE_PATTERN =
            Pattern.compile("(\\d{2}\\.\\d{2}\\.(?:\\d{2}|\\d{4}))(?:\\s+(\\d{2}:\\d{2}))?");
    private static final Pattern ISO_DATE_PATTERN =
            Pattern.compile("(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2})");
    private static final Pattern ADVANTAGE_PATTERN =
            Pattern.compile("(?i)preisvorteil.*?(-?\\d+[.,]\\d{2})");
    private static final Pattern DISCOUNT_PATTERN =
            Pattern.compile("^\\-\\d+[.,]\\d{2}$");
    private static final Pattern PRICE_ONLY_PATTERN =
            Pattern.compile("(-?\\d+[.,]\\d{2})\\s*(?:€|EUR)?\\s*[A-Z]?$");
    private static final Pattern PRICE_ELEMENT_PATTERN =
            Pattern.compile("-?\\d+[.,]\\d{2}A?");
    // Lines containing the following keywords should never be treated as item
    // names. This helps to avoid false positives such as "MWST" or "Karte" when
    // the OCR output is noisy.
    private static final Pattern IGNORE_LINE_PATTERN = Pattern.compile(
            "(?i)(TA-?Nr|TSE|Bonkopie|Seriennummer|Transaktionsnummer|UST-ID|Kartennr|Seriennr|Signatur|Beleg|Kontaktlos|Karte|MWST|Betrag|^EUR$|www\\.lidl\\.de)");

    // Certain lines on receipts should never be treated as item names even if
    // they look similar to normal text lines. These include total lines or tax
    // summaries which can easily be misinterpreted when using OCR.
    private static final List<String> FORBIDDEN_ARTICLE_NAMES = Arrays.asList(
            "zu zahlen",
            "gesamter preisvorteil",
            "a 7 %",
            "b 19%",
            "summe"
    );

    public ReceiptData parse(String text) {
        Log.d("ReceiptParser", "OCR-Rohtext:\n" + text);

        List<PurchaseItem> items = new ArrayList<>();
        double total = 0.0;
        String street = null;
        String city = null;
        LocalDateTime dateTime = null;

        String[] lines = text.split("\n");

        int startIndex = 0;
        for (int i = 0; i < lines.length; i++) {
            String l = lines[i].trim();
            String lower = l.toLowerCase();
            if (street == null && (lower.contains("straße") || lower.contains("str.") || lower.contains("platz") || lower.contains("weg"))) {
                street = l;
                if (i + 1 < lines.length && lines[i + 1].trim().matches("\\d{5}.*")) {
                    city = lines[i + 1].trim();
                    startIndex = i + 2;
                } else {
                    startIndex = i + 1;
                }
                break;
            }
        }

        if (street != null || city != null) {
            Log.d("ReceiptParser", "Adresse erkannt: " + street + ", " + city);
        }

        PurchaseItem lastItem = null;
        String pendingName = null;
        boolean afterTotalLine = false;
        for (int i = startIndex; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }

            if (line.toLowerCase().contains("zu zahlen")) {
                Matcher m = PRICE_ONLY_PATTERN.matcher(line);
                if (m.find()) {
                    total = parseDouble(m.group(1));
                } else if (i + 1 < lines.length) {
                    String next = lines[i + 1].trim();
                    Matcher n = PRICE_ONLY_PATTERN.matcher(next);
                    if (n.matches()) {
                        total = parseDouble(n.group(1));
                    }
                }
                Log.d("ReceiptParser", "Gesamtbetrag erkannt: " + total);
                break;
            }

            // first try to extract the total amount. Some receipts repeat the
            // word "Gesamt" on a separate line which should not be treated as
            // an article name.
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

            if (IGNORE_LINE_PATTERN.matcher(line).find()) {
                // Known keywords that are not part of the article list
                continue;
            }

            if (!afterTotalLine) {
            Matcher advMatcher = ADVANTAGE_PATTERN.matcher(line);
            if (advMatcher.find() && lastItem != null) {
                double adv = parseDouble(advMatcher.group(1));
                String oldName = lastItem.getName();
                double newPrice = lastItem.getPrice() + adv;
                lastItem = new PurchaseItem(oldName, newPrice);
                items.set(items.size() - 1, lastItem);
                Log.d("ReceiptParser", "Preisvorteil erkannt: " + lastItem.getName() + " + " + adv);
                continue;
            }

            if (line.toLowerCase().contains("preisvorteil")) {
                double diff = Double.NaN;

                Matcher advMatcher2 = ADVANTAGE_PATTERN.matcher(line);
                if (advMatcher2.find()) {
                    diff = parseDouble(advMatcher2.group(1));
                } else if (i + 1 < lines.length) {
                    String nextLine = lines[i + 1].trim();
                    Matcher pm = PRICE_ONLY_PATTERN.matcher(nextLine);
                    if (pm.matches()) {
                        diff = parseDouble(pm.group(1));
                        i++; // nächste Zeile überspringen
                    }
                }

                if (!Double.isNaN(diff) && lastItem != null) {
                    double newPrice = lastItem.getPrice() + diff;
                    if (newPrice < 0) {
                        items.remove(items.size() - 1);
                        Log.d("ReceiptParser", "Negativer Preisvorteil, Artikel entfernt: " + lastItem.getName());
                        lastItem = null;
                    } else {
                        lastItem = new PurchaseItem(lastItem.getName(), newPrice);
                        items.set(items.size() - 1, lastItem);
                        Log.d("ReceiptParser", "Preisvorteil: " + diff + " → Neuer Preis: " + newPrice);
                    }
                }

                pendingName = null;
                continue;
            }

            Matcher discMatcher = DISCOUNT_PATTERN.matcher(line);
            if (discMatcher.matches() && lastItem != null) {
                double disc = parseDouble(discMatcher.group());
                String oldName = lastItem.getName();
                double newPrice = lastItem.getPrice() + disc;
                lastItem = new PurchaseItem(oldName, newPrice);
                items.set(items.size() - 1, lastItem);
                Log.d("ReceiptParser", "Rabatt erkannt: " + disc + " f\u00fcr " + oldName + "; Neuer Preis: " + newPrice);
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
            }

            if (dateTime == null) {
                Matcher isoMatcher = ISO_DATE_PATTERN.matcher(line);
                if (isoMatcher.find()) {
                    dateTime = LocalDateTime.parse(isoMatcher.group(1));
                    Log.d("ReceiptParser", "Datum erkannt: " + dateTime);
                    continue;
                }
                Matcher dMatcher = DATE_PATTERN.matcher(line);
                if (dMatcher.find()) {
                    String d = dMatcher.group(1);
                    String t = dMatcher.group(2);
                    if (t != null) {
                        DateTimeFormatter df = DateTimeFormatter.ofPattern(d.length() == 8 ? "dd.MM.yy HH:mm" : "dd.MM.yyyy HH:mm");
                        dateTime = LocalDateTime.parse(d + " " + t, df);
                    } else {
                        DateTimeFormatter df = DateTimeFormatter.ofPattern(d.length() == 8 ? "dd.MM.yy" : "dd.MM.yyyy");
                        LocalDate ld = LocalDate.parse(d, df);
                        dateTime = LocalDateTime.of(ld, LocalTime.MIDNIGHT);
                    }
                    Log.d("ReceiptParser", "Datum erkannt: " + dateTime);
                    continue;
                }
            }

            // treat as potential item name if nothing else matched. Skip lines
            // that do not contain any letters (e.g. "----" or purely numeric
            // codes) to avoid creating bogus articles.
            if (line.matches(".*[A-Za-zÄÖÜäöüß].*")) {
                pendingName = line;
            }
        }

        for (PurchaseItem item : items) {
            Log.d("ReceiptParser", "Artikel: " + item.getName() + " / " + item.getPrice());
        }
        double sum = 0.0;
        for (PurchaseItem p : items) {
            sum += p.getPrice();
        }
        if (total != 0.0 && Math.abs(sum - total) > 0.01) {
            Log.d("ReceiptParser", "Warnung: Artikelsumme " + sum + " stimmt nicht mit Gesamtbetrag " + total + " überein");
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
        ReceiptParser parser = new ReceiptParser();
        ReceiptData data = parser.parse(ocrText);

        String address = "";
        if (data.getStreet() != null) {
            address += data.getStreet();
        }
        if (data.getCity() != null) {
            if (!address.isEmpty()) address += ", ";
            address += data.getCity();
        }

        String date = "";
        if (data.getDateTime() != null) {
            DateTimeFormatter df = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            date = data.getDateTime().toLocalDate().format(df);
        }

        List<Article> articleList = new ArrayList<>();
        for (PurchaseItem pi : data.getItems()) {
            articleList.add(new Article(pi.getName(), pi.getPrice()));
        }

        return new ParsedReceipt(articleList, date, data.getTotal(), address);
    }

    /**
     * Parses the structured {@link Text} result from ML Kit and extracts
     * purchase items using the bounding boxes of the detected lines. Only the
     * item name and price are returned.
     */
    public List<PurchaseItem> parseOcr(Text ocrResult) {
        // Collect all text elements and log their positions for debugging
        List<Text.Element> elements = new ArrayList<>();
        for (Text.TextBlock block : ocrResult.getTextBlocks()) {
            Rect bb = block.getBoundingBox();
            Log.d("OCR", "Block: " + block.getText().replace("\n", " ") + " | Box: " + bb);
            for (Text.Line line : block.getLines()) {
                for (Text.Element el : line.getElements()) {
                    Log.d("OCR-ELEMENT", el.getText() + " @ " + el.getBoundingBox());
                    elements.add(el);
                }
            }
        }

        // Sort elements top-to-bottom
        elements.sort(Comparator.comparingInt(e -> {
            Rect b = e.getBoundingBox();
            return b != null ? b.top : 0;
        }));

        // Group elements that are horizontally aligned
        List<List<Text.Element>> rows = new ArrayList<>();
        List<Text.Element> current = new ArrayList<>();
        int currentCenter = Integer.MIN_VALUE;
        for (Text.Element e : elements) {
            Rect b = e.getBoundingBox();
            if (b == null) continue;
            int center = b.centerY();
            if (current.isEmpty() || Math.abs(center - currentCenter) <= 20) {
                current.add(e);
                if (current.size() == 1) currentCenter = center;
            } else {
                rows.add(new ArrayList<>(current));
                current.clear();
                current.add(e);
                currentCenter = center;
            }
        }
        if (!current.isEmpty()) {
            rows.add(current);
        }

        List<PurchaseItem> items = new ArrayList<>();
        PurchaseItem lastItem = null;
        List<Text.Element> pending = null;

        for (List<Text.Element> row : rows) {
            row.sort(Comparator.comparingInt(e -> e.getBoundingBox().left));

            StringBuilder nameBuilder = new StringBuilder();
            String priceText = null;
            for (Text.Element el : row) {
                String t = el.getText();
                if (PRICE_ELEMENT_PATTERN.matcher(t).matches()) {
                    priceText = t;
                } else {
                    if (nameBuilder.length() > 0) nameBuilder.append(' ');
                    nameBuilder.append(t);
                }
            }

            String rowText = nameBuilder.toString().trim();
            if (priceText != null) {
                rowText = rowText.replaceAll("\s+", " ");
            }

            if (IGNORE_LINE_PATTERN.matcher(rowText).find()) {
                continue;
            }

            if (isForbiddenArticleName(rowText)) {
                pending = null;
                continue;
            }

            Matcher advMatcher = ADVANTAGE_PATTERN.matcher(rowText);
            if (advMatcher.find() && lastItem != null) {
                double diff = parseGermanPrice(advMatcher.group(1));
                lastItem = new PurchaseItem(lastItem.getName(), lastItem.getPrice() + diff);
                items.set(items.size() - 1, lastItem);
                continue;
            }

            Matcher discMatcher = DISCOUNT_PATTERN.matcher(rowText);
            if (discMatcher.matches() && lastItem != null) {
                double diff = parseGermanPrice(discMatcher.group());
                lastItem = new PurchaseItem(lastItem.getName(), lastItem.getPrice() + diff);
                items.set(items.size() - 1, lastItem);
                continue;
            }

            // --- BEGIN: Preisvorteil-Verarbeitung ---
            if (rowText.toLowerCase().contains("preisvorteil") && lastItem != null) {
                double diff = Double.NaN;

                // Versuche Preisvorteil in derselben Zeile
                Matcher advMatcher = ADVANTAGE_PATTERN.matcher(rowText);
                if (advMatcher.find()) {
                    diff = parseGermanPrice(advMatcher.group(1));
                } else {
                    // Alternativ: nächster Row-Preis
                    int nextIndex = rows.indexOf(row) + 1;
                    if (nextIndex < rows.size()) {
                        List<Text.Element> nextRow = rows.get(nextIndex);
                        for (Text.Element el : nextRow) {
                            String t = el.getText();
                            if (PRICE_ELEMENT_PATTERN.matcher(t).matches()) {
                                diff = parseGermanPrice(t);
                                break;
                            }
                        }
                    }
                }

                if (!Double.isNaN(diff)) {
                    double newPrice = lastItem.getPrice() + diff;
                    if (newPrice < 0) {
                        items.remove(items.size() - 1);
                        lastItem = null;
                    } else {
                        lastItem = new PurchaseItem(lastItem.getName(), newPrice);
                        items.set(items.size() - 1, lastItem);
                    }
                }

                continue; // Preisvorteilzeile nicht als Artikel speichern
            }
            // --- END ---

            if (priceText != null && rowText.length() > 0) {
                // Entferne " A" am Ende des Artikelnamens
                if (rowText.endsWith(" A")) {
                    rowText = rowText.substring(0, rowText.length() - 2).trim();
                }

                double price = parseGermanPrice(priceText);
                lastItem = new PurchaseItem(rowText, price);
                items.add(lastItem);
                pending = null;
                continue;
            }

            if (priceText == null && rowText.length() > 0) {
                pending = row;
                continue;
            }

            if (priceText != null && rowText.isEmpty() && pending != null) {
                Rect prev = pending.get(0).getBoundingBox();
                Rect box = row.get(0).getBoundingBox();
                if (prev != null && box != null) {
                    int vDist = Math.abs(box.top - prev.bottom);
                    int hDist = Math.abs(box.left - prev.left);
                    if (vDist < 40 && hDist < 50) {
                        StringBuilder nb = new StringBuilder();
                        for (Text.Element pe : pending) {
                            if (nb.length() > 0) nb.append(' ');
                            nb.append(pe.getText());
                        }
                        double price = parseGermanPrice(priceText);
                        String name = nb.toString().trim();
                        if (name.endsWith(" A")) {
                            name = name.substring(0, name.length() - 2).trim();
                        }
                        lastItem = new PurchaseItem(name, price);
                        items.add(lastItem);
                        pending = null;
                    }
                }
            }
        }

        return items;
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

        Pattern textOnly = Pattern.compile("[A-Za-zÄÖÜäöüß\\s\\-.]+");
        Pattern priceOnly = PRICE_ONLY_PATTERN;
        Pattern datePattern = Pattern.compile("\\d{2}\\.\\d{2}\\.\\d{4}");

        String[] lines = text.split("\n");
        String lastName = null;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            String lower = line.toLowerCase();

            // Adresse erkennen
            if (adresse.isEmpty() && lower.contains("straße")) {
                adresse = line;
                if (i + 1 < lines.length) {
                    String next = lines[i + 1].trim();
                    if (next.matches("\\d{5}.*")) {
                        adresse += ", " + next;
                        i++;
                    }
                }
                continue;
            }

            // Datum erkennen
            if (datum.isEmpty()) {
                Matcher dm = datePattern.matcher(line);
                if (dm.find()) {
                    datum = dm.group();
                    continue;
                }
            }

            // Bei "Zu zahlen" den Gesamtpreis ermitteln und abbrechen
            if (lower.contains("zu zahlen")) {
                for (int j = i + 1; j < lines.length; j++) {
                    String next = lines[j].trim();
                    Matcher pm = priceOnly.matcher(next);
                    if (pm.matches()) {
                        gesamtpreis = parseDouble(pm.group(1));
                        break;
                    }
                }
                break; // Keine weiteren Artikel danach erfassen!
            }

            // Zeilen wie "Summe" oder "Gesamter Preisvorteil" ignorieren
            if (lower.contains("summe") || (lower.contains("preisvorteil") && lower.contains("gesamt"))) {
                lastName = null;
                continue;
            }

            // Preisvorteil (nicht gesamt) verrechnen und Zeile ignorieren
            if (lower.contains("preisvorteil")) {
                double diff = Double.NaN;
                Matcher advMatcher = ADVANTAGE_PATTERN.matcher(line);
                if (advMatcher.find()) {
                    diff = parseDouble(advMatcher.group(1));
                } else if (i + 1 < lines.length) {
                    String next = lines[i + 1].trim();
                    Matcher pm = priceOnly.matcher(next);
                    if (pm.matches()) {
                        diff = parseDouble(pm.group(1));
                        i++; // consume the price line
                    }
                }

                if (!Double.isNaN(diff) && !artikelListe.isEmpty()) {
                    Artikel last = artikelListe.get(artikelListe.size() - 1);
                    double newPrice = last.preis + diff;
                    if (newPrice < 0) {
                        artikelListe.remove(artikelListe.size() - 1);
                        lastName = null;
                    } else {
                        last.preis = newPrice;
                    }
                }
                // Preisvorteil-Zeilen nie als eigenen Artikel aufführen
                continue;
            }

            // Artikelzeile mit Name + Preis
            Matcher itemMatcher = ITEM_PATTERN.matcher(line);
            if (itemMatcher.matches()) {
                String name = itemMatcher.group(1).trim();

                // Entferne " A" am Ende des Artikelnamens
                if (name.endsWith(" A")) {
                    name = name.substring(0, name.length() - 2).trim();
                }

                double price = parseDouble(itemMatcher.group(2));
                artikelListe.add(new Artikel(name, price));
                lastName = null;
                continue;
            }

            // Nur Preiszeile
            Matcher priceMatcher = priceOnly.matcher(line);
            if (priceMatcher.matches() && lastName != null) {
                artikelListe.add(new Artikel(lastName, parseDouble(priceMatcher.group(1))));
                lastName = null;
                continue;
            }

            // Nur Textzeile → möglicher Artikelname
            if (textOnly.matcher(line).matches()) {
                lastName = line.trim();
            }
        }

        return artikelListe;
    }

    private static String normalize(String text) {
        return text.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private static boolean isForbiddenArticleName(String text) {
        String norm = normalize(text);
        for (String f : FORBIDDEN_ARTICLE_NAMES) {
            String nf = normalize(f);
            if (norm.equals(nf) || norm.contains(nf)) {
                return true;
            }
        }
        return false;
    }

    private static double parseDouble(String value) {
        return Double.parseDouble(value.replace(",", "."));
    }

    private static double parseGermanPrice(String priceText) {
        return Double.parseDouble(priceText.replace(".", "").replace(",", "."));
    }
}
