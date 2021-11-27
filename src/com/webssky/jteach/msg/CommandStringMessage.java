package com.webssky.jteach.msg;

import com.webssky.jteach.util.JCmdTools;

public class CommandStringMessage implements Message {

    public final char symbol;
    public final int cmd;
    public final String msg;

    public CommandStringMessage(int cmd, String msg) {
        this.symbol = JCmdTools.SEND_CMD_SYMBOL;
        this.cmd = cmd;
        this.msg = msg;
    }

    @Override
    public byte[] encode() {
        return new byte[0];
    }
}
