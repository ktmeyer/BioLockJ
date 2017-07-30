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
package bioLockJ.module.parser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import bioLockJ.Constants;
import bioLockJ.Module;
import bioLockJ.node.OtuNode;
import bioLockJ.node.OtuWrapper;

/**
 * The ParserModule is used output standardized tables for any classifier.
 * hitsPerSample is populated if report.numHits=Y
 * minNumHits from prop file is used to ignore any taxa lower than this threshold number
 */
public abstract class ParserModule extends Module
{
	private static final Map<String, Integer> hitsPerSample = new HashMap<>();
	private static int minNumHits = 0;
	private static final Map<String, OtuNode> otuNodes = new HashMap<>();
	private static final String TAXA_COL_SUFFIX = "_AsColumns.txt";
	private static final String THREE_COL_SUFFIX = "_SparseThreeCol.txt";

	/**
	 * This method is used to get an R-friendly value from the input val.
	 * Comments are ignored, and # symbols are replaced by "Num_".
	 * Also, any quotes are removed.
	 *
	 * @param val
	 * @param delim
	 * @return
	 * @throws Exception
	 */
	protected static String rFormat( final String val, final String delim ) throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		final StringTokenizer st = new StringTokenizer( val, delim );
		while( st.hasMoreTokens() )
		{
			String token = st.nextToken().trim();
			final int index = token.indexOf( commentChar );
			if( index > -1 )
			{
				token = token.substring( 0, index );
			}

			token = stripQuotes( token );

			if( sb.length() != 0 )
			{
				sb.append( DELIM );
			}

			sb.append( token.replace( "#", "Num_" ).replace( " ", "_" ) );
		}

		return sb.toString();
	}

	/**
	 * Read the sparse 3 col file to get a map for each key=SampleID, with value = Map<taxa, count>
	 * @param filePath
	 * @return
	 * @throws Exception
	 */
	private static Map<String, Map<String, Integer>> getMapFromFile( final String filePath ) throws Exception
	{
		final Map<String, Map<String, Integer>> map = new HashMap<>();
		final BufferedReader reader = new BufferedReader( new FileReader( new File( filePath ) ) );
		try
		{
			String nextLine = reader.readLine();
			while( ( nextLine != null ) && ( nextLine.trim().length() > 0 ) )
			{
				final StringTokenizer sToken = new StringTokenizer( nextLine, DELIM );
				final String sample = sToken.nextToken();
				final String taxa = sToken.nextToken();
				final int count = Integer.parseInt( sToken.nextToken() );

				Map<String, Integer> innerMap = map.get( sample );
				if( innerMap == null )
				{
					innerMap = new HashMap<>();
					map.put( sample, innerMap );
				}

				if( innerMap.containsKey( taxa ) )
				{
					throw new Exception( "Duplicate OTU " + taxa );
				}

				innerMap.put( taxa, count );
				nextLine = reader.readLine();
			}
		}
		catch( final Exception ex )
		{
			throw new Exception( "Error occurred processing (" + filePath + ")", ex );
		}
		finally
		{
			reader.close();
		}

		return map;
	}

	/**
	 * Get all OTUs above the required threshold set by minNumHits.
	 * @param map
	 * @param threshold
	 * @return
	 */
	private static List<String> getOTUSAtThreshold( final Map<String, Map<String, Integer>> map )
	{
		final Map<String, Integer> countMap = new HashMap<>();
		for( final String s: map.keySet() )
		{
			final Map<String, Integer> innerMap = map.get( s );
			for( final String possibleOtu: innerMap.keySet() )
			{
				Integer oldCount = countMap.get( possibleOtu );
				if( oldCount == null )
				{
					oldCount = 0;
				}

				oldCount += innerMap.get( possibleOtu );
				countMap.put( possibleOtu, oldCount );
			}
		}

		final List<String> otuList = new ArrayList<>();
		for( final String s: countMap.keySet() )
		{
			if( countMap.get( s ) >= minNumHits )
			{
				otuList.add( s );
			}
		}

		return otuList;

	}

	/**
	 * Populate required value: report.minOtuCount
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		minNumHits = requirePositiveInteger( REPORT_MINIMUM_OTU_COUNT );
	}

	/**
	 * Create sparse 3 col tables, rawCount tables, metaMerged tables, and if
	 * configured, logNormalized tables.  Also count hits/sample.
	 */
	@Override
	public void executeProjectFile() throws Exception
	{
		createOtuNodes();
		createTaxaSparseThreeColFiles();
		createTaxaCountTables();
		populateHitsPerSample();
		appendMetaToTaxaCountTables();
	}

	/**
	 * This method determine weather or not to add a new node, or merge nodes if one already exists for
	 * the sampleID.
	 *
	 * @param id
	 * @param newNode
	 * @throws Exception
	 */
	protected void addOtuNode( final String id, final OtuNode newNode ) throws Exception
	{
		final OtuNode node = otuNodes.get( id );
		if( node == null )
		{
			otuNodes.put( id, newNode );
		}
		else
		{
			otuNodes.get( id ).mergeNode( newNode );
		}
	}

	/**
	 * Merge taxonomy level raw count and relative abundance files with metadata.
	 *
	 * @throws Exception
	 */
	protected void appendMetaToTaxaCountTables() throws Exception
	{
		final String tempDir = getTempDir().getAbsolutePath() + File.separator;
		final String outDir = getOutputDir().getAbsolutePath() + File.separator;
		for( final String taxa: taxonomyLevels )
		{
			final String rawCountFile = tempDir + taxa + TAXA_COL_SUFFIX;
			final String rawCountOut = outDir + taxa + RAW_COUNT + META_MERGED_SUFFIX;

			final String logNormFile = tempDir + taxa + LOG_NORMAL_SUFFIX;
			final String logNormOut = outDir + taxa + LOG_NORMAL + META_MERGED_SUFFIX;

			if( config.getMetaUtil() != null )
			{
				outputMetaMergeTables( rawCountFile, rawCountOut );
				outputMetaMergeTables( logNormFile, logNormOut );
			}
		}
	}

	protected abstract void createOtuNodes() throws Exception;

	/**
	 * Create rawCount and (if configured) log normalized tables based on sparse 3 col tables.
	 * @throws Exception
	 */
	protected void createTaxaCountTables() throws Exception
	{
		for( final String taxa: taxonomyLevels )
		{
			final String pathPrefix = getTempDir().getAbsolutePath() + File.separator + taxa;
			final File threeColFile = new File( pathPrefix + THREE_COL_SUFFIX );
			final Map<String, Map<String, Integer>> map = getMapFromFile( threeColFile.getAbsolutePath() );
			final File taxaColFile = new File( pathPrefix + TAXA_COL_SUFFIX );
			writeResults( map, taxaColFile );

			final OtuWrapper wrapper = new OtuWrapper( taxaColFile, logBase.toLowerCase() );
			wrapper.writeNormalizedLoggedDataToFile( config.getMetaId(), pathPrefix + LOG_NORMAL_SUFFIX );
		}
	}

	/**
	 * Read the OtuNodes to output sparse 3 col tables.  Here we limit by minNumHits if needed.
	 * @throws Exception
	 */
	protected void createTaxaSparseThreeColFiles() throws Exception
	{
		final Map<String, BufferedWriter> taxaWriters = getTaxaWriters();
		try
		{
			final TreeSet<String> ids = new TreeSet<>( otuNodes.keySet() );
			for( final String level: taxonomyLevels )
			{
				for( final String id: ids )
				{
					final BufferedWriter writer = taxaWriters.get( level );
					final TreeMap<String, Integer> map = otuNodes.get( id ).getMap( level );
					for( final String taxa: map.keySet() )
					{
						writer.write( id + DELIM + taxa + DELIM + map.get( taxa ) + "\n" );
					}
				}
			}
		}
		catch( final Exception ex )
		{
			error( "Error occurred creating file: " + THREE_COL_SUFFIX, ex );
		}
		finally
		{
			for( final BufferedWriter writer: taxaWriters.values() )
			{
				writer.flush();
				writer.close();
			}
		}
	}

	/**
	 * Get FileID by removing the PROCESSED suffix.
	 * @param file
	 * @return
	 * @throws Exception
	 */
	protected String getFileID( final File file ) throws Exception
	{
		return file.getName().replace( PROCESSED, "" );
	}

	/**
	 * Get merged line by adding metadata for the sampleID found in the first column of the line.
	 * @param line
	 * @param isLastLine
	 * @return
	 * @throws Exception
	 */
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

		return sb.toString();
	}

	/**
	 * Get taxaWriters for each taxaLevel configured in taxonomyLevels.
	 * @return
	 * @throws Exception
	 */
	protected Map<String, BufferedWriter> getTaxaWriters() throws Exception
	{
		final Map<String, BufferedWriter> taxaWriters = new HashMap<>();
		for( final String taxa: taxonomyLevels )
		{
			final BufferedWriter writer = new BufferedWriter( new FileWriter(
					new File( getTempDir().getAbsolutePath() + File.separator + taxa + THREE_COL_SUFFIX ) ) );
			taxaWriters.put( taxa, writer );
		}

		return taxaWriters;
	}

	/**
	 * Get ID from the first token.
	 * @param fileLine
	 * @return
	 * @throws Exception
	 */
	protected String parseIdFromLine( final String fileLine ) throws Exception
	{
		final StringTokenizer token = new StringTokenizer( fileLine );
		return token.nextToken();
	}

	/**
	 * Output meta-merged tables by calling getMergedLine()
	 * @param in
	 * @param out
	 * @throws Exception
	 */
	private void outputMetaMergeTables( final String in, final String out ) throws Exception
	{
		final BufferedReader reader = new BufferedReader( new FileReader( in ) );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( out ) );
		try
		{
			String line = reader.readLine();
			while( line != null )
			{
				final String nextLine = reader.readLine();
				final String output = getMergedLine( line, nextLine == null );
				if( output != null )
				{
					writer.write( output + "\n" );
				}

				line = nextLine;
			}
		}
		catch( final Exception ex )
		{
			throw new Exception( "Unable to append meta to: " + out, ex );
		}
		finally
		{
			writer.flush();
			writer.close();
			reader.close();
		}
	}

	/**
	 * Read nodes to get numHits for a given sample.
	 * @throws Exception
	 */
	private void populateHitsPerSample() throws Exception
	{
		if( reportNumHits && hitsPerSample.isEmpty() )
		{
			for( final String id: otuNodes.keySet() )
			{
				hitsPerSample.put( id, otuNodes.get( id ).getNumHits() );
			}

			config.getMetaUtil().addColumnToMetadata( this, NUM_HITS, hitsPerSample, getOutputDir() );
		}
	}

	/**
	 * This method builds tables containing raw counts (above the given threshold set by minNumHits)
	 * @param map
	 * @param file
	 * @throws Exception
	 */
	private void writeResults( final Map<String, Map<String, Integer>> map, final File file ) throws Exception
	{
		final BufferedWriter writer = new BufferedWriter( new FileWriter( file ) );
		final String metaId = config.getMetaId();
		info( "Create " + file.getName() + " with Header ID Column =  MetaId[ " + metaId
				+ " ]" );
		writer.write( metaId );
		final List<String> otuList = getOTUSAtThreshold( map );
		Collections.sort( otuList );
		for( final String s: otuList )
		{
			writer.write( DELIM + s );
		}

		writer.write( "\n" );
		final List<String> samples = new ArrayList<>();
		for( final String s: map.keySet() )
		{
			samples.add( s );
		}

		Collections.sort( samples );
		int sampleCount = 0;

		for( final String s: samples )
		{
			writer.write( s );
			final Map<String, Integer> innerMap = map.get( s );
			for( final String otu: otuList )
			{
				Integer aVal = innerMap.get( otu );
				if( aVal == null )
				{
					aVal = 0;
				}

				writer.write( DELIM + aVal );
			}

			if( ++sampleCount != samples.size() )
			{
				writer.write( "\n" );
			}

		}

		writer.write( "\n" );

		writer.flush();
		writer.close();
	}
}