package org.lionsoul.jteach.cli;

public class BoolFlag extends Flag {

    private boolean value;
    private final boolean _default;

    public BoolFlag(String name, String usage, boolean _default) {
        super(name, usage);
        this._default = _default;
    }

    @Override public boolean setValue(String str) {
        final String v = str.toLowerCase();
        if ("yes".equals(v) || "true".equals(v) || "1".equals(v) || "on".equals(v)) {
            value = true;
            isSet = true;
            return true;
        } else if ("no".equals(v) || "false".equals(v) || "0".equals(v) || "off".equals(v)) {
            value = false;
            isSet = true;
            return true;
        }
        return false;
    }

    @Override public Object getValue() {
        return isSet ? value : _default;
    }

    @Override
    public Object getDefaultValue() {
        return _default;
    }

    public static BoolFlag C(String name, String usage, boolean _default) {
        return new BoolFlag(name, usage, _default);
    }

}
