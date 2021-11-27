package org.lionsoul.jteach.msg;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SymbolMessage implements Message {

    public final char symbol;

    public SymbolMessage(char symbol) {
        this.symbol = symbol;
    }

    @Override
    public byte[] encode() throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final DataOutputStream dos = new DataOutputStream(bos);
        dos.writeChar(symbol);
        dos.flush();
        return bos.toByteArray();
    }

    public static SymbolMessage valueOf(char symbol) {
        return new SymbolMessage(symbol);
    }
}
