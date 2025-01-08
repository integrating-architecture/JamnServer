/* Authored by www.integrating-architecture.de */
package org.isa.jps.comp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * 
 */
public class VariousFunctionsTests {

    @Test
    void testQuotedStringRebuild() {
        String lTestString = "copy \"C:/Program Files (x86)/Mozilla Maintenance Service/\" C:/temp /all /y /\"some strange other\" whitespaces";
        String[] lToken = lTestString.split(" ");

        lToken = CommandLineInterface.rebuildQuotedWhitespaceStrings(lToken, false);
        assertEquals(lTestString, String.join(" ", lToken));
    }

    @Test
    void testMissingQuoteException() {
        // missing quote test
        String lTestString = "copy \"C:/Program Files (x86)/Mozilla Maintenance Service/ C:/temp /all /y /\"some strange other\" whitespaces";
        final String[] lFailureToken = lTestString.split(" ");

        Exception lExeption = assertThrows(
                RuntimeException.class,
                () -> CommandLineInterface.rebuildQuotedWhitespaceStrings(lFailureToken, false),
                "RuntimeException expected");

        assertTrue(lExeption.getMessage().contains("Missing start/end quote"));
    }

}
