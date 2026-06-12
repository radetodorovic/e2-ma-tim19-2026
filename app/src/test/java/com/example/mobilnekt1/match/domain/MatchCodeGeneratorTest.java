package com.example.mobilnekt1.match.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MatchCodeGeneratorTest {
    @Test
    public void generate_returnsValidSixCharacterCode() {
        MatchCodeGenerator generator = new MatchCodeGenerator();

        for (int i = 0; i < 100; i++) {
            assertTrue(MatchCodeGenerator.isValid(generator.generate()));
        }
    }

    @Test
    public void normalize_trimsAndUppercasesCode() {
        assertEquals("AB2CD3", MatchCodeGenerator.normalize("  ab2cd3 "));
    }

    @Test
    public void isValid_rejectsAmbiguousAndWrongLengthCodes() {
        assertFalse(MatchCodeGenerator.isValid("ABCD"));
        assertFalse(MatchCodeGenerator.isValid("ABCDO1"));
    }
}
