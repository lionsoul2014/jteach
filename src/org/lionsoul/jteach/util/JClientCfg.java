package org.lionsoul.jteach.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

/**
 * configuration for JClient
 * @author chenxin - chenxin619315@gmail.com
 */
public class JClientCfg {
	
	public static final String W_TITLE = "JTeach Client";
	public static final Dimension W_SIZE = new Dimension(350, 200);
	public static final Color TIP_BG_COLOR = Color.BLACK;
	public static final Color TIP_FRON_COLOR = Color.WHITE;
	public static final String TIP_INIT_TEXT = "JTeach> Ready To Connect To Server.";
	
	public static final Font LABLE_FONT = new Font("Arial", Font.PLAIN, 14);
	public static final Color LABEL_FRONT_COLOR = Color.BLACK;
	
	public static final String SERVER_LABLE_TEXT = "Server IP: ";
	public static final String PORT_LABLE_TEXT = "Port: ";
	public static final String CONNECT_BUTTON_TEXT = "Connect";
	public static final String ABOUT_BUTTON_TEXT = "About";
	
	public static final String ABOUT_INFO = "JTeach - Multimedia Teaching Software\n" +
			"@author: chenxin - webssky\n"+
			"{@email: chenxin619315@gmail.com}\n" +
			"{@link: http://www.lionsoul.org}";
	
	/**
	 * message from user when he is trying to exit program 
	 */
	public static final String EXIT_ONLINE_TEXT = "You are online, Are you sure to exit?";
	
}
