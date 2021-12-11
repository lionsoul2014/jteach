package org.lionsoul.jteach.cli;

public class FloatFlag extends Flag {

    private float _default;
    private float value;

    public FloatFlag(String name, String usage, float _default) {
        super(name, usage);
        this._default = _default;
    }

    @Override public boolean setValue(String str) {
        value = Float.parseFloat(str);
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

    public static FloatFlag C(String name, String usage, float _default) {
        return new FloatFlag(name, usage, _default);
    }

}
