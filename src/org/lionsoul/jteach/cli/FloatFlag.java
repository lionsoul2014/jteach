package org.lionsoul.jteach.cli;

public class FloatFlag extends Flag {

    private float value;

    public FloatFlag(String name, String usage, float value) {
        super(name, usage);
        this.value = value;
    }

    @Override public boolean setValue(String str) {
        value = Float.parseFloat(str);
        return true;
    }

    @Override public Object getValue() {
        return value;
    }

    @Override public String toString() {
        return String.valueOf(value);
    }

    public static FloatFlag C(String name, String usage, float value) {
        return new FloatFlag(name, usage, value);
    }

}
