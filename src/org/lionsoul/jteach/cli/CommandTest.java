package org.lionsoul.jteach.cli;

public class CommandTest {
    public static void main(String[] args) {
        Command.C("screen broadcast", "sb", new Flag[]{
            new StringFlag("list", "bean list exploded by ',', eg: 0,1,2,3", "")
        }, ctx -> {
            System.out.println(ctx);
        }).run("sb --list=1,2,3,4");
    }
}
