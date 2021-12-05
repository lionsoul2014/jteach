package org.lionsoul.jteach.capture;

import org.bytedeco.javacv.FFmpegFrameGrabber;

import java.awt.*;

public class Factory {

    public static final ScreenCapture create(int driver, Rectangle rect) throws CaptureException {
        if (driver == ScreenCapture.ROBOT_DRIVER) {
            try {
                return new RobotScreenCapture(rect);
            } catch (AWTException e) {
                throw new CaptureException("failed to create robot capture: " + e.getMessage());
            }
        } else if (driver == ScreenCapture.FFMPEG_DRIVER) {
            try {
                return new FFmpegFrameCapture(rect);
            } catch (FFmpegFrameGrabber.Exception e) {
                throw new CaptureException("failed to create FFmpeg capture: " + e.getMessage());
            }
        }

        throw new CaptureException("invalid driver: " + driver);
    }

}
