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

    public ScreenMessage(int driver, final Point mouse, final BufferedImage img) {
        this.driver = driver;
        this.mouse = mouse;
        this.img = img;
        this.width = img.getWidth();
        this.height = img.getHeight();
    }

    @Override
    public Packet encode() throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final DataOutputStream dos = new DataOutputStream(bos);
        dos.writeInt(driver);
        dos.writeInt(mouse.x);
        dos.writeInt(mouse.y);
        dos.writeInt(width);
        dos.writeInt(height);

        // encode the image
        if (driver == ScreenCapture.FFMPEG_DRIVER) {
            final DataBufferByte imgBuffer = (DataBufferByte) img.getRaster().getDataBuffer();
            bos.write(imgBuffer.getData());
        } else {
            ImageIO.write(img, "jpg", bos);
        }

        return new Packet(CmdUtil.SYMBOL_SEND_DATA, CmdUtil.COMMAND_NULL, bos.toByteArray());
    }

    public static final ScreenMessage decode(final Packet p) throws IOException {
        final ByteArrayInputStream bis = new ByteArrayInputStream(p.data);
        final DataInputStream dis = new DataInputStream(bis);
        final int driver = dis.readInt();
        final int x = dis.readInt(), y = dis.readInt();
        final int w = dis.readInt(), h = dis.readInt();

        // decode the image
        final BufferedImage img;
        if (driver == ScreenCapture.FFMPEG_DRIVER) {
            img = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
            img.setData(Raster.createRaster(img.getSampleModel(),
                    new DataBufferByte(p.data, p.length - 16, 16), new Point()));
        } else {
            img = ImageIO.read(bis);
        }

        return new ScreenMessage(driver, new Point(x, y), img);
    }

}
