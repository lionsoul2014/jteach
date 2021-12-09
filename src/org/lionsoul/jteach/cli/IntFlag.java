package org.lionsoul.jteach.cli;

public class IntFlag extends Flag {

    private int value;

    public IntFlag(String name, String usage, int value) {
        super(name, usage);
        this.value = value;
    }

    @Override public boolean setValue(String str) {
        value = Integer.parseInt(str);
        return true;
    }

    @Override public Object getValue() {
        return value;
    }

    @Override public String toString() {
        return String.valueOf(value);
    }

    public static IntFlag C(String name, String usage, int value) {
        return new IntFlag(name, usage, value);
    }

}
