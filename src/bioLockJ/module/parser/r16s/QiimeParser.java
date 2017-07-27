/**
 * @UNCC Fodor Lab
 * @author Anthony Fodor
 * @email anthony.fodor@gmail.com
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
package bioLockJ.module.parser.r16s;

import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import bioLockJ.module.classifier.r16s.qiime.QiimeMapping;
import bioLockJ.module.parser.ParserModule;
import bioLockJ.node.r16s.QiimeNode;

/**
 * To see file format: > head otu_table_L2.txt
 *
 * # Constructed from biom file #OTU ID 3A.1 6A.1 120A.1 7A.1
 * k__Bacteria;p__Actinobacteria 419.0 26.0 90.0 70.0
 *
 */
public class QiimeParser extends ParserModule
{
	private int mergeLineCount = 0;
	private final List<String> orderedSampleIDs = new ArrayList<>();

	/**
	 * QIIME doesn't support the demultiplex option
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		if( demultiplex )
		{
			throw new Exception( "METAPHLAN REQUIRES PROPERTY: " + INPUT_DEMULTIPLEX + "=TRUE" );
		}
	}

	/**
	 * Merge meta if executing a re-run (if needed).
	 * Convert the qiime mapping into R-friendly metadata keyed by SAMPLE_ID.
	 * Then proceed as ususal.
	 */
	@Override
	public void executeProjectFile() throws Exception
	{
		final List<File> inputFiles = getInputFiles();
		setOrderedSampleIDs( inputFiles.get( 0 ) );

		final File metadata = QiimeMapping.getMapping().getMetadata( getTempDir() );
		config.getMetaUtil().loadMetadata( metadata, config.getDescriptor() );
		super.executeProjectFile();
	}

	/**
	 * Init input files to find the most specific taxa file, this will contain all the info
	 * for all taxa levels above it.
	 */
	@Override
	public void initInputFiles( final File dir ) throws Exception
	{

		final String searchTerm = getLowestTaxaLevelFileName();
		info( "Recursively search for most specific taxa file " + searchTerm + " in: " + getName( dir ) );
		final IOFileFilter ff = new NameFileFilter( searchTerm );
		setModuleInput( dir, ff, TrueFileFilter.INSTANCE );

		boolean updateMeta = false;
		boolean updateDesc = false;

		File desc = new File( getInputDir().getAbsolutePath() + File.separator + config.getDescriptor().getName() );
		File meta = new File( getInputDir().getAbsolutePath() + File.separator + QIIME_MAPPING );

		if( desc.exists() && !desc.equals( config.getDescriptor() ) )
		{
			info( "Found new desciptor" );
			updateDesc = true;
		}
		else
		{
			desc = config.getDescriptor();
		}

		if( meta.exists() && !meta.equals( config.getMetadata() ) )
		{
			info( "Found new metadata" );
			info( "Old metadata: " + config.getMetadata().getAbsolutePath() );
			info( "New metadata: " + meta.getAbsolutePath() );
			updateMeta = true;
		}
		else
		{
			meta = config.getMetadata();
		}

		if( updateMeta || updateDesc )
		{
			config.getMetaUtil().loadMetadata( meta, desc );
		}
	}

	/**
	 * Create OTU nodes based on classifier output.  One file will have info for all sampelIDs,
	 * which are indexed within orderedSampleIDs.
	 */
	@Override
	protected void createOtuNodes() throws Exception
	{
		final File file = getInputFiles().get( 0 );
		info( "PARSE FILE = " + file.getName() );
		final BufferedReader reader = getFileReader( file );
		try
		{
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				if( !line.startsWith( "#" ) )
				{
					final StringTokenizer st = new StringTokenizer( line, DELIM );
					int index = 0;
					final String taxa = st.nextToken();
					while( st.hasMoreTokens() )
					{
						final Integer count = Double.valueOf( st.nextToken() ).intValue();
						final String id = orderedSampleIDs.get( index++ );
						if( count > 0 )
						{
							final QiimeNode node = new QiimeNode( taxa, count );
							addOtuNode( id, node );
						}
					}
				}
			}
		}
		catch( final Exception ex )
		{
			throw new Exception( "Error occurred parsing file: " + file.getName(), ex );
		}
		finally
		{
			reader.close();
		}
	}

	/**
	 * Get the output for the line, merged with its metadata.
	 */
	@Override
	protected String getMergedLine( final String line, final boolean isLastLine ) throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		final String sampleId = parseIdFromLine( line );
		if( config.getMetaUtil().getMetaFileFirstColValues().contains( sampleId ) )
		{
			sb.append( rFormat( line, DELIM ) );
			for( final String attribute: config.getMetaUtil().getAttributes( sampleId ) )
			{
				sb.append( DELIM );
				sb.append( rFormat( attribute, DELIM ) );
			}
		}
		else
		{
			warn( "Missing record for: " + sampleId + " in metadata: " + config.getMetaPath() );
			return null;
		}

		if( mergeLineCount++ < 2 )
		{
			info( "Example: Merge Metadata Line [" + sampleId + "] = " + sb.toString() );
		}

		return sb.toString();
	}

	/**
	 * Find the lowest taxa level.
	 * @return
	 * @throws Exception
	 */
	private String getLowestTaxaLevelFileName() throws Exception
	{
		String level = "";
		if( taxonomyLevels.contains( SPECIES ) )
		{
			level = "7";
		}
		else if( taxonomyLevels.contains( GENUS ) )
		{
			level = "6";
		}
		else if( taxonomyLevels.contains( FAMILY ) )
		{
			level = "5";
		}
		else if( taxonomyLevels.contains( ORDER ) )
		{
			level = "4";
		}
		else if( taxonomyLevels.contains( CLASS ) )
		{
			level = "3";
		}
		else if( taxonomyLevels.contains( PHYLUM ) )
		{
			level = "2";
		}
		else if( taxonomyLevels.contains( DOMAIN ) )
		{
			level = "1";
		}

		return OTU_TABLE_PREFIX + level + ".txt";
	}

	/**
	 * Sample IDs are read in from the header line, in order & saved to orderedSampleIDs.
	 * @param file
	 * @throws Exception
	 */
	private void setOrderedSampleIDs( final File file ) throws Exception
	{
		info( "Configure ordered list of Sample IDs based on example file: " + file.getAbsolutePath() );
		final BufferedReader reader = getFileReader( file );
		try
		{
			String header = reader.readLine(); // skip first line (its a comment)
			header = reader.readLine().replace( OTU_ID, "" );
			final String[] parts = header.split( "\\s" );
			for( final String qiimeId: parts )
			{
				if( qiimeId.trim().length() > 0 )
				{
					orderedSampleIDs.add( QiimeMapping.getMapping().getSampleId( qiimeId ) );
				}
			}
		}
		finally
		{
			reader.close();
		}
		info( "orderedSampleIDs( " + orderedSampleIDs.size() + " ) = " + orderedSampleIDs );
	}
}
