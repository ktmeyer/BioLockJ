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
package bioLockJ;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import bioLockJ.util.ConfigUtil;

/**
 * This class populates attributes corresponding to properties from ConfigUtil
 * and provides convenience methods to access property values and log output.
 */
public abstract class BioLockJ extends Constants
{
	protected static boolean addGenusFirstInitialToSpecies = false;
	protected static boolean addGenusNameToSpecies = false;
	protected static int batchSize = 0;
	protected static String chmod = null;
	protected static String classifierType = null;
	protected static String clusterCommand = null;
	protected static List<String> clusterModules = null;
	protected static String clusterParams = null;
	protected static String commentChar = null;
	protected static ConfigUtil config = null;
	protected static boolean copyInputDirs = false;
	protected static boolean deleteTempDirs = false;
	protected static boolean demultiplex = false;
	protected static String emptySpaceDelim = null;
	protected static boolean exitOnError = false;
	protected static List<String> filterAttributes = new ArrayList<>();
	protected static List<String> filterNaAttributes = new ArrayList<>();
	protected static String fwReadSuffix = null;
	protected static int histNumBreaks = 0;
	protected static List<String> ignoreInputFiles = null;
	protected static List<File> inputDirs = null;
	protected static String inputSequenceType = null;
	protected static String inputTrimPrefix = null;
	protected static String inputTrimSuffix = null;
	protected static boolean isPairedRead = false;
	protected static boolean isQiime = false;
	protected static Logger log = null;
	protected static String logBase = null;
	protected static boolean mergeOtuTables = false;
	protected static boolean mergePairs = false;
	protected static String nullChar = null;
	protected static int numThreads = 0;
	protected static boolean pickOtus = false;
	protected static boolean preProcess = false;
	protected static String qiimePickOtuScript = "";
	protected static Integer rarefyingMax = null;
	protected static Integer rarefyingMin = null;
	protected static boolean rarefySeqs = false;
	protected static List<String> reportAttributes = new ArrayList<>();
	protected static boolean reportNumHits = false;
	protected static boolean reportNumReads = false;
	protected static int rMaxTitleSize = 0;
	protected static String rPath = null;
	protected static boolean runClassifier = false;
	protected static boolean runOnCluster = false;
	protected static boolean runParser = false;
	protected static boolean runRscript = false;
	protected static String rvReadSuffix = null;
	protected static boolean sendEmail = false;
	protected static List<String> taxonomyLevels = null;
	protected static boolean trimSeqs = false;
	protected static boolean useFullNames = false;

	/**
	 * Initialize most required properties used in BioLockJ.  Missing requires property
	 * values will cause a failure here, rather than later in the program.
	 *
	 * @param configuration
	 * @throws Exception
	 */
	public static void initializeGlobalProps( final ConfigUtil configuration ) throws Exception
	{
		config = configuration;
		log = LoggerFactory.getLogger( BioLockJ.class );
		reportNumReads = requireBoolean( REPORT_NUM_READS );
		reportNumHits = requireBoolean( REPORT_NUM_HITS );
		exitOnError = requireBoolean( SCRIPT_EXIT_ON_ERROR );

		runClassifier = requireBoolean( CONTROL_RUN_CLASSIFIER );
		runParser = requireBoolean( CONTROL_RUN_PARSER );
		runRscript = requireBoolean( CONTROL_RUN_R_SCRIPT );
		runOnCluster = requireBoolean( CONTROL_RUN_ON_CLUSTER );
		trimSeqs = requireBoolean( CONTROL_TRIM_PRIMERS );
		mergePairs = requireBoolean( CONTROL_MERGE_PAIRS );
		rarefySeqs = requireBoolean( CONTROL_RAREFY_SEQS );

		deleteTempDirs = requireBoolean( PROJECT_DELETE_TEMP_FILES );
		sendEmail = requireBoolean( EMAIL_SEND_NOTIFICATION );
		copyInputDirs = requireBoolean( PROJECT_COPY_FILES );
		useFullNames = requireBoolean( REPORT_FULL_TAXONOMY_NAMES );
		isPairedRead = requireBoolean( INPUT_PAIRED_READS );

		demultiplex = requireBoolean( INPUT_DEMULTIPLEX );

		classifierType = requireString( PROJECT_CLASSIFIER_TYPE ).toUpperCase();
		chmod = requireString( SCRIPT_CHMOD_COMMAND );
		emptySpaceDelim = requireString( REPORT_EMPTY_SPACE_DELIM );

		numThreads = requirePositiveInteger( SCRIPT_NUM_THREADS );
		batchSize = requirePositiveInteger( SCRIPT_BATCH_SIZE );
		rarefyingMax = getPositiveInteger( INPUT_RAREFYING_MAX );
		rarefyingMin = getNonNegativeInteger( INPUT_RAREFYING_MIN );

		taxonomyLevels = requireList( REPORT_TAXONOMY_LEVELS );
		inputDirs = requireExistingDirectories( INPUT_DIRS );

		nullChar = getString( METADATA_NULL_VALUE );
		commentChar = getString( METADATA_COMMENT );
		inputTrimPrefix = getString( INPUT_TRIM_PREFIX );
		inputTrimSuffix = getString( INPUT_TRIM_SUFFIX );
		logBase = getString( R_LOG_BASE );

		clusterModules = getList( CLUSTER_MODULES );
		ignoreInputFiles = getList( INPUT_IGNORE_FILES );

		isQiime = ( classifierType.equals( QIIME ) );
		
		if( runOnCluster )
		{
			clusterCommand = requireString( CLUSTER_BATCH_COMMAND );
			clusterParams = requireString( CLUSTER_PARAMS );
			clusterModules = getList( CLUSTER_MODULES );
			verifyClusterParams();
		}

		if( trimSeqs || mergePairs || rarefySeqs || runClassifier )
		{
			inputSequenceType = getInputSequenceType();
		}

		if( runParser )
		{
			addGenusNameToSpecies = requireBoolean( REPORT_ADD_GENUS_NAME_TO_SPECIES );
			addGenusFirstInitialToSpecies = requireBoolean( REPORT_USE_GENUS_FIRST_INITIAL );
		}

		if( runRscript )
		{
			rPath = requireString( EXE_RSCRIPT );
			rMaxTitleSize = requirePositiveInteger( R_MAX_TITLE_SIZE );
		}

		if( isPairedRead )
		{
			fwReadSuffix = requireString( INPUT_FORWARD_READ_SUFFIX );
			rvReadSuffix = requireString( INPUT_REVERSE_READ_SUFFIX );
		}
		else
		{
			fwReadSuffix = getString( INPUT_FORWARD_READ_SUFFIX );
			rvReadSuffix = getString( INPUT_REVERSE_READ_SUFFIX );
		}

		if( runClassifier && isQiime )
		{
			mergeOtuTables = requireBoolean( QIIME_MERGE_OTU_TABLES );
			preProcess = requireBoolean( QIIME_PREPROCESS );
			pickOtus = requireBoolean( QIIME_PICK_OTUS );
			if( pickOtus )
			{
				qiimePickOtuScript = requireString( QIIME_PICK_OTU_SCRIPT );
			}
		}
	
	}
	
	/**
	 * Simply copy a file to a target directory.
	 * @param sourceFile
	 * @param targetDir
	 * @throws Exception
	 */
	protected static void copyFile( final File sourceFile, final String targetDir ) throws Exception
	{
		log.info( "Copy " + sourceFile.getAbsolutePath() + " > " + targetDir );
		final File destFile = new File( targetDir + sourceFile.getName() );
		if( !destFile.exists() )
		{
			destFile.createNewFile();
		}
		final FileInputStream fileInputStream = new FileInputStream( sourceFile );
		final FileOutputStream fileOutputStream = new FileOutputStream( destFile );
		FileChannel source = null, destination = null;
		try
		{
			source = fileInputStream.getChannel();
			destination = fileOutputStream.getChannel();
			destination.transferFrom( source, 0, source.size() );
		}
		finally
		{
			source.close();
			destination.close();
			fileInputStream.close();
			fileOutputStream.close();
		}
	}

	/**
	 * Count the number of files, return "0" if no files found.
	 * @param files
	 * @return
	 */
	protected static int count( final List<File> files )
	{
		return ( files == null ) ? 0: files.size();
	}

	/**
	 * Format integer to have a uniform number of digits, by adding leading zeros if needed.
	 * @param x
	 * @param numDigits
	 * @return
	 */
	protected static String formatInt( final int x, final int numDigits )
	{
		return String.format( "%0" + numDigits + "d", x );
	}

	/**
	 * Get a BufferedReader for standard text file or gzipped file.
	 * @param file
	 * @return
	 * @throws Exception
	 */
	protected static BufferedReader getFileReader( final File file ) throws Exception
	{
		return file.getName().toLowerCase().endsWith( ".gz" )
				? new BufferedReader( new InputStreamReader( new GZIPInputStream( new FileInputStream( file ) ) ) )
				: new BufferedReader( new FileReader( file ) );
	}

	/**
	 * Get property as list (must be comma dlimited in prop file)
	 * @param propertyName
	 * @return
	 */
	protected static List<String> getList( final String propertyName )
	{
		return config.getPropertyAsList( propertyName );
	}

	/**
	 * Get positive integer from prop file, if it exists, otherwise return null
	 * @param propertyName
	 * @return
	 * @throws Exception
	 */
	protected static Integer getNonNegativeInteger( final String propertyName ) throws Exception
	{
		final Integer val = getIntegerProp( propertyName );
		if( ( val != null ) && ( val < 0 ) )
		{
			throw new Exception( propertyName + " must contain a non-negative integer value if configured - "
					+ "instead, property value = " + ( ( val == null ) ? "null": val ) );
		}
		return val;
	}

	/**
	 * Get positive integer from prop file, if it exists, otherwise return null
	 * @param propertyName
	 * @return
	 * @throws Exception
	 */
	protected static Integer getPositiveInteger( final String propertyName ) throws Exception
	{
		final Integer val = getIntegerProp( propertyName );
		if( ( val != null ) && ( val < 1 ) )
		{
			throw new Exception( propertyName + " must contain a positive integer value if configured -  "
					+ "instead, property value = " + ( ( val == null ) ? "null": val ) );
		}
		return val;
	}

	/**
	 * Get required string value from ConfigRead4er.
	 * @param propertyName
	 * @return
	 */
	protected static String getString( final String propertyName )
	{
		return config.getAProperty( propertyName );
	}

	/**
	 * Boolean return TRUE if module exists in prop file.
	 * @param val
	 * @return
	 */
	protected static boolean moduleExists( final String val )
	{
		for( final String module: clusterModules )
		{
			if( exists( clusterCommand ) && module.contains( val ) )
			{
				return true;
			}
		}

		return false;
	}

	/**
	 * Requires boolean in prop file.
	 * @param propertyName
	 * @return
	 * @throws Exception
	 */
	protected static boolean requireBoolean( final String propertyName ) throws Exception
	{
		String val = config.getAProperty( propertyName );

		if( ( val == null ) || val.isEmpty() )
		{
			throwPropNotFoundException( propertyName );
		}

		val = val.toUpperCase();

		if( val.equals( TRUE ) )
		{
			return true;
		}

		if( val.equals( FALSE ) )
		{
			return false;
		}

		throw new Exception( propertyName + " MUST BE SET TO EITHER " + TRUE + " or " + FALSE );

	}

	/**
	 * Get list of required directories.
	 *
	 * @param propertyName
	 * @return
	 * @throws Exception
	 */
	protected static List<File> requireExistingDirectories( final String propertyName ) throws Exception
	{
		final List<String> dirs = requireList( propertyName );
		final List<File> returnDirs = new ArrayList<>();

		for( final String dirName: dirs )
		{
			final File dir = new File( dirName );
			if( !dir.exists() || !dir.isDirectory() )
			{
				throw new Exception( dir.getAbsolutePath() + " is not a directory! " );
			}
			returnDirs.add( dir );
		}

		return returnDirs;
	}

	/**
	 * Get required existing directory.
	 *
	 * @param propertyName
	 * @return
	 * @throws Exception
	 */
	protected static File requireExistingDirectory( final String propertyName ) throws Exception
	{
		final String val = config.getAProperty( propertyName );

		if( ( val == null ) || val.isEmpty() )
		{
			throwPropNotFoundException( propertyName );
		}

		final File aFile = new File( val );

		if( !aFile.exists() || !aFile.isDirectory() )
		{
			throw new Exception( aFile.getAbsolutePath() + " is not a directory! " );
		}

		return aFile;
	}

	/**
	 * Get required file.
	 *
	 * @param propertyName
	 * @return
	 * @throws Exception
	 */
	protected static File requireExistingFile( final String propertyName ) throws Exception
	{
		final String val = config.getAProperty( propertyName );

		if( ( val == null ) || val.isEmpty() )
		{
			throwPropNotFoundException( propertyName );
		}

		final File aFile = new File( val );

		if( !aFile.exists() )
		{
			throw new Exception( aFile.getAbsolutePath() + " is not an existing file! " );
		}

		return aFile;
	}
	
	/**
	 * Get a file - if it exists
	 *
	 * @param propertyName
	 * @return
	 * @throws Exception
	 */
	protected static File getExistingFile( final String propertyName ) throws Exception
	{
		final String val = config.getAProperty( propertyName );

		if( ( val == null ) || val.isEmpty() )
		{
			return null;
		}

		final File aFile = new File( val );

		if( !aFile.exists() )
		{
			return null;
		}

		return aFile;
	}
	

	/**
	 * Get required list (must be comma delimited value in prop file).
	 * @param propertyName
	 * @return
	 * @throws Exception
	 */
	protected static List<String> requireList( final String propertyName ) throws Exception
	{
		final List<String> val = config.getPropertyAsList( propertyName );

		if( ( val == null ) || val.isEmpty() )
		{
			throwPropNotFoundException( propertyName );
		}

		return val;
	}

	/**
	 * Get required positive integer from prop file.
	 * @param propertyName
	 * @return
	 * @throws Exception
	 */
	protected static int requirePositiveInteger( final String propertyName ) throws Exception
	{
		final String val = config.getAProperty( propertyName );
		if( ( val == null ) || val.trim().isEmpty() )
		{
			throwPropNotFoundException( propertyName );
		}

		Integer posInt = null;
		try
		{
			posInt = Integer.parseInt( val );
		}
		catch( final Exception ex )
		{
			log.error( ex.getMessage(), ex );
		}

		if( ( posInt == null ) || ( posInt < 1 ) )
		{
			throw new Exception( propertyName + " must be a positive integer value!" );
		}

		return posInt;
	}

	/**
	 * Get required String value from prop file.
	 * @param propertyName
	 * @return
	 * @throws Exception
	 */
	protected static String requireString( final String propertyName ) throws Exception
	{
		final String val = config.getAProperty( propertyName );

		if( ( val == null ) || val.trim().isEmpty() )
		{
			throwPropNotFoundException( propertyName );
		}

		return val.trim();
	}

	/**
	 * Utility method to remove quotes from String.
	 * @param inString
	 * @return
	 */
	protected static String stripQuotes( final String inString )
	{
		final StringBuffer buff = new StringBuffer();

		for( int x = 0; x < inString.length(); x++ )
		{
			final char c = inString.charAt( x );

			if( c != '\"' )
			{
				buff.append( c );
			}
		}

		return buff.toString();
	}

	/**
	 * Convenience methods that validates property exists, checking for null in code
	 * is less readable
	 *
	 * @param val
	 * @return
	 */
	private static boolean exists( final String val )
	{
		if( ( val != null ) && ( val.trim().length() > 0 ) )
		{
			return true;
		}

		return false;
	}

	/**
	 * Determine if fasta or fastq by checking input file format.
	 * @return
	 * @throws Exception
	 */
	private static String getInputSequenceType() throws Exception
	{
		BufferedReader reader = null;
		boolean foundIgnore = false;
		File testFile = null;
		for( final File f: inputDirs.get( 0 ).listFiles() )
		{
			final String name = f.getName();
			if( !name.startsWith( "." ) && !ignoreInputFiles.contains( name ) )
			{
				reader = getFileReader( f );
				testFile = f;
				break;
			}
			foundIgnore = true;
		}

		if( reader == null )
		{
			throw new Exception( "No input files found in: " + INPUT_DIRS
					+ ( foundIgnore ? ( " other than files listed in: " + ignoreInputFiles ): "" ) );
		}

		final String testChar = reader.readLine().trim().substring( 0, 1 );
		log.debug( "First character of test input sequence file: " + testChar );
		if( testChar.equals( ">" ) || testChar.equals( ";" ) )
		{
			return FASTA;
		}
		else if( testChar.equals( "@" ) )
		{
			return FASTQ;
		}

		throw new Exception( "Invalid file format!  Input File = " + testFile.getAbsolutePath()
				+ "\n Input files must be FASTA or FASTQ format.  "
				+ "FASTA must begin with \">\" or \";\" and  FASTQ must begin with \"@\"" );

	}

	/**
	 * Get integer value from config file, if it exists, otherwise return null
	 * @param propertyName
	 * @return integer value or null
	 * @throws Exception
	 */
	private static Integer getIntegerProp( final String propertyName ) throws Exception
	{
		final String val = config.getAProperty( propertyName );
		if( ( val == null ) || val.trim().isEmpty() )
		{
			return null;
		}

		Integer intVal = null;
		try
		{
			intVal = Integer.parseInt( val );
		}
		catch( final Exception ex )
		{
			log.error( ex.getMessage(), ex );
		}

		if( ( intVal == null ) )
		{
			throw new Exception( propertyName + " must contain an integer value if configured!" );
		}

		return intVal;

	}

	/**
	 * Generic exeception to throw when a property cannot be found in prop file.
	 * @param prop
	 * @throws Exception
	 */
	private static void throwPropNotFoundException( final String prop ) throws Exception
	{
		throw new Exception( prop + " undefined in: " + config.getPropertiesFile().getAbsolutePath() );

	}

	/**
	 * Validate cluster params num threads matches, numThreads defined in prop file.
	 * Format for UNCC HPC Cluster: #PBS -l procs=1,mem=8GB
	 * @throws Exception
	 */
	private static void verifyClusterParams() throws Exception
	{
		if( !requireBoolean( CLUSTER_VALIDATE_PARAMS ) )
		{
			return;
		}

		final StringTokenizer st = new StringTokenizer( clusterParams, "," );
		while( st.hasMoreTokens() )
		{
			String token = st.nextToken();
			if( token.contains( CLUSTER_NUM_PROCESSORS ) )
			{
				final StringTokenizer pToken = new StringTokenizer( token, "=" );
				while( pToken.hasMoreTokens() )
				{
					token = pToken.nextToken().trim();
					if( !token.contains( CLUSTER_NUM_PROCESSORS ) ) // only check right size of "="
					{
						final int numClusterProcs = Integer.valueOf( token );
						if( numClusterProcs != numThreads )
						{
							throw new Exception( "Inconsistant config values. " + SCRIPT_NUM_THREADS + "=" + numThreads
									+ "; " + CLUSTER_PARAMS + "=" + clusterParams + " (#" + CLUSTER_NUM_PROCESSORS + "="
									+ numClusterProcs + ")" );
						}
						break;
					}
				}
			}
		}
	}

	/**
	 * Convenience method to output debug with className.
	 * @param line
	 */
	protected void debug( final String line )
	{
		log.debug( getClass().getSimpleName() + " " + line + "\n" );
	}

	/**
	 * Convenience method to output debug with className.
	 * @param line
	 */
	protected void error( final String line, final Exception ex )
	{
		log.error( getClass().getSimpleName() + " " + line, ex );
	}

	/**
	 * Convenience method to output debug with className.
	 * @param line
	 */
	protected void info( final String line )
	{
		log.info( getClass().getSimpleName() + " " + line );
	}

	/**
	 * Convenience method to output debug with className.
	 * @param line
	 */
	protected void warn( final String line )
	{
		log.warn( getClass().getSimpleName() + " " + line );
	}

}
