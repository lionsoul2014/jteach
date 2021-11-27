package com.webssky.jteach.msg;

import com.webssky.jteach.util.JCmdTools;

public class CommandIntegerMessage implements Message {

    public final char symbol;
    public final int cmd;
    public final long data;

    public CommandIntegerMessage(int cmd, long data) {
        this.symbol = JCmdTools.SEND_CMD_SYMBOL;
        this.cmd = cmd;
        this.data = data;
    }

    @Override
    public byte[] encode() {
        return new byte[0];
    }
}
