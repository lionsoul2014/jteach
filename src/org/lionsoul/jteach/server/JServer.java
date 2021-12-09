package org.lionsoul.jteach.server;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.zip.Deflater;

import org.lionsoul.jteach.cli.*;
import org.lionsoul.jteach.config.TaskConfig;
import org.lionsoul.jteach.log.Log;
import org.lionsoul.jteach.msg.JBean;
import org.lionsoul.jteach.msg.Packet;
import org.lionsoul.jteach.server.task.JSTaskBase;
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
	private HashMap<String, String> arguments = null;

	private JServer(TaskConfig config) {
		this.config = config;
		beanList = Collections.synchronizedList(new ArrayList<>());
	}
	
	/* Initialize the JTeach Server */
	public JServer init() {
		println("Initialize Server...");

		try {
			server = new ServerSocket(PORT);
			println("user.dir: "+System.getProperty("user.dir"));
		} catch (IOException e) {
			log.error("failed To initialize the server, make sure port %s is valid", PORT);
			System.exit(1);
		}

		try {
			String host = InetAddress.getLocalHost().getHostAddress();
			/* get the linux's remote host*/
			if ( "LINUX".equals(JServer.OS) ) {
				HashMap<String, String> ips = CmdUtil.getNetInterface();
				String remote = ips.get(CmdUtil.HOST_REMOTE_KEY);
				if ( remote != null ) {
					host = remote;
				}
			}
			println("Monitor - Host: %s, Port: %s", host, PORT);
		} catch (UnknownHostException e) {
			log.error("failed To get host information due to %s", e.getClass().getName());
		}

		return this;
	}

	public final void printInputAsk() {
		System.out.print("JTeach>> ");
	}

	public final String format(boolean newLine, String format, Object... args) {
		final StringBuffer sb = new StringBuffer();
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

	/** run command */
	public void cmdLoader() {
		String line, _input;
		final Scanner reader = new Scanner(System.in);
		while (true) {
			printInputAsk();
			line = reader.nextLine().trim().toLowerCase();
			arguments = CmdUtil.parseCMD(line);
			_input = arguments.get(CmdUtil.CMD_KEY);
			if (_input == null) {
				continue;
			}

			/*
			 * JSTask Working thread
			 * call the _runJSTask to look for the class
			 * then start the thread, and we have to run ST to stop
			 * before another same thread could start. */
			switch (_input) {
			case CmdUtil.SB:
			case CmdUtil.UF:
			case CmdUtil.SM:
			case CmdUtil.RC:
				_runJSTask(_input);
				break;
			case CmdUtil.LS:
				/* list all the online JBeans */
				listBeans();
				break;
			case CmdUtil.MENU:
				/* show the function menu of JTeach */
				CmdUtil.showCmdMenu();
				break;
			case CmdUtil.STOP:
				/* stop and reset the current working JSTask */
				if (JSTask == null) {
					println("no active task, run menu for help");
				} else {
					JSTask.stop();
					JSTask = null;
				}
				break;
			case CmdUtil.DELE:
				/* remove JBean */
				delete();
				break;
			case CmdUtil.EXIT:
				exit();
				break;
			default:
				println("invalid command %s", _input);
				break;
			}
		}
	}
	
	/* Find And Load The Task Class */
	private void _runJSTask(String cmd) {
		if ( JSTask != null ) {
			println("task %s is running, run stop before continue command %s", JSTask.getClass().getName(), cmd);
			return;
		}

		if ( beanList.size() == 0 ) {
			println("empty client list");
			return;
		}

		final String classname = "org.lionsoul.jteach.server.task."+cmd.toUpperCase()+"Task";
		try {
			println("try to start task %s", classname);
			Class<?> _class = Class.forName(classname);
			Constructor<?> con = _class.getConstructor(JServer.class);
			JSTask = (JSTaskBase) con.newInstance(this);
			JSTask.start();
		} catch (Exception e) {
			e.printStackTrace();
			println(log.getError("failed to start task %s due to %s", classname, e.getClass().getName()));
		}
	}

	/** stop the current running JSTask */
	public void stopJSTask() {
		if (JSTask != null) {
			JSTask.stop();
			JSTask = null;
		}
	}

	/** reset the JSTask */
	public void resetJSTask() {
		JSTask = null;
	}

	/** Start Listening Thread */
	public void start() {
		if (server != null) {
			JBean.threadPool.execute(this);
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

	/**
	 * exit the program 
	 * if EXIT_CLOSE_KEY is pass.
	 * A symbol will send to all the JBeans to
	 * order them to stop the client program.
	 */
	public void exit() {
		if ( arguments != null 
				&& arguments.get(CmdUtil.EXIT_CLOSE_KEY) != null
				&& arguments.get(CmdUtil.EXIT_CLOSE_KEY).equals(CmdUtil.EXIT_CLOSE_VAL) ) {
			synchronized (beanList) {
				for (JBean b : beanList) {
					try {
						b.offer(Packet.COMMAND_EXIT);
					} catch (IllegalAccessException e) {
						b.reportClosedError();
					}
				}
			}
		}

		println("Thank you for using jteach, bye!");
		System.exit(0);
	}
	
	/** remove all/ JBean */
	private void delete() {
		if ( JSTask != null ) {
			println("JSTask %s is running, run stop first\n", JSTask.getClass().getName());
			return;
		}

		if ( arguments == null ) {
			return;
		}

		String v = arguments.get(CmdUtil.DELETE_KEY);
		if ( v == null ) {
			println("-+-i : a/Integer: Remove The All/ith client");
			return;
		}

		/* remove all the JBean */
		if ( v.equals(CmdUtil.DELETE_ALL_VAL) ) {
			synchronized (beanList) {
				final Iterator<JBean> it = beanList.iterator();
				while (it.hasNext()) {
					final JBean b = it.next();
					try {
						b.offer(Packet.COMMAND_EXIT);
					} catch (IllegalAccessException e) {
						println(b.getClosedError());
						it.remove();
					}
				}
			}
			beanList.clear();
			println("all clients cleared");
		} else {
			/* remove the Specified JBean */
			if ( !v.matches("^[0-9]{1,}$") ) {
				println("invalid client index specified %s", v);
				return;
			}

			int index = Integer.parseInt(v);
			if ( index < 0 || index >= beanList.size() ) {
				println("Index out of bounds %d", index);
			} else {
				final JBean bean = beanList.get(index);
				try {
					bean.offer(Packet.COMMAND_EXIT);
				} catch (IllegalAccessException e) {
					println(bean.getClosedError());
				}
				beanList.remove(index);
				println("client on index %d removed", index);
			}
		}
	}
	
	public HashMap<String, String> getArguments() {
		return arguments;
	}
	
	public synchronized int getRunState() {
		return STATE;
	}
	
	public synchronized void setRunState(int s) {
		STATE = s;
	}
	
	/** list all the JBeans in BeanDB */
	public void listBeans() {
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

	/* return the bean size */
	public int beanCount() {
		return beanList.size();
	}

	/**
	 * make a copy for an array
	 * @return List
	 */
	public List<JBean> copyBeanList() {
		return new ArrayList<>(beanList);
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
			server.init().start();
			CmdUtil.showCmdMenu();
			server.cmdLoader();
		}).run(args);
	}

}