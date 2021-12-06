package org.lionsoul.jteach.msg;

import java.io.IOException;

public interface Message {
    public Packet encode() throws IOException;
    public Packet encode(PacketConfig config) throws IOException;
}
