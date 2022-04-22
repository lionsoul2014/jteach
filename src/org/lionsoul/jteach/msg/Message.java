package org.lionsoul.jteach.msg;

import java.io.IOException;

public interface Message {
    Packet encode() throws IOException;
    Packet encode(PacketConfig config) throws IOException;
}
