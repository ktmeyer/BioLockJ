package bioLockJ.module.classifier.r16s.qiime;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * This file is used to help work with the Qiime mapping file.
 * @author mike
 *
 */
public class QiimeMapping extends bioLockJ.BioLockJ
{
	private static List<String> alphaMetrics = new ArrayList<>();
	private static boolean initialized = false;
	private static final Map<String, String> qiimeIdToSampleIdMap = new HashMap<>();
	private static QiimeMapping qm = null;
	private static final List<String> requiredFields = new ArrayList<>();
	private static final Map<String, String> sampleIdToQiimeIdMap = new HashMap<>();

	static
	{
		requiredFields.add( BARCODE_SEQUENCE );
		requiredFields.add( LINKER_PRIMER_SEQUENCE );
		requiredFields.add( DEMUX_COLUMN );
		requiredFields.add( DESCRIPTION );
	}

	/**
	 * When QiimeMapping is instantiaed, initialize maps and set alphaMetrics.
	 */
	public QiimeMapping()
	{
		try
		{
			initializeMaps();
			alphaMetrics = getList( QIIME_ALPHA_DIVERSITY_METRICS );
			initialized = true;
			log.info( "QiimeMapping initialized" );
		}
		catch( final Exception ex )
		{
			error( "Error occurred initializing QiimeMapping()", ex );
		}
	}

	/**
	 * If alphaMetrics are added this method must be called to update the descriptor with the
	 * new columns.
	 *
	 * @param outputDir
	 * @throws Exception
	 */
	public static void addAlphaMetricsToDescriptor( final File outputDir ) throws Exception
	{
		final List<String> metrics = getAlphaMetricColNames();
		config.getMetaUtil().updateDescriptor( metrics, outputDir );
	}

	/**
	 * Statically get a reference to a QiimeMapping instance.
	 * @return
	 * @throws Exception
	 */
	public static QiimeMapping getMapping() throws Exception
	{
		if( qm == null )
		{
			qm = new QiimeMapping();
		}
		if( initialized )
		{
			return qm;
		}

		throw new Exception( "Error occurred initializing QiimeMapping" );

	}

	/**
	 * The required "#SampleID" column must be converted to a unique name that doesn't include
	 * the "#" symbol which is a special character in R.
	 * @param headerId
	 * @param restOfLine
	 * @return
	 */
	public static String getUniqueId( final String headerId, final String restOfLine )
	{
		String id = "SampleID";
		final ArrayList<String> colNames = new ArrayList<>( Arrays.asList( restOfLine.split( DELIM ) ) );

		int x = 0;
		if( headerId.equals( QIIME_ID ) )
		{
			while( colNames.contains( id ) && ( x < 8 ) )
			{
				switch( x )
				{
					case 0:
						id = "sampleId";
						x++;
						break;
					case 1:
						id = "id";
						x++;
						break;
					case 2:
						id = "ID";
						x++;
						break;
					case 3:
						id = "sample_id";
						x++;
						break;
					case 4:
						id = "SAMPLE_ID";
						x++;
						break;
					case 5:
						id = "BioLockJ_ID";
						x++;
						break;
					case 6:
						id = "BLJ_ID";
						x++;
						break;
					case 7:
						id = "SampleId";
						x++;
						break;
					default:
						x++;
						break;
				}
			}
		}

		return id;
	}

	
	/**
	 * A handle to the fields required in the mapping file.
	 * @return
	 */
	public static List<String> requiredFields()
	{
		return requiredFields;
	}

	/**
	 * Get the names of the new columns added based on the alphaMetrics prop value.
	 * @return
	 * @throws Exception
	 */
	private static List<String> getAlphaMetricColNames() throws Exception
	{
		final List<String> colNames = new ArrayList<>();
		final String alpha = "_alpha";
		final String norm = "_normalized";
		final String label = "_label";
		for( final String m: alphaMetrics )
		{
			colNames.add( m + alpha );
			colNames.add( m + norm + alpha );
			colNames.add( m + alpha + label );
		}
		return colNames;
	}

	/**
	 * This method returns only new columns that are not already included in the header.
	 * This is to ensure unique column names.
	 *
	 * @param line must be the header line containing column names
	 * @return
	 * @throws Exception
	 */
	private static List<String> getAlphaMetricCols( final String line ) throws Exception
	{
		final List<String> alphaMetrics = getAlphaMetricColNames();
		alphaMetrics.retainAll( getColNames( line ) );
		return alphaMetrics;
	}

	/**
	 * Get the alphaMetric column names.
	 * @param restOfLine
	 * @param metricColNames
	 * @return
	 * @throws Exception
	 */
	private static List<Integer> getAlphaMetricCols( final String restOfLine, final List<String> metricColNames )
			throws Exception
	{
		final List<Integer> cols = new ArrayList<>();
		final StringTokenizer header = new StringTokenizer( restOfLine, DELIM );
		int colNum = 0;
		while( header.hasMoreTokens() )
		{
			if( metricColNames.contains( header.nextToken() ) )
			{
				cols.add( colNum );
			}
			colNum++;
		}
		return cols;
	}

	/**
	 * Mapping row_id to alpha metrics found in old meta but not new meta (including header line).
	 * header key = QIIME_ID
	 * row key = SAMPLE_ID
	 * @param cReader
	 * @param oldMeta
	 * @param newHeader
	 * @return
	 * @throws Exception
	 */
	private static Map<String, List<String>> getAlphaMetricsMap( final File oldMeta, final String newHeader )
			throws Exception
	{
		final Map<String, List<String>> map = new HashMap<>();
		List<Integer> alphaMetricCols = new ArrayList<>();
		boolean isHeaderRow = true;
		final BufferedReader reader = new BufferedReader( new FileReader( oldMeta ) );
		try
		{
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				final String rowId = parseIdFromLine( line );
				final String restOfLine = line.substring( rowId.length() );
				if( isHeaderRow )
				{
					isHeaderRow = false;
					final List<String> oldAlphaMetrics = getAlphaMetricCols( restOfLine );
					oldAlphaMetrics.removeAll( getAlphaMetricCols( newHeader ) );
					if( oldAlphaMetrics.isEmpty() )
					{
						return null;
					}
					alphaMetricCols = getAlphaMetricCols( restOfLine, oldAlphaMetrics );
					map.put( QIIME_ID, oldAlphaMetrics );
				}
				else
				{
					final StringTokenizer st = new StringTokenizer( restOfLine, DELIM );
					int colNum = 0;
					final List<String> vals = new ArrayList<>();
					while( st.hasMoreTokens() )
					{
						final String token = st.nextToken();
						if( alphaMetricCols.contains( colNum ) )
						{
							vals.add( token );
						}
						colNum++;
					}

					if( !vals.isEmpty() )
					{
						map.put( rowId, vals );
					}
				}
			}
		}
		finally
		{
			reader.close();
		}

		return map;
	}

	/**
	 * Get all column names form the line.
	 * @param line - must be a header containing column names
	 * @return
	 * @throws Exception
	 */
	private static List<String> getColNames( final String line ) throws Exception
	{
		final List<String> cols = new ArrayList<>();
		final StringTokenizer st = new StringTokenizer( line, DELIM );
		while( st.hasMoreTokens() )
		{
			cols.add( st.nextToken() );
		}
		return cols;
	}

	/**
	 * Examines the line to find the column number of the DEMUX_COLUMN
	 * @param line - must be a header containing column names
	 * @return
	 */
	private static int getFileNameColumn( final String line )
	{
		final StringTokenizer header = new StringTokenizer( line, DELIM );
		int colNum = 0;
		while( header.hasMoreTokens() )
		{
			final String token = header.nextToken();
			log.info( "column(" + colNum + ") = " + token );
			if( token.equals( DEMUX_COLUMN ) )
			{
				return colNum;
			}
			colNum++;
		}
		return -1;
	}

	/**
	 * The mapping file contains the sampleID in the DEMUX_COLUMN where it is parsed out of the
	 * formatted file name: "sampleId.fasta"
	 * @param qiimeId
	 * @param fileNameCol
	 * @return
	 * @throws Exception
	 */
	private static String getSampleIdFromMappingFile( final String qiimeId, final int fileNameCol ) throws Exception
	{
		if( fileNameCol != -1 )
		{
			return config.getMetaUtil().getAttributes( qiimeId ).get( ( fileNameCol - 1 ) ).replaceAll( "." + FASTA,
					"" );
		}

		return qiimeId;
	}

	/**
	 * The mapping file QiimeID is the first column value.
	 * @param line
	 * @return
	 * @throws Exception
	 */
	private static String parseIdFromLine( final String line ) throws Exception
	{
		final StringTokenizer st = new StringTokenizer( line, DELIM );
		return st.nextToken();
	}

	/**
	 * Converts the QIIME mapping (with key=QIIME_ID) into standard metadata (with key=QIIME_ID).
	 * Converts header column #SAMPLE_ID into an R-friendly value (without a #).
	 * Populates the maps needed to convert QIIME_ID <=> SAMPLE_ID.
	 * @param metaUtil
	 * @param outputDir
	 * @return
	 * @throws Exception
	 */
	public File getMetadata( final File outputDir ) throws Exception
	{
		final File meta = config.getMetaUtil().getMetadata();
		final String name = meta.getName();
		final File newMeta = new File( outputDir.getAbsolutePath() + File.separator + name );
		boolean isHeaderRow = true;
		final BufferedReader reader = new BufferedReader( new FileReader( meta ) );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( newMeta ) );
		try
		{
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				final String rowId = parseIdFromLine( line );
				final String restOfLine = line.substring( rowId.length() );
				if( isHeaderRow )
				{
					isHeaderRow = false;
					line = getUniqueId( rowId, restOfLine ) + restOfLine;
				}
				else
				{
					line = getSampleId( rowId ) + restOfLine;
				}
				writer.write( line + "\n" );
			}
		}
		finally
		{
			reader.close();
			writer.flush();
			writer.close();
		}

		return newMeta;
	}

	/**
	 * Get the QiimeId from the sampleIdToQiimeIdMap.
	 * @param sampleId
	 * @return
	 * @throws Exception
	 */
	public String getQiimeId( final String sampleId ) throws Exception
	{
		final String qiimeId = sampleIdToQiimeIdMap.get( sampleId );
		if( qiimeId == null )
		{
			throw new Exception( "QiimeMapping cannot find Sample ID: " + sampleId );
		}
		return qiimeId;
	}

	/**
	 * Get the SampleID from the qiimeIdToSampleIdMap.
	 * @param qiimeId
	 * @return
	 * @throws Exception
	 */
	public String getSampleId( final String qiimeId ) throws Exception
	{
		final String sampleId = qiimeIdToSampleIdMap.get( qiimeId );
		if( sampleId == null )
		{
			throw new Exception( "QiimeMapping cannot find QIIME ID: " + qiimeId );
		}
		return sampleId;
	}

	/**
	 * Populate the qiimeIdToSampleIdMap & sampleIdToQiimeIdMap by reading each row in the formatted
	 * Qiime mapping file.
	 *
	 * @throws Exception
	 */
	private void initializeMaps() throws Exception
	{
		info( "Initialize QIIME_ID to SAMPLE_ID Maps for: " + config.getMetadata().getAbsolutePath() );
		int fileNameCol = 0;
		boolean isHeaderRow = true;
		int count = 0;
		final BufferedReader reader = new BufferedReader( new FileReader( config.getMetadata() ) );
		try
		{
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				final String qiimeId = parseIdFromLine( line );
				if( isHeaderRow )
				{
					isHeaderRow = false;
					fileNameCol = getFileNameColumn( line );
					info( "Header ID (" + qiimeId + ") has " + DEMUX_COLUMN + " in column #" + fileNameCol );
				}
				else
				{
					final String sampleId = getSampleIdFromMappingFile( qiimeId, fileNameCol );

					if( count++ < 1 )
					{
						info( "[Example Id-Map Entry] QIIME_ID(" + qiimeId + ")<=>SAMPLE_ID(" + sampleId + ")" );
					}
					else
					{
						debug( "[Id-Map Entry] QIIME_ID(" + qiimeId + ")<=>SAMPLE_ID(" + sampleId + ")" );
					}

					qiimeIdToSampleIdMap.put( qiimeId, sampleId );
					sampleIdToQiimeIdMap.put( sampleId, qiimeId );
				}
			}
		}
		finally
		{
			reader.close();
		}
	}
}