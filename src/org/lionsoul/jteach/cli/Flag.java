package org.lionsoul.jteach.cli;

public abstract class Flag {

    /** flag name */
    public final String name;

    /** flag usage */
    public final String usage;

    /** is set */
    protected boolean isSet = false;

    public Flag(String name, String usage) {
        this.name = name;
        this.usage = usage;
    }

    public boolean isSet() {
        return isSet;
    }

    public void setIsSet(boolean isSet) {
        this.isSet = isSet;
    }

    public abstract boolean setValue(String str);

    public abstract Object getValue();

    public String getOptionalValues() {
        return null;
    }

}
