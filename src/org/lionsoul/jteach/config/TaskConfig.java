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

    /** Image compression quality */
    public float imgCompressionQuality;

    /** image duplicate detect */
    public boolean filterDupImg;

    public TaskConfig(String display, int compressLevel, int captureDriver, int imageEncodePolicy) {
        this(display, compressLevel, captureDriver, imageEncodePolicy, ScreenCapture.DEFAULT_FORMAT, ScreenCapture.DEFAULT_COMPRESSION_QUALITY,true);
    }

    public TaskConfig(String display, int compressLevel, int captureDriver, int imgEncodePolicy, String imgFormat, float imgCompressionQuality, boolean filterDupDetect) {
        this.display = display;
        this.compressLevel = compressLevel;
        this.captureDriver = captureDriver;
        this.imgEncodePolicy = imgEncodePolicy;
        this.imgFormat = imgFormat;
        this.imgCompressionQuality = imgCompressionQuality;
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

    public void setCaptureDriver(String driver) {
        final String v = driver.toLowerCase();
        if ("robot".equals(v)) {
            this.captureDriver = ScreenCapture.ROBOT_DRIVER;
        } else if ("ffmpeg".equals(v)) {
            this.captureDriver = ScreenCapture.FFMPEG_DRIVER;
        }
    }

    public void setImgEncodePolicy(int policy) {
        this.imgEncodePolicy = policy;
    }

    public void setImgEncodePolicy(String policy) {
        final String v = policy.toLowerCase();
        if ("imageio".equals(v)) {
            this.imgEncodePolicy = ScreenCapture.IMAGEIO_POLICY;
        } else if ("databuffer".equals(v)) {
            this.imgEncodePolicy = ScreenCapture.DATABUFFER_POLICY;
        }
    }

    public void setImgFormat(String imgFormat) {
        this.imgFormat = imgFormat;
    }

    public void setImgCompressionQuality(float imgCompressionQuality) {
        this.imgCompressionQuality = imgCompressionQuality;
    }

    public void setFilterDupImg(boolean filterDupImg) {
        this.filterDupImg = filterDupImg;
    }

    @Override public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append('[');
        sb.append("display: ").append(display).append(", ");
        sb.append("compressLevel: ").append(compressLevel).append(", ");
        sb.append("captureDriver: ").append(captureDriver).append(", ");
        sb.append("imgEncodePolicy: ").append(imgEncodePolicy).append(", ");
        sb.append("imgFormat: ").append(imgFormat).append(", ");
        sb.append("imgCompressQuality: ").append(imgCompressionQuality).append(", ");
        sb.append("filterDupImg: ").append(filterDupImg ? "true" : "false");
        sb.append(']');
        return sb.toString();
    }

    public static TaskConfig createDefault() {
        return new TaskConfig(
            ":1", Deflater.BEST_COMPRESSION, ScreenCapture.FFMPEG_DRIVER,
            ScreenCapture.IMAGEIO_POLICY, ScreenCapture.DEFAULT_FORMAT,
            ScreenCapture.DEFAULT_COMPRESSION_QUALITY, true);
    }

}
