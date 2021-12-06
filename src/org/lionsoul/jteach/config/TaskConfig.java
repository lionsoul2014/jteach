package org.lionsoul.jteach.config;

import org.lionsoul.jteach.capture.ScreenCapture;

import java.util.zip.Deflater;

public class TaskConfig {

    /** display device */
    public String display;

    /** message compress level */
    public int compressLevel;

    /** screen capture driver */
    public int captureDriver;

    /** image encode policy */
    public int imgEncodePolicy;

    /** Image format */
    public String imgFormat;

    public TaskConfig(String display, int compressLevel, int captureDriver, int imageEncodePolicy) {
        this(display, compressLevel, captureDriver, imageEncodePolicy, "jpeg");
    }

    public TaskConfig(String display, int compressLevel, int captureDriver, int imgEncodePolicy, String imgFormat) {
        this.display = display;
        this.compressLevel = compressLevel;
        this.captureDriver = captureDriver;
        this.imgEncodePolicy = imgEncodePolicy;
        this.imgFormat = imgFormat;
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

    public void setImgEncodePolicy(int policy) {
        this.imgEncodePolicy = policy;
    }

    public void setImgFormat(String imgFormat) {
        this.imgFormat = imgFormat;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append('[');
        sb.append("display: ").append(display).append(", ");
        sb.append("compressLevel: ").append(compressLevel).append(", ");
        sb.append("captureDriver: ").append(captureDriver).append(", ");
        sb.append("imgEncodePolicy: ").append(imgEncodePolicy).append(", ");
        sb.append("imgFormat: ").append(imgFormat);
        sb.append(']');
        return sb.toString();
    }

    public static TaskConfig createDefault() {
        return new TaskConfig(":1", Deflater.BEST_COMPRESSION, ScreenCapture.FFMPEG_DRIVER, ScreenCapture.DATABUFFER_POLICY, "jpeg");
    }

}
