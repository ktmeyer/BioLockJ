/**
 * @UNCC BINF 8380
 *
 * @author Michael Sioda
 * @date Jun 8, 2017
 */
package bioLockJ.module.classifier.r16s;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import bioLockJ.module.classifier.ClassifierModule;
import bioLockJ.module.classifier.r16s.qiime.QiimeMapping;
import bioLockJ.util.BashScriptUtil;

/**
 * QiimeClassifier is a superclass to several classes in the qiime package so hold shared methods
 * for these classes.  When called directly, OTUs have been picked so we add alphaDiversityMetrics
 * if configured and calls QIIME summary scripts.
 */
public class QiimeClassifier extends ClassifierModule
{
	private String alphaDiversityMetrics = null;
	private String awk = null;
	private String switches = null;

	/**
	 * Read in required QIIME prop values.
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		awk = requireString( EXE_AWK );
		alphaDiversityMetrics = getString( QIIME_ALPHA_DIVERSITY_METRICS );
	}

	/**
	 * Build bash scripts for input files.
	 */
	@Override
	public void executeProjectFile() throws Exception
	{
		BashScriptUtil.buildScripts( this, buildScript( getInputFiles() ), getInputFiles(), batchSize );
	}

	/**
	 * Subclasses call this method to input files to only those named OTU_TABLE in dir (and its subdirs).
	 */
	@Override
	public void initInputFiles( final File dir ) throws Exception
	{
		info( "Get input files from " + getName( dir ) + " named: " + OTU_TABLE );
		final IOFileFilter ff = new NameFileFilter( OTU_TABLE );
		setModuleInput( dir, ff, TrueFileFilter.INSTANCE );
	}

	/**
	 * Subclasses call this method to add lines to pick OTUs, first by adding labels and then by calling
	 * the configured OTU scirpt.
	 * @param lines
	 * @param fastaDir
	 * @param mapping
	 * @param outputDir
	 */
	protected void addPickOtuLines( final List<String> lines, final String fastaDir, final String mapping,
			final String outputDir )
	{
		final String fnaFile = outputDir + File.separator + COMBINED_FNA;
		lines.add( SCRIPT_ADD_LABELS + fastaDir + " -m " + mapping + " -c " + DEMUX_COLUMN + " -o " + outputDir );
		lines.add( qiimePickOtuScript + switches + "-i " + fnaFile + " -fo " + outputDir );
	}

	/**
	 * Build script that calls QIIME summary scripts.  If alphaDiversityMetrics are configured, add
	 * lines to add the metrics and also update the metadata descriptor to include the new columns.
	 */
	@Override
	protected List<List<String>> buildScript( final List<File> files ) throws Exception
	{
		final String outDir = getOutputDir().getAbsolutePath() + File.separator;
		final List<List<String>> data = new ArrayList<>();
		final List<String> lines = new ArrayList<>();

		final String line1 = SCRIPT_SUMMARIZE_TAXA + files.get( 0 ) + " -L " + getQiimeTaxaLevels() + " -o " + outDir
				+ OTU_DIR;

		final String line2 = SCRIPT_SUMMARIZE_BIOM + files.get( 0 ) + " -o " + outDir + OTU_SUMMARY_FILE;

		lines.add( line1 );
		lines.add( line2 );

		if( alphaDiversityMetrics != null )
		{
			final File newMapping = new File( outDir + QIIME_MAPPING );
			final String alphaLine1 = SCRIPT_CALC_ALPHA_DIVERSITY + files.get( 0 ) + " -m " + alphaDiversityMetrics
					+ " -o " + outDir + ALPHA_DIVERSITY_TABLE; // + " -t " + outDir +TAXA_TREE ;

			final String alphaLine2 = SCRIPT_ADD_ALPHA_DIVERSITY + config.getMetadata() + " -i " + outDir
					+ ALPHA_DIVERSITY_TABLE + " -o " + newMapping;

			lines.add( alphaLine1 );
			lines.add( alphaLine2 );

			QiimeMapping.addAlphaMetricsToDescriptor( getOutputDir() );
			config.setMetadata( newMapping );
		}

		data.add( lines );

		return data;
	}

	/**
	 * QIIME supports only unpaired reads, so if paired the mergeUtil must be used to merge the files
	 * so by the time this class is called, we are dealing with single fw reads, so call buildScripts,
	 * as used by single fw reads.
	 */
	@Override
	protected List<List<String>> buildScriptForPairedReads( final List<File> files ) throws Exception
	{
		return buildScript( files );
	}

	/**
	 * Subclasses call this method to check dependencies before picking OTUs.  Must verify
	 * conflicting params are not used.
	 * @throws Exception
	 */
	protected void checkOtuPickingDependencies() throws Exception
	{
		switches = getProgramSwitches();
		if( ( switches.indexOf( "-i " ) > -1 ) || ( switches.indexOf( "--input_fp " ) > -1 ) )
		{
			throw new Exception( "INVALID CLASSIFIER OPTION (-i or --input_fp) FOUND IN PROPERTY ("
					+ EXE_CLASSIFIER_PARAMS + "). PLEASE REMOVE.  INPUT DETERMINED BY: " + INPUT_DIRS );
		}
		if( ( switches.indexOf( "-o " ) > -1 ) || ( switches.indexOf( "--output_dir " ) > -1 ) )
		{
			throw new Exception( "INVALID CLASSIFIER OPTION (-o or --output_dir) FOUND IN PROPERTY ("
					+ EXE_CLASSIFIER_PARAMS + "). PLEASE REMOVE THIS VALUE FROM PROPERTY FILE. " );
		}
		if( ( switches.indexOf( "-a " ) > -1 ) || ( switches.indexOf( "-O " ) > -1 ) )
		{
			throw new Exception( "INVALID CLASSIFIER OPTION (-a or -O) FOUND IN PROPERTY (" + EXE_CLASSIFIER_PARAMS
					+ "). BIOLOCKJ DERIVES THIS VALUE FROM: " + SCRIPT_NUM_THREADS );
		}
		if( switches.indexOf( "-f " ) > -1 )
		{
			throw new Exception( "INVALID CLASSIFIER OPTION (-f or --force) FOUND IN PROPERTY (" + EXE_CLASSIFIER_PARAMS
					+ "). OUTPUT OPTIONS AUTOMATED BY BIOLOCKJ." );
		}

		addHardCodedSwitches();
	}

	/**
	 * Get the awk executable.
	 * @return
	 */
	protected String getAwk()
	{
		return awk;
	}

	/**
	 * Get program switches from the classifierParams prop value by adding correct number of dashes "-".
	 */
	@Override
	protected String getProgramSwitches() throws Exception
	{
		String formattedSwitches = " ";
		final List<String> switches = classifierParams;
		final Iterator<String> it = switches.iterator();
		while( it.hasNext() )
		{
			final String token = it.next();
			final StringTokenizer sToken = new StringTokenizer( token, " " );
			if( sToken.nextToken().length() == 1 )
			{
				formattedSwitches += "-" + token + " ";
			}
			else
			{
				formattedSwitches += "--" + token + " ";
			}
		}

		return formattedSwitches;
	}

	/**
	 * Subclasses that pick OTUs call this method to initialize the QIIME Mapping file from the output dir
	 * of the previous executor.  If the previous executor output a new qiime mapping (via VALIDATED_MAPPING),
	 * update the config.metadata file.  If this is a re-run attempt this method overrides the configured
	 * metadata & descriptor files in the prop file, with those found in the INPUT_DIR
	 * @param dir
	 * @throws Exception
	 */
	protected void initMappingFile( final File dir ) throws Exception
	{
		if( dir == null )
		{
			info( "Starting with QIIME Pick OTU step so must configure QIIME specific mapping file: " + METADATA_FILE
					+ " + descriptor: " + METADATA_DESCRIPTOR );
			return;
		}
		String searchTerm = "*" + VALIDATED_MAPPING;
		info( "Get mapping file from " + getName( dir ) + " ending in: " + searchTerm );
		IOFileFilter ff = new WildcardFileFilter( searchTerm );
		List<File> files = (List<File>) FileUtils.listFiles( dir, ff, TrueFileFilter.INSTANCE );
		int count = count( files );
		if( count == 0 )
		{
			throw new Exception( "Unable to find QIIME mapping file ending in " + VALIDATED_MAPPING
					+ " in input directory: " + dir.getAbsolutePath() );
		}
		else if( count > 1 )
		{
			throw new Exception( "Too many QIIME mapping files (total=" + count + ") found ending in " + searchTerm
					+ " in input directory: " + dir.getAbsolutePath() );
		}

		config.setMetadata( files.get( 0 ) );

		searchTerm = config.getDescriptor().getName();

		ff = new NameFileFilter( searchTerm );
		files = (List<File>) FileUtils.listFiles( dir, ff, null );
		count = count( files );
		if( count == 0 )
		{
			throw new Exception( "Unable to find QIIME descriptor named: " + searchTerm + " in input directory: "
					+ dir.getAbsolutePath() );
		}
		else if( count > 1 )
		{
			throw new Exception( "Too many QIIME descriptor files (total=" + count + ") found named " + searchTerm
					+ " in input directory: " + dir.getAbsolutePath() );
		}

		config.setDescriptor( files.get( 0 ) );

		config.getMetaUtil().loadMetadata( config.getMetadata(), config.getDescriptor() );

	}

	/**
	 * Overrid basic function, details output to qsub/bash output.
	 */
	@Override
	protected void logVersion()
	{
		// handled in bash script
	}

	/**
	 * Typically we verify no duplicate file names are used, but for QIIME we may be combining
	 * multiple files with the same name, so we skip this impl for QIIME classifier.
	 */
	@Override
	protected void validateFileNames( final Set<String> fileNames, final File f ) throws Exception
	{
		// Not needed for QIIME.  Multiple file named otu_table.biom & others exist.
	}

	/**
	 * Set the numThreads param.
	 * @throws Exception
	 */
	private void addHardCodedSwitches() throws Exception
	{
		switches += "-aO" + " " + numThreads + " ";
	}

	/**
	 * Set the taxa level indicators based on config taxonomyLevels.
	 * @return
	 * @throws Exception
	 */
	private String getQiimeTaxaLevels() throws Exception
	{
		String levels = "";
		if( taxonomyLevels.contains( DOMAIN ) )
		{
			levels += "1";
		}
		if( taxonomyLevels.contains( PHYLUM ) )
		{
			levels += ( levels.isEmpty() ? "": "," ) + "2";
		}
		if( taxonomyLevels.contains( CLASS ) )
		{
			levels += ( levels.isEmpty() ? "": "," ) + "3";
		}
		if( taxonomyLevels.contains( ORDER ) )
		{
			levels += ( levels.isEmpty() ? "": "," ) + "4";
		}
		if( taxonomyLevels.contains( FAMILY ) )
		{
			levels += ( levels.isEmpty() ? "": "," ) + "5";
		}
		if( taxonomyLevels.contains( GENUS ) )
		{
			levels += ( levels.isEmpty() ? "": "," ) + "6";
		}
		if( taxonomyLevels.contains( SPECIES ) )
		{
			levels += ( levels.isEmpty() ? "": "," ) + "7";
		}

		return levels;
	}
}