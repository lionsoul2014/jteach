package org.lionsoul.jteach.msg;

import java.io.IOException;

public interface Message {
    public byte[] encode() throws IOException;
}
