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
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

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
	
	/**
	 * Parses a single XML file.<br>
	 * Validation is enforced.
	 * @param f the XML file to parse.
	 * @return the document with the parsed data.
	 */
	public Document parse(File f)
	{
		if (!xmlFilter.accept(f))
		{
			_log.warning(getClass().getSimpleName() + ": Could not parse " + f.getName() + " is not a file or it doesn't exist!");
			return null;
		}
		
		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		dbf.setValidating(true);
		dbf.setIgnoringComments(true);
		Document doc = null;
		try
		{
			dbf.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
			final DocumentBuilder db = dbf.newDocumentBuilder();
			db.setErrorHandler(new XMLErrorHandler());
			doc = db.parse(f);
		}
		catch (Exception e)
		{
			_log.warning(getClass().getSimpleName() + ": Could not parse " + f.getName() + " file: " + e.getMessage());
			return null;
		}
		return doc;
	}
	
	/**
	 * Wrapper for {@link #loadDirectory(File)}.
	 * @param path the path to the directory where the XML files are.
	 * @return {@code false} if it fails to find the directory, {@code true} otherwise.
	 */
	public boolean loadDirectory(String path)
	{
		return loadDirectory(new File(path));
	}
	
	/**
	 * Loads all XML files from {@code path} and calls {@link #parse(File)} for each one of them.<br>
	 * If the file was successfully parsed, call {@link #parseDoc(Document)} for the parsed document.
	 * @param dir the directory object to scan.
	 * @return {@code false} if it fails to find the directory, {@code true} otherwise.
	 */
	public boolean loadDirectory(File dir)
	{
		if (!dir.exists())
		{
			_log.warning(getClass().getSimpleName() + ": Folder " + dir.getAbsolutePath() + " doesn't exist!");
			return false;
		}
		
		final File[] listOfFiles = dir.listFiles(xmlFilter);
		for (File f : listOfFiles)
		{
			final Document doc = parse(f);
			if (doc != null)
			{
				parseDoc(doc);
			}
		}
		return true;
	}
	
	/**
	 * Abstract method that when implemented will parse a document.<br>
	 * Is expected to be used along with {@link #parse(File)}.
	 * @param doc the document to parse.
	 */
	protected abstract void parseDoc(Document doc);
	
	/**
	 * Simple XML error handler.
	 * @author Zoey76
	 */
	public class XMLErrorHandler implements ErrorHandler
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
