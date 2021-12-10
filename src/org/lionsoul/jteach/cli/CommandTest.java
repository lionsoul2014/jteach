package org.lionsoul.jteach.cli;

public class CommandTest {

    private static final Command sbCommand = Command.C("sb", "server screen broadcast", new Flag[] {
        StringFlag.C("list", "bean list exploded by ',', eg: 0,1,2,3", ""),
    }, ctx -> {
        System.out.printf("%s is running with list=%s", ctx.name, ctx.stringVal("list"));
    });

    private static final Command smCommand = Command.C("sm", "client screen monitor", new Flag[] {
        IntFlag.C("index", "bean index", 0),
        BoolFlag.C("control", "control the specified client bean", false),
    }, new Command[] {
        Command.C("sb", "screen monitor and broadcast to clients", new Flag[] {
            StringFlag.C("list", "bean list exploded by ',', eg: 0,1,2,3", "")
        }, ctx -> {
            System.out.printf("sm sb is running with list=%s", ctx.stringVal("list"));
        })
    }, ctx -> {
        System.out.printf("%s is running with index=%d, control=%b", ctx.name, ctx.intVal("index"), ctx.boolVal("control"));
    });

    private static final Command ufCommand = Command.C("uf", "upload file to clients", new Flag[] {
        StringFlag.C("list", "bean list exploded by ',', eg: 0,1,2,3", ""),
    }, ctx -> {
        System.out.printf("%s is running with list=%s", ctx.name, ctx.stringVal("list"));
    });


    public static void main(String[] args) {
        Command.C("jteach server", "jteach server", new Command[] {
            sbCommand, smCommand, ufCommand,
        }).run("sm --index=0 --help sb --list=1,2");
    }

}
