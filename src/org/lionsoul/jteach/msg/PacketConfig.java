package org.lionsoul.jteach.msg;

import java.util.zip.Deflater;

public class PacketConfig {

    /** default packet config */
    public static final PacketConfig Default = new PacketConfig(true, Deflater.DEFLATED, 65535);

    private boolean autoCompress;
    private int compressLevel;
    private int minCompressBytes;

    public PacketConfig(boolean autoCompress, int compressLevel) {
        this.autoCompress = autoCompress;
        this.compressLevel = compressLevel;
        this.minCompressBytes = 65535;
    }

    public PacketConfig(boolean autoCompress, int compressLevel, int minCompressBytes) {
        this.autoCompress = autoCompress;
        this.compressLevel = compressLevel;
        this.minCompressBytes = minCompressBytes;
    }

    public boolean isAutoCompress() {
        return autoCompress;
    }

    public void setAutoCompress(boolean autoCompress) {
        this.autoCompress = autoCompress;
    }

    public int getCompressLevel() {
        return compressLevel;
    }

    public void setCompressLevel(int compressLevel) {
        this.compressLevel = compressLevel;
    }

    public int getMinCompressBytes() {
        return minCompressBytes;
    }

    public void setMinCompressBytes(int minCompressBytes) {
        this.minCompressBytes = minCompressBytes;
    }

}
