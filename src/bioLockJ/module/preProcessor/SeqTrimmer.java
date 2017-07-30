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
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import bioLockJ.Module;

/**
 * This utility trims primers configured using regular expressions.
 */
public class SeqTrimmer extends Module
{
	private static Set<String> fileNames = new HashSet<>();
	private static Hashtable<String, Integer> numLinesNoPrimer = new Hashtable<>();
	private static Hashtable<String, Integer> numLinesWithPrimer = new Hashtable<>();
	private static File trimSeqFile = null;
	private static boolean keepSeqsMissingPrimer = false;

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
		keepSeqsMissingPrimer = requireBoolean( INPUT_KEEP_SEQS_MISSING_PRIMER );
	}

	/**
	 * Will trim primers from fasta or fastq files (typcially fastq).
	 * @throws Exception
	 */
	@Override
	public void executeProjectFile() throws Exception
	{
		trimFileSeqs();
		
		TreeSet<String> ids = new TreeSet<String>( numLinesWithPrimer.keySet() );
		for( String name: fileNames )
		{
			boolean found = false;
			for( String id: ids )
			{
				if( name.startsWith( id ) )
				{
					found = true;
				}
			}
			if( !found )
			{
				warn( "File contains no primers!  File name: " + name );
			}
		}

		if( !keepSeqsMissingPrimer )
		{
			warn( INPUT_KEEP_SEQS_MISSING_PRIMER + "=Y so any sequences without a primer have been discarded" );
		}
		
		long totalPrimer = 0;
		long totalNoPrimer = 0;
		for( final String key: ids )
		{
			int a = numLinesWithPrimer.get( key );
			int b = numLinesNoPrimer.get( key );
			totalPrimer += a;
			totalNoPrimer += b;
			info( key + " reads with primer = " + a + "/" + ( a + b ) );
		}
		
		if( (totalPrimer + totalNoPrimer) > 1.0 )
		{
			DecimalFormat df = new DecimalFormat("##.##%");
			double ratio = totalNoPrimer/(totalPrimer + totalNoPrimer);
			String formattedPercent = df.format(ratio);
			info( "Percentage of reads missing primers = " + formattedPercent );
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
		final int target = isFastA() ? 2: 4;
		int fileCount = 0;
		info( "Trimming primers from " + count + " " + ( isFastA() ? FASTA: FASTQ ) + " files..." );
		for( final File file: files )
		{
			String fileName = file.getName();
			fileCount++;
			if( !fileName.contains( rvReadSuffix )  )
			{
				if( fileName.toLowerCase().endsWith( ".gz" ) )
				{
					fileName = fileName.substring( 0, fileName.length() - 3 );
				}
				fileNames.add( fileName );
			}
			
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
				int seqCount = 0;
				boolean validRecord = false;
				String[] seqLines = new String[ target ];
				
				for( String line = reader.readLine(); line != null; line = reader.readLine() )
				{
					boolean found = false;
					line = line.trim();

					if( isFastQ() )
					{
						if( ( lineCounter % target ) == 2 )
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
								validRecord = true;
								Integer x = numLinesWithPrimer.get( trimFileName );
								if( x == null )
								{
									x = 0;
								}
								numLinesWithPrimer.put( trimFileName, x + 1 );
							}

						}
						else if( ( lineCounter % target ) == 0 )
						{
							line = line.substring( seqLength );
						}
					}
					else
					{
						if( ( lineCounter % target ) == 0 )
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
									validRecord = true;
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
					
					
					seqLines[seqCount++] = line;
					if( seqCount == target )
					{
						if( keepSeqsMissingPrimer || validRecord )
						{
							for( int j=0; j<target; j++ )
							{
								writer.write( seqLines[j] + "\n" );
							}
						}
						seqCount = 0;
						validRecord = false;
					}
					
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

			if( ( ( fileCount + 1 ) % 25 ) == 0 )
			{
				info( "Done trimming " + fileCount + "/" + count + " files." );
			}
		}

		info( "Done trimming " + fileCount + "/" + count + " files." );
	}
}