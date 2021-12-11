package org.lionsoul.jteach.server;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.zip.Deflater;

import org.lionsoul.jteach.cli.*;
import org.lionsoul.jteach.config.TaskConfig;
import org.lionsoul.jteach.log.Log;
import org.lionsoul.jteach.msg.JBean;
import org.lionsoul.jteach.msg.Packet;
import org.lionsoul.jteach.server.task.*;
import org.lionsoul.jteach.util.CmdUtil;

/**
 * JTeach Server
 * @author chenxin<chenxin619315@gmail.com>
 */
public class JServer implements Runnable {

	/* Server port */
	public static int PORT = 55535;
	public static ServerSocket server = null;
	public static final String OS = System.getProperty("os.name").toUpperCase();
	public static final Log log = Log.getLogger(JServer.class);

	/* config item need by the task running */
	public final TaskConfig config;

	public static final int M_RUN = 1;
	public static final int M_OVER = 0;
	private int STATE = M_RUN;
	
	private volatile JSTaskBase JSTask = null;
	private final List<JBean> beanList;

	/* command application */
	private final Command app = Command.C("jteach-server", "jteach server", new Command[] {
		Command.C("sb", "start the screen broadcast task", new Flag[] {
			StringFlag.C("list", "client index list separated by commas", "")
		}, ctx -> _runJSTask(SBTask.class, Packet.COMMAND_BROADCAST_START, ctx)),
		Command.C("sm", "start the client screen monitor task", new Flag[] {
			StringFlag.C("list", "client index, will only take the first one", "")
		}, ctx -> _runJSTask(SMTask.class, Packet.COMMAND_SCREEN_MONITOR, ctx)),
		Command.C("uf", "start upload file to clients task", new Flag[] {
			StringFlag.C("list", "client index list separated by commas","")
		}, ctx -> _runJSTask(UFTask.class, Packet.COMMAND_UPLOAD_START, ctx)),
		Command.C("rc", "start the remote command execution task", new Flag[] {
			StringFlag.C("list", "client index list separated by commas","")
		}, ctx -> {
			final String[] list = ctx.stringList("list");
			_runJSTask(RCTask.class, list.length == 1
					? Packet.COMMAND_RCMD_SINGLE_EXECUTE : Packet.COMMAND_RCMD_ALL_EXECUTE, ctx);
		}),
		Command.C("ls", "list all the clients and clean up offline", Flag.EMPTY, this::listBeans),
		Command.C("del", "delete the specified client and send exit command", new Flag[] {
			StringFlag.C("list", "(Must) client index list separated by commas","")
		}, this::delete),
		Command.C("stop", "stop the current running task", Flag.EMPTY, ctx -> _stopJSTask()),
		Command.C("exit", "shutdown and exit the program", new Flag[] {
			BoolFlag.C("sync", "tell the clients to exit", false)
		}, this::exit),
	});

	private JServer(TaskConfig config) {
		this.config = config;
		beanList = Collections.synchronizedList(new ArrayList<>());
	}
	
	/* Initialize the JTeach Server */
	public JServer init() throws IOException {
		println("Initialize Server...");
		server = new ServerSocket(PORT);

		String host = InetAddress.getLocalHost().getHostAddress();
		/* get the linux's remote host*/
		if ( "LINUX".equals(JServer.OS) ) {
			HashMap<String, String> ips = CmdUtil.getNetInterface();
			String remote = ips.get(CmdUtil.HOST_REMOTE_KEY);
			if ( remote != null ) {
				host = remote;
			}
		}

		println("JTeach  : Multimedia teaching platform");
		println("@Author : Lion<chenxin619315@gmail.com>");
		println("user.dir: %s", System.getProperty("user.dir"));
		println("server  : {Host: %s, Port: %s}", host, PORT);
		println("");
		return this;
	}

	/** Start Listening Thread */
	public void start() {
		JBean.threadPool.execute(this);
		app.run("help");
		final Scanner reader = new Scanner(System.in);
		while (true) {
			printInputAsk();
			app.run(reader.nextLine().trim().toLowerCase());
		}
	}

	/* Listening task */
	@Override
	public void run() {
		while ( getRunState() == M_RUN ) {
			try {
				final Socket s = server.accept();

				/*
				 * get a Socket from the Socket Queue
				 * and create new JBean Object to manager it */
				final JBean bean = new JBean(s);
				beanList.add(bean);
				bean.start();
				lnPrintln("new client %s connected", bean.getName());
				printInputAsk();

				/* check and add the new client to the running task */
				if (JSTask != null) {
					JSTask.addClient(bean);
				}
			} catch (IOException e) {
				lnPrintln("server monitor thread stopped due to %s", e.getClass());
				break;
			}
		}
	}

	/* Find And Load The Task Class */
	private void _runJSTask(Class<? extends JSTaskBase> _class, Packet startPacket, Command ctx) {
		if ( JSTask != null ) {
			println("task %s is running, run stop before continue", JSTask.getClass().getName());
			return;
		}

		if ( beanList.size() == 0 ) {
			println("empty client list");
			return;
		}

		/* parser the list arguments and make the bean list for the current task */
		final String[] list = ctx.stringList("list");
		final List<JBean> taskBeans = new ArrayList<>();
		if (list.length == 0) {
			taskBeans.addAll(beanList);
		} else {
			/* check and parser the bean list (duplicated) */
			for (String idx : list) {
				final int i = Integer.parseInt(idx);
				if (i < 0 || i >= beanList.size()) {
					println("invalid index %s", idx);
					return;
				}
				taskBeans.add(beanList.get(i));
			}
		}

		if (_class == SMTask.class && taskBeans.size() > 1) {
			final JBean first = taskBeans.get(0);
			taskBeans.clear();
			taskBeans.add(first);
			println("WARN: use only the first client %s ", first.getHost());
		}

		try {
			println("try to start task %s", _class.getName());
			final Constructor<?> con = _class.getConstructor(JServer.class);
			final JSTaskBase task = (JSTaskBase) con.newInstance(this);
			if (task.start(taskBeans, startPacket)) {
				JSTask = task;
			}
		} catch (NoSuchMethodException | InvocationTargetException
				| InstantiationException | IllegalAccessException e) {
			println(log.getError("failed to start task %s due to %s: %s",
					_class.getName(), e.getClass().getName(), e.getMessage()));
		}
	}

	/** stop the current running JSTask */
	public void _stopJSTask() {
		if (JSTask == null) {
			println("no running task to stop");
			return;
		}

		JSTask.stop();
		JSTask = null;
	}

	/** reset the JSTask */
	public void resetJSTask() {
		JSTask = null;
	}

	/** list all the JBeans in BeanDB */
	public void listBeans(Command ctx) {
		synchronized (beanList) {
			int j = 0;
			final Iterator<JBean> it = beanList.iterator();
			while ( it.hasNext() ) {
				final JBean b = it.next();

				String num = CmdUtil.formatString(j+"", 2, '0');
				j++;

				// send the ARP to the client
				try {
					b.offer(Packet.ARP);
					println("client {index: %s, host: %s}", num, b.getHost());
				} catch (IllegalAccessException e) {
					println("client {index: %s, host: %s} was removed", num, b.getHost());
					it.remove();
				}
			}
		}
	}

	/** remove the specified beans */
	private void delete(Command ctx) {
		final String[] list = ctx.stringList("list");
		if (list.length == 0) {
			app.run("del --help");
		} else {
			/* parser the index list */
			List<Integer> idxList = new ArrayList<>();
			for (String idx : list) {
				final int i = Integer.parseInt(idx);
				if (i < 0 || i >= beanList.size()) {
					println("invalid index %s", idx);
					return;
				}
				idxList.add(i);
			}

			for (int i : idxList) {
				final JBean bean = beanList.get(i);
				beanList.remove(i);
				try {
					bean.offer(Packet.COMMAND_EXIT);
				} catch (IllegalAccessException e) {
					println(bean.getClosedError());
				}
			}

			println("clients on index [%s] removed", ctx.stringVal("list"));
		}
	}

	/** exit the program */
	public void exit(Command ctx) {
		if (ctx.boolVal("sync")) {
			synchronized (beanList) {
				for (JBean b : beanList) {
					try {
						b.offer(Packet.COMMAND_EXIT);
					} catch (IllegalAccessException e) {
						println(b.getClosedError());
					}
				}
			}
		}

		println("Thank you for using jteach, bye!");
		System.exit(0);
	}

	public synchronized int getRunState() {
		return STATE;
	}
	
	public synchronized void setRunState(int s) {
		STATE = s;
	}

	public final void printInputAsk() {
		System.out.print("JTeach>> ");
	}

	public final String format(boolean newLine, String format, Object... args) {
		final StringBuilder sb = new StringBuilder();
		if (newLine) {
			sb.append("\n");
		}
		sb.append(String.format(format, args));
		return sb.toString();
	}

	public final void print(String format, Object... args) {
		final String str = format(false, format, args);
		System.out.print(str);
		System.out.flush();
	}

	public final void println(String format, Object... args) {
		final String str = format(false, format, args);
		System.out.println(str);
		System.out.flush();
	}

	public final void println(String str) {
		System.out.println(str);
		System.out.flush();
	}

	public final void lnPrintln(String format, Object... args) {
		final String str = format(true, format, args);
		System.out.println(str);
		System.out.flush();
	}

	public static void main(String[] args) {
		Command.C("jteach-server", "jteach-server", new Flag[] {
			IntFlag.C("port", "listening port", 55535),
			StringFlag.C("log-level", "log level set", "info", new String[]{"debug", "info", "warn", "error"}),
			StringFlag.C("display", "ffmpeg display device", ":1"),
			IntFlag.C("compression-level", "packet compression level", Deflater.BEST_COMPRESSION),
			StringFlag.C("capture-driver", "capture driver", "ffmpeg", new String[]{"robot", "ffmpeg"}),
			StringFlag.C("img-encode-policy", "screen image transfer encode policy", "imageio", new String[]{"imageio", "databuffer"}),
			StringFlag.C("img-format", "screen image encode format", "JPG", new String[]{"jpg", "jpeg", "png", "gif"}),
			FloatFlag.C("img-compression-quality", "image encode compression quality", 0.86f),
			BoolFlag.C("filter-dup-img", "filter the transfer of the duplication screen image", true)
		}, (Command ctx) -> {
			Log.setLevel(ctx.stringVal("log-level"));
			final TaskConfig config = TaskConfig.createDefault();
			config.setDisplay(ctx.stringVal("display"));
			config.setCompressLevel(ctx.intVal("compression-level"));
			config.setCaptureDriver(ctx.stringVal("capture-driver"));
			config.setImgEncodePolicy(ctx.stringVal("img-encode-policy"));
			config.setImgCompressionQuality(ctx.floatVal("img-compression-quality"));
			config.setFilterDupImg(ctx.boolVal("filter-dup-img"));
			config.setImgFormat(ctx.stringVal("img-format"));
			final JServer server = new JServer(config);
			server.println("config: %s", config.toString());
			try {
				server.init().start();
			} catch (IOException e) {
				server.println("failed to start jteach server %s: %s", e.getClass().getName(), e.getMessage());
			}
		}).run(args);
	}

}