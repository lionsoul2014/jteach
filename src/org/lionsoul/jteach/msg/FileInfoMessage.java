package org.lionsoul.jteach.msg;

import org.lionsoul.jteach.util.CmdUtil;

import java.io.*;

public class FileInfoMessage implements Message {

    public final long length;
    public final String name;

    public FileInfoMessage(final long length, final String name) {
        this.length = length;
        this.name = name;
    }

    @Override
    public Packet encode() throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final DataOutputStream dos = new DataOutputStream(bos);
        dos.writeLong(length);
        dos.writeUTF(name);
        return new Packet(CmdUtil.SYMBOL_SEND_DATA, CmdUtil.COMMAND_NULL, bos.toByteArray());
    }

    public static final FileInfoMessage decode(final Packet p) throws IOException {
        final ByteArrayInputStream bis = new ByteArrayInputStream(p.data);
        final DataInputStream dis = new DataInputStream(bis);
        return new FileInfoMessage(dis.readLong(), dis.readUTF());
    }

}
