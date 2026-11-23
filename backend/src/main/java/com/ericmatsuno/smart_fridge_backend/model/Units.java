package com.ericmatsuno.smart_fridge_backend.model;

import java.util.Locale;

/**
 * Minimal unit handling: "Cups", "cup " and "cups" are treated as the same unit.
 * Real conversion (grams to cups) is deliberately out of scope for now; quantities
 * in different units are simply not added together.
 */
public final class Units {

    private Units() {
    }

    public static String normalize(String unit) {
        if (unit == null || unit.isBlank()) {
            return null;
        }
        String u = unit.trim().toLowerCase(Locale.ROOT);
        if (u.length() > 3 && u.endsWith("s")) {
            u = u.substring(0, u.length() - 1); // naive plural strip: pieces -> piece
        }
        return u;
    }

    /** A missing unit on either side counts as compatible, since we can't prove otherwise. */
    public static boolean compatible(String a, String b) {
        String na = normalize(a);
        String nb = normalize(b);
        return na == null || nb == null || na.equals(nb);
    }
}
