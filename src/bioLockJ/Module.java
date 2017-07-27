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
package bioLockJ;

import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import bioLockJ.util.BashScriptUtil;

/**
 * Superclass for executors (classifiers, parsers, & batching utils).
 */
public abstract class Module extends BioLockJ
{
	private static final Map<String, Integer> readsPerSample = new HashMap<>();
	private File executorDir = null;
	private File failureDir = null;
	private File inputDir = null;
	private final List<File> inputFiles = new ArrayList<>();
	private File mainScript = null;
	private File outputDir = null;
	private File qsubDir = null;
	private final List<File> scriptFiles = new ArrayList<>();
	private File scriptsDir = null;
	private File tempDir = null;

	protected static boolean isFastA()
	{
		if( inputSequenceType.equals( FASTA ) )
		{
			return true;
		}

		return false;
	}

	protected static boolean isFastQ()
	{
		if( inputSequenceType.equals( FASTQ ) )
		{
			return true;
		}

		return false;
	}

	private static int countNumReads( final File f ) throws Exception
	{
		int count = 0;
		final BufferedReader r = getFileReader( f );
		for( String line = r.readLine(); line != null; line = r.readLine() )
		{
			count++;
		}
		r.close();

		return ( count / ( isFastA() ? 2: 4 ) );
	}

	public void addScriptFile( final File f )
	{
		scriptFiles.add( f );
	}

	public abstract void checkDependencies() throws Exception;

	public abstract void executeProjectFile() throws Exception;

	public File getExecutorDir()
	{
		return executorDir;
	}

	public File getFailureDir() throws Exception
	{
		if( failureDir == null )
		{
			failureDir = createSubDir( "failures" );
		}

		return failureDir;
	}

	public File getInputDir()
	{
		return inputDir;
	}

	public List<File> getInputFiles() throws Exception
	{
		if( inputFiles.isEmpty() )
		{
			initInputFiles( inputDir );
		}
		return inputFiles;
	}

	public File getMainScript() throws Exception
	{
		if( mainScript == null )
		{
			qsubDir = createSubDir( "qsub" );
			info( "Create Qsub Directory: " + qsubDir.getAbsolutePath() );

			final List<String> qsubDirs = getList( QSUBS );
			qsubDirs.add( qsubDir.getAbsolutePath() );
			config.setProperty( QSUBS, qsubDirs );

			mainScript = BashScriptUtil.createMainScript( this );
			info( "Create script: " + mainScript.getAbsolutePath() );
		}

		return mainScript;
	}

	public File getOutputDir() throws Exception
	{
		if( outputDir != null )
		{
			return outputDir;
		}
		outputDir = createSubDir( "output" );
		info( "Create Output Directory: " + outputDir.getAbsolutePath() );
		return outputDir;
	}

	public File getQsubDir()
	{
		return qsubDir;
	}

	public File getScriptDir() throws Exception
	{
		if( scriptsDir != null )
		{
			return scriptsDir;
		}
		scriptsDir = createSubDir( "scripts" );
		info( "Create Script Directory: " + scriptsDir.getAbsolutePath() );
		return scriptsDir;
	}

	public List<File> getScriptFiles()
	{
		return scriptFiles;
	}

	public File getTempDir() throws Exception
	{
		if( tempDir == null )
		{
			tempDir = createSubDir( "temp" );
		}

		return tempDir;
	}

	public boolean hasScripts()
	{
		if( scriptsDir == null )
		{
			return false;
		}
		return true;
	}

	public void initInputFiles( final File dir ) throws Exception
	{
		setModuleInput( dir, TrueFileFilter.INSTANCE, null );
	}

	public boolean poll()
	{
		return true;
	}

	public void setExecutorDir( final String name, final int index ) throws Exception
	{
		final String fullPath = requireString( ROOT_DIR ) + index + "_" + name;
		final File dir = new File( fullPath );
		if( !dir.mkdirs() )
		{
			throw new Exception( "ERROR: Unable to create: " + fullPath );
		}

		info( "Create Executor Directory: " + fullPath );
		executorDir = dir;
	}

	public void setInputDir( final File dir ) throws Exception
	{
		inputDir = dir;
	}

	/**
	 * Important method, called to pull Sample ID out of the passed in String.
	 * Possibly input is a file name, if so, we remove file extensions, etc.
	 * or if  demultiplexing (RDP/Kraken support this option), input a sequence header
	 * @param sampleName
	 * @return
	 * @throws Exception
	 */
	public String trimSampleID( final String sampleName ) throws Exception
	{
		String id = sampleName;
		try
		{
			if( !demultiplex ) // must be a file
			{
				// trim if gzipped
				if( id.toLowerCase().endsWith( ".gz" ) )
				{
					id = id.substring( 0, id.length() - 3 );
				}

				if( id.endsWith( MERGE_SUFFIX ) )
				{
					id = id.replace( MERGE_SUFFIX, "" );
				}
				else
				{
					id = id.replace( "." + FASTQ, "" );
					id = id.replace( "." + FASTA, "" );

					if( isPairedRead )
					{
						if( isForwardRead( id ) )
						{
							id = id.substring( 0, id.lastIndexOf( fwReadSuffix ) );
						}
						else
						{
							id = id.substring( 0, id.lastIndexOf( rvReadSuffix ) );
						}
					}
				}
			}

			if( ( inputTrimPrefix != null ) && ( inputTrimPrefix.length() > 0 )
					&& ( id.indexOf( inputTrimPrefix ) > -1 ) )
			{
				final int index = id.indexOf( inputTrimPrefix );
				final int size = inputTrimPrefix.length();
				id = id.substring( size + index );
			}

			if( ( inputTrimSuffix != null ) && ( inputTrimSuffix.length() > 0 )
					&& ( id.indexOf( inputTrimSuffix ) > -1 ) )
			{
				id = id.substring( 0, id.lastIndexOf( inputTrimSuffix ) );
			}

		}
		catch( final Exception ex )
		{
			error( "Unable to get SampleID from: " + sampleName, ex );
			throw ex;
		}

		return id;
	}

	protected File createSubDir( final String subDirPath ) throws Exception
	{
		final String subDir = getExecutorDir().getAbsolutePath() + File.separator + subDirPath;
		final File dir = new File( subDir );
		if( !dir.mkdirs() )
		{
			throw new Exception( "ERROR: Unable to create: " + dir );
		}
		return dir;
	}

	protected List<File> getInputDirs( final File dir ) throws Exception
	{
		List<File> returnDirs = new ArrayList<>();
		if( dir != null )
		{
			info( "Set input dir = " + dir );
			returnDirs.add( dir );
			setInputDir( dir );
		}
		else
		{
			setInputDir( inputDirs.get( 0 ) );
			returnDirs = inputDirs;
		}

		return returnDirs;
	}

	protected List<File> getInputDirsFromPropFile() throws Exception
	{
		setInputDir( inputDirs.get( 0 ) );
		return inputDirs;
	}

	protected String getName( final File dir )
	{
		return ( dir == null ) ? INPUT_DIRS: dir.getAbsolutePath();
	}

	protected Map<File, File> getPairedReads( final List<File> files ) throws Exception
	{
		info( "Calling getPairedReads for " + count( files ) + " files " );
		final Map<File, File> map = new HashMap<>();
		for( final File fwRead: files )
		{
			info( "getPairedReads fwRead: " + fwRead );
			if( !isForwardRead( fwRead.getAbsolutePath() ) )
			{
				continue;
			}

			File rvRead = null;
			final String sampleID = trimSampleID( fwRead.getName() );
			for( final File searchFile: files )
			{
				if( searchFile.getName().contains( sampleID ) && !isForwardRead( searchFile.getAbsolutePath() ) )
				{
					rvRead = searchFile;
					break;
				}
			}

			if( rvRead != null )
			{
				map.put( fwRead, rvRead );
			}
			else
			{
				warn( "PAIRED_READS=TRUE - UNPAIRED FORWARD READ FOUND = " + fwRead.getAbsolutePath() );
			}
		}

		return map;
	}

	protected Map<String, Integer> getReadsPerSample()
	{
		return readsPerSample;
	}

	/**
	 * Suffix may be part of regular file name, so only remove the last index
	 *
	 * @param id
	 * @return
	 */
	protected boolean isForwardRead( final String name )
	{
		if( !isPairedRead || name.contains( MERGE_SUFFIX ) || ( rvReadSuffix == null ) || ( fwReadSuffix == null ) )
		{
			return true;
		}

		// both forward & reverse suffixes must be defined
		final int fwIndex = name.lastIndexOf( fwReadSuffix );
		final int rvIndex = name.lastIndexOf( rvReadSuffix );
		if( ( fwIndex > 0 ) && ( fwIndex > rvIndex ) )
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	/**
	 * Register num reads for each sample by parsing the files & counting number of lines.
	 * @param files
	 * @param targetDir
	 * @throws Exception
	 */
	protected void registerNumReadsPerSample( final List<File> files, final File targetDir ) throws Exception
	{
		if( !readsPerSample.isEmpty() )
		{
			return;
		}

		final int numFiles = count( files );
		if( numFiles == 0 )
		{
			warn( "registerNumReadsPerSample() passed ZERO files! " );
		}
		else
		{
			info( "Counting # reads/sample for " + numFiles + " files" );
		}

		for( final File f: files )
		{
			if( !isForwardRead( f.getName() ) )
			{
				continue;
			}

			final int count = countNumReads( f );

			info( "Num Reads (File Name: " + f.getName() + "): key[" + trimSampleID( f.getName() ) + "] = " + count );

			readsPerSample.put( trimSampleID( f.getName() ), count );
		}

		config.getMetaUtil().addColumnToMetadata( this, NUM_READS, readsPerSample, targetDir );
	}

	/**
	 * Set input directory and set inputFiles to any file in top level of dir that doesn't
	 * start with "." to avoid hidden files.
	 *
	 * defaults
	 *
	 * @param dir
	 * @throws Exception
	 */
	protected void setModuleInput( final File dir, final IOFileFilter ff, final IOFileFilter recursive )
			throws Exception
	{
		final List<File> returnDirs = getInputDirs( dir );
		final Set<String> fileNames = new HashSet<>();
		int index = 1;
		for( final File inDir: returnDirs )
		{
			final List<File> files = (List<File>) FileUtils.listFiles( inDir, ff, recursive );
			if( ( files == null ) || files.isEmpty() )
			{
				throw new Exception( "No input files found in directory: " + inDir );
			}

			info( "# Files found: " + count( files ) );
			for( final File f: files )
			{
				if( !f.isDirectory() && !f.getName().startsWith( "." ) && !ignoreInputFiles.contains( f.getName() ) )
				{
					validateFileNames( fileNames, f );
					fileNames.add( f.getName() );
					info( "INPUT FILE[" + index++ + "] = " + f.getAbsolutePath() );
					inputFiles.add( f );
				}
				else
				{
					warn( "Skipping non-sequence file: " + f.getName() );
				}
			}
		}

		Collections.sort( inputFiles );
	}

	protected void validateFileNames( final Set<String> fileNames, final File f ) throws Exception
	{
		if( fileNames.contains( f.getName() ) )
		{
			throw new Exception( "File names must be unique!  Duplicate file: " + f.getAbsolutePath() );
		}
	}
}
