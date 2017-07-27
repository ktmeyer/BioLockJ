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
import java.util.List;
import bioLockJ.Module;

/**
 * This class updates headers
 */
public class Multiplexer extends Module
{
	private static int seqCount = 1;

	private static String getHeader( final String sampleId ) throws Exception
	{
		return sampleId + "_" + sampleId + "." + seqCount++ + " ";
	}

	/**
	 * No new dependencies
	 */
	@Override
	public void checkDependencies() throws Exception
	{

	}

	/**
	 * Will...
	 * @throws Exception
	 */
	@Override
	public void executeProjectFile() throws Exception
	{
		final List<File> files = getInputFiles();
		final int count = count( files );
		final int numRows = isFastA() ? 2: 4;
		final int target = isFastA() ? 0: 2;
		int numProced = 0;
		info( "Combining " + count + " files..." );

		final String newFileName = getOutputDir() + File.separator + "combinedSeqs." + FASTA;
		final File newFile = new File( newFileName );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( newFile ) );
		for( final File file: files )
		{
			final String sampleId = trimSampleID( file.getName() );
			final BufferedReader reader = getFileReader( file );
			seqCount = 1;
			try
			{
				int lineCounter = 1;
				for( String line = reader.readLine(); line != null; line = reader.readLine() )
				{
					line = line.trim();
					if( ( lineCounter % numRows ) == 1 )
					{
						line = ">" + getHeader( sampleId ) + line.substring( 1 );
						writer.write( line + "\n" );
					}
					else if( ( lineCounter % numRows ) == target )
					{
						writer.write( line + "\n" );
					}

					lineCounter++;
				}
			}
			catch( final Exception ex )
			{
				error( "Error combining file = " + file.getAbsolutePath(), ex );
			}
			finally
			{
				reader.close();
				writer.flush();
			}

			if( ( numProced++ % 25 ) == 0 )
			{
				info( "Done combining " + numProced + "/" + count + " files" );
			}
		}

		writer.flush();
		writer.close();

		info( "Done combining " + numProced + "/" + count + " files" );
	}
}
