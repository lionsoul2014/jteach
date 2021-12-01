package org.lionsoul.jteach.msg;

import org.lionsoul.jteach.util.CmdUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

public class ScreenMessage implements Message {

    public final Point mouse;
    public final BufferedImage img;

    public ScreenMessage(final Point mouse, final BufferedImage img) {
        this.mouse = mouse;
        this.img = img;
    }

    @Override
    public Packet encode() throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final DataOutputStream dos = new DataOutputStream(bos);
        dos.writeInt(mouse.x);
        dos.writeInt(mouse.y);
        ImageIO.write(img, "jpeg", bos);
        return new Packet(CmdUtil.SYMBOL_SEND_DATA, CmdUtil.COMMAND_NULL, bos.toByteArray());
    }

    public static final ScreenMessage decode(final Packet p) throws IOException {
        final ByteArrayInputStream bis = new ByteArrayInputStream(p.data);
        final DataInputStream dis = new DataInputStream(bis);
        return new ScreenMessage(new Point(dis.readInt(), dis.readInt()), ImageIO.read(bis));
    }

}
