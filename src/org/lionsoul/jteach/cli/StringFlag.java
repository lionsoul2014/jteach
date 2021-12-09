package org.lionsoul.jteach.cli;

import java.util.Objects;

public class StringFlag extends Flag {

    private String value;
    public final String[] options;

    public StringFlag(String name, String usage, String value) {
        this(name, usage, value, null);
    }

    public StringFlag(String name, String usage, String value, String[] options) {
        super(name, usage);
        this.value = value;
        this.options = options;
    }

    @Override
    public boolean setValue(String str) {
        if (options == null) {
            value = str;
            return true;
        }

        for (String v : options) {
            if (Objects.equals(str, v)) {
                value = str;
                return true;
            }
        }

        return false;
    }

    @Override public Object getValue() {
        return value;
    }

    @Override public String getOptions() {
        if (options == null) {
            return null;
        }

        final StringBuilder sb = new StringBuilder();
        for (String str : options) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(str);
        }

        return sb.toString();
    }

    @Override public String toString() {
        return String.valueOf(value);
    }

    public static StringFlag C(String name, String usage, String value) {
        return new StringFlag(name, usage, value);
    }

    public static StringFlag C(String name, String usage, String value, String[] options) {
        return new StringFlag(name, usage, value, options);
    }

}
