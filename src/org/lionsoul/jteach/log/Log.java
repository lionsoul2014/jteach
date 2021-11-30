package org.lionsoul.jteach.log;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * command log implementation
 *
 * @author chenxin<chenxin619315@gmail.com>
 */
public class Log {

    /* Log level constants define */
    public static final int DEBUG = 0;
    public static final int INFO = 1;
    public static final int WARN = 2;
    public static final int ERROR = 3;
    public static final String[] level_string = new String[] {
        "DEBUG",
        "INFO",
        "WARN",
        "ERROR"
    };

    public final Class<?> baseClass;
    private static int level;

    public Log(Class<?> baseClass) {
        this.baseClass = baseClass;
    }

    public static final Log getLogger(Class<?> baseClass) {
        return new Log(baseClass);
    }

    private void print(int level, String format, Object... args) {
        if (level < DEBUG || level > ERROR) {
            throw new IndexOutOfBoundsException("invalid level index " + level);
        }

        // level filter
        if (level < this.level) {
            return;
        }

        // append the datetime
        final StringBuffer sb = new StringBuffer();
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd HH:mm:ss");
        sb.append(String.format("%s %-5s ", sdf.format(new Date()), level_string[level]));

        // append the class name
        sb.append(baseClass.getName()).append(' ');
        sb.append(String.format(format, args));
        System.out.println(sb);
        System.out.flush();
    }

    public void debug(String format, Object... args) {
        print(DEBUG, format, args);
    }

    public void info(String format, Object... args) {
        print(INFO, format, args);
    }

    public void warn(String format, Object... args) {
        print(WARN, format, args);
    }

    public void error(String format, Object... args) {
        print(ERROR, format, args);
    }

    public static void setLevel(int level) {
        Log.level = level;
    }

}
