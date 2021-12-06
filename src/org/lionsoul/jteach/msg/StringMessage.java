package org.lionsoul.jteach.msg;

import org.lionsoul.jteach.util.CmdUtil;

import java.io.*;

public class StringMessage implements Message {

    public final String str;

    public StringMessage(final String str) {
        this.str = str;
    }

    @Override
    public Packet encode() throws IOException {
        return encode(PacketConfig.Default);
    }

    @Override
    public Packet encode(PacketConfig config) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final DataOutputStream dos = new DataOutputStream(bos);
        dos.writeUTF(str);
        return new Packet(CmdUtil.SYMBOL_SEND_DATA, CmdUtil.COMMAND_NULL, bos.toByteArray(), config);
    }

    public static StringMessage decode(final Packet p) throws IOException {
        final ByteArrayInputStream bis = new ByteArrayInputStream(p.input);
        final DataInputStream dis = new DataInputStream(bis);
        return new StringMessage(dis.readUTF());
    }

}
