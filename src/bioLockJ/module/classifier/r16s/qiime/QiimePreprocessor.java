/**
 * @UNCC BINF 8380
 *
 * @author Michael Sioda
 * @date Jun 7, 2017
 */
package bioLockJ.module.classifier.r16s.qiime;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.commons.io.filefilter.TrueFileFilter;
import bioLockJ.module.classifier.r16s.QiimeClassifier;
import bioLockJ.util.BashScriptUtil;

/**
 * This class prepares QIIME input files. 1. Reorder any metadata columns if
 * required for QIIME mapping. 2. Add columns to metadata if required for QIIME
 * mapping. 3. Decompress gzipped fasta/fastq files, if any. 4.
 * Convert FastQ files to FastA format, if any.
 */
public class QiimePreprocessor extends QiimeClassifier
{
	private static final String SORTED_MAP = "sortedMapping.txt";
	private final List<File> failFiles = new ArrayList<>();
	private boolean formatMetadata;

	/**
	 * Read in user prefernece, do we format metadata to use QIIME mapping format?
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		formatMetadata = requireBoolean( QIIME_FORMAT_METADATA );
	}

	/**
	 * Register num reads persample and create build script.
	 */
	@Override
	public void executeProjectFile() throws Exception
	{
		if( reportNumReads )
		{
			registerNumReadsPerSample( getInputFiles(), formatMetadata ? getTempDir(): getOutputDir() );
		}
		BashScriptUtil.buildScripts( this, buildScript( getInputFiles() ), failFiles, batchSize );
	}

	/**
	 * Input files must be initialized.  If first executor, read in input dir from props, otherwise,
	 * check for merged files.  Call setModuleInput based on merged files, or files in dir.
	 */
	@Override
	public void initInputFiles( File dir ) throws Exception
	{
		if( dir == null ) // get from prop file
		{
			dir = getInputDirs( dir ).get( 0 );
		}

		setModuleInput( dir, TrueFileFilter.INSTANCE, null );

		//		final IOFileFilter ff = new WildcardFileFilter( "*" + MERGE_SUFFIX );
		//		final List<File> files = (List<File>) FileUtils.listFiles( dir, ff, null );
		//		info( "Check for previously merged files returns: " + count( files ) + " files in directory: " + getName( dir )
		//				+ " that end with suffix: " + MERGE_SUFFIX );
		//		if( count( files ) > 0 )
		//		{
		//			setModuleInput( dir, ff, null );
		//		}
		//		else
		//		{
		//			setModuleInput( dir, TrueFileFilter.INSTANCE, null );
		//		}
	}

	/**
	 * This script will unzip if gzipped files are found and will convert fastQ to fastA if needed.
	 * Otherwise, files are simply loaded to the output dir for next executor.  Last script will
	 * also create the Qiime corrected mapping file by using QIIME verifyMapping python script.
	 */
	@Override
	protected List<List<String>> buildScript( final List<File> files ) throws Exception
	{
		final List<List<String>> data = new ArrayList<>();
		final String tempDir = getTempDir().getAbsolutePath() + File.separator;
		final String outDir = getOutputDir().getAbsolutePath() + File.separator;
		String ext = "." + ( isFastA() ? FASTA: FASTQ );

		for( final File f: files )
		{
			final ArrayList<String> lines = new ArrayList<>();
			final String fileId = trimSampleID( f.getName() );
			final String zipExe = getZipExe( f );

			if( isPairedRead )
			{
				if( isForwardRead( f.getName() ) )
				{
					ext = fwReadSuffix + ext;
				}
				else
				{
					ext = rvReadSuffix + ext;
				}
			}

			String filePath = f.getAbsolutePath();

			if( zipExe != null )
			{
				filePath = ( isFastQ() ? tempDir: outDir ) + fileId + ext;
				lines.add( unzip( zipExe, f, filePath ) );
			}

			if( isFastQ() )
			{
				lines.add( convert2fastA( filePath, fileId, outDir ) );
			}

			if( ( zipExe == null ) && isFastA() )
			{
				lines.add( copyToOutputDir( filePath, fileId + ext ) );
			}

			failFiles.add( f );
			data.add( lines );
		}

		data.add( createQiimeCorrectedMapping() );
		failFiles.add( new File( config.getMetadata().getAbsolutePath() ) );

		return data;
	}

	/**
	 * Convert file format using awk.
	 *
	 * @param filePath
	 * @param fileId
	 * @param outDir
	 * @return
	 */
	private String convert2fastA( final String filePath, final String fileId, final String outDir )
	{
		return "cat " + filePath + " | " + getAwk() + " '{if(NR%4==1) {printf(\">%s\\n\",substr($0,2));} "
				+ "else if(NR%4==2) print;}' > " + outDir + fileId + "." + FASTA;
	}

	/**
	 * Copy files to output dir.
	 * @param source
	 * @param target
	 * @return
	 * @throws Exception
	 */
	private String copyToOutputDir( final String source, final String target ) throws Exception
	{
		return "cp " + source + " " + getOutputDir().getAbsolutePath() + File.separator + target;
	}

	/**
	 * First, print_qiime_config.py will output version info, next we create the QIIME Mapping file
	 * and rearranging columns as required by QIIME format rules.  Finally we call validate_mapping_file.py
	 * to add the proper QIIME script call to the bash script.
	 *
	 * @return
	 * @throws Exception
	 */
	private List<String> createQiimeCorrectedMapping() throws Exception
	{
		final List<String> lines = new ArrayList<>();
		lines.add( SCRIPT_PRINT_CONFIG );

		if( formatMetadata )
		{
			info( "Create QIIME Specific Mapping File" );
			createQiimeMapping();
			final String alignColLine = getAlignedMetadataColumns();
			if( alignColLine != null )
			{
				info( "Add line to BASH script to arrange QIIME columns in metadata." );
				lines.add( alignColLine );
			}
		}
		else
		{
			info( "User Config indicates metadata already formatted for QIIME: (" + QIIME_FORMAT_METADATA
					+ "=FALSE).  BioLockJ will use metadata for QIIME mapping file." );
		}

		lines.add( sortMetadata() );
		lines.add( validateMapping() );
		return lines;
	}

	/**
	 * Create QIIME mapping based on metadata file, output to temp/QIIME_MAPPING.
	 * Add required fields if missing.
	 * Add any new fields to the existing metadata descriptor file.
	 * @throws Exception
	 */
	private void createQiimeMapping() throws Exception
	{
		final File newMapping = new File( getTempDir().getAbsolutePath() + File.separator + QIIME_MAPPING );
		final BufferedReader metaReader = new BufferedReader(
				new FileReader( config.getMetadata().getAbsolutePath() ) );
		final BufferedWriter metaWriter = new BufferedWriter( new FileWriter( newMapping ) );

		try
		{
			config.getMetaUtil().updateDescriptor( QiimeMapping.requiredFields(), getOutputDir() );

			final boolean hasQm1 = config.getMetaUtil().getAttributeNames().contains( BARCODE_SEQUENCE );
			final boolean hasQm2 = config.getMetaUtil().getAttributeNames().contains( LINKER_PRIMER_SEQUENCE );
			final boolean hasQm3 = config.getMetaUtil().getAttributeNames().contains( DEMUX_COLUMN );
			final boolean hasQm4 = config.getMetaUtil().getAttributeNames().contains( DESCRIPTION );

			boolean isHeaderRow = true;
			for( String line = metaReader.readLine(); line != null; line = metaReader.readLine() )
			{
				final StringTokenizer st = new StringTokenizer( line, DELIM );
				boolean firstColumn = true;
				String id = null;

				while( st.hasMoreTokens() )
				{
					final String next = st.nextToken();
					if( firstColumn )
					{
						firstColumn = false;
						if( isHeaderRow )
						{
							metaWriter.write( QIIME_ID + DELIM );

							if( !hasQm1 )
							{
								metaWriter.write( BARCODE_SEQUENCE + DELIM );
							}

							if( !hasQm2 )
							{
								metaWriter.write( LINKER_PRIMER_SEQUENCE + DELIM );
							}
						}
						else
						{
							id = config.getMetaUtil().rScriptFormat( next );
							metaWriter.write( id + DELIM );

							if( !hasQm1 )
							{
								metaWriter.write( DELIM );
							}

							if( !hasQm2 )
							{
								metaWriter.write( DELIM );
							}
						}
					}
					else
					{
						metaWriter.write( config.getMetaUtil().rScriptFormat( next ) + DELIM );
					}
				}

				if( isHeaderRow )
				{
					if( !hasQm3 )
					{
						metaWriter.write( DEMUX_COLUMN + DELIM );
					}

					if( !hasQm4 )
					{
						metaWriter.write( DESCRIPTION );
					}

					isHeaderRow = false;
				}
				else
				{
					if( !hasQm3 )
					{
						metaWriter.write( id + "." + FASTA + DELIM );
					}

					if( !hasQm4 )
					{
						metaWriter.write( QIIME_COMMENT );
					}
				}

				metaWriter.write( "\n" );
			}
		}
		catch( final Exception ex )
		{
			error( "Error occcurred creating QIIME mapping file: ", ex );
		}
		finally
		{
			metaReader.close();
			metaWriter.flush();
			metaWriter.close();
			config.getMetaUtil().setMetaId( QIIME_ID );
			config.getMetaUtil().loadMetadata( newMapping, config.getDescriptor() );
		}
	}

	/**
	 * If QIIME required fields exist in metadata, but are not in proper position,
	 * output line for bash script that will move the column to the proper position.
	 * --> BarcodeSequence = col 2
	 * --> LinkerPrimerSequence = col 3
	 * --> InputFileName = 2nd to last col
	 * --> Description = last col
	 * @return String - line for bash script
	 * @throws Exception
	 */
	private String getAlignedMetadataColumns() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getAwk() + " -F'\\t' -v OFS=\"\\t\" '{ print $1" );
		final List<String> cols = config.getMetaUtil().getAttributes( config.getMetaId() );

		final boolean hasQm1 = config.getMetaUtil().getAttributeNames().contains( BARCODE_SEQUENCE );
		final boolean hasQm2 = config.getMetaUtil().getAttributeNames().contains( LINKER_PRIMER_SEQUENCE );
		final boolean hasQm3 = config.getMetaUtil().getAttributeNames().contains( DEMUX_COLUMN );
		final boolean hasQm4 = config.getMetaUtil().getAttributeNames().contains( DESCRIPTION );

		final int numCols = cols.size();
		int demuxIndex = numCols;
		int descIndex = numCols + 1;

		final List<Integer> colsToSkip = new ArrayList<>();
		if( hasQm1 && !cols.get( 0 ).equals( BARCODE_SEQUENCE ) )
		{
			skipIndex( BARCODE_SEQUENCE, cols, sb, colsToSkip, "column #2" );
		}
		if( hasQm2 && ( numCols > 1 ) && !cols.get( 1 ).equals( LINKER_PRIMER_SEQUENCE ) )
		{
			skipIndex( LINKER_PRIMER_SEQUENCE, cols, sb, colsToSkip, "column #3" );
		}
		if( hasQm3 && ( numCols > 2 ) && !cols.get( ( numCols - 2 ) ).equals( DEMUX_COLUMN ) )
		{
			demuxIndex = skipIndex( DEMUX_COLUMN, cols, sb, colsToSkip, " 2nd to last column" );
		}
		if( hasQm4 && !cols.get( ( numCols - 1 ) ).equals( DESCRIPTION ) )
		{
			descIndex = skipIndex( DESCRIPTION, cols, sb, colsToSkip, " last column" );
		}

		if( colsToSkip.isEmpty() )
		{
			info( "Metadata does not contain QIIME specific fields to reorder." );
			return null;
		}

		for( int i = 0; i < colsToSkip.size(); i++ )
		{
			debug( "colsToSkip(" + i + ")=" + colsToSkip.get( i ) );
		}

		for( int i = 2; i < ( numCols + 2 ); i++ )
		{
			if( !colsToSkip.contains( i ) )
			{
				debug( "colsToSkip() must not contain =" + i );
				sb.append( ", $" + i );
			}
		}

		if( demuxIndex != numCols )
		{
			sb.append( ", $" + demuxIndex );
		}

		if( descIndex != ( numCols + 1 ) )
		{
			sb.append( ", $" + descIndex );
		}

		final String path = getTempDir().getAbsolutePath() + File.separator + ORDERED_MAPPING;
		final File orderedMapping = new File( path );

		sb.append( " }' " + config.getMetaPath() + " > " + path );

		config.setMetadata( orderedMapping );

		return sb.toString();
	}

	/**
	 * Get mapping dir, called "mapping" which is the directory the new mapping is output by Qiime
	 * validate_mapping_file.py.
	 * @return
	 * @throws Exception
	 */
	private String getMappingDir() throws Exception
	{
		final File dir = new File( getOutputDir().getAbsolutePath() + File.separator + "mapping" );
		if( !dir.mkdirs() )
		{
			throw new Exception( "ERROR: Unable to create: " + dir );
		}

		return dir.getAbsolutePath() + File.separator;
	}

	private String getSortedMap() throws Exception
	{
		return getTempDir().getAbsolutePath() + File.separator + SORTED_MAP;
	}

	/**
	 * Get zipExe from prop file.
	 * @param f
	 * @return
	 * @throws Exception
	 */
	private String getZipExe( final File f ) throws Exception
	{
		final String name = f.getName().toLowerCase();
		if( name.endsWith( ".gz" ) )
		{
			return requireString( EXE_GZIP );
		}

		return null;
	}

	/**
	 * When rearranging files, skip any index when adding columns, if it will be moved.
	 * @param field
	 * @param cols
	 * @param sb
	 * @param colsToSkip
	 * @param colMsg
	 * @return
	 */
	private int skipIndex( final String field, final List<String> cols, final StringBuffer sb,
			final List<Integer> colsToSkip, final String colMsg )
	{
		final int index = ( cols.indexOf( field ) + 2 );
		sb.append( ", $" + index ); // $9
		colsToSkip.add( index ); // 9
		info( field + " found in column #" + index + " but QIIME requires " + colMsg );
		return index;
	}

	private String sortMetadata() throws Exception
	{
		final String map = config.getMetadata().getAbsolutePath();
		return "(head -n 1 " + map + " && tail -n +2 " + map + " | sort -n) > " + getSortedMap();
	}

	/**
	 * Get line for bash script to unzip file.
	 * @param zipExe
	 * @param f
	 * @param filePath
	 * @return
	 */
	private String unzip( final String zipExe, final File f, final String filePath )
	{
		return zipExe + " -cd " + f.getAbsolutePath() + " > " + filePath;
	}

	/**
	 * Call validate_mapping_file.py to get corrected QiimeMapping.
	 * @return
	 * @throws Exception
	 */
	private String validateMapping() throws Exception
	{
		return SCRIPT_VALIDATE_MAPPING + getSortedMap() + " -o " + getMappingDir() + " -j " + DEMUX_COLUMN;
	}
}
