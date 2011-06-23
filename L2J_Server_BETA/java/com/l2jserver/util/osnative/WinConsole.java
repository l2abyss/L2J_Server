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
package com.l2jserver.util.osnative;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

/**
 * 
 * @author mrTJO
 */
public interface WinConsole extends StdCallLibrary
{
	WinConsole INSTANCE = (WinConsole)Native.loadLibrary("kernel32", WinConsole.class, 
    		W32APIOptions.UNICODE_OPTIONS);
	
	public boolean SetConsoleOutputCP(int codePage);
	
	public int GetConsoleOutputCP();
	
	public Pointer GetStdHandle(int stream);
	
	public boolean WriteConsoleW(Pointer stream, char[] text, int textLen, 
			IntByReference caretPosition, Pointer reservedNull);
}
