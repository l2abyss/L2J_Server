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

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * 
 * @author mrTJO
 */
public class LocalizationParser
{
	private String LANGUAGES_DIRECTORY = "../languages/";
	private Map<String, String> _msgMap = new HashMap<String, String>();
	private final static Logger _log = Logger.getLogger(LocalizationParser.class.getName());
	private String _baseName;
	
	public LocalizationParser(String dir, String baseName, Locale locale)
	{
		LANGUAGES_DIRECTORY += dir+"/";
		_baseName = baseName;
		
		String language = locale.getLanguage();
        //String script = locale.getScript();
        String country = locale.getCountry();
        String variant = locale.getVariant();
        
		StringBuilder sb = new StringBuilder();
        sb.append(language);
        if (country != "")
        	sb.append(country);
        if (variant != "")
        	sb.append('_'+variant);
        // Java 7 Function 
        /*if (script != "")
    		sb.append('_'+script);*/
        
		File xml = getTranslationFile(sb.toString());
        parseXml(xml);
	}
	
	public LocalizationParser(String dir, String baseName, String locale)
	{
		LANGUAGES_DIRECTORY += dir+"/";
		_baseName = baseName;
		File xml = getTranslationFile(locale);
        parseXml(xml);
	}
	
	/**
	 * Parse translation xml
	 * 
	 * @param xml
	 */
	private void parseXml(File xml)
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringComments(true);
		Document doc = null;
        
		if (xml.exists())
		{
			try
			{
				doc = factory.newDocumentBuilder().parse(xml);
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, "Could not load localization file");
			}
			
			Node n = doc.getFirstChild();
			NamedNodeMap docAttr = n.getAttributes();
			if (docAttr.getNamedItem("extends") != null)
			{
				String baseLang = docAttr.getNamedItem("extends").getNodeValue();
				parseXml(getTranslationFile(baseLang));
			}
			for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
			{
				if (d.getNodeName().equals("message"))
				{
					NamedNodeMap attrs = d.getAttributes();
					String id = attrs.getNamedItem("id").getNodeValue();
					String text = attrs.getNamedItem("text").getNodeValue();
					_msgMap.put(id, text);
				}
			}
		}
	}
	
	/**
	 * Search the translation file
	 * 
	 * @param language
	 * @return
	 */
	private File getTranslationFile(String language)
	{
		File xml = null;
		if (language.length() > 0)
			xml = new File(LANGUAGES_DIRECTORY+_baseName+'_'+language+".xml");
		
		if (language.length() > 2 && (xml == null || !xml.exists()))
			xml = new File(LANGUAGES_DIRECTORY+_baseName+'_'+language.substring(0, 2)+".xml");
		
		if (xml == null || !xml.exists())
        	xml = new File(LANGUAGES_DIRECTORY+_baseName+".xml");
		return xml;
	}
	
	/**
	 * Return string from specified id
	 * 
	 * @param id
	 * @return
	 */
	protected String getStringFromId(String id)
	{
		return _msgMap.get(id);
	}
}
