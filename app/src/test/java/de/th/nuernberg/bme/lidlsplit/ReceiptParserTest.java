package de.th.nuernberg.bme.lidlsplit;

import org.junit.Test;

import java.time.LocalDateTime;

import static org.junit.Assert.*;

public class ReceiptParserTest {
    @Test
    public void parseSampleReceipt() {
        String text = "Allersberger Straße 130\n" +
                "90461 Nürnberg\n" +
                "Cherrystrauchtomaten\n" +
                "1,79 A\n" +
                "Preisvorteil -0,20\n" +
                "Laugenbrezel 10er\n" +
                "1,99 A\n" +
                "---\n" +
                "zu zahlen             19,86\n" +
                "18.06.2025";

        ReceiptParser parser = new ReceiptParser();
        ReceiptData data = parser.parse(text);

        assertEquals("Allersberger Straße 130", data.getStreet());
        assertEquals("90461 Nürnberg", data.getCity());
        assertEquals(2, data.getItems().size());
        assertEquals("Cherrystrauchtomaten", data.getItems().get(0).getName());
        assertEquals(1.59, data.getItems().get(0).getPrice(), 0.001);
        assertEquals("Laugenbrezel 10er", data.getItems().get(1).getName());
        assertEquals(1.99, data.getItems().get(1).getPrice(), 0.001);
        assertEquals(19.86, data.getTotal(), 0.001);
        assertNull(data.getDateTime());
    }

    @Test
    public void ignoresJunkLines() {
        String text = "Allersberger Straße 130\n" +
                "90461 Nürnberg\n" +
                "Cherrystrauchtomaten\n" +
                "1,79\n" +
                "Preisvorteil -0,20\n" +
                "Laugenbrezel 10er\n" +
                "1,99\n" +
                "MWST 7%\n" +
                "Karte Visa\n" +
                "zu zahlen 19,86\n" +
                "18.06.2025";

        ReceiptParser parser = new ReceiptParser();
        ReceiptData data = parser.parse(text);

        assertEquals(2, data.getItems().size());
        assertEquals("Cherrystrauchtomaten", data.getItems().get(0).getName());
        assertEquals(1.59, data.getItems().get(0).getPrice(), 0.001);
        assertEquals("Laugenbrezel 10er", data.getItems().get(1).getName());
        assertEquals(1.99, data.getItems().get(1).getPrice(), 0.001);
        assertEquals(19.86, data.getTotal(), 0.001);
    }

    @Test
    public void parseTotalWithBetrag() {
        String text = "Allersberger Straße 130\n" +
                "90461 Nürnberg\n" +
                "Betrag 19,86\n" +
                "18.06.2025";

        ReceiptParser parser = new ReceiptParser();
        ReceiptData data = parser.parse(text);

        assertEquals(19.86, data.getTotal(), 0.001);
        assertEquals("Allersberger Straße 130", data.getStreet());
        assertEquals("90461 Nürnberg", data.getCity());
    }

    @Test
    public void stopsParsingAfterZuZahlen() {
        String text = "Allersberger Straße 130\n" +
                "90461 Nürnberg\n" +
                "Cherrystrauchtomaten\n" +
                "1,79\n" +
                "Laugenbrezel 10er\n" +
                "1,99\n" +
                "Zu zahlen 19,86\n" +
                "Pfand 0,25\n" +
                "18.06.2025";

        ReceiptParser parser = new ReceiptParser();
        ReceiptData data = parser.parse(text);

        assertEquals(2, data.getItems().size());
        assertEquals(19.86, data.getTotal(), 0.001);
        assertEquals(LocalDateTime.of(2025, 6, 18, 0, 0), data.getDateTime());
    }
}
