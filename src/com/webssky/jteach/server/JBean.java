package com.webssky.jteach.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Client Bean. <br />
 * @author chenxin 
 */
public class JBean {
	
	private volatile Socket socket = null;
	private String name = null;
	private DataOutputStream out = null;
	private DataInputStream in = null;
	
	public JBean(Socket s) {
		setSocket(s);
	}
	
	public JBean(Socket s, String name) {
		setSocket(s);
		this.name = name;
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
		if ( name != null ) {
			return name;
		}

		if ( socket == null ) {
			return "Unknow Bean";
		}

		return socket.getInetAddress().getHostName();
	}
	
	/**
	 * return the bean's IP 
	 */
	public String getIP() {
		if ( socket == null ) {
			return "Unkown Bean";
		}

		return socket.getInetAddress().getHostAddress();
	}
	
	private void setSocket(Socket s) {
		if (s == null) {
			throw new NullPointerException();
		}

		try {
			socket = s;
			//socket.setTcpNoDelay(true);
			// socket.setSoTimeout(3 * 1000);
			out = new DataOutputStream(socket.getOutputStream());
			in  = new DataInputStream(socket.getInputStream());
		} catch (IOException e) {
			System.out.println("Failed To Create DataOutputStream Object.");
			clear();
		}
	}
	
	public Socket getSocket() {
		return socket;
	}
	
	public void clear() {
		try {
			if ( in != null ) in.close();
			if ( out != null ) out.close();
			if ( socket != null ) socket.close();
			System.out.println("GC:: ["+getIP()+"] was removed!");
			//JServerLang.INPUT_ASK();
		} catch ( IOException e ) {}
	}
	
	/**
	 * just send symbol 
	 * @throws IOException 
	 */
	public void send(char symbol) throws IOException {
		if ( out != null ) {
			out.writeChar(symbol);
			out.flush();
		}
	}
	
	/**
	 * Send symbol and Command 
	 * @throws IOException 
	 */
	public void send(char symbol, int cmd) throws IOException {
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
	public void send(char symbol, byte[] b) throws IOException {
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
	public void send(int symbol, int length,
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
	public void send(int symbol,int x, int y, int length,
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
	public void send(String str, long x) throws IOException {
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
	public void send(String str) throws IOException {
		if ( out != null ) {
			out.writeUTF(str);
			out.flush();
		}
	}
	
	/**
	 * send symbol and string 
	 * @throws IOException 
	 */
	public void send(char symbol, String str) throws IOException {
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
	public void send(int x) throws IOException {
		if ( out != null ) {
			out.writeInt(x);
			out.flush();
		}
	}
	
	/**
	 * send byte[] 
	 * @throws IOException 
	 */
	public void send(byte[] b, int start, int length) throws IOException {
		if ( out != null ) {
			out.write(b, 0, length);
			out.flush();
		}
	}

	public void reportSendError() {
		System.err.printf("failed to send data to client %s[%s]", this.getName(), this.getIP());
	}

	public String toString() {
		return "IP:"+getIP()+", HOST:"+getName();
	}
}
