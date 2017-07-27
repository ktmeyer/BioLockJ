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
 * This class builds the scripts used to call Metaphlan for classification of WGS data.
 */
public class MetaphlanClassifier extends ClassifierModule
{
	private static final String bowtie2ext = ".bowtie2.bz2";
	private static Map<String, String> taxaLevelMap = new HashMap<>();
	private String pythonExe = null;
	private String switches = null;

	static
	{
		taxaLevelMap.put( SPECIES, METAPHLAN_SPECIES );
		taxaLevelMap.put( GENUS, METAPHLAN_GENUS );
		taxaLevelMap.put( FAMILY, METAPHLAN_FAMILY );
		taxaLevelMap.put( ORDER, METAPHLAN_ORDER );
		taxaLevelMap.put( CLASS, METAPHLAN_CLASS );
		taxaLevelMap.put( PHYLUM, METAPHLAN_PHYLUM );
		taxaLevelMap.put( DOMAIN, METAPHLAN_DOMAIN );
	}

	/**
	 * Verify python exe is found and only valid params are configured in prop file.
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		pythonExe = requireString( EXE_PYTHON );
		switches = getProgramSwitches();

		if( switches.indexOf( "--input_type " ) > -1 )
		{
			throw new Exception( "Invalid classifier option (--input_type) found in property(" + EXE_CLASSIFIER_PARAMS
					+ "). BioLockJ derives this value by examinging one of the input files." );
		}
		if( switches.indexOf( "--nproc " ) > -1 )
		{
			throw new Exception( "Invalid classifier option (--nproc) found in property(" + EXE_CLASSIFIER_PARAMS
					+ "). BioLockJ derives this value from property: " + SCRIPT_NUM_THREADS );
		}
		if( switches.indexOf( "--bowtie2out " ) > -1 )
		{
			throw new Exception( "Invalid classifier option (--bowtie2out) found in property(" + EXE_CLASSIFIER_PARAMS
					+ "). BioLockJ outputs bowtie2out files to MetaphlanClassifier/temp." );
		}
		if( switches.indexOf( "-t rel_ab_w_read_stats " ) > -1 )
		{
			throw new Exception( "Invalid classifier option (-t rel_ab_w_read_stats). BioLockJ hard codes this "
					+ "option for MetaPhlAn so must not be included in the property file." );
		}
		if( switches.indexOf( "--tax_lev " ) > -1 )
		{
			throw new Exception( "Invalid classifier option (--tax_lev) found in property(" + EXE_CLASSIFIER_PARAMS
					+ "). BioLockJ sets this value based on: " + REPORT_TAXONOMY_LEVELS );
		}
		if( switches.indexOf( "-s " ) > -1 )
		{
			throw new Exception( "Invalid classifier option (-s) found in property(" + EXE_CLASSIFIER_PARAMS
					+ "). SAM output not supported.  BioLockJ outputs .tsv files." );
		}
		if( switches.indexOf( "-o " ) > -1 )
		{
			throw new Exception( "Invalid classifier option (-o) found in property(" + EXE_CLASSIFIER_PARAMS
					+ "). BioLockJ outputs results to: " + getOutputDir().getAbsolutePath() + File.separator );
		}

		setRankSwitch();
		addHardCodedSwitches();

	}

	/**
	 * Build scripts that use python to execute Metaphlan scripts.
	 */
	@Override
	protected List<List<String>> buildScript( final List<File> files ) throws Exception
	{
		final List<List<String>> data = new ArrayList<>();
		for( final File file: files )
		{
			final String fileId = trimSampleID( file.getName() );
			final String outputFile = getOutputDir().getAbsolutePath() + File.separator + fileId + PROCESSED;
			final String bowtie2Out = getTempDir().getAbsolutePath() + File.separator + fileId + bowtie2ext;
			final ArrayList<String> lines = new ArrayList<>();
			lines.add( pythonExe + " " + classifierExe + switches + file.getAbsolutePath() + " --bowtie2out "
					+ bowtie2Out + " > " + outputFile );
			data.add( lines );
		}

		return data;
	}

	/**
	 * Build scripts for paried reads that use python to execute Metaphlan scripts.
	 */
	@Override
	protected List<List<String>> buildScriptForPairedReads( final List<File> files ) throws Exception
	{
		final List<List<String>> data = new ArrayList<>();
		final Map<File, File> map = getPairedReads( files );
		for( final File file: map.keySet() )
		{
			final String fileId = trimSampleID( file.getName() );
			final String outputFile = getOutputDir().getAbsolutePath() + File.separator + fileId + PROCESSED;
			final String bowtie2Out = getTempDir().getAbsolutePath() + File.separator + fileId + bowtie2ext;
			final ArrayList<String> lines = new ArrayList<>();
			lines.add( pythonExe + " " + classifierExe + switches + file.getAbsolutePath() + ","
					+ map.get( file ).getAbsolutePath() + " --bowtie2out " + bowtie2Out + " > " + outputFile );
			data.add( lines );
		}

		return data;
	}

	/**
	 * All Metaphlan queries will set --input_type, --nproc, and -t
	 * @return
	 * @throws Exception
	 */
	protected Map<String, String> getMetaphlanHardCodedSwitches() throws Exception
	{
		final Map<String, String> metaphlanSwitches = new HashMap<>();
		metaphlanSwitches.put( "--input_type", inputSequenceType );
		metaphlanSwitches.put( "--nproc", new Integer( numThreads ).toString() );
		metaphlanSwitches.put( "-t", "rel_ab_w_read_stats" );
		return metaphlanSwitches;
	}

	/**
	 * Add getMetaphlanHardCodedSwitches() to switches value.
	 * @throws Exception
	 */
	private void addHardCodedSwitches() throws Exception
	{
		final Map<String, String> hardCodedSwitches = getMetaphlanHardCodedSwitches();
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
	 * Set the rankSwitch based on the configured taxonomyLevels in the prop file if only one
	 * taxonomy level is to be reported.
	 * @throws Exception
	 */
	private void setRankSwitch() throws Exception
	{
		if( taxonomyLevels.size() == 1 )
		{
			switches += "--tax_lev " + taxaLevelMap.get( taxonomyLevels.get( 0 ) ) + " ";
		}
	}
}