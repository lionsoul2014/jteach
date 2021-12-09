package org.lionsoul.jteach.cli;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Command {

    /** argument list from the main args */
    private final String[] args;

    /** Application name */
    private final String name;

    /** Usage */
    private final String usage;

    /** flags list */
    private final List<Flag> flagList;

    /** flag map */
    private final Map<String, Flag> flagMap;

    /** action object */
    private final Action action;

    public Command(String input, String name, String usage, Flag[] flags, Action action) {
        this(input.split("\\s"), name, usage, flags, action);
    }

    public Command(String[] args, String name, String usage, Flag[] flags, Action action) {
        this.args = args;
        this.name = name;
        this.usage = usage;
        this.flagList = new ArrayList<>();
        this.action = action;
        this.flagMap = new HashMap<>();

        /* push the default help flag */
        flagList.addAll(Arrays.asList(flags));
        flagList.add(new StringFlag("help", "print this help menu", "true"));

        /* init the flag map */
        for (Flag flag : flagList) {
            flagMap.put(flag.name, flag);
        }
    }

    /** get the String value */
    public String stringVal(String key) {
        final Flag flag = flagMap.get(key);
        if (flag == null) {
            throw new NullPointerException("no such flag " + key);
        }

        return flag.toString();
    }

    /** get the int value */
    public int intVal(String key) {
        final Flag flag = flagMap.get(key);
        if (flag == null) {
            throw new NullPointerException("no such flag " + key);
        }

        if (flag.getClass() != IntFlag.class) {
            throw new ClassCastException("flag " + key + " not int");
        }

        return (int) flag.getValue();
    }

    /** get the float value */
    public float floatVal(String key) {
        final Flag flag = flagMap.get(key);
        if (flag == null) {
            throw new NullPointerException("no such flag " + key);
        }

        if (flag.getClass() != FloatFlag.class) {
            throw new ClassCastException("flag " + key + " not float");
        }

        return (float) flag.getValue();
    }

    /** get the boolean value */
    public boolean boolVal(String key) {
        final Flag flag = flagMap.get(key);
        if (flag == null) {
            throw new NullPointerException("no such flag " + key);
        }

        if (flag.getClass() != BoolFlag.class) {
            throw new ClassCastException("flag " + key + " not bool");
        }

        return (boolean) flag.getValue();
    }

    public void start() {
        /* parser the input args with --key=value */
        final Pattern p = Pattern.compile("--([^=]+)=?([^\\s]+)?");
        final Map<String, String> args_map = new HashMap<>();
        for (String str : args) {
            final Matcher m = p.matcher(str);
            if (m.find()) {
                final String args_name = m.group(1);
                if (!flagMap.containsKey(args_name)) {
                    System.out.printf("invalid flag %s, no such flag defined\n", args_name);
                    return;
                }
                args_map.put(m.group(1), m.groupCount() > 2 ? "true" : m.group(2));
            }
        }

        /* check and override the isSet and value */
        for (Flag flag : flagList) {
            if (args_map.containsKey(flag.name)) {
                flag.setIsSet(true);
                flag.setValue(args_map.get(flag.name));
            } else {
                flag.setIsSet(false);
            }
        }

        if (args_map.containsKey("help")) {
            System.out.println(getHelpInfo());
        } else {
            action.run(this);
        }
    }

    protected String getHelpInfo() {
        final StringBuilder sb = new StringBuilder();
        sb.append("NAME: \n")
            .append("  ").append(name).append("\n\n");
        sb.append("USAGE: \n")
            .append("  ").append(usage).append(" [option=value]\n\n");
        sb.append("OPTIONS: \n");

        /* get the max name length */
        int maxLen = 0;
        for (Flag flag : flagList) {
            if (flag.name.length() > maxLen) {
                maxLen = flag.name.length();
            }
        }

        for (Flag flag : flagList) {
            sb.append("  ").append("--").append(flag.name)
                .append(repeat(" ", maxLen - flag.name.length()))
                .append("  ").append(flag.usage).append(" (Default: ").append(flag.getValue()).append(')');
            final String optional = flag.getOptionalValues();
            if (optional != null) {
                sb.append(" Optionals: [").append(optional).append("]");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    @Override public String toString() {
        return getHelpInfo();
    }

    public static String repeat(String str, int num) {
        final StringBuffer sb = new StringBuffer();
        for (int i = 0; i < num; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    public static Command create(String input, String name, String usage, Flag[] flags, Action action) {
        return new Command(input, name, usage, flags, action);
    }

    public static Command create(String[] args, String name, String usage, Flag[] flags, Action action) {
        return new Command(args, name, usage, flags, action);
    }

}
