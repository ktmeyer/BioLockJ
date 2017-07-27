/**
 * @UNCC BINF 8380
 *
 * @author Michael Sioda
 * @date Mar 27, 2017
 */
package bioLockJ.module.classifier;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import bioLockJ.Module;
import bioLockJ.util.BashScriptUtil;
import bioLockJ.util.ProcessUtil;

/**
 * This is the Classifier superclass used by all WGS & 16S classifiers.
 */
public abstract class ClassifierModule extends Module
{
	protected static String classifierExe = null;
	protected static List<String> classifierParams = null;
	protected static final Logger log = LoggerFactory.getLogger( ClassifierModule.class );

	/**
	 * Check dependencies as we read in generic classifier props.
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		classifierParams = getList( EXE_CLASSIFIER_PARAMS );
		classifierExe = getString( EXE_CLASSIFIER );
		if( !moduleExists( getString( EXE_CLASSIFIER ) ) && !isQiime )
		{
			requireExistingFile( EXE_CLASSIFIER );
		}
		else if( !isQiime )
		{
			classifierExe = requireString( EXE_CLASSIFIER );
		}

		logVersion();
	}

	/**
	 * Default classifier execution registers numReads for each sample (if report.numReads=Y)
	 * Then call abstract method for paired or single reads based on paried read prop.
	 * Finally Bash Scripts must be built for execution.
	 */
	@Override
	public void executeProjectFile() throws Exception
	{
		final List<File> files = getInputFiles();
		if( reportNumReads )
		{
			registerNumReadsPerSample( files, getOutputDir() );
		}

		final List<List<String>> data = isPairedRead ? buildScriptForPairedReads( files ): buildScript( files );
		BashScriptUtil.buildScripts( this, data, files, batchSize );
	}

	protected abstract List<List<String>> buildScript( final List<File> files ) throws Exception;

	protected abstract List<List<String>> buildScriptForPairedReads( final List<File> files ) throws Exception;

	/**
	 * Get the basic classifier switches from the prop file.
	 * @return
	 * @throws Exception
	 */
	protected String getProgramSwitches() throws Exception
	{
		if( classifierParams == null )
		{
			return "";
		}

		String formattedSwitches = " ";

		final Iterator<String> it = classifierParams.iterator();
		while( it.hasNext() )
		{
			formattedSwitches += "--" + it.next() + " ";
		}

		return formattedSwitches;
	}

	/**
	 * Log the version info to the log file.
	 */
	protected void logVersion()
	{
		logVersion( classifierExe, "--version" );
	}

	/**
	 * Another method to log version to handle cases with a unique version switch param.
	 * @param programExe
	 * @param versionSwitch
	 */
	private void logVersion( final String programExe, final String versionSwitch )
	{
		try
		{
			final String[] cmd = new String[ 2 ];
			cmd[ 0 ] = programExe;
			cmd[ 1 ] = versionSwitch;
			ProcessUtil.submit( cmd );
		}
		catch( final Exception ex )
		{
			error( "Version not found: " + programExe + " " + versionSwitch, ex );
		}
	}
}
