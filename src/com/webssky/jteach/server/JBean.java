package com.webssky.jteach.server;

import com.webssky.jteach.msg.Message;
import com.webssky.jteach.util.JCmdTools;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Client Bean. <br />
 * @author chenxin 
 */
public class JBean {
	
	private Socket socket = null;
	private DataOutputStream out = null;
	private DataInputStream in = null;

	private String name = null;
	private String addr = null;

	private long lastReadAt = 0;

	/* message read/send blocking queue */
	private final BlockingDeque<Message> sendPool;
	private final BlockingDeque<Message> readPool;

	public JBean(Socket s) {
		setSocket(s);
		this.sendPool = new LinkedBlockingDeque(12);
		this.readPool = new LinkedBlockingDeque(12);
	}

	/* Message read thread */
	private class ReadTask implements Runnable {
		@Override
		public void run() {
			int offlineCount = 0;
			while (true) {
				if (socket == null) {
					System.out.printf("client %s stop cuz of empty socket\n");
					break;
				}

				try {
					final char symbol = in.readChar();

					// update the last read at
					lastReadAt = System.currentTimeMillis();
					// do the message receive decode
					switch (symbol) {
						case JCmdTools.SEND_HBT_SYMBOL -> {
							// heartbeat
						}
						case JCmdTools.SEND_ARP_SYMBOL -> {
							// task level heartbeat
						}
						case JCmdTools.SEND_CMD_SYMBOL -> {
							// command message
						}
						case JCmdTools.SEND_DATA_SYMBOL -> {
							// data message
						}
					}
				} catch (SocketTimeoutException e) {
					System.out.printf("client %s read timeout count %d\n", name, offlineCount);
					offlineCount++;
					if (offlineCount > 100) {
						System.out.printf("client %s went offline due to too many read timeout\n", name);
						break;
					}
				} catch (IOException e) {
					System.out.printf("client %s went offline due to read IOException\n", name);
					clear();
					break;
				}
			}
		}
	}

	/* data send thread */
	private class SendTask implements Runnable {
		@Override
		public void run() {
			while (true) {
				try {
					final Message msg = sendPool.take();
					out.write(msg.encode());
					out.flush();
				} catch (InterruptedException e) {
					System.out.printf("client %s send pool take were interrupted\n", name);
				} catch (IOException e) {
					System.out.printf("client %s went offline due to send IOException\n", name);
					clear();
					break;
				}
			}
		}
	}

	public long getLastReadAt() {
		return lastReadAt;
	}

	public long getLastReadFromNow() {
		final long now = System.currentTimeMillis();
		return now - lastReadAt;
	}
	
	public OutputStream getOutputStream() throws IOException {
		return socket.getOutputStream();
	}
	
	/**
	 * return DataInputStream Object 
	 */
	public DataInputStream getReader() {
		return in;
	}
	
	/**
	 * return the bean's host name 
	 */
	public String getName() {
		return name;
	}

	/**
	 * return the bean's IP 
	 */
	public String getIP() {
		return addr;
	}
	
	private void setSocket(Socket s) {
		if (s == null) {
			throw new NullPointerException();
		}

		try {
			socket = s;
			// socket.setTcpNoDelay(true);
			socket.setSoTimeout(JCmdTools.SO_TIMEOUT);
			name = socket.getInetAddress().getHostName();
			addr = socket.getInetAddress().getHostAddress();
			out = new DataOutputStream(socket.getOutputStream());
			in  = new DataInputStream(socket.getInputStream());
		} catch (IOException e) {
			System.out.println("Failed To Create DataOutputStream Object.");
			name = "Unknown";
			addr = "UnKnown";
			clear();
		}
	}
	
	public Socket getSocket() {
		return socket;
	}
	
	public void clear() {
		try {
			if ( in != null ) {
				in.close();
			}

			if ( out != null ) {
				out.close();
			}

			if ( socket != null ) {
				socket.close();
			}

			System.out.println("GC:: ["+getIP()+"] was removed!");
			//JServerLang.INPUT_ASK();
		} catch ( IOException e ) {
			System.out.println("client cleared");
		}

		in = null;
		out = null;
		socket = null;
		readPool.clear();
		sendPool.clear();
	}
	
	/**
	 * just send symbol 
	 * @throws IOException 
	 */
	public synchronized void send(char symbol) throws IOException {
		if ( out != null ) {
			out.writeChar(symbol);
			out.flush();
		}
	}
	
	/**
	 * Send symbol and Command 
	 * @throws IOException 
	 */
	public synchronized void send(char symbol, int cmd) throws IOException {
		if ( out != null ) {
			out.writeChar(symbol);
			out.writeInt(cmd);
			out.flush();
		}
	}
	
	/**
	 * Send symbol and byte[] 
	 * @throws IOException 
	 */
	public synchronized void send(char symbol, byte[] b) throws IOException {
		if ( out != null ) {
			out.writeChar(symbol);
			out.write(b);
			out.flush();
		}
	}
	
	/**
	 * Send symbol and Byte Data 
	 * @throws IOException 
	 */
	public synchronized void send(int symbol, int length,
			byte[] data) throws IOException {
		if ( out != null ) {
			out.writeChar(symbol);
			out.writeInt(length);
			out.write(data);
			out.flush();
		}
	}
	
	/**
	 * send symbol and Mouse position and byte data
	 * @throws IOException 
	 */
	public synchronized void send(int symbol,int x, int y, int length,
			byte[] data) throws IOException {
		if ( out != null ) {
			out.writeChar(symbol);
			out.writeInt(x);
			out.writeInt(y);
			out.writeInt(length);
			out.write(data);
			out.flush();
		}
	}
	
	/**
	 * send String and long
	 * @throws IOException 
	 */
	public synchronized void send(String str, long x) throws IOException {
		if ( out != null ) {
			out.writeUTF(str);
			out.writeLong(x);
			out.flush();
		}
	}
	
	/**
	 * send string 
	 * @throws IOException 
	 */
	public synchronized void send(String str) throws IOException {
		if ( out != null ) {
			out.writeUTF(str);
			out.flush();
		}
	}
	
	/**
	 * send symbol and string 
	 * @throws IOException 
	 */
	public synchronized void send(char symbol, String str) throws IOException {
		if ( out != null ) {
			out.writeChar(symbol);
			out.writeUTF(str);
			out.flush();
		}
	}
	
	/**
	 * send integer 
	 * @throws IOException 
	 */
	public synchronized void send(int x) throws IOException {
		if ( out != null ) {
			out.writeInt(x);
			out.flush();
		}
	}
	
	/**
	 * send byte[] 
	 * @throws IOException 
	 */
	public synchronized void send(byte[] b, int length) throws IOException {
		if ( out != null ) {
			out.write(b, 0, length);
			out.flush();
		}
	}

	public void reportSendError() {
		System.out.printf("failed to send data to client %s\n", this.getIP());
	}

	public String toString() {
		return "IP:"+getIP()+", HOST:"+getName();
	}
}
