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
package com.l2jserver.dbinstaller;

import java.awt.HeadlessException;
import javax.swing.UIManager;

import com.l2jserver.dbinstaller.console.DBInstallerConsole;
import com.l2jserver.dbinstaller.gui.DBConfigGUI;

/**
 * Contains main class for Database Installer
 * If system doesn't support the graphical UI,
 * start the installer in console mode.
 * 
 * @author mrTJO
 */
public class LauncherCS
{
	public static void main(String[] args)
	{
		String mode = "l2jcs";
		String dir = "../sql/community/";
		String cleanUp = "cs_cleanup.sql";
		
		try
		{
			// Set OS Look And Feel
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e) { }
		
		try
		{
			new DBConfigGUI(mode, dir, cleanUp);
		}
		catch (HeadlessException e)
		{
			new DBInstallerConsole(mode, dir, cleanUp);
		}
	}
}
