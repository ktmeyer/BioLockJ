/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 16, 2017
 * @disclaimer 	This code is free software; you can redistribute it and/or
 * 				modify it under the terms of the GNU General Public License
 * 				as published by the Free Software Foundation; either version 2
 * 				of the License, or (at your option) any later version,
 * 				provided that any use properly credits the author.
 * 				This program is distributed in the hope that it will be useful,
 * 				but WITHOUT ANY WARRANTY; without even the implied warranty of
 * 				MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * 				GNU General Public License for more details at http://www.gnu.org *
 */
package bioLockJ.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import bioLockJ.Constants;

/**
 * ConfigUtil reads in the properties file and is used to initialize the root directory.
 */
public class ConfigUtil extends Constants
{
	private MetadataUtil metaUtil = null;
	private File propertiesFile = null;
	private Properties props = null;

	/**
	 * A new ConfigUtil simply must read in props from the prop file.
	 * @param file
	 * @throws Exception
	 */
	public ConfigUtil( final File file ) throws Exception
	{
		propertiesFile = file;
		props = getPropsFromFile( propertiesFile );
	}

	/**
	 * Working method to read in the props.
	 * @param propertiesFile
	 * @return
	 * @throws Exception
	 */
	private static Properties getPropsFromFile( final File propertiesFile ) throws Exception
	{
		final InputStream in = new FileInputStream( propertiesFile );
		final Properties tempProps = new Properties();
		tempProps.load( in );
		in.close();
		return tempProps;
	}

	/**
	 * Gets the value for a given property.
	 * @param propName
	 * @return
	 */
	public String getAProperty( final String propName )
	{
		Object obj = props.get( propName );

		if( obj == null )
		{
			return null;
		}

		String val = obj.toString();

		// allow statements like thisDir = $someOtherDir to avoid re-typing
		// paths
		if( val.startsWith( "$" ) )
		{
			obj = props.getProperty( val.substring( 1 ) );

			if( obj == null )
			{
				return null;
			}

			val = obj.toString();
		}

		return val.trim();
	}

	/**
	 * Convenience method for metaUtil access.
	 * @return
	 */
	public File getDescriptor()
	{
		return getMetaUtil().getDescriptor();
	}

	/**
	 * Convenience method for metaUtil access.
	 * @return
	 */
	public String getDescriptorPath()
	{
		return getDescriptor().getAbsolutePath();
	}

	/**
	 * Convenience method for metaUtil access.
	 * @return
	 */
	public File getMetadata()
	{
		return getMetaUtil().getMetadata();
	}

	/**
	 * Convenience method for metaUtil access.
	 * @return
	 */
	public String getMetaPath()
	{
		return getMetadata().getAbsolutePath();
	}

	/**
	 * Convenience method for metaUtil access.
	 * @return
	 */
	public MetadataUtil getMetaUtil()
	{
		return metaUtil;
	}

	/**
	 * Convenience method for metaUtil access.
	 * @return
	 */
	public String getMetaId()
	{
		return ( ( metaUtil == null ) ? Constants.DEFAULT_META_ID : getMetaUtil().getMetaId() );
	}
	
	/**
	 * Convenience method returns all prop values as a Map.
	 * @return
	 */
	public Map<String, String> getProperties() throws Exception
	{
		final Properties tempProps = getPropsFromFile( propertiesFile );
		final Map<String, String> map = new HashMap<>();
		final Iterator<String> it = tempProps.stringPropertyNames().iterator();
		while( it.hasNext() )
		{
			final String key = it.next();
			map.put( key, tempProps.getProperty( key ) );
		}
		return map;
	}

	/**
	 * Convenience method to get propFile.
	 * @return
	 */
	public File getPropertiesFile()
	{
		return propertiesFile;
	}

	/**
	 * Get a property as list (must be comma delimited)
	 * @param propName
	 * @return
	 */
	public List<String> getPropertyAsList( final String propName )
	{
		final List<String> list = new ArrayList<>();
		final String val = getAProperty( propName );
		if( ( val != null ) && ( val.trim().length() > 0 ) )
		{
			final StringTokenizer st = new StringTokenizer( val, "," );
			while( st.hasMoreTokens() )
			{
				list.add( st.nextToken().trim() );
			}
		}

		return list;
	}

	/**
	 * Set decriptor in meaUtil.
	 * @param f
	 */
	public void setDescriptor( final File f )
	{
		getMetaUtil().setDescriptor( f );
	}

	/**
	 * Set metadata file in meaUtil.
	 * @param f
	 */
	public void setMetadata( final File f )
	{
		getMetaUtil().setMetadata( f );
	}

	/**
	 * Set meaUtil handle.
	 * @param f
	 */
	public void setMetaUtil( final MetadataUtil x )
	{
		metaUtil = x;
	}

	/**
	 * Set a prop value for a list.
	 * @param name
	 * @param list
	 */
	public void setProperty( final String name, final List<String> list )
	{
		final StringBuffer sb = new StringBuffer();
		for( final String val: list )
		{
			if( sb.length() > 0 ) // add to existing list string
			{
				sb.append( "," );
			}
			sb.append( val );
		}

		props.setProperty( name, sb.toString() );
	}

	/**
	 * Set a prop value for a single value.
	 * @param name
	 * @param value
	 */
	public void setProperty( final String name, final String value )
	{
		props.setProperty( name, value );
	}

}
