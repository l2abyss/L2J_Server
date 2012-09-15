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
package com.l2jserver.gameserver.engines;

import java.io.File;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import com.l2jserver.Config;
import com.l2jserver.util.file.filter.XMLFilter;

/**
 * Abstract class for XML parsers.<br>
 * It's in <i>beta</i> state, so it's expected to change over time.
 * @author Zoey76
 */
public abstract class DocumentParser
{
	protected final Logger _log = Logger.getLogger(getClass().getName());
	
	private static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
	private static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
	
	private static final XMLFilter xmlFilter = new XMLFilter();
	
	private File _currentFile;
	
	private Document _currentDocument;
	
	/**
	 * This method can be used to load/reload the data.<br>
	 * It's highly recommended to clear the data storage, either the list or map.
	 */
	public abstract void load();
	
	/**
	 * Wrapper for {@link #parseFile(File)} method.
	 * @param path the relative path to the datapack root of the XML file to parse.
	 */
	protected void parseDatapackFile(String path)
	{
		parseFile(new File(Config.DATAPACK_ROOT, path));
	}
	
	/**
	 * Parses a single XML file.<br>
	 * If the file was successfully parsed, call {@link #parseDocument(Document)} for the parsed document.<br>
	 * <b>Validation is enforced.</b>
	 * @param f the XML file to parse.
	 */
	protected void parseFile(File f)
	{
		if (!xmlFilter.accept(f))
		{
			_log.warning(getClass().getSimpleName() + ": Could not parse " + f.getName() + " is not a file or it doesn't exist!");
			return;
		}
		
		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		dbf.setValidating(true);
		dbf.setIgnoringComments(true);
		_currentDocument = null;
		_currentFile = f;
		try
		{
			dbf.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
			final DocumentBuilder db = dbf.newDocumentBuilder();
			db.setErrorHandler(new XMLErrorHandler());
			_currentDocument = db.parse(f);
		}
		catch (Exception e)
		{
			_log.warning(getClass().getSimpleName() + ": Could not parse " + f.getName() + " file: " + e.getMessage());
			return;
		}
		parseDocument();
	}
	
	/**
	 * Gets the current file.
	 * @return the current file
	 */
	public File getCurrentFile()
	{
		return _currentFile;
	}
	
	/**
	 * Gets the current document.
	 * @return the current document
	 */
	protected Document getCurrentDocument()
	{
		return _currentDocument;
	}
	
	/**
	 * Wrapper for {@link #parseDirectory(File, boolean)}.
	 * @param file the path to the directory where the XML files are.
	 * @return {@code false} if it fails to find the directory, {@code true} otherwise.
	 */
	protected boolean parseDirectory(File file)
	{
		return parseDirectory(file, false);
	}
	
	/**
	 * Wrapper for {@link #parseDirectory(File, boolean)}.
	 * @param path the path to the directory where the XML files are.
	 * @return {@code false} if it fails to find the directory, {@code true} otherwise.
	 */
	protected boolean parseDirectory(String path)
	{
		return parseDirectory(new File(path), false);
	}
	
	/**
	 * Wrapper for {@link #parseDirectory(File, boolean)}.
	 * @param path the path to the directory where the XML files are.
	 * @param recursive parses all sub folders if there is.
	 * @return {@code false} if it fails to find the directory, {@code true} otherwise.
	 */
	protected boolean parseDirectory(String path, boolean recursive)
	{
		return parseDirectory(new File(path), recursive);
	}
	
	/**
	 * Loads all XML files from {@code path} and calls {@link #parseFile(File)} for each one of them.
	 * @param dir the directory object to scan.
	 * @param recursive parses all sub folders if there is.
	 * @return {@code false} if it fails to find the directory, {@code true} otherwise.
	 */
	protected boolean parseDirectory(File dir, boolean recursive)
	{
		if (!dir.exists())
		{
			_log.warning(getClass().getSimpleName() + ": Folder " + dir.getAbsolutePath() + " doesn't exist!");
			return false;
		}
		
		final File[] listOfFiles = dir.listFiles();
		for (File f : listOfFiles)
		{
			if (recursive && f.isDirectory())
			{
				parseDirectory(f, recursive);
			}
			else if (xmlFilter.accept(f))
			{
				parseFile(f);
			}
		}
		return true;
	}
	
	/**
	 * Overridable method that could parse a custom document.<br>
	 * @param doc the document to parse.
	 */
	protected void parseDocument(Document doc)
	{
		// Do nothing, to be overridden in sub-classes.
	}
	
	/**
	 * Abstract method that when implemented will parse the current document.<br>
	 * Is expected to be call from {@link #parseFile(File)}.
	 */
	protected abstract void parseDocument();
	
	/**
	 * Parses the int.
	 * @param n the named node map.
	 * @param name the attribute name.
	 * @return a parsed integer.
	 */
	protected static int parseInt(NamedNodeMap n, String name)
	{
		return Integer.parseInt(n.getNamedItem(name).getNodeValue());
	}
	
	/**
	 * Parses the integer.
	 * @param n the named node map.
	 * @param name the attribute name.
	 * @return a parsed integer object.
	 */
	protected static Integer parseInteger(NamedNodeMap n, String name)
	{
		return Integer.valueOf(n.getNamedItem(name).getNodeValue());
	}
	
	/**
	 * Parses the int.
	 * @param n the node to parse.
	 * @return the parsed integer.
	 */
	protected static int parseInt(Node n)
	{
		return Integer.parseInt(n.getNodeValue());
	}
	
	/**
	 * Parses the integer.
	 * @param n the node to parse.
	 * @return the parsed integer object.
	 */
	protected static Integer parseInteger(Node n)
	{
		return Integer.valueOf(n.getNodeValue());
	}
	
	/**
	 * Parses the long.
	 * @param n the named node map.
	 * @param name the attribute name.
	 * @return a parsed integer.
	 */
	protected static Long parseLong(NamedNodeMap n, String name)
	{
		return Long.valueOf(n.getNamedItem(name).getNodeValue());
	}
	
	/**
	 * Parses the double.
	 * @param n the named node map.
	 * @param name the attribute name.
	 * @return a parsed double.
	 */
	protected static Double parseDouble(NamedNodeMap n, String name)
	{
		return Double.valueOf(n.getNamedItem(name).getNodeValue());
	}
	
	/**
	 * Parses the boolean.
	 * @param n the named node map.
	 * @param name the attribute name.
	 * @return {@code true} if the attribute exists and it's value is {@code true}, {@code false} otherwise.
	 */
	protected static boolean parseBoolean(NamedNodeMap n, String name)
	{
		final Node b = n.getNamedItem(name);
		return (b != null) && Boolean.parseBoolean(b.getNodeValue());
	}
	
	/**
	 * @param n the named node map
	 * @param name the attribute name
	 * @return the node string value for the given node name and named node map if exist, otherwise an empty string
	 */
	protected static String parseString(NamedNodeMap n, String name)
	{
		final Node b = n.getNamedItem(name);
		return (b == null) ? "" : b.getNodeValue();
	}
	
	/**
	 * Simple XML error handler.
	 * @author Zoey76
	 */
	protected class XMLErrorHandler implements ErrorHandler
	{
		@Override
		public void warning(SAXParseException e) throws SAXParseException
		{
			throw e;
		}
		
		@Override
		public void error(SAXParseException e) throws SAXParseException
		{
			throw e;
		}
		
		@Override
		public void fatalError(SAXParseException e) throws SAXParseException
		{
			throw e;
		}
	}
}
