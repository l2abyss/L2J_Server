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
	private static WinConsole _wcon = Platform.isWindows() ? WinConsole.INSTANCE : null;
	private static Pointer _stdout = _wcon != null ? _wcon.GetStdHandle(-11) : null;
	private static PrintStream _out;
	
	Scanner _scn = new Scanner(System.in);
	String _baseName = "NGLConsole";
	
	public ConsoleLocalizator(String dir, String baseName)
	{
		this(dir, baseName, Locale.getDefault());
	}
	
	public ConsoleLocalizator(String dir, String baseName, Locale locale)
	{
		super(dir, baseName, locale);
	}
	
	public ConsoleLocalizator(String dir, String baseName, String locale)
	{
		super(dir, baseName, locale);
	}
	
	public void print(String id, Object... args)
	{
		String msg = getStringFromId(id);
		if (msg == null)
			msg = formatText("Untranslated id: %s", id);
		else
			msg = formatText(msg, args);
		directPrint(msg);
	}
	
	public void println()
	{
		directPrint("\n");
	}
	
	public void println(String id, Object... args)
	{
		String msg = getStringFromId(id);
		if (msg == null)
			msg = formatText("Untranslated id: %s\n", id);
		else
			msg = formatText(msg+"\n", args);
		directPrint(msg);
	}
	
	public String inputString(String id, Object... args)
	{
		print(id, args);
		directPrint(": ");
		String ret = _scn.next();
		return ret;
	}
	
	public String getString(String id, Object... args)
	{
		String msg = getStringFromId(id);
		if (msg == null)
			return formatText("Untranslated id: %s", id);
		else
			return formatText(msg, args);
	}
	
	private String formatText(String text, Object... args)
	{
		Formatter form = new Formatter();
		return form.format(text, args).toString();
	}
	
	private static void directPrint(String message)
	{
		if (_wcon == null)
			_out.print(message);
		else
			_wcon.WriteConsoleW(_stdout, message.toCharArray(),
					message.length(), new IntByReference(), null);
	}
	
	static
	{
		if (_wcon != null && _wcon.GetConsoleOutputCP() != 0)
		{
			_wcon.SetConsoleOutputCP(CodePage.CP_UTF8);
		}
		else
		{
			try
			{
				_out = _wcon == null ? new PrintStream(System.out, true, "UTF-8") : null;
			}
			catch (UnsupportedEncodingException e)
			{
				_out = _wcon == null ? new PrintStream(System.out, true) : null;
				directPrint("Unsupported Encoding\n");
			}
		}
	}
}
