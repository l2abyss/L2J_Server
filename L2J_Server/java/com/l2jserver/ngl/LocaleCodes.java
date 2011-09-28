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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 
 * @author mrTJO
 */
public class LocaleCodes
{
	Map<String, Locale> _locales = new HashMap<String, Locale>();
	
	public static LocaleCodes getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private LocaleCodes()
	{
		loadCodes();
	}
	
	private void loadCodes()
	{
		for (Locale locale : Locale.getAvailableLocales())
		{
			String language = locale.getLanguage();
            //String script = locale.getScript();
            String country = locale.getCountry();
            String variant = locale.getVariant();

            if (language == "" && country == "" && variant == "")
            {
                continue;
            }
            
            StringBuilder lang = new StringBuilder();
            lang.append(language);
            if (country != "")
            	lang.append(country);
            if (variant != "")
            	lang.append('_'+variant);
            /*if (script != "")
        		lang.append('_'+script);*/
            _locales.put(lang.toString(), locale);
		}
	}
	
	public Locale getLanguage(String lang)
	{
		return _locales.get(lang);
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final LocaleCodes _instance = new LocaleCodes();
	}
}
