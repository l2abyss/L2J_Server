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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.sql.Connection;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.l2jserver.dbinstaller.DBOutputInterface;
import com.l2jserver.images.ImagesTable;

/**
 * 
 * @author mrTJO
 */
public class DBInstallerGUI extends JFrame implements DBOutputInterface
{
	private static final long serialVersionUID = -1005504757826370170L;
	
	private JProgressBar _progBar;
	private JTextArea _progArea;
	private Connection _con;
	
	public DBInstallerGUI(Connection con)
	{
		super("L2J Database Installer");
		setLayout(new BorderLayout());
		setDefaultLookAndFeelDecorated(true);
		setIconImage(ImagesTable.getImage("l2j.png").getImage());
		
		
		_con = con;
		
		int width = 480;
		int height = 360;
		Dimension resolution = Toolkit.getDefaultToolkit().getScreenSize();
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds((resolution.width-width)/2, (resolution.height-height)/2, width, height);
		setResizable(false);
		
		_progBar = new JProgressBar();
		_progBar.setIndeterminate(true);
		add(_progBar, BorderLayout.PAGE_START);
		
		_progArea = new JTextArea();
		JScrollPane scrollPane = new JScrollPane(_progArea);
		
		_progArea.setEditable(false);
		appendToProgressArea("Connected");
		
		add(scrollPane, BorderLayout.CENTER);
	}
	
	public void setProgressIndeterminate(boolean value)
	{
		_progBar.setIndeterminate(value);
	}
	
	public void setProgressMaximum(int maxValue)
	{
		_progBar.setMaximum(maxValue);
	}
	
	public void setProgressValue(int value)
	{
		_progBar.setValue(value);
	}
	
	public void appendToProgressArea(String text)
	{
		_progArea.append(text+"\n");
		_progArea.setCaretPosition(_progArea.getDocument().getLength());
	}
	
	public Connection getConnection()
	{
		return _con;
	}

	@Override
	public void setFrameVisible(boolean value)
	{
		setVisible(value);
	}

	@Override
	public int requestConfirm(String title, String message, int type)
	{
		return JOptionPane.showConfirmDialog(null, message, title, type);
	}

	@Override
	public void showMessage(String title, String message, int type) {
		JOptionPane.showMessageDialog(null, message, title, type);
	}
}
