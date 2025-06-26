package de.th.nuernberg.bme.lidlsplit;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class ParseReceiptMethodTest {
    @Test
    public void parseReceiptExtractsData() {
        String text = "Allersberger Straße 130\n" +
                "90461 Nürnberg\n" +
                "Cherrystrauchtomaten\n" +
                "1,79 A\n" +
                "Preisvorteil -0,20\n" +
                "Laugenbrezel 10er\n" +
                "1,99\n" +
                "Gesamtsumme 19,86\n" +
                "18.06.2025";

        ParsedReceipt result = ReceiptParser.parseReceipt(text);

        assertEquals("Allersberger Straße 130, 90461 Nürnberg", result.getAddress());
        assertEquals("18.06.2025", result.getDate());
        assertEquals(19.86, result.getTotal(), 0.001);

        List<Artikel> items = result.getItems();
        assertEquals(2, items.size());
        assertEquals("Cherrystrauchtomaten", items.get(0).name);
        assertEquals(1.59, items.get(0).preis, 0.001);
        assertEquals("Laugenbrezel 10er", items.get(1).name);
        assertEquals(1.99, items.get(1).preis, 0.001);
    }
}
