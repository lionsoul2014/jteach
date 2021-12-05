package org.lionsoul.jteach.capture;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.lionsoul.jteach.config.TaskConfig;

import java.awt.*;

public class Factory {

    public static final ScreenCapture create(int driver, Rectangle rect, TaskConfig config) throws CaptureException {
        if (driver == ScreenCapture.ROBOT_DRIVER) {
            try {
                return new RobotScreenCapture(rect, config);
            } catch (AWTException e) {
                throw new CaptureException("failed to create robot capture: " + e.getMessage());
            }
        } else if (driver == ScreenCapture.FFMPEG_DRIVER) {
            try {
                return new FFmpegFrameCapture(rect, config);
            } catch (FFmpegFrameGrabber.Exception e) {
                throw new CaptureException("failed to create FFmpeg capture: " + e.getMessage());
            }
        }

        throw new CaptureException("invalid driver: " + driver);
    }

}
