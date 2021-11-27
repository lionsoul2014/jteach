package org.lionsoul.jteach.msg;

public class SymbolMessage implements Message {

    public final char symbol;

    public SymbolMessage(char symbol) {
        this.symbol = symbol;
    }

    @Override
    public byte[] encode() {
        return new byte[0];
    }
}
