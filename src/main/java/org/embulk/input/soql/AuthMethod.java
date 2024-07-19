package org.embulk.input.soql;

public enum AuthMethod {
    oauth("oauth"),
    user_password("user_password");

    private final String string;

    AuthMethod(final String string) {
        this.string = string;
    }
}
