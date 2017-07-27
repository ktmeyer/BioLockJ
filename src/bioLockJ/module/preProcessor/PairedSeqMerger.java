/**
 * @UNCC BINF 8380
 *
 * @author Michael Sioda
 * @date Jun 7, 2017
 */
package bioLockJ.module.preProcessor;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import bioLockJ.Module;
import bioLockJ.util.BashScriptUtil;

/**
 * This class will merge forward & reverse fastQ files.
 */
public class PairedSeqMerger extends Module
{

	public static String pear = null;
	public static List<String> pearParams = null;
	private static List<File> fwReads = new ArrayList<>();

	/**
	 * Verify pear props are valid and inputType is fastQ.
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		pear = requireString( EXE_PEAR );
		pearParams = getList( EXE_PEAR_PARAMS );

		if( !isFastQ() )
		{
			throw new Exception( "PAIRED READS CAN ONLY BE ASSEMBLED WITH <FASTQ> FILE INPUT" );
		}

		if( !moduleExists( pear ) )
		{
			requireExistingFile( EXE_PEAR );
		}
	}

	/**
	 * Create lines for the bash scripts.
	 */
	@Override
	public void executeProjectFile() throws Exception
	{
		final List<List<String>> mergedLines = getMergeLines( getInputFiles() );
		BashScriptUtil.buildScripts( this, mergedLines, fwReads, batchSize );
		isPairedRead = false;
	}

	/**
	 * Get merge file lines for the bash script.
	 * @param files
	 * @return
	 * @throws Exception
	 */
	private List<List<String>> getMergeLines( final List<File> files ) throws Exception
	{
		info( "Generating merge files from " + count( files ) + " total files." );

		final List<List<String>> data = new ArrayList<>();
		final Map<File, File> map = getPairedReads( files );
		final String tempDir = getTempDir().getAbsolutePath() + File.separator;
		final String outDir = getOutputDir().getAbsolutePath() + File.separator;
		final TreeSet<File> keys = new TreeSet<>( map.keySet() );
		final String params = getPearSwitches( pearParams );

		for( final File file: keys )
		{
			final List<String> lines = new ArrayList<>();
			fwReads.add( file );
			final String sampleId = trimSampleID( file.getName() );
			lines.add( pear + " -f " + file.getAbsolutePath() + " -r " + map.get( file ).getAbsolutePath() + " -o "
					+ tempDir + sampleId + params );
			lines.add( "mv " + tempDir + sampleId + MERGE_SUFFIX + " " + outDir + sampleId + "." + FASTQ );
			data.add( lines );
		}

		return data;
	}

	/**
	 * Get formatted pear switches as provided in prop file (if any).
	 * @param switches
	 * @return
	 * @throws Exception
	 */
	private String getPearSwitches( final List<String> switches ) throws Exception
	{
		String formattedSwitches = " ";
		final Iterator<String> it = switches.iterator();
		while( it.hasNext() )
		{
			formattedSwitches += "-" + it.next() + " ";
		}

		return formattedSwitches;
	}

}
