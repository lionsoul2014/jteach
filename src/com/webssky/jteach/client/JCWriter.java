package com.webssky.jteach.client;

import java.io.DataOutputStream;
import java.io.IOException;

import javax.swing.JOptionPane;

/**
 * write method collection for JClient. <br />
 * 
 * @author chenxin - chenxin619315@gmail.com <br />
 */
public class JCWriter {
	
	private DataOutputStream out = null;
	
	public JCWriter() {
		try {
			out = JClient.getInstance().getWriter();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null,
					"Fail To Get InputStream From Socket",
					"JTeach:", JOptionPane.ERROR_MESSAGE);
			JClient.getInstance().offLineClear();
		}
	}
	
	/**
	 * simply send symbol. <br />
	 * @throws IOException 
	 */
	public void send(char symbol) throws IOException {
		//if ( out == null ) return;
		out.writeChar(symbol);
		out.flush();
	}
	
	/**
	 * send string. <br />
	 * 
	 * @throws IOException 
	 */
	public void send( String str ) throws IOException {
		//if ( out == null ) return;
		out.writeUTF(str);
		out.flush();
	}
	
	/**
	 * send symbol and mouse info and byte data. <br />
	 * @throws IOException 
	 */
	public void send(char symbol, int x, int y, int length, byte[] b) throws IOException {
		//if ( out == null ) return;
		out.writeChar(symbol);
		out.writeInt(x);
		out.writeInt(y);
		out.writeInt(length);
		out.write(b);
		out.flush();
	}
	
	/**
	 * send symbol and a heart beat data. <br />
	 * @throws IOException  
	 */
	public void send( char symbol, int x ) throws IOException {
		//if ( out == null ) return; 
		out.writeChar(symbol);
		out.writeInt(x);
		out.flush();
	}
}
