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

    /** image duplicate detect */
    public boolean filterDupImg;

    public TaskConfig(String display, int compressLevel, int captureDriver, int imageEncodePolicy) {
        this(display, compressLevel, captureDriver, imageEncodePolicy, "jpeg", true);
    }

    public TaskConfig(String display, int compressLevel, int captureDriver, int imgEncodePolicy, String imgFormat, boolean filterDupDetect) {
        this.display = display;
        this.compressLevel = compressLevel;
        this.captureDriver = captureDriver;
        this.imgEncodePolicy = imgEncodePolicy;
        this.imgFormat = imgFormat;
        this.filterDupImg = filterDupDetect;
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

    public void setFilterDupImg(boolean filterDupImg) {
        this.filterDupImg = filterDupImg;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append('[');
        sb.append("display: ").append(display).append(", ");
        sb.append("compressLevel: ").append(compressLevel).append(", ");
        sb.append("captureDriver: ").append(captureDriver).append(", ");
        sb.append("imgEncodePolicy: ").append(imgEncodePolicy).append(", ");
        sb.append("imgFormat: ").append(imgFormat).append(", ");
        sb.append("filterDupImg: ").append(filterDupImg ? "true" : "false");
        sb.append(']');
        return sb.toString();
    }

    public static TaskConfig createDefault() {
        return new TaskConfig(":1", Deflater.BEST_COMPRESSION, ScreenCapture.FFMPEG_DRIVER, ScreenCapture.IMAGEIO_POLICY, "jpeg", true);
    }

}
