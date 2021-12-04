package org.lionsoul.jteach.msg;

import org.lionsoul.jteach.util.CmdUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.*;

public class ScreenMessage implements Message {

    public final Point mouse;
    public final int width;
    public final int height;
    public final BufferedImage img;

    public ScreenMessage(final Point mouse, final BufferedImage img) {
        this.mouse = mouse;
        this.img = img;
        this.width = img.getWidth();
        this.height = img.getHeight();
    }

    @Override
    public Packet encode() throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream(img.getWidth() * img.getHeight() + 16);
        final DataOutputStream dos = new DataOutputStream(bos);
        dos.writeInt(mouse.x);
        dos.writeInt(mouse.y);
        dos.writeInt(width);
        dos.writeInt(height);
        long start = System.currentTimeMillis();
        // ImageIO.write(img, "jpeg", bos);
        final DataBufferByte buffer = (DataBufferByte) img.getData().getDataBuffer();
        bos.write(buffer.getData());
        System.out.printf("end write, type: %d, cost: %dms\n", img.getType(), System.currentTimeMillis() - start);
        return new Packet(CmdUtil.SYMBOL_SEND_DATA, CmdUtil.COMMAND_NULL, bos.toByteArray());
    }

    public static final ScreenMessage decode(final Packet p) throws IOException {
        final ByteArrayInputStream bis = new ByteArrayInputStream(p.data);
        final DataInputStream dis = new DataInputStream(bis);
        final int x = dis.readInt(), y = dis.readInt();
        final int w = dis.readInt(), h = dis.readInt();
        final BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        img.setData(Raster.createRaster(img.getSampleModel(), new DataBufferByte(p.data, 16, p.data.length - 16), new Point()));
        return new ScreenMessage(new Point(x, y), img);
    }

}
