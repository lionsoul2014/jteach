package org.lionsoul.jteach.config;

import org.lionsoul.jteach.capture.ScreenCapture;

import java.util.zip.Deflater;

public class TaskConfig {

    /* display device */
    public String display;

    /* message compress level */
    public int compressLevel;

    /* screen capture driver */
    public int captureDriver;

    public TaskConfig(String display, int compressLevel, int captureDriver) {
        this.display = display;
        this.compressLevel = compressLevel;
        this.captureDriver = captureDriver;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public void setCompressLevel(int compressLevel) {
        this.compressLevel = compressLevel;
    }

    public void setCaptureDriver(int captureDriver) {
        this.captureDriver = captureDriver;
    }

    public static TaskConfig createDefault() {
        return new TaskConfig(":1", Deflater.BEST_COMPRESSION, ScreenCapture.FFMPEG_DRIVER);
    }

}
