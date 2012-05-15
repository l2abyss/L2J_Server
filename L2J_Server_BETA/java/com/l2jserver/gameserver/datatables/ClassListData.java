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
package com.l2jserver.gameserver.datatables;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2jserver.gameserver.engines.DocumentParser;
import com.l2jserver.gameserver.model.base.ClassId;
import com.l2jserver.gameserver.model.base.ClassInfo;

/**
 * This class holds the list of classes and it's info.<br>
 * It's in <i>beta</i> state, so it's expected to change over time.
 * @author Zoey76
 */
public final class ClassListData extends DocumentParser
{
	private static final Map<ClassId, ClassInfo> _classData = new HashMap<>();
	
	/**
	 * Instantiates a new class list data.
	 */
	protected ClassListData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_classData.clear();
		parseDatapackFile("data/stats/chars/classList.xml");
		_log.info(getClass().getSimpleName() + ": Loaded " + _classData.size() + " Class data.");
	}
	
	@Override
	protected void parseDocument()
	{
		NamedNodeMap attrs;
		Node attr;
		ClassId classId;
		String className;
		String classServName;
		ClassId parentClassId;
		for (Node n = getCurrentDocument().getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equals(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					attrs = d.getAttributes();
					if ("class".equals(d.getNodeName()))
					{
						attr = attrs.getNamedItem("classId");
						classId = ClassId.getClassId(parseInt(attr));
						attr = attrs.getNamedItem("name");
						className = attr.getNodeValue();
						attr = attrs.getNamedItem("serverName");
						classServName = attr.getNodeValue();
						attr = attrs.getNamedItem("parentClassId");
						parentClassId = (attr != null) ? ClassId.getClassId(parseInt(attr)) : null;
						_classData.put(classId, new ClassInfo(classId, className, classServName, parentClassId));
					}
				}
			}
		}
	}
	
	/**
	 * Gets the class list.
	 * @return the complete class list.
	 */
	public Map<ClassId, ClassInfo> getClassList()
	{
		return _classData;
	}
	
	/**
	 * Gets the class info.
	 * @param classId the class Id.
	 * @return the class info related to the given {@code classId}.
	 */
	public ClassInfo getClass(final ClassId classId)
	{
		return _classData.get(classId);
	}
	
	/**
	 * Gets the class info.
	 * @param classId the class Id as integer.
	 * @return the class info related to the given {@code classId}.
	 */
	public ClassInfo getClass(final int classId)
	{
		final ClassId id = ClassId.getClassId(classId);
		return (id != null) ? _classData.get(id) : null;
	}
	
	/**
	 * Gets the class info.
	 * @param classServName the server side class name.
	 * @return the class info related to the given {@code classServName}.
	 */
	public ClassInfo getClass(final String classServName)
	{
		for (final ClassInfo classInfo : _classData.values())
		{
			if (classInfo.getClassServName().equals(classServName))
			{
				return classInfo;
			}
		}
		return null;
	}
	
	/**
	 * Gets the single instance of ClassListData.
	 * @return single instance of ClassListData
	 */
	public static ClassListData getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final ClassListData _instance = new ClassListData();
	}
}