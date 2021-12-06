package org.lionsoul.jteach.capture;

import org.lionsoul.jteach.config.TaskConfig;

import java.awt.*;
import java.awt.image.BufferedImage;

public abstract class ScreenCapture {
    public static final int ROBOT_DRIVER  = 1;
    public static final int FFMPEG_DRIVER = 2;

    public static final int IMAGEIO_POLICY = 1;
    public static final int DATABUFFER_POLICY = 2;

    protected final Rectangle rect;
    protected final TaskConfig config;

    public ScreenCapture(Rectangle rect, TaskConfig config) {
        this.rect = rect;
        this.config = config;
    }

    public Rectangle getRect() {
        return rect;
    }

    public abstract int getDriver();

    public abstract String getDriverName();

    public abstract BufferedImage capture() throws CaptureException;

}
