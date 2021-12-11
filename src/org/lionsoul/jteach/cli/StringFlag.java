package org.lionsoul.jteach.cli;

import java.util.Objects;

public class StringFlag extends Flag {

    private String _default;
    private String value;
    public final String[] options;

    public StringFlag(String name, String usage, String _default) {
        this(name, usage, _default, null);
    }

    public StringFlag(String name, String usage, String _default, String[] options) {
        super(name, usage);
        this._default = _default;
        this.options = options;
    }

    @Override
    public boolean setValue(String str) {
        if (options == null) {
            value = str;
            isSet = true;
            return true;
        }

        for (String v : options) {
            if (Objects.equals(str, v)) {
                value = str;
                isSet = true;
                return true;
            }
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

    public static StringFlag C(String name, String usage, String _default) {
        return new StringFlag(name, usage, _default);
    }

    public static StringFlag C(String name, String usage, String _default, String[] options) {
        return new StringFlag(name, usage, _default, options);
    }

}
