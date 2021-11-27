package com.webssky.jteach.msg;

import com.webssky.jteach.util.JCmdTools;

public class StringDataMessage implements Message {

    public final char symbol;
    public final String data;

    public StringDataMessage(String data) {
        this.symbol = JCmdTools.SEND_DATA_SYMBOL;
        this.data = data;
    }

    @Override
    public byte[] encode() {
        return new byte[0];
    }
}
