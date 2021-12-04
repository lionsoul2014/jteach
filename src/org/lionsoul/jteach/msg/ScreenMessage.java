package org.lionsoul.jteach.msg;

import org.lionsoul.jteach.log.Log;
import org.lionsoul.jteach.util.CmdUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.*;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class ScreenMessage implements Message {

    private static final Log log = Log.getLogger(ScreenMessage.class);

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

        // ImageIO.write(img, "jpeg", bos);
        long start = System.currentTimeMillis();
        final DataBufferByte imgBuffer = (DataBufferByte) img.getRaster().getDataBuffer();
        final byte[] imgData = imgBuffer.getData();

        // compress the data
        int len = 0;
        final Deflater deflater = new Deflater();
        deflater.setInput(imgData);
        // deflater.setLevel(Deflater.BEST_COMPRESSION);
        deflater.setLevel(Deflater.DEFLATED);
        deflater.finish();
        final byte[] buffer = new byte[8192];
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            bos.write(buffer, 0, count);
            len += count;
        }

        log.debug("end write, l: %d, cl: %d, cost: %dms\n", imgData.length, len, System.currentTimeMillis() - start);
        return new Packet(CmdUtil.SYMBOL_SEND_DATA, CmdUtil.COMMAND_NULL, bos.toByteArray());
    }

    public static final ScreenMessage decode(final Packet p) throws IOException {
        final ByteArrayInputStream bis = new ByteArrayInputStream(p.data);
        final DataInputStream dis = new DataInputStream(bis);
        final int x = dis.readInt(), y = dis.readInt();
        final int w = dis.readInt(), h = dis.readInt();

        // decompress the data
        final Inflater inflater = new Inflater();
        inflater.setInput(p.data, 16, p.data.length - 16);
        final byte[] buffer = new byte[8192];
        final ByteArrayOutputStream bos = new ByteArrayOutputStream(p.data.length * 2);
        while (!inflater.finished()) {
            int count = 0;
            try {
                count = inflater.inflate(buffer);
            } catch (DataFormatException e) {
                throw new IOException("image inflate error");
            }

            bos.write(buffer, 0, count);
        }

        final byte[] imgData = bos.toByteArray();
        final BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
        img.setData(Raster.createRaster(img.getSampleModel(), new DataBufferByte(imgData, imgData.length), new Point()));

        return new ScreenMessage(new Point(x, y), img);
    }

}
