package com.example.mobilnekt1.regions.domain;

import static org.junit.Assert.*;
import org.junit.Test;

public class RegionCatalogTest {
    @Test public void canonical_isCaseInsensitiveAndRejectsUnknownRegion() {
        assertEquals("Beograd", RegionCatalog.canonical(" beograd "));
        assertNull(RegionCatalog.canonical("Backa"));
    }
    @Test public void pointFor_isStableAndNearSafeRegionalCenter() {
        double[] first = RegionCatalog.pointFor("Vojvodina", "user-1");
        double[] second = RegionCatalog.pointFor("Vojvodina", "user-1");
        assertArrayEquals(first, second, 0.0);
        assertTrue(first[0] > 45.20 && first[0] < 45.34);
        assertTrue(first[1] > 19.76 && first[1] < 19.90);
    }
}
