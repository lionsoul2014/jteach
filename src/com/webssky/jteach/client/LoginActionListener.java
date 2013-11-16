package com.webssky.jteach.client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * keyListener class for Login. <br />
 * 
 * @author chenxin - chenxin619315@gmail.com <br />
 * {@link http://www.webssky.com}
 */
public class LoginActionListener implements ActionListener {
	
	private static LoginActionListener _instnace = null;
	
	public static LoginActionListener getInstance() {
		if ( _instnace == null ) _instnace = new LoginActionListener();
		return _instnace;
	}
	
	public LoginActionListener() {}

	@Override
	public void actionPerformed(ActionEvent e) {
		JClient.getInstance().getConnectionJButton().doClick();
	}

}
