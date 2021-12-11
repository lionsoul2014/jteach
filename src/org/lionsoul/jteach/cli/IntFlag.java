package org.lionsoul.jteach.cli;

public class IntFlag extends Flag {

    private int _default;
    private int value;

    public IntFlag(String name, String usage, int _default) {
        super(name, usage);
        this._default = _default;
    }

    @Override public boolean setValue(String str) {
        value = Integer.parseInt(str);
        isSet = true;
        return true;
    }

    @Override public Object getValue() {
        return isSet ? value : _default;
    }

    @Override
    public Object getDefaultValue() {
        return _default;
    }

    public static IntFlag C(String name, String usage, int _default) {
        return new IntFlag(name, usage, _default);
    }

}
