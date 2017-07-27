/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Apr 5, 2017
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
package bioLockJ.module.classifier.r16s.qiime;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.filefilter.TrueFileFilter;
import bioLockJ.module.classifier.r16s.QiimeClassifier;
import bioLockJ.util.BashScriptUtil;

/**
 * This class separates the fasta files into batches of a size defined in the prop file.
 * Separate mapping files are created for each batch. Finally, call pick OTU script, as
 * defined in qiimePickOtuScript config param on each batch.
 *
 */
public class ClosedRefClassifier extends QiimeClassifier
{

	private final List<File> failFiles = new ArrayList<>();

	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		checkOtuPickingDependencies();
	}

	@Override
	public void executeProjectFile() throws Exception
	{
		BashScriptUtil.buildScripts( this, buildScript( getInputFiles() ), failFiles, 1 );
	}

	/**
	 * Directory top level holds only fasta files and it's subdirectory holds the corrected
	 * QIIME specific mapping file as output by QiimePreprocessor.java
	 *
	 */
	@Override
	public void initInputFiles( File dir ) throws Exception
	{
		if( dir == null )
		{
			dir = getInputDirsFromPropFile().get( 0 );
		}
		else
		{
			initMappingFile( dir );
		}
		setModuleInput( dir, TrueFileFilter.INSTANCE, null );
	}

	/**
	 * Split up mapping & fastas into each batch with size = numJobsPerThread.
	 *
	 * Output files found under each output/batch_# include:
	 * 		1. batchMapping.txt
	 * 		2. output/batch_#/fasta
	 * 		3. combined_seqs.fna
	 */
	@Override
	protected List<List<String>> buildScript( final List<File> files ) throws Exception
	{
		info( "Processing " + count( files ) + " files" );

		final List<List<String>> data = new ArrayList<>();
		List<String> lines = new ArrayList<>();
		int startIndex = 1;
		int batchNum = 0;
		int sampleCount = 0;
		for( final File f: files )
		{
			sampleCount++;
			lines.add( "cp " + f.getAbsolutePath() + " " + getBatchFastaDir( batchNum ) );
			if( addNextBatch( sampleCount ) )
			{
				data.add( getBatch( lines, batchNum++, startIndex ) );
				startIndex = startIndex + batchSize;
				lines = new ArrayList<>();
			}
		}
		if( addFinalBatch( sampleCount ) )
		{
			data.add( getBatch( lines, batchNum, startIndex ) );
		}
		return data;
	}

	/**
	 * Add final batch if additional samples were processed since last group of
	 * getBatchSize() samples were added (via addNextBatch( sampleCount).
	 * @param sampleCount
	 * @return boolean
	 */
	private boolean addFinalBatch( final int sampleCount )
	{
		if( ( sampleCount % batchSize ) != 0 )
		{
			return true;
		}
		return false;
	}

	/**
	 * Add next batch each time we process getBatchSize() samples.
	 * @param sampleCount
	 * @return boolean
	 */
	private boolean addNextBatch( final int sampleCount )
	{
		if( ( sampleCount % batchSize ) == 0 )
		{
			return true;
		}
		return false;
	}

	/**
	 *
	 * @param lines - so far contains lines used to copy fasta to batch_#/fasta
	 * @param i - batch number
	 * @param index - start index used to break up mapping file
	 * @return List<String> lines to process batch, including: cp fastas, create batchMapping.txt,
	 * 						SCRIPT_ADD_LABELS, SCRIPT_PICK_CLOSED_REF_OTUS
	 * @throws Exception
	 */
	private List<String> getBatch( final List<String> lines, final int i, final int index ) throws Exception
	{
		final File batchDir = getBatchDir( i );
		final String mapping = batchDir.getAbsolutePath() + File.separator + BATCH_MAPPING;
		final String fastaDir = getBatchFastaDir( i );
		final String mapFile = config.getMetaPath();
		final int endIndex = index + batchSize;

		lines.add( getAwk() + " 'NR==1' " + mapFile + " > " + mapping );
		lines.add( getAwk() + " 'NR>" + index + "&&NR<=" + endIndex + "' " + mapFile + " >> " + mapping );
		failFiles.add( batchDir );
		addPickOtuLines( lines, fastaDir, mapping, batchDir.getAbsolutePath() );

		return lines;
	}

	/**
	 * Get  batchDir #i
	 * @param i
	 * @return
	 * @throws Exception
	 */
	private File getBatchDir( final int i ) throws Exception
	{
		final File dir = new File( getOutputDir().getAbsolutePath() + File.separator + "batch_" + i );
		if( !dir.exists() )
		{
			dir.mkdirs();
		}

		return dir;
	}

	/**
	 * Get batch#1/fasta dir
	 * @param i
	 * @return
	 * @throws Exception
	 */
	private String getBatchFastaDir( final int i ) throws Exception
	{
		final File f = new File( getBatchDir( i ) + File.separator + FASTA );
		if( !f.exists() )
		{
			f.mkdirs();
		}
		return f.getAbsolutePath();
	}

}