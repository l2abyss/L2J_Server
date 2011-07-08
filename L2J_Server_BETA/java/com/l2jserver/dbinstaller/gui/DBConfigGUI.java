/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver.dbinstaller.gui;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

import com.l2jserver.dbinstaller.RunTasks;
import com.l2jserver.dbinstaller.util.mysql.MySqlConnect;
import com.l2jserver.dbinstaller.util.swing.SpringUtilities;
import com.l2jserver.images.ImagesTable;

/**
 * 
 * @author mrTJO
 */
public class DBConfigGUI extends JFrame
{
	private static final long serialVersionUID = -8391792251140797076L;
	
	JTextField _dbHost;
	JTextField _dbPort;
	JTextField _dbUser;
	JPasswordField _dbPass;
	JTextField _dbDbse;
	
	String _db;
	String _dir;
	String _cleanUp;
	
	Preferences _prop;
	
	public DBConfigGUI(String db, String dir, String cleanUp)
	{
		super("L2J Database Installer");
		setLayout(new SpringLayout());
		setDefaultLookAndFeelDecorated(true);
		setIconImage(ImagesTable.getImage("l2j.png").getImage());
		
		_db = db;
		_dir = dir;
		_cleanUp = cleanUp;
		
		int width = 260;
		int height = 220;
		Dimension resolution = Toolkit.getDefaultToolkit().getScreenSize();
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds((resolution.width-width)/2, (resolution.height-height)/2, width, height);
		setResizable(false);
		
		_prop = Preferences.userRoot();
		
		// Host
		JLabel labelDbHost = new JLabel("Host: ", JLabel.LEFT);
		add(labelDbHost);
		_dbHost = new JTextField(15);
		_dbHost.setText(_prop.get("dbHost_"+db, "localhost"));
		labelDbHost.setLabelFor(_dbHost);
		add(_dbHost);
		
		// Port
		JLabel labelDbPort = new JLabel("Port: ", JLabel.LEFT);
		add(labelDbPort);
		_dbPort = new JTextField(15);
		_dbPort.setText(_prop.get("dbPort_"+db, "3306"));
		labelDbPort.setLabelFor(_dbPort);
		add(_dbPort);
		
		// Username
		JLabel labelDbUser = new JLabel("Username: ", JLabel.LEFT);
		add(labelDbUser);
		_dbUser = new JTextField(15);
		_dbUser.setText(_prop.get("dbUser_"+db, "root"));
		labelDbUser.setLabelFor(_dbUser);
		add(_dbUser);
		
		// Password
		JLabel labelDbPass = new JLabel("Password: ", JLabel.LEFT);
		add(labelDbPass);
		_dbPass = new JPasswordField(15);
		_dbPass.setText(_prop.get("dbPass_"+db, ""));
		labelDbPass.setLabelFor(_dbPass);
		add(_dbPass);
		
		// Database
		JLabel labelDbDbse = new JLabel("Database: ", JLabel.LEFT);
		add(labelDbDbse);
		_dbDbse = new JTextField(15);
		_dbDbse.setText(_prop.get("dbDbse_"+db, db));
		labelDbDbse.setLabelFor(_dbDbse);
		add(_dbDbse);
		
		ActionListener cancelListener = new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				System.exit(0);
			}
		};
		
		// Cancel
		JButton btnCancel = new JButton("Cancel");
		btnCancel.addActionListener(cancelListener);
		add(btnCancel);
		
		ActionListener connectListener = new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				MySqlConnect connector = new MySqlConnect(_dbHost.getText(), _dbPort.getText(),
						_dbUser.getText(), new String(_dbPass.getPassword()), _dbDbse.getText(), false);
				
				if (connector.getConnection() != null)
				{
					_prop.put("dbHost_"+_db, _dbHost.getText());
					_prop.put("dbPort_"+_db, _dbPort.getText());
					_prop.put("dbUser_"+_db, _dbUser.getText());
					_prop.put("dbDbse_"+_db, _dbDbse.getText());
					
					boolean cleanInstall = false;
					DBInstallerGUI dbi = new DBInstallerGUI(connector.getConnection());
					setVisible(false);
					
					Object[] options = { "Full Install", "Upgrade", "Exit" };
					int n = JOptionPane.showOptionDialog(null, "Select Installation Type",
							"Installation Type", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
							null, options, options[1]);
					
					if (n == 2 || n == -1)
						System.exit(0);
					
					if (n == 0)
					{
						int conf = JOptionPane.showConfirmDialog(null, "Do you really want to destroy your db?",
								"Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
						
						if (conf == 1)
							System.exit(0);
						
						cleanInstall = true;
					}
					
					dbi.setVisible(true);
					
					RunTasks task = new RunTasks(dbi, _db, _dir, _cleanUp, cleanInstall);
					task.setPriority(Thread.MAX_PRIORITY);
					task.start();
				}
			}
		};
		
		// Connect
		JButton btnConnect = new JButton("Connect");
		btnConnect.addActionListener(connectListener);
		add(btnConnect);
		
		SpringUtilities.makeCompactGrid(this.getContentPane(), 6, 2, 5, 5, 5, 5);
		
		setVisible(true);
	}
}
