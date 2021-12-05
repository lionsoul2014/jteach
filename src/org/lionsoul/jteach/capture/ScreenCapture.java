package org.lionsoul.jteach.capture;

import org.bytedeco.javacv.FFmpegFrameGrabber;

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

    public abstract String getDriverName();

    public abstract BufferedImage capture() throws CaptureException;

    public static final ScreenCapture create(int driver, Rectangle rect) throws CaptureException {
        if (driver == ROBOT_DRIVER) {
            try {
                return new RobotScreenCapture(rect);
            } catch (AWTException e) {
                throw new CaptureException("failed to create robot capture: " + e.getMessage());
            }
        } else if (driver == FFMPEG_DRIVER) {
            try {
                return new FFmpegFrameCapture(rect);
            } catch (FFmpegFrameGrabber.Exception e) {
                throw new CaptureException("failed to create FFmpeg capture: " + e.getMessage());
            }
        }

       throw new CaptureException("invalid driver: " + driver);
    }

}
