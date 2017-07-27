/**
 * @UNCC BINF 8380
 *
 * @author Michael Sioda
 * @date Jul 14, 2017
 */
package bioLockJ.module.preProcessor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.IntStream;
import bioLockJ.Module;

/**
 *
 */
public class Rarefier extends Module
{
	private static Set<String> badSamples = new HashSet<>();

	/**
	 * Parameter rarefyingMin will be set to 0 if undefined in the config file.
	 * Parameter rarefyingMax must be defined in the config file as a poitive integer > 1.
	 * @Exception thrown if rarefyingMax < 2
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		if( ( rarefyingMax == null ) || ( rarefyingMax < 1 ) || ( rarefyingMin == null )
				|| ( rarefyingMin > rarefyingMax ) )
		{
			throw new Exception( "Invalid parameter value.  Control property: (" + CONTROL_RAREFY_SEQS + " = Y) "
					+ "so it is required that (" + INPUT_RAREFYING_MIN + " <= " + INPUT_RAREFYING_MAX + ") & ("
					+ INPUT_RAREFYING_MAX + " > 1)" );
		}
	}

	/**
	 * Shuffle list of sequences
	 */
	@Override
	public void executeProjectFile() throws Exception
	{
		if( isPairedRead )
		{
			throw new Exception( "Paired reads must be merged before rarefying!" );
		}

		final List<File> files = getInputFiles();
		final int numFiles = count( files );
		registerNumReadsPerSample( files, getTempDir() );

		info( "Rarefying " + numFiles + " " + ( isFastA() ? FASTA: FASTQ ) + " files..." );
		info( "=====> Min # Reads = " + rarefyingMin );
		info( "=====> Max # Reads = " + rarefyingMax );

		int i = 0;
		for( final File f: files )
		{
			rarefy( f );
			if( ( ++i % 5 ) == 0 )
			{
				info( "Done rarefying " + i + "/" + numFiles + " files." );
			}
		}

		if( ( i % 5 ) != 0 )
		{
			info( "Done rarefying " + i + "/" + numFiles + " files." );
		}

		removeBadSamples();
	}

	private void buildRarefiedFile( final File input, final List<Integer> indexes ) throws Exception
	{
		final String fileExt = "." + ( isFastA() ? FASTA: FASTQ );
		final int blockSize = isFastA() ? 2: 4;
		final String name = getOutputDir().getAbsolutePath() + File.separator + trimSampleID( input.getName() )
				+ fileExt;
		final File output = new File( name );
		final BufferedReader reader = getFileReader( input );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( output ) );
		try
		{
			int index = 0;
			int i = 0;
			final Set<Integer> usedIndexes = new HashSet<>();
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				if( indexes.contains( index ) )
				{
					usedIndexes.add( index );
					writer.write( line + "\n" );
				}

				if( ( ++i % blockSize ) == 0 )
				{
					index++;
				}
			}

			if( !usedIndexes.containsAll( indexes ) )
			{
				indexes.removeAll( usedIndexes );
				warn( "Error occurred rarefying indexes for: " + input.getAbsolutePath() );
				for( final Integer x: indexes )
				{
					warn( "Missing index: " + x );
				}
			}
		}
		catch( final Exception ex )
		{
			error( "Error occurred rarefying " + input.getAbsolutePath(), ex );
		}
		finally
		{
			reader.close();
			writer.flush();
			writer.close();
		}
	}

	private void rarefy( final File f ) throws Exception
	{
		final String sampleId = trimSampleID( f.getName() );
		final int numReads = getReadsPerSample().get( sampleId );
		info( "Sample[" + sampleId + "] - numReads = " + numReads );
		if( numReads >= rarefyingMin )
		{
			final int[] range = IntStream.rangeClosed( 0, ( numReads - 1 ) ).toArray();
			final List<Integer> indexes = new ArrayList<>();
			Collections.addAll( indexes, Arrays.stream( range ).boxed().toArray( Integer[]::new ) );
			Collections.shuffle( indexes );
			indexes.subList( rarefyingMax, indexes.size() ).clear();
			buildRarefiedFile( f, indexes );
		}
		else
		{
			info( "Remove sample [" + sampleId + "] - contains less than minimum # reads (" + rarefyingMin + ")" );
			badSamples.add( sampleId );
		}
	}

	private void removeBadSamples() throws Exception
	{
		if( badSamples.isEmpty() )
		{
			info( "All samples rarefied & meet minimum read threshold - none will be ommitted..." );
			return;
		}

		info( "Removing samples below rarefying threshold" );
		info( "Removing bad samples ===> " + badSamples );

		final File newMapping = new File(
				getOutputDir().getAbsolutePath() + File.separator + config.getMetadata().getName() );
		final BufferedReader reader = new BufferedReader( new FileReader( config.getMetadata().getAbsolutePath() ) );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( newMapping ) );

		try
		{
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				final StringTokenizer st = new StringTokenizer( line, DELIM );
				final String id = st.nextToken();
				if( !badSamples.contains( id ) )
				{
					writer.write( line + "\n" );
				}
			}
		}
		finally
		{
			reader.close();
			writer.flush();
			writer.close();
			config.getMetaUtil().loadMetadata( newMapping, config.getDescriptor() );
		}

	}

	//	private void print( final Collection<Integer> indexes )
	//	{
	//		info( "PRINT FIRST 3 INDEXES..." );
	//		int i = 0;
	//		for( final Integer index: indexes )
	//		{
	//			if( i++ < 3 )
	//			{
	//				info( "ROW[" + i + "] = " + index );
	//			}
	//		}
	//	}
}
