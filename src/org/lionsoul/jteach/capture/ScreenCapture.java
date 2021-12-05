package org.lionsoul.jteach.capture;

import java.awt.*;
import java.awt.image.BufferedImage;

public abstract class ScreenCapture {
    public static final int ROBOT_DRIVER  = 1;
    public static final int FFMPEG_DRIVER = 2;

    protected final Rectangle rect;

    public ScreenCapture(Rectangle rect) {
        this.rect = rect;
    }

    public Rectangle getRect() {
        return rect;
    }

    public abstract int getDriver();

    public abstract String getDriverName();

    public abstract BufferedImage capture() throws CaptureException;

}
