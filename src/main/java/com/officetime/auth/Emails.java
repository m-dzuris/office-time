package com.officetime.auth;

import java.util.Locale;

/** Email addresses are compared case-insensitively, so they are always stored and looked up in one form. */
public final class Emails {

    private Emails() {
    }

    public static String normalize(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
