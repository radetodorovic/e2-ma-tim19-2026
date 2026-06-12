package com.example.mobilnekt1.auth.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AuthValidatorTest {
    @Test
    public void validEmail_isAccepted() {
        assertTrue(AuthValidator.isValidEmail("igrac@example.com"));
    }

    @Test
    public void malformedEmail_isRejected() {
        assertFalse(AuthValidator.isValidEmail("igrac@"));
    }

    @Test
    public void username_acceptsSupportedCharacters() {
        assertTrue(AuthValidator.isValidUsername("Igrac_19"));
    }

    @Test
    public void username_rejectsSpacesAndShortValues() {
        assertFalse(AuthValidator.isValidUsername("ab"));
        assertFalse(AuthValidator.isValidUsername("ime igraca"));
    }

    @Test
    public void password_requiresEightCharactersLetterAndDigit() {
        assertTrue(AuthValidator.isStrongPassword("slagalica1"));
        assertFalse(AuthValidator.isStrongPassword("12345678"));
        assertFalse(AuthValidator.isStrongPassword("slagalica"));
        assertFalse(AuthValidator.isStrongPassword("abc1"));
    }

    @Test
    public void usernameNormalization_isLocaleIndependent() {
        assertEquals("igrac_19", AuthValidator.normalizeUsername(" IGRAC_19 "));
    }
}
