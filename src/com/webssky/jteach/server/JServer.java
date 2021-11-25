package com.webssky.jteach.server;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.webssky.jteach.server.task.JSTaskInterface;
import com.webssky.jteach.util.JCmdTools;
import com.webssky.jteach.util.JServerLang;

/**
 * JTeach Server <br />
 * @author chenxin - chenxin619315@gmail.com <br />
 */
public class JServer {
	public static final Dimension SCREEN_SIZE = Toolkit.getDefaultToolkit().getScreenSize();

	/* Server port */
	public static int PORT = 55535;
	public static ServerSocket server = null;
	public static final String OS = System.getProperty("os.name").toUpperCase();

	/* default number of JBeans for one group */
	private int gOpacity = 7;
	
	public static final int M_RUN = 1;
	// public static final int M_OVER = 0;
	private int STATE = M_RUN;
	
	private volatile JSTaskInterface JSTask = null;
	private final List<JBean> beanList;
	private HashMap<String, String> arguments = null;

	public static ExecutorService threadPool = Executors.newCachedThreadPool();
	private static JServer _instance = null;

	public static JServer getInstance() {
		if ( _instance == null ) {
			_instance = new JServer();
		}
		return _instance;
	}
	
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
	public void resetJSTask() {
		JSTask = null;
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
			}
			/*list all the online JBeans */
			else if ( _input.equals(JCmdTools.LS) ) {
				listBeans();
			}
			
			/*show the function menu of JTeach */
			else if ( _input.equals(JCmdTools.MENU) ) {
				JCmdTools.showCmdMenu();
			}
			/*
			 * stop the current JSTask working thread
			 * and reset the JSTask 
			 */
			else if ( _input.equals(JCmdTools.STOP) ) {
				if ( JSTask == null ) {
					JServerLang.STOP_NULL_THREAD();
				} else {
					JSTask.stopTask();
					JSTask = null;
				}
			} 
			/*remove JBean*/
			else if ( _input.equals(JCmdTools.DELE) ) {
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
			String classname = "com.webssky.jteach.server.task."+cmd.toUpperCase()+"Task";
			JServerLang.TASK_PATH_INFO(classname);
			Class<?> _class = Class.forName(classname);
			Constructor<?> con = _class.getConstructor(JServer.class);
			JSTask = (JSTaskInterface) con.newInstance(this);
			JSTask.startTask();
		} catch (Exception e) {
			//e.printStackTrace();
			JServerLang.RUN_COMMAND_ERROR(cmd);
		}
	}
	
	/** Start Listening Thread */
	public void StartMonitorThread() {
		if (server == null) return;
		threadPool.execute(new ConnectMonitor());
	}
	
	/** Listening Task inner class */
	private class ConnectMonitor implements Runnable {
		@Override
		public void run() {
			while ( getRunState() == M_RUN ) {
				try {
					Socket s = server.accept();
					/*
					 * get a Socket from the Socket Queue
					 * and create new JBean Object to manager it 
					 */
					final JBean bean = new JBean(s);
					beanList.add(bean);

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
	 * if EXIT_CLOSE_KEY is pass. <br />
	 * A symbol will send to all the JBeans to
	 * order them to stop the client program. <br />
	 */
	public void exit() {
		if ( arguments != null 
				&& arguments.get(JCmdTools.EXIT_CLOSE_KEY) != null
				&& arguments.get(JCmdTools.EXIT_CLOSE_KEY).equals(JCmdTools.EXIT_CLOSE_VAL) ) {
			synchronized ( beanList ) {
				for (JBean b : beanList) {
					try {
						b.send(JCmdTools.SEND_CMD_SYMBOL, JCmdTools.SERVER_EXIT_CMD);
					} catch (IOException e) {}
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

		/*remove all the JBean */
		if ( v.equals(JCmdTools.DELETE_ALL_VAL) ) {
			for (JBean b : beanList) {
				try {
					if ( b.getSocket() != null ) b.getSocket().close();
					if ( b.getOutputStream() != null ) b.getOutputStream().close();
				} catch (IOException e) {}
			}

			beanList.clear();
			System.out.println("Clear Ok.");
		}
		/*remove the Specified JBean*/
		else {
			if ( !v.matches("^[0-9]{1,}$") ) {
				JServerLang.DELETE_JBEAN_EMPTY_ARGUMENTS();
				return;
			}

			int index = Integer.parseInt(v);
			if ( index < 0 || index >= beanList.size() ) {
				System.out.println(index+" Index out of bounds");
			} else {
				beanList.remove(index);
				System.out.println("Remove Ok.");
			}
		}
	}
	
	/**
	 * return the arguments HashMap <br />
	 * 
	 * @return HashMap<String, String>
	 */
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

				// check and remove the cleared bean
				if (b.getSocket() == null) {
					it.remove();
					continue;
				}

				String num = JCmdTools.formatString(j+"", 2, '0');
				j++;

				// send the ARP to the client
				try {
					b.getSocket().sendUrgentData(0xff);
					b.send(JCmdTools.SEND_ARP_SYMBOL);
					System.out.println("-+-index:"+num+", "+b+"---+-");
				} catch (IOException e) {
					it.remove();b.clear();
					System.out.println("-+-index:"+num+", "+b+"(gc)---+-");
				}
			}
		}
	}

	/* remove the specified bean */
	public void removeBean(int index) {
		beanList.remove(index);
	}

	/* return the bean size */
	public int beanCount() {
		return beanList.size();
	}

	/* get the bean list */
	public List<JBean> getBeanList() {
		return beanList;
	}

	/* get the specified bean */
	public JBean getBean(int index) {
		return beanList.get(index);
	}
	
	/**
	 * make a copy for an array
	 * @return List
	 */
	public List<JBean> copyBeanList() {
		final List<JBean> list = new ArrayList<>();
		synchronized (beanList) {
			// for (JBean b : beanList) {
			// 	list.add(b);
			// }
			list.addAll(beanList);
		}

		return list;
	}
	
	/** set the default opacity of JBeans for one group */
	public void setGroupOpacity( int opacity ) {
		gOpacity = opacity;
	}
	
	/** get the default opacity for one group */
	public int getGroupOpacity() {
		return gOpacity;
	}
	
	public static void main(String[] args) {
		int opacity = 0;
		if ( args.length > 0 ) {
			opacity = Integer.parseInt(args[0]);
			if ( args.length > 1 ) {
				int p = Integer.parseInt(args[1]);
				if ( p >= 1024 && p <= 65535 ) {
					PORT = p;
				}
			}
		}

		if ( opacity != 0 ) {
			JServer.getInstance().setGroupOpacity(opacity);
		}

		JServer.getInstance().initServer();
		JServer.getInstance().StartMonitorThread();
		JCmdTools.showCmdMenu();
		JServer.getInstance()._CmdLoader();
	}

}
