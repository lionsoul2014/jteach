package org.lionsoul.jteach.cli;

public class BoolFlag extends Flag {

    private boolean value;

    public BoolFlag(String name, String usage, boolean value) {
        super(name, usage);
        this.value = value;
    }

    @Override public boolean setValue(String str) {
        final String v = str.toLowerCase();
        if ("yes".equals(v) || "true".equals(v) || "1".equals(v) || "on".equals(v)) {
            value = true;
            return true;
        } else if ("no".equals(v) || "false".equals(v) || "0".equals(v) || "off".equals(v)) {
            value = false;
            return true;
        }
        return false;
    }

    @Override public Object getValue() {
        return value;
    }

    @Override public String toString() {
        return String.valueOf(value);
    }

    public static BoolFlag C(String name, String usage, boolean value) {
        return new BoolFlag(name, usage, value);
    }

}
