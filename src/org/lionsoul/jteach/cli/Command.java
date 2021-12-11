package org.lionsoul.jteach.cli;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Command {

    public static final Command[] EMPTY = new Command[0];
    public static final Pattern flagPattern = Pattern.compile("--([^=]+)=?([^\\s]+)?");

    /** Application name */
    public final String name;

    /** Usage */
    public final String usage;

    /** flags list */
    private final List<Flag> flagList;

    /** flag map */
    private final Map<String, Flag> flagMap;

    /** action object */
    private final Action action;

    /** sub command list */
    private final List<Command> subCommandList;

    /** sub command map */
    private final Map<String, Command> subCommandMap;


    public Command(String name, String usage, Flag[] flags, Action action) {
        this(name, usage, flags, Command.EMPTY, action);
    }

    public Command(String name, String usage, Command[] commands) {
        this(name, usage, Flag.EMPTY, commands, null);
    }

    public Command(String name, String usage, Flag[] flags, Command[] commands, Action action) {
        this.name = name;
        this.usage = usage;
        this.flagList = new ArrayList<>(Arrays.asList(flags));
        this.flagMap = new HashMap<>();
        this.subCommandList = new ArrayList<>(Arrays.asList(commands));
        this.subCommandMap = new HashMap<>();
        this.action = action == null ? ctx -> {} : action;

        /* push the default help flag and init the flag map */
        flagList.add(BoolFlag.C("help", "print this help menu", false));
        for (Flag flag : flagList) {
            flagMap.put(flag.name, flag);
        }

        /* push the default help command and init the command map */
        if (commands.length > 0) {
            subCommandList.add(Command.C("help",
            "show list of commands or help for one command", Flag.EMPTY, ctx -> {
                System.out.println(getHelpInfo());
            }));
            for (Command cmd : subCommandList) {
                subCommandMap.put(cmd.name, cmd);
            }
        }
    }

    /** add a new flag */
    public void addFlag(Flag flag) {
        this.flagList.add(flag);
    }

    public void run(String input) {
        run(input.trim(), 0);
    }

    public void run(String input, int i) {
        final String str = input.trim();
        run(str.length() == 0 ? new String[0] : str.split("\\s+"), i);
    }

    public void run(String[] args) {
        run(args, 0);
    }

    public void run(String[] args, int i) {
        /* empty input interception */
        if (args.length == 0) {
            if (subCommandList.size() == 0) {
                action.run(this);
            } else {
                System.out.println(getHelpInfo());
            }
            return;
        }

        /* index out of bounds interception */
        if (i >= args.length) {
            return;
        }

        /* check and get the command */
        Command cmd = this;
        if (isCommand(args[i])) {
            cmd = subCommandMap.get(args[i]);
            if (cmd == null) {
                System.out.printf("undefined command %s\n", args[i]);
                return;
            }
            i++;
        }

        /* parser the input args with --key=value */
        final Map<String, String> args_map = new HashMap<>();
        for (;i < args.length; i++) {
            final String str = args[i];
            /* break the command here */
            if (isCommand(str)) {
                break;
            }

            final Matcher m = flagPattern.matcher(str);
            if (!m.find()) {
                System.out.printf("invalid flag specified %s for command %s\n", str, name);
                return;
            }

            final String args_name = m.group(1);
            if (!cmd.flagMap.containsKey(args_name)) {
                System.out.printf("undefined flag %s for command %s\n", args_name, name);
                return;
            }

            final String val = m.group(2) == null ? "true" : m.group(2);
            // System.out.printf("push flag {name: %s, value: %s}\n", args_name, val);
            args_map.put(args_name, val);
        }

        /* check and set the value */
        for (Flag flag : cmd.flagList) {
            if (args_map.containsKey(flag.name)) {
                flag.setValue(args_map.get(flag.name));
            }
        }

        if (cmd.boolVal("help")) {
            System.out.println(cmd.getHelpInfo());
        } else if (cmd == this) {
            cmd.action.run(cmd);
        } else if (i >= args.length) {
            cmd.action.run(cmd);
        } else {
            cmd.run(args, i);
        }

        /* check and close all the isSet to restore the default value */
        for (Flag flag : cmd.flagList) {
            flag.setIsSet(false);
        }
    }

    /** get the String value */
    public String stringVal(String key) {
        final Flag flag = flagMap.get(key);
        if (flag == null) {
            throw new NullPointerException("no such flag " + key);
        }

        return flag.toString().trim();
    }

    /** get the string list, separated by comma */
    public String[] stringList(String key) {
        return stringList(key, ",");
    }

    public String[] stringList(String key, String separator) {
        final Flag flag = flagMap.get(key);
        if (flag == null) {
            throw new NullPointerException("no such flag " + key);
        }

        final String str = flag.toString().trim();
        return str.length() == 0 ? new String[0] : str.split(separator);
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

    protected String getHelpInfo() {
        final StringBuilder sb = new StringBuilder();
        sb.append("NAME: \n")
            .append("  ").append(name).append("\n\n");

        /* print the usage with optional command arguments */
        sb.append("USAGE: \n")
            .append("  ").append(usage);
        if (subCommandList.size() > 0) {
            sb.append(" [command]");
        }
        if (flagList.size() > 0) {
            sb.append(" [arguments]");
        }
        sb.append("\n\n");

        /* print the command list */
        if (subCommandList.size() > 0) {
            sb.append("COMMANDS: \n");

            /* get the max name length */
            int maxLen = 0;
            for (Command cmd : subCommandList) {
                if (cmd.name.length() > maxLen) {
                    maxLen = cmd.name.length();
                }
            }

            for (Command cmd : subCommandList) {
                sb.append("  ").append(cmd.name)
                    .append(repeat(" ", maxLen - cmd.name.length()))
                    .append("  ").append(cmd.usage);
                sb.append("\n");
            }

            sb.append("\n");
        }

        /* print the flag list */
        if (flagList.size() > 0) {
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

                final String v = flag.getDefaultValue().toString();
                if (v.length() > 0) {
                    sb.append(" (Default: ").append(v).append(')');
                }

                final String optional = flag.getOptions();
                if (optional != null) {
                    sb.append(" Options: [").append(optional).append("]");
                }
                sb.append("\n");
            }
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

    public static boolean isFlag(String str) {
        if (str.length() <= 2) {
            return false;
        }

        char ch0 = str.charAt(0);
        char ch1 = str.charAt(1);
        char ch2 = str.charAt(2);
        if (ch0 == '-' && ch1 == '-' && ch2 != '=') {
            return true;
        }

        return false;
    }

    public static boolean isCommand(String str) {
        for (int i = 0; i < str.length(); i++) {
            final char c = str.charAt(0);
            if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z')) {
                continue;
            }
            return false;
        }
        return true;
    }


    /** static command create interface */
    public static Command C(String name, String usage, Command[] commands) {
        return new Command(name, usage, commands);
    }

    public static Command C(String name, String usage, Flag[] flags, Action action) {
        return new Command(name, usage, flags, action);
    }

    public static Command C(String name, String usage, Flag[] flags, Command[] commands, Action action) {
        return new Command(name, usage, flags, commands, action);
    }

}
