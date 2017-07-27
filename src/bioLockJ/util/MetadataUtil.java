/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 9, 2017
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.math.NumberUtils;
import bioLockJ.BioLockJ;
import bioLockJ.Module;

/**
 * The metadataUtil helps access and modify data in the metadata & descriptor files.
 */
public class MetadataUtil extends BioLockJ
{
	private final Map<String, Set<String>> attributeMap = new HashMap<>();
	private File descriptorFile = null;
	private Map<String, List<String>> descriptorMap = null;
	private File metadataFile = null;
	private Map<String, List<String>> metadataMap = null;
	private String metaId = "id";
	private final Set<String> rScriptFields = new TreeSet<>();

	/**
	 * MetadataUtil is only instantiated one time.  Hacked in welcome message here.
	 * @throws Exception
	 */
	public MetadataUtil() throws Exception
	{
		logWelcomeHeader();
		logConfigFileSettings();

		metadataFile = requireExistingFile( METADATA_FILE );
		descriptorFile = requireExistingFile( METADATA_DESCRIPTOR );

		setRscriptFields();

		copyFile( metadataFile, requireString( ROOT_DIR ) );
		copyFile( descriptorFile, requireString( ROOT_DIR ) );

		metadataFile = new File( requireString( ROOT_DIR ) + metadataFile.getName() );
		descriptorFile = new File( requireString( ROOT_DIR ) + descriptorFile.getName() );
		if( !metadataFile.exists() || !descriptorFile.exists() )
		{
			throw new Exception( "Unable to load metadata and descriptor files from " + requireString( ROOT_DIR ) );
		}

		loadMetadata( metadataFile, descriptorFile );
		verifyReportFields();
	}

	/**
	 * Log config file settings in welcome message - just a hack, should probably move to ApplicationManager.
	 * @throws Exception
	 */
	private static void logConfigFileSettings() throws Exception
	{
		log.info( LOG_SPACER );
		log.info( "Runtime Configuration" );
		log.info( LOG_SPACER );
		final Map<String, String> map = config.getProperties();
		final Iterator<String> it = new TreeSet<>( map.keySet() ).iterator();
		while( it.hasNext() )
		{
			final String key = it.next();
			log.info( key + " = " + map.get( key ) );
		}
		log.info( LOG_SPACER );
	}

	/**
	 * Output welcome message to the log file with BioLockJ version, lab citation,
	 * and freeware msg.
	 */
	private static void logWelcomeHeader()
	{
		final String lab = "Launching BioLockJ v.1.0 ~ Distributed by UNCC Fodor Lab @2017\n";
		final String msg = "This code is free software; you can redistribute and/or modify it\n"
				+ "under the terms of the GNU General Public License as published by\n"
				+ "the Free Software Foundation; either version 2 of the License, or\n"
				+ "any later version, provided proper credit is given to the authors.\n"
				+ "This program is distributed in the hope that it will be useful,\n"
				+ "but WITHOUT ANY WARRANTY; without even the implied warranty of\n"
				+ "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n"
				+ "GNU General Public License for more details at http://www.gnu.org\n";

		log.info( "\n" + LOG_SPACER + "\n" + lab + LOG_SPACER + "\n" + msg + LOG_SPACER + "\n" );
	}

	/**
	 * When a new column is added to metadata, this method will add the column, with all row values.
	 * Here we also update the descriptor and set the new values in the ConfigUtil.  The updated
	 * files are output to the "outputDir" to be picked up by the next executor.
	 * @param module
	 * @param name
	 * @param map
	 * @param fileDir
	 * @throws Exception
	 */
	public void addColumnToMetadata( final Module module, final String name, final Map<String, Integer> map,
			final File fileDir ) throws Exception
	{
		final String metaName = metadataFile.getName();
		final String descName = descriptorFile.getName();

		final File newMeta = new File( fileDir.getAbsolutePath() + File.separator + metaName );
		final File newDesc = new File( fileDir.getAbsolutePath() + File.separator + descName );
		final BufferedReader metaReader = new BufferedReader( new FileReader( metadataFile ) );
		final BufferedWriter metaWriter = new BufferedWriter( new FileWriter( newMeta ) );
		final BufferedReader descReader = new BufferedReader( new FileReader( descriptorFile ) );
		final BufferedWriter descWriter = new BufferedWriter( new FileWriter( newDesc ) );

		log.info( "Adding new attribute [" + name + "] to metadata" );
		printDescriptor();
		boolean isHeaderRow = true;
		try
		{
			for( String line = descReader.readLine(); line != null; line = descReader.readLine() )
			{
				log.info( "Copy existing descriptor line = " + line );
				descWriter.write( line + "\n" );
			}

			descWriter.write( name + DELIM + CONTINUOUS + DELIM + QIIME_COMMENT + "\n" );

			for( String line = metaReader.readLine(); line != null; line = metaReader.readLine() )
			{
				final StringTokenizer st = new StringTokenizer( line, DELIM );
				if( isHeaderRow )
				{
					isHeaderRow = false;
					line += DELIM + name;
				}
				else
				{
					line += DELIM + map.get( st.nextToken() );
				}

				metaWriter.write( line + "\n" );
			}
		}
		catch( final Exception ex )
		{
			log.error( "Error occurred updating metadata with new attribute [" + name + "]", ex );
		}
		finally
		{
			metaReader.close();
			descReader.close();
			metaWriter.flush();
			descWriter.flush();
			metaWriter.close();
			descWriter.close();
			loadMetadata( newMeta, newDesc );
		}
	}

	/**
	 * Get a list of all attribute names from the metadata file column names.
	 * @return
	 */
	public List<String> getAttributeNames()
	{
		return metadataMap.get( metaId );
	}

	/**
	 * Get attribute values from metadata (get row for a given ID).
	 * @param id
	 * @return
	 * @throws Exception
	 */
	public List<String> getAttributes( final String id ) throws Exception
	{
		try
		{
			return metadataMap.get( id );
		}
		catch( final Exception ex )
		{
			throw new Exception( "Invalid ID: " + id );
		}

	}

	/**
	 * Get attribute type from the descriptor file based on the name provided.
	 * @param attribute
	 * @return
	 * @throws Exception
	 */
	public String getAttributeType( final String attribute ) throws Exception
	{
		try
		{
			return getAttributeDescriptor( attribute ).get( ATT_TYPE_INDEX ).toUpperCase();
		}
		catch( final Exception ex )
		{
			throw new Exception( "Invalid attribute: " + attribute );
		}
	}

	public File getDescriptor()
	{
		return descriptorFile;
	}

	public File getMetadata()
	{
		return metadataFile;
	}

	/**
	 * Get the first column from the metadata file.
	 * @return
	 */
	public Set<String> getMetaFileFirstColValues()
	{
		return metadataMap.keySet();
	}

	public String getMetaId()
	{
		return metaId;
	}

	/**
	 * Get value of metadata attribute for one id.
	 * @param id
	 * @param attribute
	 * @return value of attribute for given id
	 * @throws Exception
	 */
	public String getValue( final String id, final String attribute ) throws Exception
	{
		final String val = "";
		try
		{
			final List<String> colNames = metadataMap.get( metaId );
			final int index = colNames.indexOf( attribute );

			final List<String> values = metadataMap.get( id );
			return values.get( index );
		}
		catch( final Exception ex )
		{
			throw new Exception( "Invalid attribute: " + attribute );
		}
	}

	/**
	 * Get all values in the metadata for any one column.  Used to verify by type.
	 * @param attribute
	 * @return
	 * @throws Exception
	 */
	public List<String> getValues( final String attribute ) throws Exception
	{
		try
		{
			return new ArrayList<>( attributeMap.get( attribute ) );
		}
		catch( final Exception ex )
		{
			throw new Exception( "Invalid attribute: " + attribute );
		}
	}

	/**
	 * Loading new metadata & descriptor will set the static field values and populate the attributeMap.
	 * @param metadata
	 * @param descriptor
	 * @throws Exception
	 */
	public void loadMetadata( final File metadata, final File descriptor ) throws Exception
	{
		info( "Loading Metadata: " + metadata.getAbsolutePath() );
		info( "Loading Descriptor: " + descriptor.getAbsolutePath() );
		processMetadata( processFile( descriptor ), MAP_TYPE_DESCRIPTOR );
		processMetadata( processFile( metadata ), MAP_TYPE_METADATA );
		populateAttributeMap();
		metadataFile = metadata;
		descriptorFile = descriptor;
		ignoreInputFiles.add( metadataFile.getName() );
		ignoreInputFiles.add( descriptorFile.getName() );
		info( "Metadata Attributes: " + getAttributeNames() );
		info( "Metadata 1st Column (Header ID name & Sample IDs): " + getMetaFileFirstColValues() );
		info( LOG_SPACER );
		info( "loadMetadata: New " + MAP_TYPE_METADATA + " = " + metadataFile.getAbsolutePath() );
		info( "loadMetadata: New " + MAP_TYPE_DESCRIPTOR + " = " + descriptorFile.getAbsolutePath() );
		info( "Metadata/Descriptor updates complete!" );
		info( LOG_SPACER );
	}

	/**
	 * The attributeMap maps attributes to their set of values.  Only done for metadata that will
	 * be used in the R-script.
	 *
	 * @throws Exception
	 */
	public void populateAttributeMap() throws Exception
	{
		final Map<String, Integer> map = new HashMap<>();
		int index = 0;
		for( final String attribute: metadataMap.get( getMetaId() ) )
		{
			if( rScriptFields.contains( attribute ) )
			{
				info( "Initialize Attribute Map | attribute(" + attribute + ") = index(" + index + ")" );
				map.put( attribute, index );
				attributeMap.put( attribute, new HashSet<String>() );
			}
			index++;
		}

		for( final String attribute: map.keySet() )
		{
			final int target = map.get( attribute );
			info( "populate attribute map for: " + attribute + " [target index = " + target + "]" );
			for( final String key: metadataMap.keySet() )
			{
				if( key.equals( getMetaId() ) )
				{
					continue;
				}
				int i = 0;
				final List<String> row = metadataMap.get( key );
				for( final String value: row )
				{
					if( ( i++ == target ) && !value.equals( nullChar ) )
					{
						attributeMap.get( attribute ).add( value );
					}
				}
			}
		}

		for( final String key: attributeMap.keySet() )
		{
			final Set<String> vals = attributeMap.get( key );
			final String type = getAttributeType( key );
			if( reportAttributes.contains( key ) && type.equals( BINARY ) && ( vals.size() != 2 ) )
			{
				throw new Exception( "Property " + REPORT_ATTRIBUTES + " contains a binary attribute that has "
						+ vals.size() + " values in the metadata file.  Binary attributes must contain"
						+ " exactly 2 values if reported on in the R-Script >>>" + key + "=" + vals );
			}
			if( reportAttributes.contains( key ) && type.equals( CATEGORICAL ) && ( vals.size() < 2 ) )
			{
				throw new Exception( "Property " + REPORT_ATTRIBUTES + " contains a categorical attribute that has "
						+ vals.size() + " values in the metadata file.  Categorical attributes must contain"
						+ " exactly at least 2 values if reported on in the R-Script. >>>" + key + "=" + vals );
			}
			if( reportAttributes.contains( key ) && type.equals( CONTINUOUS ) )
			{
				for( final String val: vals )
				{
					final String cleanVal = rScriptFormat( val );
					if( ( cleanVal == null ) || !NumberUtils.isNumber( cleanVal ) )
					{
						throw new Exception( "Property " + REPORT_ATTRIBUTES
								+ " contains a continuous attribute that is non-numeric." + " Value found = " + val );
					}
				}
			}
		}

		for( final String key: attributeMap.keySet() )
		{
			info( "Attribute Map (" + key + ") = " + attributeMap.get( key ) );
		}
	}

	/**
	 * Clean values avoid commas, and replace spaces with underscores.
	 * @param val
	 * @return
	 */
	public String rScriptFormat( String val )
	{
		if( val == null )
		{
			return null;
		}

		final int index = val.indexOf( commentChar );
		if( index > -1 )
		{
			val = val.substring( 0, val.indexOf( commentChar ) );
		}

		return val.trim().replace( " ", emptySpaceDelim );
	}

	public void setDescriptor( final File f )
	{
		descriptorFile = f;
	}

	public void setMetadata( final File f )
	{
		metadataFile = f;
	}

	public String setMetaId( final String id )
	{
		metaId = id.trim();
		return metaId;
	}

	/**
	 * Update a descriptor with new fields (only categorical for now)
	 * @param newFields
	 * @param targetDir
	 * @throws Exception
	 */
	public void updateDescriptor( final List<String> newFields, final File targetDir ) throws Exception
	{
		info( "Updating descriptor: " + descriptorFile.getAbsolutePath() );
		printDescriptor();
		final String name = descriptorFile.getName();
		final File newDesc = new File( targetDir.getAbsolutePath() + File.separator + name );
		final BufferedReader reader = new BufferedReader( new FileReader( descriptorFile ) );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( newDesc ) );
		try
		{
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				debug( "updateDescriptor: add existing line = " + line );
				writer.write( line + "\n" );
			}

			for( final String field: newFields )
			{
				if( !getAttributeNames().contains( field ) )
				{
					debug( "updateDescriptor: add new line = " + field + DELIM + CATEGORICAL + DELIM + QIIME_COMMENT );
					writer.write( field + DELIM + CATEGORICAL + DELIM + QIIME_COMMENT + "\n" );
				}
			}
		}
		finally
		{
			reader.close();
			writer.flush();
			writer.close();
		}

		descriptorFile = newDesc;
	}

	/**
	 * Set collection with values formatted for R Script
	 * @param list
	 * @param rScriptCollection
	 */
	private void addRScriptValues( final List<String> list, final Collection<String> rScriptCollection )
	{
		if( list != null )
		{
			for( final String att: list )
			{
				rScriptCollection.add( rScriptFormat( att ) );
			}
		}
	}

	/**
	 * Get the descriptor for an attribute (read from descriptor file).
	 * @param attribute
	 * @return
	 */
	private List<String> getAttributeDescriptor( final String attribute )
	{
		return descriptorMap.get( attribute );
	}

	private void printDescriptor()
	{
		debug( "PRINT FULL DESCRIPTOR MAP " );
		if( descriptorMap == null )
		{
			debug( "descriptorMap is not initialized " );
		}
		else
		{
			try
			{
				debug( "DESCRIPTOR MAP SIZE = " + descriptorMap.keySet().size() );
				for( final String key: descriptorMap.keySet() )
				{
					debug( "key[" + key + "]=" + descriptorMap.get( key ) );
					debug( "type[" + key + "]=" + descriptorMap.get( key ).get( ATT_TYPE_INDEX ) );
				}
			}
			catch( final Exception ex )
			{
				debug( "ERROR! " + ex.getMessage() );
				ex.printStackTrace();
			}
		}
	}

	/**
	 * Process a file by getting clean values for each cell in the spreadsheet.
	 * @param file
	 * @return
	 */
	private List<List<String>> processFile( final File file )
	{
		final List<List<String>> data = new ArrayList<>();
		FileReader fileReader = null;
		CSVParser csvFileParser = null;
		try
		{
			fileReader = new FileReader( file );
			csvFileParser = new CSVParser( fileReader, CSVFormat.DEFAULT.withDelimiter( TAB ) );
			final Iterator<CSVRecord> csvRecords = csvFileParser.getRecords().iterator();

			while( csvRecords.hasNext() )
			{
				final ArrayList<String> record = new ArrayList<>();
				final CSVRecord csvRow = csvRecords.next();
				final Iterator<String> it = csvRow.iterator();
				while( it.hasNext() )
				{
					record.add( rScriptFormat( it.next() ) );
				}

				data.add( record );
			}
		}
		catch( final Exception ex )
		{
			error( "Error occurred processing file: " + file.getAbsolutePath(), ex );
			return null;
		}
		finally
		{
			try
			{
				fileReader.close();
				csvFileParser.close();
			}
			catch( final IOException ex )
			{
				error( "Error occurred clsoing file: " + file.getAbsolutePath(), ex );
			}
		}

		return data;
	}

	/**
	 * Process metadata & output some values to log file for verification.
	 * @param data
	 * @param mapType
	 */
	private void processMetadata( final List<List<String>> data, final String mapType )
	{
		final boolean isMeta = mapType.equals( MAP_TYPE_METADATA );
		final Map<String, List<String>> map = new HashMap<>();
		final int digits = new Integer( data.size() ).toString().length();
		int rowNum = 0;
		final Iterator<List<String>> rows = data.iterator();
		while( rows.hasNext() )
		{
			final List<String> row = trim( rows.next() );
			final String id = row.get( 0 ).trim();
			row.remove( 0 );

			if( isMeta && ( rowNum == 0 ) )
			{
				info( "Loading METADATA [ID = " + setMetaId( id ) + "] with " + row.size() + " attribute columns" );
			}
			else if( !isMeta && ( rowNum == 0 ) )
			{
				info( "Loading DESCRIPTOR for " + ( data.size() - 1 ) + " attributes" );
			}

			if( isMeta && ( rowNum < 2 ) )
			{
				info( "Example Metadata Row[" + formatInt( rowNum, digits ) + "]: Key(" + id + "): " + row );

			}
			else if( !isMeta && ( rowNum > 0 ) ) // ignore header
			{
				info( "Descriptor Row[" + formatInt( rowNum, digits ) + "]: Attribute Type(" + id + ") = "
						+ row.get( 0 ) );
			}

			map.put( id, row );
			rowNum++;
		}

		if( isMeta )
		{
			metadataMap = map;
		}
		else
		{
			descriptorMap = map;
		}
	}

	/**
	 * Set rScriptFields variable =  all metadata attributes referenced in R Script:
	 * Uses 3 config file props: reportAttributes, filterNaAttributes, & filterAttributes
	 */
	private void setRscriptFields()
	{
		addRScriptValues( getList( REPORT_ATTRIBUTES ), reportAttributes );
		addRScriptValues( getList( R_FILTER_NA_ATTRIBUTES ), filterNaAttributes );
		addRScriptValues( getList( R_FILTER_ATTRIBUTES ), filterAttributes );
		rScriptFields.addAll( reportAttributes );
		rScriptFields.addAll( filterNaAttributes );
		rScriptFields.addAll( filterAttributes );
	}

	/**
	 * Trim all values in row.
	 * @param row
	 * @return
	 */
	private List<String> trim( final List<String> row )
	{
		final List<String> formattedRow = new ArrayList<>();
		final Iterator<String> it = row.iterator();
		while( it.hasNext() )
		{
			formattedRow.add( it.next().trim() );
		}

		return formattedRow;
	}

	/**
	 * Verify any fields to be used in R scripts.
	 * @throws Exception
	 */
	private void verifyReportFields() throws Exception
	{
		for( final String field: rScriptFields )
		{
			if( getAttributeType( field ) == null )
			{
				throw new Exception( field + " is undefined in descriptor: " + getDescriptor().getAbsolutePath() );
			}
			if( !getAttributeNames().contains( field ) )
			{
				throw new Exception( field + " is not found in metadata: " + getMetadata().getAbsolutePath() );
			}
		}
	}

}