package com.webssky.jteach.msg;

public class SymbolPacket implements Packet {

    public final char symbol;

    public SymbolPacket(char symbol) {
        this.symbol = symbol;
    }

    @Override
    public byte[] encode() {
        return new byte[0];
    }
}
