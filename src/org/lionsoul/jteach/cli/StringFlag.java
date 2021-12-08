package org.lionsoul.jteach.cli;

import java.util.Objects;

public class StringFlag extends Flag {

    private String value;
    public final String[] optional;

    public StringFlag(String name, String usage, String value) {
        this(name, usage, value, null);
    }

    public StringFlag(String name, String usage, String value, String[] optional) {
        super(name, usage);
        this.value = value;
        this.optional = optional;
    }

    @Override
    public boolean setValue(String str) {
        if (optional == null) {
            value = str;
            return true;
        }

        for (String v : optional) {
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

    @Override public String getOptionalValues() {
        if (optional == null) {
            return null;
        }

        final StringBuffer sb = new StringBuffer();
        for (String str : optional) {
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

}
