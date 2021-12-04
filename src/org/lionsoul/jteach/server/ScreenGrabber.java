package org.lionsoul.jteach.server;

import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.FFmpegFrameGrabber;

public class ScreenGrabber {
    public static void main(String[] args) throws Exception {
        int x = 0, y = 0, w = 1920, h = 1080;
        FFmpegFrameGrabber grabber = FFmpegFrameGrabber.createDefault(":1.0+" + x + "," + y);
        grabber.setFormat("x11grab");
        grabber.setImageWidth(w);
        grabber.setImageHeight(h);
        grabber.start();

        CanvasFrame frame = new CanvasFrame("Screen Capture");
        while (frame.isVisible()) {
            frame.showImage(grabber.grabImage());
            Thread.sleep(16);
        }
        frame.dispose();
        grabber.stop();
    }
}
