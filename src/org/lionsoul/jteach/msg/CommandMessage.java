package org.lionsoul.jteach.msg;

import org.lionsoul.jteach.util.JCmdTools;

public class CommandMessage implements Message {

    public final char symbol;
    public final int cmd;

    public CommandMessage(final int cmd) {
        this.symbol = JCmdTools.SEND_CMD_SYMBOL;
        this.cmd = cmd;
    }

    @Override
    public byte[] encode() {
        return new byte[0];
    }
}
