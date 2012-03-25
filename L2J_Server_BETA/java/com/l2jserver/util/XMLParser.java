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
package com.l2jserver.util;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

/**
 * @author UnAfraid
 *
 */
public abstract class XMLParser
{
	private static final Logger _log = Logger.getLogger(XMLParser.class.getName());
	
	private final File _file;
	public XMLParser(File f)
	{
		_file = f;
		doParse();
	}
	
	public boolean isIgnoringComments()
	{
		return false;
	}
	
	public boolean isValidating()
	{
		return false;
	}
	
	public void doParse()
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(isValidating());
		factory.setIgnoringComments(isIgnoringComments());
		Document doc = null;
		
		if (getXML().exists())
		{
			try
			{
				doc = factory.newDocumentBuilder().parse(getXML());
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, "Could not parse " + getXML().getName() + " file: " + e.getMessage(), e);
				return;
			}
			
			try
			{
				parseDoc(doc);
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, "Error while parsing doc: " + e.getMessage(), e);
			}
		
		}
		else
		{
			_log.log(Level.WARNING, "Could not found " + getXML().getName() + " file!");
		}
	}
	
	public File getXML()
	{
		return _file;
	}
	
	public abstract void parseDoc(Document doc);
}
