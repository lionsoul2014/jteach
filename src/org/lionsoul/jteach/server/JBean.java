package org.lionsoul.jteach.server;

import org.lionsoul.jteach.msg.Message;
import org.lionsoul.jteach.util.JCmdTools;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Client Bean. <br />
 * @author chenxin 
 */
public class JBean {

	private final Socket socket;
	private volatile boolean closed;
	private final DataOutputStream output;
	private final DataInputStream input;

	private String name;
	private String addr;

	private long lastReadAt = 0;

	/* message read/send blocking queue */
	private final BlockingDeque<Message> sendPool;
	private final BlockingDeque<Message> readPool;

	public JBean(Socket s) throws IOException {
		if (s == null) {
			throw new NullPointerException();
		}

		this.socket = s;
		// this.socket.setTcpNoDelay(true);
		this.socket.setSoTimeout(JCmdTools.SO_TIMEOUT);
		this.name = socket.getInetAddress().getHostName();
		this.addr = socket.getInetAddress().getHostAddress();
		this.output = new DataOutputStream(socket.getOutputStream());
		this.input  = new DataInputStream(socket.getInputStream());

		/* create the message pool */
		this.sendPool = new LinkedBlockingDeque(12);
		this.readPool = new LinkedBlockingDeque(12);
	}

	public void start() {
		JServer.threadPool.execute(new ReadTask());
		JServer.threadPool.execute(new SendTask());
	}

	public void stop() {
		clear();
		sendPool.notify();
	}

	public long getLastReadAt() {
		return lastReadAt;
	}

	public long getLastReadFromNow() {
		final long now = System.currentTimeMillis();
		return now - lastReadAt;
	}

	public boolean isClosed() {
		return closed;
	}

	public String getName() {
		return name;
	}

	public String getAddr() {
		return addr;
	}

	public Socket getSocket() {
		return socket;
	}

	/** send a message immediately */
	public void send(Message msg) throws IOException, IllegalAccessException {
		if (isClosed()) {
			throw new IllegalAccessException("socket closed exception");
		}

		synchronized (output) {
			output.write(msg.encode());
			output.flush();
		}
	}

	/** wait until the message push to the queue */
	public void put(Message msg) throws IllegalAccessException {
		if (isClosed()) {
			throw new IllegalAccessException("socket closed exception");
		}

		try {
			sendPool.put(msg);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/** offer or throw an exception for the message */
	public boolean offer(Message msg) throws IllegalAccessException {
		if (isClosed()) {
			throw new IllegalAccessException("socket closed exception");
		}

		return sendPool.offer(msg);
	}

	/** read the first message */
	public Message poll() throws IllegalAccessException {
		if (isClosed()) {
			throw new IllegalAccessException("socket closed exception");
		}

		return readPool.poll();
	}

	/** take or wait for the first message */
	public Message take() throws InterruptedException, IllegalAccessException {
		if (isClosed()) {
			throw new IllegalAccessException("socket closed exception");
		}

		return readPool.take();
	}

	/** Message read thread */
	private class ReadTask implements Runnable {
		@Override
		public void run() {
			int offlineCount = 0;
			while (true) {
				if (closed) {
					System.out.printf("client %s read thread closed\n");
					break;
				}

				try {
					final char symbol = input.readChar();

					// update the last read at
					lastReadAt = System.currentTimeMillis();
					// do the message receive decode
					switch (symbol) {
					case JCmdTools.SEND_HBT_SYMBOL:
						// heartbeat
						break;
					case JCmdTools.SEND_ARP_SYMBOL:
						// task level heartbeat
						break;
					case JCmdTools.SEND_CMD_SYMBOL:
						// command message
						break;
					case JCmdTools.SEND_DATA_SYMBOL:
						// data message
						break;
					}
				} catch (SocketTimeoutException e) {
					System.out.printf("client %s read timeout count %d\n", name, offlineCount);
					offlineCount++;
					if (offlineCount > 30) {
						System.out.printf("client %s went offline due to too many read timeout\n", name);
						clear();
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

	/** data send thread */
	private class SendTask implements Runnable {
		@Override
		public void run() {
			while (true) {
				if (closed) {
					System.out.printf("client %s send thread closed\n");
					break;
				}

				try {
					final Message msg = sendPool.take();
					/* lock the socket and send the message data */
					synchronized (output) {
						output.write(msg.encode());
						output.flush();
					}
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

	public void clear() {
		try {
			input.close();
			output.close();
			socket.close();
		} catch (IOException e) {}

		closed = true;
		readPool.clear();
		sendPool.clear();
	}
	
	/**
	 * just send symbol 
	 * @throws IOException 
	 */
	public synchronized void send(char symbol) throws IOException {
		if ( output != null ) {
			output.writeChar(symbol);
			output.flush();
		}
	}
	
	/**
	 * Send symbol and Command 
	 * @throws IOException 
	 */
	public synchronized void send(char symbol, int cmd) throws IOException {
		if ( output != null ) {
			output.writeChar(symbol);
			output.writeInt(cmd);
			output.flush();
		}
	}
	
	/**
	 * Send symbol and byte[] 
	 * @throws IOException 
	 */
	public synchronized void send(char symbol, byte[] b) throws IOException {
		if ( output != null ) {
			output.writeChar(symbol);
			output.write(b);
			output.flush();
		}
	}
	
	/**
	 * Send symbol and Byte Data 
	 * @throws IOException 
	 */
	public synchronized void send(int symbol, int length,
			byte[] data) throws IOException {
		if ( output != null ) {
			output.writeChar(symbol);
			output.writeInt(length);
			output.write(data);
			output.flush();
		}
	}
	
	/**
	 * send symbol and Mouse position and byte data
	 * @throws IOException 
	 */
	public synchronized void send(int symbol,int x, int y, int length,
			byte[] data) throws IOException {
		if ( output != null ) {
			output.writeChar(symbol);
			output.writeInt(x);
			output.writeInt(y);
			output.writeInt(length);
			output.write(data);
			output.flush();
		}
	}
	
	/**
	 * send String and long
	 * @throws IOException 
	 */
	public synchronized void send(String str, long x) throws IOException {
		if ( output != null ) {
			output.writeUTF(str);
			output.writeLong(x);
			output.flush();
		}
	}
	
	/**
	 * send string 
	 * @throws IOException 
	 */
	public synchronized void send(String str) throws IOException {
		if ( output != null ) {
			output.writeUTF(str);
			output.flush();
		}
	}
	
	/**
	 * send symbol and string 
	 * @throws IOException 
	 */
	public synchronized void send(char symbol, String str) throws IOException {
		if ( output != null ) {
			output.writeChar(symbol);
			output.writeUTF(str);
			output.flush();
		}
	}
	
	/**
	 * send integer 
	 * @throws IOException 
	 */
	public synchronized void send(int x) throws IOException {
		if ( output != null ) {
			output.writeInt(x);
			output.flush();
		}
	}
	
	/**
	 * send byte[] 
	 * @throws IOException 
	 */
	public synchronized void send(byte[] b, int length) throws IOException {
		if ( output != null ) {
			output.write(b, 0, length);
			output.flush();
		}
	}

	public void reportClosedError() {
		System.out.printf("client %s closed\n", name);
	}

	public String toString() {
		return "IP:" + addr + ", HOST:" + name;
	}

}
