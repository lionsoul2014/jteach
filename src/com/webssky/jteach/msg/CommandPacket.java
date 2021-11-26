package com.webssky.jteach.msg;

import com.webssky.jteach.util.JCmdTools;

public class CommandPacket implements Packet {

    public final char symbol;
    public final int cmd;

    public CommandPacket(final int cmd) {
        this.symbol = JCmdTools.SEND_CMD_SYMBOL;
        this.cmd = cmd;
    }

    @Override
    public byte[] encode() {
        return new byte[0];
    }
}
