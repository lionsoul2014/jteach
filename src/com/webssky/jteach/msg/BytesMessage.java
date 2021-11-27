package com.webssky.jteach.msg;

import com.webssky.jteach.util.JCmdTools;

public class BytesMessage implements Message {

    public final char symbol;
    public final byte[] data;

    public BytesMessage(byte[] data) {
        this.symbol = JCmdTools.SEND_DATA_SYMBOL;
        this.data = data;
    }

    @Override
    public byte[] encode() {
        return new byte[0];
    }
}
