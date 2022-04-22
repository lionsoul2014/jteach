package org.lionsoul.jteach.msg;

import org.lionsoul.jteach.util.CmdUtil;

public class BytePacket {

    public final byte[] data;

    public BytePacket(byte[] input) {
        this.data = input;
    }

    public final boolean isSymbol(byte symbol) {
        return data[0] == symbol;
    }

    public final boolean isCommand(int... cmd_list) {
        final int cmd = getCommand();
        for (int c : cmd_list) {
            if (c == cmd) {
                return true;
            }
        }
        return false;
    }

    /** return the symbol from a byte input packet */
    public byte getSymbol() {
        return data[0];
    }

    /** return the attribute from the byte input packet */
    public byte getAttr() {
        return data[1];
    }

    /** return the command from the byte input packet */
    public int getCommand() {
        final byte attr = data[1];
        return (attr & Packet.HAS_CMD) == 0 ? CmdUtil.COMMAND_NULL : data[2];
    }

    /** return the data length from the byte input packet */
    public int dataLength() {
        final byte attr = data[1];
        if ((attr & Packet.HAS_DATA) == 0) {
            return 0;
        }

        int i = 2;
        if ((attr & Packet.HAS_CMD) != 0) {
            i += 4;
        }

        final int ch1 = data[i];
        final int ch2 = data[i+1];
        final int ch3 = data[i+2];
        final int ch4 = data[i+3];
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4));
    }

    /** return if the data packet is compressed */
    public boolean isCompressed() {
        final byte attr = data[1];
        return (attr & Packet.HAS_COMPRESSED) != 0;
    }

    /** wrap the byte array input */
    public static BytePacket wrap(byte[] input) {
        return new BytePacket(input);
    }

}
