/**
 * @UNCC BINF 8380
 *
 * @author Michael Sioda
 * @date Jun 7, 2017
 */
package bioLockJ.module.preProcessor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import bioLockJ.Module;

/**
 * This utility trims primers configured using regular expressions.
 */
public class SeqTrimmer extends Module
{
	private static Hashtable<String, Integer> numLinesNoPrimer = new Hashtable<>();
	private static Hashtable<String, Integer> numLinesWithPrimer = new Hashtable<>();
	private static File trimSeqFile = null;

	/**
	 * Verify file containing primers to trim exists.
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		trimSeqFile = new File( requireString( INPUT_TRIM_SEQ_PATH ) );
		if( !trimSeqFile.exists() )
		{
			throw new Exception( "File configured in property " + INPUT_TRIM_SEQ_PATH + " does not exist!" );
		}
	}

	/**
	 * Will trim primers from fasta or fastq files (typcially fastq).
	 * @throws Exception
	 */
	@Override
	public void executeProjectFile() throws Exception
	{

		trimFileSeqs();

		for( final String key: numLinesWithPrimer.keySet() )
		{
			warn( "# Primers removed from [" + key + "] = " + numLinesWithPrimer.get( key ) );
		}

		for( final String key: numLinesNoPrimer.keySet() )
		{
			warn( "# Missing Primers in [" + key + "] = " + numLinesNoPrimer.get( key ) );
		}

	}

	private Set<String> getSeqs() throws Exception
	{
		final Set<String> seqs = new HashSet<>();
		final BufferedReader reader = getFileReader( trimSeqFile );
		try
		{
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				if( !line.startsWith( "#" ) )
				{
					final String seq = line.trim();
					if( seq.length() > 0 )
					{
						info( "Found primer to trim: " + seq );
						seqs.add( seq );
					}
				}
			}
		}
		finally
		{
			reader.close();
		}

		if( seqs.size() < 1 )
		{
			throw new Exception( "No Primers found in: " + trimSeqFile.getAbsolutePath() );
		}

		return seqs;
	}

	private void trimFileSeqs() throws Exception
	{
		final Set<String> seqs = getSeqs();
		final List<File> files = getInputFiles();
		final int count = count( files );
		final String fileExt = "." + ( isFastA() ? FASTA: FASTQ );
		final int numTrimmed = 0;
		info( "Trimming primers from " + count + " " + ( isFastA() ? FASTA: FASTQ ) + " files..." );
		for( final File file: files )
		{
			final String trimFileName;
			if( isFastQ() )
			{
				String suffix = "";
				if( isPairedRead )
				{
					if( isForwardRead( file.getName() ) )
					{
						suffix = fwReadSuffix;
					}
					else
					{
						suffix = rvReadSuffix;
					}
				}

				trimFileName = getOutputDir() + File.separator + trimSampleID( file.getName() ) + suffix + fileExt;
			}
			else
			{
				trimFileName = getOutputDir() + File.separator + trimSampleID( file.getName() ) + fileExt;
			}

			final File trimmedFile = new File( trimFileName );
			final BufferedReader reader = getFileReader( file );
			final BufferedWriter writer = new BufferedWriter( new FileWriter( trimmedFile ) );
			try
			{
				int lineCounter = 1;
				int seqLength = 0;
				for( String line = reader.readLine(); line != null; line = reader.readLine() )
				{
					boolean found = false;
					line = line.trim();

					if( isFastQ() )
					{
						if( ( lineCounter % 4 ) == 2 )
						{
							seqLength = 0;
							for( final String seq: seqs )
							{
								seqLength = line.length();
								line = line.replaceFirst( seq, "" );
								if( seqLength != line.length() )
								{
									seqLength = seqLength - line.length();
									found = true;
									break;
								}
							}

							if( !found )
							{
								seqLength = 0;
								Integer x = numLinesNoPrimer.get( trimFileName );
								if( x == null )
								{
									x = 0;
								}
								numLinesNoPrimer.put( trimFileName, x + 1 );
							}
							else
							{
								Integer x = numLinesWithPrimer.get( trimFileName );
								if( x == null )
								{
									x = 0;
								}
								numLinesWithPrimer.put( trimFileName, x + 1 );
							}

						}
						else if( ( lineCounter % 4 ) == 0 )
						{
							line = line.substring( seqLength );
						}
					}
					else
					{
						if( ( lineCounter % 2 ) == 0 )
						{
							seqLength = 0;
							for( final String seq: seqs )
							{
								seqLength = line.length();
								line = line.replaceFirst( seq, "" );
								if( seqLength != line.length() )
								{
									seqLength = seqLength - line.length();
									found = true;
									break;
								}

								if( !found )
								{
									seqLength = 0;
									Integer x = numLinesNoPrimer.get( trimFileName );
									if( x == null )
									{
										x = 0;
									}
									numLinesNoPrimer.put( trimFileName, x + 1 );
								}
								else
								{
									Integer x = numLinesWithPrimer.get( trimFileName );
									if( x == null )
									{
										x = 0;
									}
									numLinesWithPrimer.put( trimFileName, x + 1 );
								}
							}
						}
					}

					writer.write( line + "\n" );
					lineCounter++;
				}
			}
			catch( final Exception ex )
			{
				error( "Error removing primers from file = " + file.getAbsolutePath(), ex );
			}
			finally
			{
				reader.close();
				writer.flush();
				writer.close();
			}

			if( ( ( numTrimmed + 1 ) % 25 ) == 0 )
			{
				info( "Done trimming " + numTrimmed + "/" + count + " files." );
			}
		}

		info( "Done trimming " + numTrimmed + "/" + count + " files." );
	}
}