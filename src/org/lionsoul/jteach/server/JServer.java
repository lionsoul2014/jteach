package org.lionsoul.jteach.server;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.zip.Deflater;

import org.lionsoul.jteach.config.TaskConfig;
import org.lionsoul.jteach.log.Log;
import org.lionsoul.jteach.msg.JBean;
import org.lionsoul.jteach.msg.Packet;
import org.lionsoul.jteach.capture.ScreenCapture;
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
	public void initServer() {
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
		do {
			printInputAsk();
			line = reader.nextLine().trim().toLowerCase();
			arguments = CmdUtil.parseCMD(line);
			_input = arguments.get(CmdUtil.CMD_KEY);
			if (_input == null) {
				continue;
			}

			/*
			 * JSTask Working thread
			 * call the _runCommand to look for the class
			 * then start the thread, and we have to run ST to stop
			 * before another same thread could start. */
			if ( _input.equals(CmdUtil.SB) || _input.equals(CmdUtil.UF)
					|| _input.equals(CmdUtil.SM) || _input.equals(CmdUtil.RC)) {
				_runJSTask(_input);
			} else if ( _input.equals(CmdUtil.LS) ) {
				/* list all the online JBeans */
				listBeans();
			} else if ( _input.equals(CmdUtil.MENU) ) {
				/* show the function menu of JTeach */
				CmdUtil.showCmdMenu();
			} else if ( _input.equals(CmdUtil.STOP) ) {
				/* stop and reset the current working JSTask */
				if ( JSTask == null ) {
					println("no active task, run menu for help");
				} else {
					JSTask.stop();
					JSTask = null;
				}
			} else if ( _input.equals(CmdUtil.DELE) ) {
				/* remove JBean */
				delete();
			} else if ( _input.equals(CmdUtil.EXIT) ) {
				exit();
			} else {
				println("invalid command %s", _input);
			}
		} while ( true);
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
	public void startMonitorThread() {
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
		final List<JBean> list = new ArrayList<>();
		synchronized (beanList) {
			list.addAll(beanList);
		}

		return list;
	}
	
	public static void main(String[] args) {
		Log.setLevel(Log.INFO);	// default log level to info
		final TaskConfig config = TaskConfig.createDefault();
		for (int j = 0; j < args.length; j++) {
			if ("--port".equals(args[j])) {
				if (j + 1 >= args.length) {
					System.out.println("missing value for --port option");
					return;
				}

				int p = Integer.parseInt(args[j + 1]);
				if (p >= 1024 && p <= 65525) {
					PORT = p;
				}
			} else if ("--log-level".equals(args[j])) {
				if (j + 1 >= args.length) {
					System.out.println("missing value for --log-level option");
					return;
				}

				final String str = args[j + 1].toLowerCase();
				if (str.equals("debug")) {
					Log.setLevel(Log.DEBUG);
				} else if (str.equals("info")) {
					Log.setLevel(Log.INFO);
				} else if (str.equals("warn")) {
					Log.setLevel(Log.WARN);
				} else if (str.equals("error")) {
					Log.setLevel(Log.ERROR);
				} else {
					System.out.printf("invalid --log-level %s specified\n", str);
					return;
				}
			} else if ("--display".equals(args[j])) {
				if (j + 1 >= args.length) {
					System.out.println("missing value for --display option");
					return;
				}
				config.setDisplay(args[j + 1]);
			} else if ("--compress-level".equals(args[j])) {
				if (j + 1 >= args.length) {
					System.out.println("missing value for --compress-level option");
				}

				int level = Integer.parseInt(args[j + 1]);
				if (level < 1 || level > Deflater.BEST_COMPRESSION) {
					System.out.printf("invalid compress level %s specified\n", args[j + 1]);
					return;
				}
				config.setCompressLevel(level);
			} else if ("--capture-driver".equals(args[j])) {
				if (j + 1 >= args.length) {
					System.out.println("missing value for --capture-driver option");
					return;
				}

				final String str = args[j + 1].toLowerCase();
				if ("robot".equals(str)) {
					config.setCaptureDriver(ScreenCapture.ROBOT_DRIVER);
				} else if ("ffmpeg".equals(str)) {
					config.setCaptureDriver(ScreenCapture.FFMPEG_DRIVER);
				} else {
					System.out.printf("invalid capture-driver specified %s\n", str);
					return;
				}
			} else if ("--img-encode-policy".equals(args[j])) {
				if (j + 1 >= args.length) {
					System.out.println("missing value for --img-encode-policy option");
					return;
				}

				final String str = args[j + 1].toLowerCase();
				if ("imageio".equals(str)) {
					config.setImgEncodePolicy(ScreenCapture.IMAGEIO_POLICY);
				} else if ("databuffer".equals(str)) {
					config.setImgEncodePolicy(ScreenCapture.DATABUFFER_POLICY);
				} else {
					System.out.printf("invalid img-encode-policy specified %s\n", str);
					return;
				}
			} else if ("--img-format".equals(args[j])) {
				if (j + 1 >= args.length) {
					System.out.println("missing value for --img-format option");
					return;
				}

				config.setImgFormat(args[j+1]);
			} else if ("--img-compression-quality".equals(args[j])) {
				if (j + 1 >= args.length) {
					System.out.println("missing value for --img-compression-quality option");
					return;
				}

				final float quality = Float.valueOf(args[j]);
				if (quality >= 0f && quality <= 1.0f) {
					config.setImgCompressionQuality(quality);
				} else {
					System.out.printf("invalid img-compression-quality specified %f\n", quality);
					return;
				}
			} else if ("--filter-dup-img".equals(args[j])) {
				if (j + 1 >= args.length) {
					System.out.println("missing value for --filter-dup-img option");
					return;
				}

				final String str = args[j + 1].toLowerCase();
				if (str.equals("true") || str.equals("1") || str.equals("yes")) {
					config.setFilterDupImg(true);
				} else if (str.equals("false") || str.equals("0") || str.equals("no")) {
					config.setFilterDupImg(false);
				} else {
					System.out.printf("invalid detect-dup-img specified %s\n", str);
					return;
				}
			}
		}

		log.debug("starting server with config: %s", config.toString());
		final JServer server = new JServer(config);
		server.initServer();
		server.startMonitorThread();
		CmdUtil.showCmdMenu();
		server.cmdLoader();
	}

}