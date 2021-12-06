package org.lionsoul.jteach.msg;

import org.lionsoul.jteach.capture.ScreenCapture;
import org.lionsoul.jteach.util.CmdUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.*;

public class ScreenMessage implements Message {

    public final int driver;
    public final Point mouse;
    public final int width;
    public final int height;
    public final BufferedImage img;
    public final int encodePolicy;

    private String format;


    public ScreenMessage(int driver, final Point mouse, final BufferedImage img, int encodePolicy) {
        this.driver = driver;
        this.mouse = mouse;
        this.img = img;
        this.width = img.getWidth();
        this.height = img.getHeight();
        this.encodePolicy = encodePolicy;
        this.format = "jpeg";
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String str) {
        this.format = str;
    }

    @Override
    public Packet encode() throws IOException {
        return encode(PacketConfig.Default);
    }

    @Override
    public Packet encode(PacketConfig config) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final DataOutputStream dos = new DataOutputStream(bos);
        dos.writeInt(driver);
        dos.writeInt(mouse.x);
        dos.writeInt(mouse.y);
        dos.writeInt(width);
        dos.writeInt(height);
        dos.writeInt(encodePolicy);

        // encode the image
        if (encodePolicy == ScreenCapture.DATABUFFER_POLICY) {
            final DataBufferByte imgBuffer = (DataBufferByte) img.getRaster().getDataBuffer();
            bos.write(imgBuffer.getData());
        } else {
            ImageIO.write(img, format, bos);
        }

        return new Packet(CmdUtil.SYMBOL_SEND_DATA, CmdUtil.COMMAND_NULL, bos.toByteArray(), config);
    }

    public static ScreenMessage decode(final Packet p) throws IOException {
        final ByteArrayInputStream bis = new ByteArrayInputStream(p.input);
        final DataInputStream dis = new DataInputStream(bis);
        final int driver = dis.readInt();
        final int x = dis.readInt(), y = dis.readInt();
        final int w = dis.readInt(), h = dis.readInt();
        final int encodePolicy = dis.readInt();

        // decode the image
        final BufferedImage img;
        if (encodePolicy == ScreenCapture.DATABUFFER_POLICY) {
            img = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
            img.setData(Raster.createRaster(img.getSampleModel(),
                    new DataBufferByte(p.input, p.length - 24, 24), new Point()));
        } else {
            img = ImageIO.read(bis);
        }

        return new ScreenMessage(driver, new Point(x, y), img, encodePolicy);
    }

}