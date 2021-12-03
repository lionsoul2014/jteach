package org.lionsoul.jteach.msg;

import org.lionsoul.jteach.util.CmdUtil;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/*
 * <p>data transfer Packet class.</p>
 * <pre>
 * packet protocol:
 * +---------+---------+------------+----------+----------------+
 * | symbol  + attr    | [command] | length   | data           |
 * +---------+---------+-----------+----------+----------------+
 * | 1 byte  + 1 byte  | 4 bytes  | 4 bytes  | dynamic length |
 * +---------+--------+----------+---------+-----------------+
 * </pre>
 *
 * @author  chenxin<chenxin619315@gmail.com>
 */
public class Packet {

    /** packet attribute bit mask */
    public static final int HAS_CMD  = 0x01 << 0;
    public static final int HAS_DATA = 0x01 << 1;

    public final byte symbol;
    public final byte attr;
    public final int cmd;

    public final int offset;
    public final int length;
    public final byte[] data;

    public Packet(byte symbol, int cmd, byte[] data) {
        this(symbol, cmd, data, 0, data == null ? 0 : data.length);
    }

    public Packet(byte symbol, int cmd, byte[] data, int offset, int length) {
        this.symbol = symbol;
        this.cmd = cmd;
        this.data = data;

        // define the packet attribute byte
        byte attr = 0;
        if (cmd != CmdUtil.COMMAND_NULL) {
            attr |= HAS_CMD;
        }

        this.offset = offset;
        this.length = length;
        if (data != null) {
            attr |= HAS_DATA;
        }

        this.attr = attr;
    }

    public final boolean isSymbol(byte symbol) {
        return this.symbol == symbol;
    }

    public final boolean isCommand(int... cmd_list) {
        for (int cmd : cmd_list) {
            if (cmd == this.cmd) {
                return true;
            }
        }
        return false;
    }

    public byte[] encode() throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final DataOutputStream dos = new DataOutputStream(bos);

        // write the symbol char
        dos.writeByte(symbol);
        dos.writeByte(attr);

        // check and write the command
        if (cmd != CmdUtil.COMMAND_NULL) {
            dos.writeInt(cmd);
        }

        // check and write the data
        if (data != null) {
            dos.writeInt(length);
            dos.write(data, offset, length);
        }

        return bos.toByteArray();
    }

    /* symbol packet */
    public static final Packet ARP = new Packet(CmdUtil.SYMBOL_SEND_ARP, CmdUtil.COMMAND_NULL, null);
    public static final Packet HEARTBEAT = new Packet(CmdUtil.SYMBOL_SEND_HBT, CmdUtil.COMMAND_NULL, null);

    /* not a real data packet to transfer between */
    public static final Packet SOCKET_CLOSED = new Packet(CmdUtil.SYMBOL_SOCKET_CLOSED, CmdUtil.COMMAND_NULL, null);

    /* command packet */
    public static final Packet COMMAND_EXIT = new Packet(CmdUtil.SYMBOL_SEND_CMD, CmdUtil.COMMAND_EXIT, null);
    public static final Packet COMMAND_TASK_STOP = new Packet(CmdUtil.SYMBOL_SEND_CMD, CmdUtil.COMMAND_TASK_STOP, null);
    public static final Packet COMMAND_BROADCAST_START = new Packet(CmdUtil.SYMBOL_SEND_CMD, CmdUtil.COMMAND_BROADCAST_START, null);
    public static final Packet COMMAND_UPLOAD_START = new Packet(CmdUtil.SYMBOL_SEND_CMD, CmdUtil.COMMAND_UPLOAD_START, null);
    public static final Packet COMMAND_SCREEN_MONITOR = new Packet(CmdUtil.SYMBOL_SEND_CMD, CmdUtil.COMMAND_SCREEN_MONITOR, null);
    public static final Packet COMMAND_RCMD_SINGLE_EXECUTE = new Packet(CmdUtil.SYMBOL_SEND_CMD, CmdUtil.COMMAND_RCMD_SINGLE_EXECUTION, null);
    public static final Packet COMMAND_RCMD_ALL_EXECUTE = new Packet(CmdUtil.SYMBOL_SEND_CMD, CmdUtil.COMMAND_RCMD_ALL_EXECUTION, null);

    /* create data packet from basic type */
    public static final Packet valueOf(String str) throws IOException {
        return new StringMessage(str).encode();
    }

    public static final Packet valueOf(byte[] bytes) {
        return new Packet(CmdUtil.SYMBOL_SEND_DATA, CmdUtil.COMMAND_NULL, bytes);
    }

    public static final Packet valueOf(byte[] bytes, int off, int length) {
        return new Packet(CmdUtil.SYMBOL_SEND_DATA, CmdUtil.COMMAND_NULL, bytes, off, length);
    }

    public static final Packet valueOf(String str, long length) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final DataOutputStream dos = new DataOutputStream(bos);
        dos.writeUTF(str);
        dos.writeLong(length);
        return new Packet(CmdUtil.SYMBOL_SEND_DATA, CmdUtil.COMMAND_NULL, bos.toByteArray());
    }

}
