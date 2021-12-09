package org.lionsoul.jteach.cli;

public class CommandTest {
    public static void main(String[] args) {
        Command.create("sb --list=1,2,3", "screen broadcast", "sb", new Flag[]{
                new StringFlag("list", "bean list exploded by ',', eg: 0,1,2,3", "")
        }, ctx -> {
            System.out.printf("list=%s", ctx.stringVal("list"));
        }).start();
    }
}
