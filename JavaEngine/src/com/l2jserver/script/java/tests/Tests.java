package com.l2jserver.script.java.tests;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class Tests
{
	public static void main(String args[]) throws ScriptException, IOException
	{
		ScriptEngineManager sem = new ScriptEngineManager();
		ScriptEngine se = sem.getEngineByExtension("java");
		se.getContext().setAttribute("classpath", "C:/TestCode", 100);
		se.getContext().setAttribute("mainClass", "Test", 100);
		se.getContext().setAttribute("javax.script.filename", "Test.java", 100);
		
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("TestCode/Test.java")));
		se.eval(br);
		System.err.flush();
		System.out.flush();
		System.err.println("PRESS ANY KEY AFTER EDITING FILE");
		System.in.read();
		br = new BufferedReader(new InputStreamReader(new FileInputStream("TestCode/Test.java")));
		se.eval(br);
	}
}
