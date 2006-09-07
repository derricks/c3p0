/*
 * Distributed as part of c3p0 v.0.9.1-pre7
 *
 * Copyright (C) 2005 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 2.1, as 
 * published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; see the file LICENSE.  If not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 */


package com.mchange.v2.c3p0.cfg;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import com.mchange.v2.cfg.*;
import com.mchange.v2.log.*;
import com.mchange.v2.c3p0.impl.*;

import com.mchange.v1.xml.DomParseUtils;

public final class C3P0ConfigXmlUtils
{
    public final static String XML_CONFIG_RSRC_PATH     = "/c3p0-config.xml";

    final static MLogger logger = MLog.getLogger( C3P0ConfigXmlUtils.class );

    public final static String LINESEP;

    static
    {
	String ls;

	try
	    { ls = System.getProperty("line.separator", "\r\n"); }
	catch (Exception e)
	    { ls = "\r\n"; }

	LINESEP = ls;
    }
    
    public static C3P0Config extractXmlConfigFromDefaultResource() throws Exception
    {
	InputStream is = null;

	try
	    {
		is = C3P0ConfigUtils.class.getResourceAsStream(XML_CONFIG_RSRC_PATH);
		return (is == null ? null : extractXmlConfigFromInputStream( is ));
	    }
	finally
	    {
		try { if (is != null) is.close(); }
		catch (Exception e)
		    {
			if ( logger.isLoggable( MLevel.FINE ) )
			    logger.log(MLevel.FINE,"Exception on resource InputStream close.", e);
		    }
	    }
    }

    public static C3P0Config extractXmlConfigFromInputStream(InputStream is) throws Exception
    {
	DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
	DocumentBuilder db = fact.newDocumentBuilder();
	Document doc = db.parse( is );

	return extractConfigFromXmlDoc(doc);
    }

    public static C3P0Config extractConfigFromXmlDoc(Document doc) throws Exception
    {
	Element docElem = doc.getDocumentElement();
	if (docElem.getTagName().equals("c3p0-config"))
	    {
		NamedScope defaults;
		HashMap configNamesToNamedScopes = new HashMap();

		Element defaultConfigElem = DomParseUtils.uniqueChild( docElem, "default-config" );
		if (defaultConfigElem != null)
		    defaults = extractNamedScopeFromLevel( defaultConfigElem );
		else
		    defaults = new NamedScope();
		NodeList nl = DomParseUtils.immediateChildElementsByTagName(docElem, "named-config");
		for (int i = 0, len = nl.getLength(); i < len; ++i)
		    {
			Element namedConfigElem = (Element) nl.item(i);
			String configName = namedConfigElem.getAttribute("name");
			if (configName != null && configName.length() > 0)
			    {
				NamedScope namedConfig = extractNamedScopeFromLevel( namedConfigElem );
				configNamesToNamedScopes.put( configName, namedConfig);
			    }
			else
			    logger.warning("Configuration XML contained named-config element without name attribute: " + namedConfigElem);
		    }
		return new C3P0Config( defaults, configNamesToNamedScopes );
	    }
	else
	    throw new Exception("Root element of c3p0 config xml should be 'c3p0-config', not '" + docElem.getTagName() + "'.");
    }

    private static NamedScope extractNamedScopeFromLevel(Element elem)
    {
	HashMap props = extractPropertiesFromLevel( elem );
	HashMap userNamesToOverrides = new HashMap();

	NodeList nl = DomParseUtils.immediateChildElementsByTagName(elem, "user-overrides");
	for (int i = 0, len = nl.getLength(); i < len; ++i)
	    {
		Element perUserConfigElem = (Element) nl.item(i);
		String userName = perUserConfigElem.getAttribute("user");
		if (userName != null && userName.length() > 0)
		    {
			HashMap userProps = extractPropertiesFromLevel( perUserConfigElem );
			userNamesToOverrides.put( userName, userProps );
		    }
		else
		    logger.warning("Configuration XML contained user-overrides element without user attribute: " + LINESEP + perUserConfigElem);
	    }

	return new NamedScope(props, userNamesToOverrides);
    }

    private static HashMap extractPropertiesFromLevel(Element elem)
    {
	// System.err.println( "extractPropertiesFromLevel()" );

	HashMap out = new HashMap();

	try
	    {
		NodeList nl = DomParseUtils.immediateChildElementsByTagName(elem, "property");
		int len = nl.getLength();
		for (int i = 0; i < len; ++i)
		    {
			Element propertyElem = (Element) nl.item(i);
			String propName = propertyElem.getAttribute("name");
			if (propName != null && propName.length() > 0)
			    {
				String propVal = DomParseUtils.allTextFromElement(propertyElem, true);
				out.put( propName, propVal );
				//System.err.println( propName + " -> " + propVal );
			    }
			else
			    logger.warning("Configuration XML contained property element without name attribute: " + LINESEP + propertyElem);
		    }
	    }
	catch (Exception e)
	    {
		logger.log( MLevel.WARNING, 
			    "An exception occurred while reading config XML. " +
			    "Some configuration information has probably been ignored.", 
			    e );
	    }

	return out;
    }

    private C3P0ConfigXmlUtils()
    {}
}