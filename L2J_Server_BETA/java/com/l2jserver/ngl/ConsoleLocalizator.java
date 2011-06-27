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
package com.l2jserver.ngl;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Formatter;
import java.util.Locale;
import java.util.Scanner;

import com.l2jserver.util.osnative.CodePage;
import com.l2jserver.util.osnative.WinConsole;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

/**
 * 
 * @author mrTJO
 */
public class ConsoleLocalizator extends LocalizationParser
{
	private WinConsole _wcon;
	private Pointer _stdout;
	private static PrintStream _out;
	
	Scanner _scn = new Scanner(System.in);
	String _baseName = "NGLConsole";
	
	/**
	 * Load ConsoleLocalizator by using Default Locale
	 * 
	 * @param dir
	 * @param baseName
	 */
	public ConsoleLocalizator(String dir, String baseName)
	{
		this(dir, baseName, Locale.getDefault());
	}
	
	/**
	 * Load ConsoleLocalizator by using a specified Locale
	 * 
	 * @param dir
	 * @param baseName
	 * @param locale
	 */
	public ConsoleLocalizator(String dir, String baseName, Locale locale)
	{
		super(dir, baseName, locale);
		loadConsole();
	}
	
	/**
	 * Load ConsoleLocalizator by using a custom xml file
	 * ../languages/<dir>/<baseName>_<locale>.xml
	 * 
	 * @param dir
	 * @param baseName
	 * @param locale
	 */
	public ConsoleLocalizator(String dir, String baseName, String locale)
	{
		super(dir, baseName, locale);
		loadConsole();
	}
	
	/**
	 * Choose the appropriate output stream for console
	 */
	private void loadConsole()
	{
		if (Platform.isWindows())
		{
			try
			{
				_wcon = WinConsole.INSTANCE;
				
				if (_wcon.GetConsoleOutputCP() != 0)
				{
					// Set Console Output to UTF8
					_wcon.SetConsoleOutputCP(CodePage.CP_UTF8);
					
					// Set Output to STDOUT
					_stdout = _wcon.GetStdHandle(-11);
				}
				else
				{
					// Not running from windows console
					_wcon = null;
				}
			}
			catch (Exception e)
			{
				// Missing function in Kernel32
				_wcon = null;
			}
		}
		
		if (_wcon == null) // Not running windows console
		{
			try
			{
				// UTF-8 Print Stream
				_out = new PrintStream(System.out, true, "UTF-8");
			}
			catch (UnsupportedEncodingException e)
			{
				// UTF-8 Not Supported
				_out = new PrintStream(System.out, true);
				directPrint("Your system doesn't support UTF-8 encoding\n");
			}
		}
	}
	
	/**
	 * Get string from translation, add arguments
	 * and write it to console.
	 * 
	 * @param id
	 * @param args
	 */
	public void print(String id, Object... args)
	{
		String msg = getStringFromId(id);
		if (msg == null)
			msg = formatText("Untranslated id: %s", id);
		else
			msg = formatText(msg, args);
		directPrint(msg);
	}
	
	/**
	 * Write a new line
	 */
	public void println()
	{
		directPrint("\n");
	}
	
	/**
	 * Get string from translation, add arguments
	 * and write it to console with a newline at the
	 * end of string.
	 * 
	 * @param id
	 * @param args
	 */
	public void println(String id, Object... args)
	{
		String msg = getStringFromId(id);
		if (msg == null)
			msg = formatText("Untranslated id: %s\n", id);
		else
			msg = formatText(msg+"\n", args);
		directPrint(msg);
	}
	
	/**
	 * Get string from translation, add arguments
	 * and write it to console.
	 * Wait for an input and return in form of string.
	 * 
	 * @param id
	 * @param args
	 * @return Input String
	 */
	public String inputString(String id, Object... args)
	{
		print(id, args);
		directPrint(": ");
		String ret = _scn.next();
		return ret;
	}
	
	/**
	 * Read string from translation file and append
	 * arguments.
	 * 
	 * @param id
	 * @param args
	 * @return
	 */
	public String getString(String id, Object... args)
	{
		String msg = getStringFromId(id);
		if (msg == null)
			return formatText("Untranslated id: %s", id);
		else
			return formatText(msg, args);
	}
	
	/**
	 * Append arguments to specified string.
	 * 
	 * @param text
	 * @param args
	 * @return
	 */
	private String formatText(String text, Object... args)
	{
		Formatter form = new Formatter();
		return form.format(text, args).toString();
	}
	
	/**
	 * Write the text into console by using UTF-8
	 * PrintStream under UNIX environment, and 
	 * Kernel32.dll under Windows.
	 * 
	 * @param message
	 */
	private void directPrint(String message)
	{
		if (_wcon == null)
			_out.print(message);
		else
			_wcon.WriteConsoleW(_stdout, message.toCharArray(),
					message.length(), new IntByReference(), null);
	}
}
