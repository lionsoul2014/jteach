package org.lionsoul.jteach.server;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;

import org.lionsoul.jteach.log.Log;
import org.lionsoul.jteach.msg.JBean;
import org.lionsoul.jteach.msg.Packet;
import org.lionsoul.jteach.server.task.JSTaskInterface;
import org.lionsoul.jteach.util.JCmdTools;
import org.lionsoul.jteach.util.JServerLang;

/**
 * JTeach Server
 * @author chenxin<chenxin619315@gmail.com>
 */
public class JServer {

	/* Server port */
	public static int PORT = 55535;
	public static ServerSocket server = null;
	public static final String OS = System.getProperty("os.name").toUpperCase();
	public static final Log log = Log.getLogger(JServer.class);

	public static final int M_RUN = 1;
	public static final int M_OVER = 0;
	private int STATE = M_RUN;
	
	private volatile JSTaskInterface JSTask = null;
	private final List<JBean> beanList;
	private HashMap<String, String> arguments = null;

	private JServer() {
		beanList = Collections.synchronizedList(new ArrayList<>());
	}
	
	/* Initialize the JTeach Server */
	public void initServer() {
		JServerLang.SERVER_INIT();

		try {
			server = new ServerSocket(PORT);
			System.out.println("user.dir: "+System.getProperty("user.dir"));
		} catch (IOException e) {
			JServerLang.SERVER_INIT_FAILED();
			System.exit(1);
		}

		try {
			JServerLang.MONITOR_INFO();
		} catch (UnknownHostException e) {
			JServerLang.GETINFO_FAILED();
		}
	}
	
	/** reset JSTask */
	public void stopJSTask() {
		if (JSTask != null) {
			JSTask.stop();
			JSTask = null;
		}
	}
	
	/** run command */
	public void _CmdLoader() {
		String line, _input;
		final Scanner reader = new Scanner(System.in);
		do {
			JServerLang.INPUT_ASK();
			line = reader.nextLine().trim().toLowerCase();
			arguments = JCmdTools.parseCMD(line);
			_input = arguments.get(JCmdTools.CMD_KEY);
			if (_input == null) {
				continue;
			}

			/*
			 * JSTask Working thread
			 * call the _runCommand to look for the class
			 * then start the thread, and we have to run ST to stop
			 * before another same thread could start.
			 */
			if ( _input.equals(JCmdTools.SB) || _input.equals(JCmdTools.UF)
					|| _input.equals(JCmdTools.SM) || _input.equals(JCmdTools.RC)) {
				_runJSTask(_input);
			} else if ( _input.equals(JCmdTools.LS) ) {
				/* list all the online JBeans */
				listBeans();
			} else if ( _input.equals(JCmdTools.MENU) ) {
				/* show the function menu of JTeach */
				JCmdTools.showCmdMenu();
			} else if ( _input.equals(JCmdTools.STOP) ) {
				/* stop the current JSTask working thread
				 * and reset the JSTask */
				if ( JSTask == null ) {
					JServerLang.STOP_NULL_THREAD();
				} else {
					JSTask.stop();
					JSTask = null;
				}
			} else if ( _input.equals(JCmdTools.DELE) ) {
				/* remove JBean */
				delete();
			} else if ( _input.equals(JCmdTools.EXIT) ) {
				exit();
			} else {
				JServerLang.UNKNOW_COMMAND();
			}
		} while ( true);
	}
	
	/* Find And Load The Task Class */
	private void _runJSTask(String cmd) {
		if ( JSTask != null ) {
			JServerLang.START_THREAD_RUNNING();
			return;
		}

		if ( beanList.size() == 0 ) {
			JServerLang.EMPTY_JBENAS();
			return;
		}

		try {
			String classname = "org.lionsoul.jteach.server.task."+cmd.toUpperCase()+"Task";
			JServerLang.TASK_PATH_INFO(classname);
			Class<?> _class = Class.forName(classname);
			Constructor<?> con = _class.getConstructor(JServer.class);
			JSTask = (JSTaskInterface) con.newInstance(this);
			if ( !JSTask.start() ) {
				stopJSTask();
			}
		} catch (Exception e) {
			//e.printStackTrace();
			JServerLang.RUN_COMMAND_ERROR(cmd);
		}
	}
	
	/** Start Listening Thread */
	public void StartMonitorThread() {
		if (server != null) {
			JBean.threadPool.execute(new ConnectMonitor());
		}
	}
	
	/** Listening Task inner class */
	private class ConnectMonitor implements Runnable {
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
					log.debug("new client %s connected", bean.getName());

					/* check and add the new client to the running task */
					if (JSTask != null) {
						JSTask.addClient(bean);
					}
				} catch (IOException e) {
					JServerLang.SERVER_ACCEPT_ERROR();
					break;
				}
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
				&& arguments.get(JCmdTools.EXIT_CLOSE_KEY) != null
				&& arguments.get(JCmdTools.EXIT_CLOSE_KEY).equals(JCmdTools.EXIT_CLOSE_VAL) ) {
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

		JServerLang.PROGRAM_OVERED();
		System.exit(0);
	}
	
	/** remove all/ JBean */
	private void delete() {
		if ( JSTask != null ) {
			JServerLang.START_THREAD_RUNNING();
			return;
		}

		if ( arguments == null ) {
			return;
		}

		String v = arguments.get(JCmdTools.DELETE_KEY);
		if ( v == null ) {
			JServerLang.DELETE_JBEAN_EMPTY_ARGUMENTS();
			return;
		}

		/* remove all the JBean */
		if ( v.equals(JCmdTools.DELETE_ALL_VAL) ) {
			for (JBean b : beanList) {
				b.clear();
			}

			beanList.clear();
			log.debug("all clients cleared");
		} else {
			/* remove the Specified JBean */
			if ( !v.matches("^[0-9]{1,}$") ) {
				JServerLang.DELETE_JBEAN_EMPTY_ARGUMENTS();
				return;
			}

			int index = Integer.parseInt(v);
			if ( index < 0 || index >= beanList.size() ) {
				log.debug("Index out of bounds %d", index);
			} else {
				beanList.remove(index);
				log.debug("client on index %d removed", index);
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

				String num = JCmdTools.formatString(j+"", 2, '0');
				j++;

				// send the ARP to the client
				try {
					b.offer(Packet.ARP);
					System.out.printf("client {index: %s, host: %s}\n", num, b.getHost());
				} catch (IllegalAccessException e) {
					// b.reportClosedError();
					System.out.printf("client {index: %s, host: %s} was removed\n", num, b.getHost());
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
		for (int j = 0; j < args.length; j++) {
			if ("--port".equals(args[j])) {
				if (j < args.length) {
					int p = Integer.parseInt(args[j+1]);
					if (p >= 1024 && p <= 65525) {
						PORT = p;
					}
				} else {
					System.out.println("missing value for --port option");
				}
			} else if ("--log-level".equals(args[j])) {
				if (j < args.length) {
					final String str = args[j+1].toLowerCase();
					if (str.equals("debug")) {
						Log.setLevel(Log.DEBUG);
					} else if (str.equals("info")) {
						Log.setLevel(Log.INFO);
					} else if (str.equals("warn")) {
						Log.setLevel(Log.WARN);
					} else if (str.equals("error")) {
						Log.setLevel(Log.ERROR);
					}
				} else {
					System.out.println("missing value for --log-level option");
				}
			}
		}

		final JServer server = new JServer();
		server.initServer();
		server.StartMonitorThread();
		JCmdTools.showCmdMenu();
		server._CmdLoader();
	}

}