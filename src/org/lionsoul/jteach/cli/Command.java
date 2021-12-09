package org.lionsoul.jteach.cli;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Command {

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

    /** sub command list */
    private final List<Command> subCommandList;

    public Command(String name, String usage, Flag[] flags, Action action) {
        this(name, usage, flags, action, new Command[0]);
    }

    public Command(String name, String usage, Flag[] flags, Action action, Command[] subCommandList) {
        this.name = name;
        this.usage = usage;
        this.flagList = new ArrayList<>(Arrays.asList(flags));
        this.action = action;
        this.flagMap = new HashMap<>();
        this.subCommandList = new ArrayList<>(Arrays.asList(subCommandList));

        /* push the default help flag */
        flagList.add(new BoolFlag("help", "print this help menu", false));

        /* init the flag map */
        for (Flag flag : flagList) {
            flagMap.put(flag.name, flag);
        }
    }

    /** add a new flag */
    public void addFlag(Flag flag) {
        this.flagList.add(flag);
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

    public void run(String input) {
        run(input.split("\\s"));
    }

    public void run(String[] args) {
        /* parser the input args with --key=value */
        final Pattern p = Pattern.compile("--([^=]+)=?([^\\s]+)?");
        final Map<String, String> args_map = new HashMap<>();
        for (String str : args) {
            final Matcher m = p.matcher(str);
            if (!m.find()) {
                System.out.printf("invalid flag specified %s for command %s\n", str, name);
                return;
            }

            final String args_name = m.group(1);
            if (!flagMap.containsKey(args_name)) {
                System.out.printf("undefined flag %s for command %s\n", args_name, name);
                return;
            }

            final String val = m.groupCount() > 2 ? m.group(2) : "true";
            // System.out.printf("name: %s, value: %s\n", m.group(1), val);
            args_map.put(m.group(1), val);
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

        if (boolVal("help")) {
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
            .append("  ").append(usage).append(" [arguments]\n\n");
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
                .append("  ").append(flag.usage);

            if (!flag.isSet()) {
                final String v = flag.getValue().toString();
                if (v.length() > 0) {
                    sb.append(" (Default: ").append(flag.getValue()).append(')');
                }
            }

            final String optional = flag.getOptions();
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

    public static Command C(String name, String usage, Flag[] flags, Action action) {
        return new Command(name, usage, flags, action);
    }

    public static Command C(String name, String usage, Flag[] flags, Action action, Command[] commands) {
        return new Command(name, usage, flags, action, commands);
    }

}
