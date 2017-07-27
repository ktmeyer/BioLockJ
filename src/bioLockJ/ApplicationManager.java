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
 * 				GNU General Public License for more details at http://www.gnu.org
 */
package bioLockJ;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import bioLockJ.module.classifier.r16s.QiimeClassifier;
import bioLockJ.module.classifier.r16s.RdpClassifier;
import bioLockJ.module.classifier.r16s.qiime.ClosedRefClassifier;
import bioLockJ.module.classifier.r16s.qiime.DeNovoClassifier;
import bioLockJ.module.classifier.r16s.qiime.MergeOtuTables;
import bioLockJ.module.classifier.r16s.qiime.OpenRefClassifier;
import bioLockJ.module.classifier.r16s.qiime.QiimePreprocessor;
import bioLockJ.module.classifier.wgs.KrakenClassifier;
import bioLockJ.module.classifier.wgs.MetaphlanClassifier;
import bioLockJ.module.classifier.wgs.SlimmClassifier;
import bioLockJ.module.parser.r16s.QiimeParser;
import bioLockJ.module.parser.r16s.RdpParser;
import bioLockJ.module.parser.wgs.KrakenParser;
import bioLockJ.module.parser.wgs.MetaphlanParser;
import bioLockJ.module.parser.wgs.SlimmParser;
import bioLockJ.module.postProcessor.RScriptBuilder;
import bioLockJ.module.preProcessor.PairedSeqMerger;
import bioLockJ.module.preProcessor.Rarefier;
import bioLockJ.module.preProcessor.SeqTrimmer;
import bioLockJ.util.ConfigUtil;
import bioLockJ.util.MailUtil;
import bioLockJ.util.MetadataUtil;
import bioLockJ.util.ProcessUtil;

/**
 * This is the main program used to control top level execution.
 */
public class ApplicationManager extends BioLockJ
{
	private static final HashMap<String, String> classifierMap = new HashMap<>();
	private static final List<File> failures = new ArrayList<>();
	private static Logger log = null;
	private static final HashMap<String, String> parserMap = new HashMap<>();
	private static int pollCounter = 0;
	private static final int pollTime = 60;
	private static final List<String> runTimes = new ArrayList<>();

	private static final long startTime = System.currentTimeMillis();

	private static String statusMsg = "";

	/**
	 * Get the running list of failures.
	 * @return
	 */
	public static List<File> getFailures()
	{
		return Collections.unmodifiableList( failures );
	}

	/**
	 * The main method is the first method called when BioLockJ is run. Here we
	 * read property file, copy it to project directory, initialize ConfigUtil
	 * and call runProgram().
	 *
	 * If the password param is given, the password is envrypted & stored to the
	 * prop file.
	 *
	 * @param args - args[0] path to property file - args[1] cleartext admin
	 *        email password
	 */
	public static void main( final String[] args )
	{
		try
		{
			if( ( args == null ) || ( args.length < 1 ) || ( args.length > 2 ) )
			{
				System.out.println( "USAGE: BIOLOCKJ <PROP_FILE_PATH>\n"
						+ "USAGE: BIOLOCKJ <PROP_FILE_PATH> <NEW_EMAIL_PASSWORD>\n" + "TERMINATE PROGRAM" );
				System.exit( 1 );
			}

			final File propFile = new File( args[ 0 ] );
			if( !propFile.exists() || propFile.isDirectory() )
			{
				throw new Exception( propFile.getAbsolutePath() + " is not a valid file" );
			}

			final ConfigUtil configUtil = new ConfigUtil( propFile );
			buildNewProject( configUtil );

			if( args.length == 2 )
			{
				MailUtil.getMailUtil().encryptAndStoreEmailPassword( args[ 1 ] );
				return;
			}

			copyFile( propFile, getProjectDir() );

			runProgram();
		}
		catch( final Exception ex )
		{
			ex.printStackTrace();
			if( log != null )
			{
				log.error( ex.getMessage(), ex );
			}
		}
		finally
		{
			try
			{
				if( sendEmail )
				{
					MailUtil.getMailUtil().sendEmailNotification( getSummary() );
				}
			}
			catch( final Exception ex )
			{
				ex.printStackTrace();
				log.error( "Error occurred sending email! ", ex );
			}
		}
	}

	/**
	 * Add an executor to the execution list.  Set the input dir to the previous executor's output if
	 * it exists.  Otherwise, initialize input files from the prop file input.dirs param.
	 *
	 * @param name
	 * @param list
	 * @param count
	 * @throws Exception
	 */
	private static void addExecutor( final String name, final List<Module> list, final int count ) throws Exception
	{
		final Module executor = (Module) Class.forName( name ).newInstance();
		executor.setExecutorDir( executor.getClass().getSimpleName(), count );
		if( list.isEmpty() )
		{
			executor.initInputFiles( null );
		}
		else
		{
			executor.setInputDir( list.get( list.size() - 1 ).getOutputDir() );
		}

		list.add( executor );
	}

	/**
	 * Create the project root dir, set log file name, and set props to save the values.
	 * @throws Exception
	 */
	private static void buildNewProject( final ConfigUtil configUtil ) throws Exception
	{
		final String projectRoot = configUtil.getAProperty( PROJECTS_DIR );
		//System.out.println( "projectRoot = " +  projectRoot );
		if( ( projectRoot == null ) || ( projectRoot.trim().length() < 1 ) )
		{
			throw new Exception( "Required property missing: " + PROJECTS_DIR );
		}

		final File root = new File( projectRoot );
		if( !root.exists() )
		{
			root.mkdirs();
		}

		final String projectDir = createProjectDir( configUtil, projectRoot );
		final String logFileName = projectDir + configUtil.getAProperty( PROJECT_NAME ) + ".log";
		System.setProperty( LOG_FILE, logFileName );
		configUtil.setProperty( LOG_FILE, logFileName );
		configUtil.setProperty( ROOT_DIR, projectDir );
		log = LoggerFactory.getLogger( ApplicationManager.class );

		initializeGlobalProps( configUtil );
		configUtil.setMetaUtil( new MetadataUtil() );
		initializeMaps();
		log.info( "BioLockJ initialized" );
	}

	/**
	 * If user prop indicates they need a copy of the input files, copy them to the project dir.
	 *
	 * @throws Exception
	 */
	private static void copyInputDirs() throws Exception
	{
		final File copyDir = new File( getProjectDir() + File.separator + "input" );
		copyDir.mkdir();
		for( final File srcDir: inputDirs )
		{
			log.info( "Copying input files from " + srcDir + " to " + copyDir );
			FileUtils.copyDirectory( srcDir, copyDir );
		}
	}

	/**
	 * This method creates the subdir under projects by attaching a timestamp to the project name.
	 * Check to ensure the output dir is unique, otherwise pause & generate a new timestamp.
	 * @param bljRoot
	 * @return
	 * @throws Exception
	 */
	private static String createProjectDir( final ConfigUtil configUtil, String projectRoot ) throws Exception
	{
		if( !projectRoot.endsWith( File.separator ) )
		{
			projectRoot += File.separator;
		}

		final String projectName = configUtil.getAProperty( PROJECT_NAME );
		//System.out.println( "projectName = " + projectName );
		final String pathToProj = projectRoot + projectName;

		File projectDir = null;
		while( ( projectDir == null ) || projectDir.exists() )
		{
			final String runTimeStamp = new SimpleDateFormat( "yyyyMMdd_kkmmss" ).format( new Date() );
			projectDir = new File( pathToProj + "_" + runTimeStamp );
			if( projectDir.exists() )
			{
				Thread.sleep( 1000 );
			}
		}

		if( !projectDir.mkdirs() )
		{
			throw new Exception( "ERROR: Unable to create: " + projectDir.getAbsolutePath() );
		}

		return projectDir.getAbsolutePath() + File.separator;
	}

	/**
	 * Execute the Module scripts (if any).
	 *
	 * @param module
	 * @throws Exception
	 */
	private static void executeAndWaitForScriptsIfAny( final Module module ) throws Exception
	{
		module.executeProjectFile();
		if( module.hasScripts() )
		{
			executeCHMOD( module.getScriptDir() );
			executeFile( module.getMainScript() );
			pollAndSpin( module.getScriptFiles(), module.getMainScript() );
		}
	}

	/**
	 * Execute the chmod param to make the new bash scripts executable.
	 * @param scriptDir
	 * @throws Exception
	 */
	private static void executeCHMOD( final File scriptDir ) throws Exception
	{

		final File[] listOfFiles = scriptDir.listFiles();
		for( final File file: listOfFiles )
		{
			if( !file.getName().startsWith( "." ) )
			{
				ProcessUtil.submit( getArgs( chmod, file.getAbsolutePath() ) );
			}
		}
	}

	/**
	 * Execute the given script via ProcessUtil.
	 * @param script
	 * @throws Exception
	 */
	private static void executeFile( final File script ) throws Exception
	{
		if( script == null )
		{
			return;
		}

		String[] cmd = new String[ 1 ];
		cmd[ 0 ] = script.getAbsolutePath();

		if( script.getName().endsWith( RScriptBuilder.R_SCRIPT_NAME ) )
		{
			cmd = new String[ 2 ];
			cmd[ 0 ] = rPath;
			cmd[ 1 ] = script.getAbsolutePath();
		}

		log.info( "Executing Script: " + script.getName() );

		ProcessUtil.submit( cmd );
	}

	/**
	 * Populate args to pass to ProcessUtil.
	 * @param command
	 * @param filePath
	 * @return
	 */
	private static String[] getArgs( final String command, final String filePath )
	{
		final StringTokenizer sToken = new StringTokenizer( command + " " + filePath );
		final List<String> list = new ArrayList<>();
		while( sToken.hasMoreTokens() )
		{
			list.add( sToken.nextToken() );
		}

		final String[] args = new String[ list.size() ];
		for( int x = 0; x < list.size(); x++ )
		{
			args[ x ] = list.get( x );
		}

		return args;
	}

	/**
	 * Get the list of executors based on the prop file control flags.
	 * @return
	 * @throws Exception
	 */
	private static List<Module> getListToRun() throws Exception
	{
		final List<Module> list = new ArrayList<>();
		int count = 0;

		//		if( count == 0 )
		//		{
		//			addExecutor( PrepData.class.getName(), list, count++ );
		//			addExecutor( Multiplexer.class.getName(), list, count++ );
		//			return list;
		//		}

		final boolean qiimeClosedRef = qiimePickOtuScript.equals( SCRIPT_PICK_CLOSED_REF_OTUS );
		final boolean qiimeOpenRef = qiimePickOtuScript.equals( SCRIPT_PICK_OPEN_REF_OTUS );
		final boolean qiimeDeNovo = qiimePickOtuScript.equals( SCRIPT_PICK_OPEN_REF_OTUS );

		if( runClassifier && copyInputDirs )
		{
			copyInputDirs();
		}

		if( runClassifier && trimSeqs )
		{
			addExecutor( SeqTrimmer.class.getName(), list, count++ );
		}

		if( runClassifier && mergePairs )
		{
			addExecutor( PairedSeqMerger.class.getName(), list, count++ );
		}

		if( runClassifier && rarefySeqs )
		{
			addExecutor( Rarefier.class.getName(), list, count++ );
		}

		if( runClassifier && preProcess )
		{
			addExecutor( QiimePreprocessor.class.getName(), list, count++ );
		}

		if( runClassifier && qiimeClosedRef )
		{
			if( pickOtus )
			{
				addExecutor( ClosedRefClassifier.class.getName(), list, count++ );
			}

			if( mergeOtuTables )
			{
				addExecutor( MergeOtuTables.class.getName(), list, count++ );
			}
		}
		else if( runClassifier && qiimeOpenRef && pickOtus )
		{
			addExecutor( OpenRefClassifier.class.getName(), list, count++ );
		}
		else if( runClassifier && qiimeDeNovo && pickOtus )
		{
			addExecutor( DeNovoClassifier.class.getName(), list, count++ );
		}

		if( runClassifier )
		{
			addExecutor( classifierMap.get( classifierType ), list, count++ );
		}

		if( runParser )
		{
			addExecutor( parserMap.get( classifierType ), list, count++ );
		}

		if( runRscript )
		{
			addExecutor( RScriptBuilder.class.getName(), list, count++ );
		}

		return list;
	}

	/**
	 * Get the project dir.
	 * @return
	 */
	private static String getProjectDir()
	{
		return config.getAProperty( ROOT_DIR );
	}

	/**
	 * Get runtime message based on startTime passed.
	 *
	 * @param title
	 * @param startTime
	 * @return
	 */
	private static String getRunTime( final String title, final long startTime )
	{
		final long runTime = System.currentTimeMillis() - startTime;
		final String format = String.format( "%%0%dd", 2 );
		final long elapsedTime = runTime / 1000;
		final String seconds = String.format( format, elapsedTime % 60 );
		final String minutes = String.format( format, ( elapsedTime % 3600 ) / 60 );
		final String hours = String.format( format, elapsedTime / 3600 );
		return title + " = " + hours + " hours : " + minutes + " minutes : " + seconds + " seconds";
	}

	/**
	 * Summary of runtimes to be output to log file & to be included in summary email.
	 * @return
	 */
	private static String getSummary()
	{
		final StringBuffer sb = new StringBuffer();
		if( log != null )
		{
			log.info( LOG_SPACER );
			log.info( "Main Program Complete!" );
			log.info( LOG_SPACER );
			sb.append( LOG_SPACER + "\n" );
			for( final String runTimeOutput: runTimes )
			{
				sb.append( runTimeOutput + "\n" );
				log.info( runTimeOutput );
			}
			final String totalRuntime = getRunTime( "Total Runtime", startTime );
			sb.append( LOG_SPACER + "\n" );
			sb.append( totalRuntime + "\n" );
			sb.append( LOG_SPACER + "\n" );
			log.info( LOG_SPACER );
			log.info( totalRuntime );
			log.info( LOG_SPACER );
		}
		return sb.toString();
	}

	/**
	 * Set classifier & parser maps.
	 */
	private static void initializeMaps()
	{
		classifierMap.put( RDP, RdpClassifier.class.getName() );
		classifierMap.put( QIIME, QiimeClassifier.class.getName() );
		classifierMap.put( KRAKEN, KrakenClassifier.class.getName() );
		classifierMap.put( SLIMM, SlimmClassifier.class.getName() );
		classifierMap.put( METAPHLAN, MetaphlanClassifier.class.getName() );
		parserMap.put( RDP, RdpParser.class.getName() );
		parserMap.put( QIIME, QiimeParser.class.getName() );
		parserMap.put( KRAKEN, KrakenParser.class.getName() );
		parserMap.put( SLIMM, SlimmParser.class.getName() );
		parserMap.put( METAPHLAN, MetaphlanParser.class.getName() );
	}

	/**
	 * Poll checks the Module's script dir for flag files indicating either
	 * SUCCESS or FAILURE.  Output message to log indicating num pass/fail.
	 * Exit if failures found and exitOnFailure flag set to Y.
	 *
	 * @param scriptFiles
	 * @param mainScript
	 * @return
	 * @throws Exception
	 */
	private static boolean poll( final List<File> scriptFiles, final File mainScript ) throws Exception
	{
		File failure = null;
		int numSuccess = 0;
		int numFailed = 0;
		for( final File f: scriptFiles )
		{
			final File testSuccess = new File( f.getAbsolutePath() + SCRIPT_SUCCEEDED );

			if( testSuccess.exists() )
			{
				numSuccess++;
			}
			else
			{
				final File testFailure = new File( f.getAbsolutePath() + SCRIPT_FAILED );
				if( testFailure.exists() )
				{
					failure = testFailure;
					numFailed++;
				}
			}
		}

		final int numScripts = scriptFiles.size();

		final File mainFailed = new File( mainScript.getAbsolutePath() + SCRIPT_FAILED );
		if( mainFailed.exists() )
		{
			failure = mainFailed;
		}

		final String logMsg = mainScript.getName() + " Status (Total=" + numScripts + "): Success=" + numSuccess
				+ "; Failure=" + numFailed;

		if( !statusMsg.equals( logMsg ) )
		{
			statusMsg = logMsg;
			log.info( logMsg );
		}
		else if( ( pollCounter++ % 10 ) == 0 )
		{
			log.info( logMsg );
		}

		if( mainFailed.exists() || ( exitOnError && ( failure != null ) && failure.exists() ) )
		{
			throw new Exception( "SCRIPT FAILED: " + failure.getAbsolutePath() );
		}

		return ( numSuccess + numFailed ) == numScripts;
	}

	/**
	 * This method calls poll to check status of scripts and then sleeps for pollTime seconds.
	 * @param scripts
	 * @param mainScript
	 * @throws Exception
	 */
	private static void pollAndSpin( final List<File> scripts, final File mainScript ) throws Exception
	{
		boolean finished = false;
		while( !finished )
		{
			finished = poll( scripts, mainScript );

			if( !finished )
			{
				Thread.sleep( pollTime * 1000 );
			}
		}
		pollCounter = 0;
	}

	/**
	 * Called by main(args[]) to check all of the executor dependencies, execute scripts,
	 * and then clean up by deleting temp dirs if needed.
	 * @throws Exception
	 */
	private static void runProgram() throws Exception
	{
		final List<Module> executors = getListToRun();

		for( final Module e: executors )
		{
			e.checkDependencies();
		}

		for( final Module e: executors )
		{
			final long startTime = System.currentTimeMillis();
			log.info( LOG_SPACER );
			log.info( "STARTING " + e.getClass().getSimpleName() );
			log.info( LOG_SPACER );
			executeAndWaitForScriptsIfAny( e );
			log.info( LOG_SPACER );
			log.info( "FINISHED " + e.getClass().getSimpleName() );
			log.info( LOG_SPACER );
			runTimes.add( getRunTime( e.getClass().getSimpleName(), startTime ) );
		}

		for( final Module e: executors )
		{
			final File f = new File( e.getExecutorDir().getAbsoluteFile() + File.separator + "failures" );
			if( f.exists() )
			{
				failures.addAll( Arrays.asList( f.listFiles() ) );
			}
		}

	}
}