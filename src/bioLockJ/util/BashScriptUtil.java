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
package bioLockJ.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import bioLockJ.BioLockJ;
import bioLockJ.Module;

/**
 * This class generates the bash scripts based on the lines provided by the metagenomeClassifier.
 */
public class BashScriptUtil extends BioLockJ
{
	private static String failMessage = "failure";
	private static int scriptBatchSize = 0;

	/**
	 * Lines are wrapped in control statements to allow exit of script based on a single failure,
	 * or to simply track if any failure occur (if exitOnFailure=N).  Exit codes are capture in the
	 * event of a failure to be attached to the failureFile ( 0KB flag file).
	 * @param writer
	 * @param failureFile
	 * @param lines
	 * @throws Exception
	 */
	public static void addDependantLinesToScript( final BufferedWriter writer, final String failPath, String failMsg,
			final List<String> lines ) throws Exception
	{
		writer.write( ERROR_ON_PREVIOUS_LINE + "=false \n" );
		boolean firstLine = true;
		boolean indent = false;
		for( final String line: lines )
		{
			if( exitOnError )
			{
				indent = true;
				writer.write( "if [[ " + ( firstLine ? "": "$" + ERROR_ON_PREVIOUS_LINE + " == false && " ) + "$"
						+ ERROR_DETECTED + " == false ]]; then \n" );
			}
			else if( !firstLine )
			{
				indent = true;
				writer.write( "if [[ $" + ERROR_ON_PREVIOUS_LINE + " == false ]]; then \n" );
			}

			final String[] parts = line.split( "\\s" );
			if( parts[ 0 ].endsWith( ".py" ) )
			{
				failMsg = parts[ 0 ];
			}

			writer.write( ( indent ? INDENT: "" ) + line + "\n" );
			writer.write( ( indent ? INDENT: "" ) + EXIT_CODE + "=$? \n" );
			writer.write( ( indent ? INDENT: "" ) + "if [[ $" + EXIT_CODE + " != \"0\" ]]; then \n" );
			writer.write( ( indent ? INDENT: "" ) + INDENT + ERROR_ON_PREVIOUS_LINE + "=true \n" );
			writer.write( ( indent ? INDENT: "" ) + INDENT + ERROR_DETECTED + "=true \n" );
			writer.write( ( indent ? INDENT: "" ) + INDENT + FAILURE_CODE + "=$" + EXIT_CODE + " \n" );
			writer.write( ( indent ? INDENT: "" ) + INDENT + "touch " + failPath + failMsg + SCRIPT_FAILED
					+ "_exitCode_$" + EXIT_CODE + " \n" );
			writer.write( ( indent ? INDENT: "" ) + "fi \n" );
			writer.write( indent ? "fi \n": "" );
			firstLine = false;
		}
	}

	/**
	 * Adds cluster modules to script.
	 * @param writer
	 * @param blje
	 * @throws Exception
	 */
	public static void addModules( final BufferedWriter writer, final Module blje ) throws Exception
	{
		for( final String module: clusterModules )
		{
			writer.write( "module load " + module + "\n" );
		}
	}

	/**
	 * Scripts are build for the Module based on the data lines provided.
	 * @param blje
	 * @param data - classifier lines for the bash script
	 * @param files - failure flag files, just used for their names.
	 * @throws Exception
	 */
	public static void buildScripts( final Module blje, final List<List<String>> data, final List<File> files )
			throws Exception
	{
		final int count = count( files );
		log.info( blje.getClass().getSimpleName() + " Building bash scripts: # Sequence Files =" + data.size()
				+ "; If failures occur, failure indicator file name = "
				+ ( ( files == null ) ? failMessage + "": ( count + " unique names (based on SampleID)" ) ) );

		final BufferedWriter allWriter = new BufferedWriter( new FileWriter( blje.getMainScript(), true ) );
		int scriptCount = 0;
		int sampleCount = 0;
		int samplesInScript = 0;
		final int digits = new Integer( count ).toString().length();
		boolean needNewScript = false;
		File subScript = null;
		BufferedWriter subScriptWriter = null;

		final String failPath = blje.getFailureDir().getAbsolutePath() + File.separator;

		for( final List<String> lines: data )
		{
			if( lines.size() == 0 )
			{
				throw new Exception(
						blje.getClass().getSimpleName() + " has no lines in " + blje.getMainScript() + " subscript!" );
			}
			if( ( subScript == null ) || needNewScript )
			{
				subScript = createSubScript( blje, allWriter, scriptCount++, digits );
				subScriptWriter = new BufferedWriter( new FileWriter( subScript, true ) );
			}

			String failMsg = failMessage;
			if( count > 0 )
			{
				failMsg = files.get( sampleCount ).getName();
			}

			addDependantLinesToScript( subScriptWriter, failPath, failMsg, lines );
			needNewScript = needNewScript( ++samplesInScript, ++sampleCount, data.size() );
			if( needNewScript || ( sampleCount == data.size() ) )
			{
				samplesInScript = 0;
				closeScript( subScriptWriter, subScript );
				printFile( subScript );
			}
		}

		closeScript( allWriter, blje.getMainScript() );
		log.info( LOG_SPACER );
		log.info( blje.getClass().getSimpleName() + " Bash scripts successfully generated" );
		log.info( LOG_SPACER );
	}

	public static void buildScripts( final Module blje, final List<List<String>> data, final List<File> files,
			final int size ) throws Exception
	{
		scriptBatchSize = size;
		buildScripts( blje, data, files );
	}

	/**
	 * Some BioLockJExecutors do not have a line for each file, these pass a single fail message.
	 * @param blje
	 * @param data
	 * @param msg
	 * @throws Exception
	 */
	public static void buildScripts( final Module blje, final List<List<String>> data, final String msg )
			throws Exception
	{
		scriptBatchSize = batchSize;
		failMessage = msg;
		buildScripts( blje, data, new ArrayList<File>() );
		failMessage = "failure";
	}

	/**
	 * Close the script, set the status message, close the writer.
	 * @param writer
	 * @param script
	 * @throws Exception
	 */
	public static void closeScript( final BufferedWriter writer, final File script ) throws Exception
	{
		writer.write( "if [[ $" + ERROR_DETECTED + " == false ]]; then \n" );
		writer.write( INDENT + "touch " + script + SCRIPT_SUCCEEDED + " \n" );
		writer.write( "else \n" );
		writer.write( INDENT + "touch " + script + SCRIPT_FAILED + " \n" );
		writer.write( INDENT + "touch " + script + SCRIPT_FAILED + "_failureCode_$" + FAILURE_CODE + " \n" );
		writer.write( INDENT + "exit 1 \n" );
		writer.write( "fi \n" );
		writer.flush();
		writer.close();
	}

	/**
	 * Create the main script for the Module.
	 * @param blje
	 * @return
	 * @throws Exception
	 */
	public static File createMainScript( final Module blje ) throws Exception
	{
		final File f = new File( blje.getScriptDir().getAbsolutePath() + File.separator + MAIN_SCRIPT + "_"
				+ blje.getExecutorDir().getName() + ".sh" );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( f ) );
		writer.write( "### This script submits multiple subscripts for parallel processing ### \n" );
		writer.write( "cd " + blje.getQsubDir().getAbsolutePath() + " \n" );
		writer.write( ERROR_DETECTED + "=false \n" );
		writer.write( FAILURE_CODE + "=0 \n" );
		writer.flush();
		writer.close();
		return f;
	}

	/**
	 * Create the numbered subscript (a worker script)
	 * @param blje
	 * @param allWriter
	 * @param countNum
	 * @param digits
	 * @return
	 * @throws Exception
	 */
	private static File createSubScript( final Module blje, final BufferedWriter allWriter, final int countNum,
			final int digits ) throws Exception
	{
		final String main = blje.getMainScript().getName().replaceAll( MAIN_SCRIPT, "" );
		final String cType = classifierType.substring( 0, 1 ) + classifierType.substring( 1 ).toLowerCase();
		final String jobName = classifierType + main.substring( 0, 2 ) + "." + formatInt( countNum, digits )
				+ main.substring( 2 ).replace( cType, "" );

		final File script = new File( blje.getScriptDir().getAbsolutePath() + File.separator + jobName );
		log.info( blje.getClass().getSimpleName() + " Create Sub Script: " + script.getAbsolutePath() );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( script ) );

		final String executeCommand = ( runOnCluster ? ( clusterCommand + " " ): "" ) + script.getAbsolutePath();

		writer.write( runOnCluster ? ( clusterParams + "\n" ): "" );
		addModules( writer, blje );
		writer.write( ERROR_DETECTED + "=false \n" );
		writer.write( FAILURE_CODE + "=0 \n" );
		writer.flush();
		writer.close();

		allWriter.write( "if [[ $" + ERROR_DETECTED + " == false ]]; then \n" );
		allWriter.write( INDENT + executeCommand + "\n" );
		allWriter.write( INDENT + EXIT_CODE + "=$? \n" );
		allWriter.write( INDENT + "if [[ $" + EXIT_CODE + " != \"0\" ]]; then \n" );
		allWriter.write( INDENT + INDENT + ERROR_DETECTED + "=true \n" );
		allWriter.write( INDENT + INDENT + FAILURE_CODE + "=$" + EXIT_CODE + " \n" );
		allWriter.write( INDENT + "fi \n" );
		allWriter.write( "fi \n" );
		allWriter.flush();
		blje.addScriptFile( script );
		return script;
	}

	/**
	 * Determine if a new script is needed - do #samples = batch size?
	 * @param samplesInScript
	 * @param sampleCount
	 * @param totalSampleCount
	 * @return
	 */
	private static boolean needNewScript( final int samplesInScript, final int sampleCount, final int totalSampleCount )
	{
		if( ( scriptBatchSize > 0 ) && ( samplesInScript == scriptBatchSize ) && ( sampleCount < totalSampleCount ) )
		{
			return true;
		}

		return false;
	}

	/**
	 * Print the bash script to the log file.
	 * @param file
	 */
	private static void printFile( final File file )
	{
		log.debug( "BashScriptUtil PRINT FILE => " + file.getAbsolutePath() );
		try
		{
			final BufferedReader in = new BufferedReader( new FileReader( file ) );
			String line;
			while( ( line = in.readLine() ) != null )
			{
				log.debug( line );
			}
			in.close();

		}
		catch( final Exception ex )
		{
			log.error( "BashScriptUtil Error occurred printing DEBUG for file: " + file, ex );
		}
	}

}
