package com.example.mobilnekt1.regions.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class RegionCatalog {
    public static final List<String> NAMES = Collections.unmodifiableList(Arrays.asList(
            "Beograd", "Vojvodina", "Sumadija i Zapadna Srbija", "Juzna i Istocna Srbija"));

    private static final double[][] SAFE_CENTERS = {
            {44.8125, 20.4612}, {45.2671, 19.8335}, {43.8914, 20.3497}, {43.3209, 21.8958}
    };

    private RegionCatalog() { }

    public static boolean isSupported(String value) {
        return canonical(value) != null;
    }

    public static String canonical(String value) {
        if (value == null) return null;
        for (String name : NAMES) if (name.equalsIgnoreCase(value.trim())) return name;
        return null;
    }

    public static double[] pointFor(String region, String seed) {
        String canonical = canonical(region);
        if (canonical == null) throw new IllegalArgumentException("Nepoznat region.");
        int index = NAMES.indexOf(canonical);
        int hash = seed == null ? 0 : seed.hashCode();
        double latJitter = (((hash & 0xff) / 255.0) - 0.5) * 0.08;
        double lngJitter = ((((hash >>> 8) & 0xff) / 255.0) - 0.5) * 0.08;
        return new double[]{SAFE_CENTERS[index][0] + latJitter, SAFE_CENTERS[index][1] + lngJitter};
    }
}
