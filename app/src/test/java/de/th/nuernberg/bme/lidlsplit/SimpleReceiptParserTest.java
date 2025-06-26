package de.th.nuernberg.bme.lidlsplit;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class SimpleReceiptParserTest {
    @Test
    public void parseBonExtractsData() {
        String text = "Allersberger Straße 130\n" +
                "90461 Nürnberg\n" +
                "18.06.2025\n" +
                "Cherrystrauchtomaten\n" +
                "1,79 A\n" +
                "Preisvorteil -0,20\n" +
                "Laugenbrezel 10er\n" +
                "1,99 A\n" +
                "Zu zahlen 19,86";

        List<Artikel> items = ReceiptParser.parseBon(text);

        assertEquals("Allersberger Straße 130, 90461 Nürnberg", ReceiptParser.adresse);
        assertEquals("18.06.2025", ReceiptParser.datum);
        assertEquals(19.86, ReceiptParser.gesamtpreis, 0.001);
        assertEquals(2, items.size());
        assertEquals("Cherrystrauchtomaten", items.get(0).name);
        assertEquals(1.59, items.get(0).preis, 0.001);
        assertEquals("Laugenbrezel 10er", items.get(1).name);
        assertEquals(1.99, items.get(1).preis, 0.001);
    }

    @Test
    public void parseBonIgnoresNonArticleLines() {
        String text = "Allersberger Straße 130\n" +
                "90461 Nürnberg\n" +
                "Cherrystr.Tomaten\n" +
                "1,79 A\n" +
                "Preisvorteil               -0,20\n" +
                "Laugenbrezel 10er\n" +
                "1,99 A\n" +
                "Signaturzähler: 000031\n" +
                "TA-Nr. 12345\n" +
                "zu zahlen                 19,86\n" +
                "18.06.2025 16:34";

        List<Artikel> items = ReceiptParser.parseBon(text);

        assertEquals("Allersberger Straße 130, 90461 Nürnberg", ReceiptParser.adresse);
        assertEquals("", ReceiptParser.datum);
        assertEquals(19.86, ReceiptParser.gesamtpreis, 0.001);
        assertEquals(2, items.size());
        assertEquals("Cherrystr.Tomaten", items.get(0).name);
        assertEquals(1.59, items.get(0).preis, 0.001);
        assertEquals("Laugenbrezel 10er", items.get(1).name);
        assertEquals(1.99, items.get(1).preis, 0.001);
    }

    @Test
    public void parseBonStopsAfterZuZahlen() {
        String text = "Allersberger Straße 130\n" +
                "90461 Nürnberg\n" +
                "Cherrystrauchtomaten\n" +
                "1,79\n" +
                "Laugenbrezel 10er\n" +
                "1,99\n" +
                "Zu zahlen 19,86\n" +
                "Pfand\n" +
                "0,25\n" +
                "18.06.2025";

        List<Artikel> items = ReceiptParser.parseBon(text);

        assertEquals(2, items.size());
        assertEquals(19.86, ReceiptParser.gesamtpreis, 0.001);
        assertEquals("Cherrystrauchtomaten", items.get(0).name);
        assertEquals("Laugenbrezel 10er", items.get(1).name);
    }

    @Test
    public void parseBonHandlesMultilinePreisvorteil() {
        String text = "Brot\n" +
                "1,99\n" +
                "PREISVORTEIL\n" +
                "-0,20\n" +
                "Zu zahlen 1,79";

        List<Artikel> items = ReceiptParser.parseBon(text);

        assertEquals(1, items.size());
        assertEquals("Brot", items.get(0).name);
        assertEquals(1.79, items.get(0).preis, 0.001);
    }
}
