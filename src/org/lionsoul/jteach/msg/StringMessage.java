package org.lionsoul.jteach.msg;

import org.lionsoul.jteach.util.JCmdTools;

import java.io.*;

public class StringMessage implements Message {

    public final String str;

    public StringMessage(final String str) {
        this.str = str;
    }

    @Override
    public Packet encode() throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final DataOutputStream dos = new DataOutputStream(bos);
        dos.writeUTF(str);
        return new Packet(JCmdTools.SYMBOL_SEND_DATA, JCmdTools.COMMAND_NULL, bos.toByteArray());
    }

    public static final StringMessage decode(final Packet p) throws IOException {
        final ByteArrayInputStream bis = new ByteArrayInputStream(p.data);
        final DataInputStream dis = new DataInputStream(bis);
        return new StringMessage(dis.readUTF());
    }

}
