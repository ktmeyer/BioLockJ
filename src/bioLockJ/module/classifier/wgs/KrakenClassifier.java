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
package bioLockJ.module.classifier.wgs;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import bioLockJ.module.classifier.ClassifierModule;

/**
 * This class builds the Kraken classifier scripts.
 */
public class KrakenClassifier extends ClassifierModule
{
	private File krakenDatabase;
	private String switches;

	/**
	 * Verify the input switches are valid and the kraken database exists.
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		krakenDatabase = requireExistingFile( KRAKEN_DATABASE );
		switches = getProgramSwitches();

		if( switches.indexOf( "--fasta-input " ) > -1 )
		{
			switches.replaceAll( "--fasta-input", "" );
		}
		if( switches.indexOf( "--fastq-input " ) > -1 )
		{
			switches.replaceAll( "--fastq-input", "" );
		}
		if( switches.indexOf( "--threads " ) > -1 )
		{
			throw new Exception( "Invalid classifier option (--threads) found in property(" + EXE_CLASSIFIER_PARAMS
					+ "). BioLockJ derives this value from property: " + SCRIPT_NUM_THREADS );
		}
		if( switches.indexOf( "--paired " ) > -1 )
		{
			throw new Exception( "Invalid classifier option (--paired) found in property(" + EXE_CLASSIFIER_PARAMS
					+ "). BioLockJ derives this value from property: " + INPUT_PAIRED_READS );
		}
		if( switches.indexOf( "--output " ) > -1 )
		{
			throw new Exception( "Invalid classifier option (--output) found in property(" + EXE_CLASSIFIER_PARAMS
					+ "). BioLockJ hard codes this value based on Sample IDs found in: " + INPUT_DIRS );
		}
		if( switches.indexOf( "--db " ) > -1 )
		{
			throw new Exception( "Invalid classifier option (--db) found in property(" + EXE_CLASSIFIER_PARAMS
					+ "). BioLockJ hard codes this value based on Sample IDs found in: " + KRAKEN_DATABASE );
		}

		addHardCodedSwitches();
	}

	/**
	 * Build scripts that outputs initial Kraken classification files to tempDir.
	 * Next, call kraken-translate and output results to outputDir.
	 */
	@Override
	protected List<List<String>> buildScript( final List<File> files ) throws Exception
	{
		final List<List<String>> data = new ArrayList<>();
		for( final File file: files )
		{
			final String fileId = trimSampleID( file.getName() );
			final String tempFile = getTempDir().getAbsolutePath() + File.separator + fileId + KRAKEN_FILE;
			final String krakenOutput = getOutputDir().getAbsolutePath() + File.separator + fileId + PROCESSED;

			final ArrayList<String> lines = new ArrayList<>( 2 );

			lines.add( classifierExe + switches + "--output " + tempFile + " " + file.getAbsolutePath() );
			lines.add( classifierExe + "-translate --db " + krakenDatabase.getAbsolutePath() + " --mpa-format "
					+ tempFile + " > " + krakenOutput );

			data.add( lines );
		}

		return data;
	}

	/**
	 * Build scripts for paired reads that outputs initial Kraken classification files to tempDir.
	 * Next, call kraken-translate and output results to outputDir.
	 */
	@Override
	protected List<List<String>> buildScriptForPairedReads( final List<File> files ) throws Exception
	{
		final List<List<String>> data = new ArrayList<>();
		final Map<File, File> map = getPairedReads( files );
		for( final File file: map.keySet() )
		{
			final String fileId = trimSampleID( file.getName() );
			final String tempFile = getTempDir().getAbsolutePath() + File.separator + fileId + KRAKEN_FILE;
			final String krakenOutput = getOutputDir().getAbsolutePath() + File.separator + fileId + PROCESSED;

			final ArrayList<String> lines = new ArrayList<>( 2 );

			lines.add( classifierExe + " --db " + krakenDatabase.getAbsolutePath() + switches + "--output " + tempFile
					+ file.getAbsolutePath() + " " + map.get( file ).getAbsolutePath() );

			lines.add( classifierExe + "-translate --db " + krakenDatabase.getAbsolutePath() + " --mpa-format "
					+ tempFile + " > " + krakenOutput );

			data.add( lines );
		}

		return data;
	}

	/**
	 * Add hard coded switches to classifier switches value.
	 * @throws Exception
	 */
	private void addHardCodedSwitches() throws Exception
	{
		final Map<String, String> hardCodedSwitches = getHardCodedKrakenSwitches();
		for( final String key: hardCodedSwitches.keySet() )
		{
			String val = hardCodedSwitches.get( key ).trim();
			if( val.length() > 0 )
			{
				val = " " + val;
			}
			switches += key + val + " ";
		}
	}

	/**
	 * All calls to classifier requires setting number of threads, type of input files,
	 * set paired switch if needed, and finally, set the database param.
	 * @return
	 * @throws Exception
	 */
	private Map<String, String> getHardCodedKrakenSwitches() throws Exception
	{
		final Map<String, String> krakenSwitches = new HashMap<>();
		krakenSwitches.put( "--db", krakenDatabase.getAbsolutePath() );
		krakenSwitches.put( "--threads", new Integer( numThreads ).toString() );
		krakenSwitches.put( getInputSwitch(), "" );
		if( isPairedRead )
		{
			krakenSwitches.put( "--paired", "" );
		}

		return krakenSwitches;
	}

	/**
	 * Set the input switch based on inputSequenceType.
	 * @return
	 * @throws Exception
	 */
	private String getInputSwitch() throws Exception
	{
		if( inputSequenceType.equals( FASTA ) )
		{
			return "--fasta-input";
		}
		else if( inputSequenceType.equals( FASTQ ) )
		{
			return "--fastq-input";
		}
		else
		{
			throw new Exception( "Invalid input type.  Must be " + FASTA + " or " + FASTQ );
		}
	}
}