package org.lionsoul.jteach.capture;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.lionsoul.jteach.config.TaskConfig;

import java.awt.*;
import java.awt.image.BufferedImage;

public class FFmpegFrameCapture extends ScreenCapture {

    private final FFmpegFrameGrabber frameGrabber;

    public FFmpegFrameCapture(Rectangle rect, TaskConfig config) throws FFmpegFrameGrabber.Exception {
        super(rect, config);
        frameGrabber = FFmpegFrameGrabber.createDefault(config.display+".0+0,0");
        frameGrabber.setFormat("x11grab");
        frameGrabber.setImageWidth(rect.width);
        frameGrabber.setImageHeight(rect.height);
        frameGrabber.start();
    }

    @Override
    public int getDriver() {
        return ScreenCapture.FFMPEG_DRIVER;
    }

    @Override
    public String getDriverName() {
        return "ffmpeg";
    }

    @Override
    public BufferedImage capture() throws CaptureException {
        try {
            final Java2DFrameConverter converter = new Java2DFrameConverter();
            final Frame image = frameGrabber.grab();
            return converter.getBufferedImage(image, 1.0D, false, null);
        } catch (FFmpegFrameGrabber.Exception e) {
            throw new CaptureException("failed to grab: " + e.getMessage());
        }
    }
}
