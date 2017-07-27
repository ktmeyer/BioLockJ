/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 18, 2017
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
package bioLockJ.module.postProcessor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import bioLockJ.Module;
import bioLockJ.util.MetadataUtil;

/**
 * This class builds the R script.
 */
public class RScriptBuilder extends Module
{
	private static final ArrayList<String> data = new ArrayList<>();
	private static List<String> filterOperators = null;
	private static List<String> filterValues = null;
	private static boolean logNormal;

	private static final String MAIN_BODY_INDENT = INDENT + INDENT + INDENT;
	private static final List<String> queryFilters = new ArrayList<>();
	private static final String R_SCRIPT_SUCCESS = R_SCRIPT_NAME + SCRIPT_SUCCEEDED;
	private static String rareTaxaLimit = null;
	private static File rScript = null;
	private static BufferedWriter writer = null;
	private static String ylab = "Raw Abundance";

	/**
	 * Set the y-label based on if logNormal or not.
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		logNormal = requireBoolean( R_LOG_NORMALIZED );
		rareTaxaLimit = requireString( R_RARE_OTU_THRESHOLD );
		histNumBreaks = requirePositiveInteger( R_NUM_HISTAGRAM_BREAKS );
		filterOperators = getList( R_FILTER_OPERATORS );
		filterValues = getList( R_FILTER_VALUES );
		if( logNormal )
		{
			ylab = RELATIVE_ABUNDANCE;
		}

		initQueryFilters();
	}

	/**
	 * Add numRead & numHits if configured.  Build the R script.
	 */
	@Override
	public void executeProjectFile() throws Exception
	{
		if( reportNumReads )
		{
			if( config.getMetaUtil().getAttributeNames().contains( NUM_READS ) )
			{
				reportAttributes.add( NUM_READS );
			}
			else
			{
				warn( "Unable to report NUM_READS - value not found in metadata: "
						+ config.getMetadata().getAbsolutePath() );
			}

		}
		if( reportNumHits )
		{
			if( config.getMetaUtil().getAttributeNames().contains( NUM_HITS ) )
			{
				reportAttributes.add( NUM_HITS );
			}
			else
			{
				warn( "Unable to report NUM_HITS - value not found in metadata: "
						+ config.getMetadata().getAbsolutePath() );
			}
		}

		initRScript();
		initRStructures();
		populateOutputVectors();
		getFDRpAdjustedVectors();
		plotBoxPlots();
		plotSummaryHistograms();
		outputSummaryTable();
		closeRScript();
	}

	/**
	 * Get the name of the rScript from the prop file.
	 */
	@Override
	public File getMainScript() throws Exception
	{
		if( rScript != null )
		{
			return rScript;
		}

		throw new Exception( "R Script not found: " + rScript.getAbsolutePath() );

	}

	/**
	 * Determine the model type based on attribute type.
	 * @param attribute
	 * @throws Exception
	 */
	private void addAttributeAnalysis( final String attribute ) throws Exception
	{
		final String type = config.getMetaUtil().getAttributeType( attribute );
		switch( type )
		{
			case BINARY:
				addBinaryAnalysis( attribute );
				break;
			case CATEGORICAL:
				addCategoricalAnalysis( attribute );
				break;
			case CONTINUOUS:
				addContinuousAnalysis( attribute );
				break;
			case ORDINAL:
				addOrdinalAnalysis( attribute );
				break;
			default:
				throw new Exception(
						"Error occurred in addAttributeAnalysis(" + attribute + ")" + "INVALID TYPE ( " + type + ")" );
		}
	}

	private void addBinaryAnalysis( final String attribute ) throws Exception
	{
		final String cap = capitalize( attribute );
		final List<String> typedValues = getTypedBinaryValues( attribute );
		addLine( MAIN_BODY_INDENT + "myLm = lm( colVals ~ " + attribute + ", na.action=na.exclude )" );
		addLine( MAIN_BODY_INDENT + P_VALS + cap + "[index] = pvalue( wilcox_test( myT[myT$" + attribute + "=="
				+ typedValues.get( 0 ) + ", i], myT[myT$" + attribute + "==" + typedValues.get( 1 ) + ", i] ) )" );
		addLine( MAIN_BODY_INDENT + R_SQUARED + cap + "[index] = summary( myLm )$r.squared" );
	}

	private void addCategoricalAnalysis( final String attribute ) throws Exception
	{
		final String cap = capitalize( attribute );
		addLine( MAIN_BODY_INDENT + "myLm = lm( colVals ~ " + attribute + ", na.action=na.exclude )" );
		addLine( MAIN_BODY_INDENT + "myAnova = anova( myLm )" );
		addLine( MAIN_BODY_INDENT + P_VALS + cap + "[index] = myAnova$\"Pr(>F)\"[1]" );
		addLine( MAIN_BODY_INDENT + R_SQUARED + cap + "[index] = summary( myLm )$r.squared" );
	}

	private void addContinuousAnalysis( final String attribute ) throws Exception
	{
		final String cap = capitalize( attribute );
		final String rSqrIndex = R_SQUARED + cap + "[index]";
		addLine( MAIN_BODY_INDENT + P_VALS + cap + "[index] = Kendall( colVals, " + attribute + " )$sl[1]" );
		addLine( MAIN_BODY_INDENT + rSqrIndex + " = cor( colVals, " + attribute
				+ ", use=\"na.or.complete\", method=\"kendall\" )" );
		addLine( MAIN_BODY_INDENT + rSqrIndex + " = " + rSqrIndex + " * " + rSqrIndex );
	}

	private void addLine( final String line ) throws Exception
	{
		writer.write( line + "\n" );
	}

	private void addOrdinalAnalysis( final String attribute ) throws Exception
	{
		addLine( MAIN_BODY_INDENT + "(TBD) addOrdinalAnalysis( " + attribute + ")" );
	}

	private String capitalize( final String val )
	{
		final String firstChar = val.substring( 0, 1 );
		return firstChar.toUpperCase() + val.substring( 1 );
	}

	private void closeRScript() throws Exception
	{
		addLine( INDENT + "dev.off()" );
		addLine( "} \n" );
		writer.flush();
		writer.close();
	}

	/**
	 * Filter can be a rawCount value, or a percentage or rows value (if between 0 & 1).
	 * @return
	 */
	private String filterRareTaxa()
	{
		if( ( rareTaxaLimit == null ) || rareTaxaLimit.equals( "0" ) || rareTaxaLimit.equals( "1" ) )
		{
			return null;
		}

		String filterValue = "";
		try
		{
			final Double val = Double.valueOf( rareTaxaLimit );

			if( val < 0 )
			{
				throw new Exception();
			}
			if( val < 1 )
			{
				filterValue = "( nrow(myT) * " + rareTaxaLimit + " )";
			}
			else
			{
				filterValue = rareTaxaLimit;
			}
		}
		catch( final Exception ex )
		{
			error( "Unable to filter rare tax in the R Script.  Property (" + R_RARE_OTU_THRESHOLD
					+ ") must be a numeric value.  If between 0-1 will filter as percentage, otherwise as the minimum threshold.",
					ex );

		}

		return "if( sum( myT[,i] > 0 ) >= " + filterValue + " )";
	}

	private String getAdjP( final String adjPval )
	{
		return "format(" + adjPval + "[index], digits=3 )";
	}

	/**
	 * Get all of the fields in the script.
	 * @return
	 */
	private String getData()
	{
		final StringBuffer sb = new StringBuffer();
		final Iterator<String> it = data.iterator();
		while( it.hasNext() )
		{
			sb.append( it.next() );
			if( it.hasNext() )
			{
				sb.append( ", " );
			}
		}

		return sb.toString();
	}

	/**
	 * Get adjusted pvalue using method=BH
	 * @throws Exception
	 */
	private void getFDRpAdjustedVectors() throws Exception
	{
		addLine( "\n" );
		for( final String attribute: reportAttributes )
		{
			addLine( INDENT + P_VALS + "Adjusted_" + capitalize( attribute ) + " = p.adjust( " + P_VALS
					+ capitalize( attribute ) + ", method = \"BH\") " );
		}
	}

	private String getHistNumBreaks()
	{
		return String.valueOf( histNumBreaks );
	}

	/**
	 * Add table filter if configured in prop file.
	 * @return
	 * @throws Exception
	 */
	private String getPreFilter() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		if( ( queryFilters != null ) && !queryFilters.isEmpty() )
		{
			sb.append( "myT = myT[ " );
			for( final String filter: queryFilters )
			{
				sb.append( ( sb.length() == 0 ) ? filter: " && " + filter );
			}

			sb.append( ", ]" );
		}

		return sb.toString();
	}

	private String getTaxa()
	{
		return "strtrim( names[index], " + rMaxTitleSize + " )";
	}

	private String getTaxaLevels() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( "taxaLevels = c( " );

		final Iterator<String> it = taxonomyLevels.iterator();
		while( it.hasNext() )
		{
			sb.append( "\"" + it.next() + "\"" );
			if( it.hasNext() )
			{
				sb.append( ", " );
			}
		}

		sb.append( " )" );

		return sb.toString();
	}

	/**
	 * Typed values required quotes if string values, otherwise just the number value.
	 * @param attribute
	 * @return
	 * @throws Exception
	 */
	private List<String> getTypedBinaryValues( final String attribute ) throws Exception
	{
		final List<String> values = config.getMetaUtil().getValues( attribute );
		try
		{
			Integer.parseInt( values.get( 0 ) );
			Integer.parseInt( values.get( 1 ) );
			debug( "Getting binary attribute ( " + attribute + " ) value as type = integer, values=" + values.get( 0 )
					+ ", " + values.get( 1 ) );
		}
		catch( final Exception ex )
		{
			final String val0 = values.get( 0 );
			final String val1 = values.get( 1 );
			values.clear();
			values.add( "\"" + val0 + "\"" );
			values.add( "\"" + val1 + "\"" );
			debug( "Getting binary attribute ( " + attribute + " ) value as type = character, values=" + "\"" + val0
					+ "\"" + ", " + "\"" + val1 + "\"" );
		}

		return values;
	}

	/**
	 * Add quotes if needed.
	 * @param attribute
	 * @return
	 * @throws Exception
	 */
	private List<String> getTypedCategoryValues( final String attribute ) throws Exception
	{
		final List<String> returnVals = new ArrayList<>();
		final List<String> values = config.getMetaUtil().getValues( attribute );

		for( final String val: values )
		{
			try
			{
				Integer.parseInt( val );
				debug( "Getting category attribute ( " + attribute + " ) value as type = integer, value=" + val );
				returnVals.add( val );
			}
			catch( final Exception ex )
			{
				returnVals.add( "\"" + val + "\"" );
				debug( "Getting category attribute ( " + attribute + " ) value as type = character, value=" + "\"" + val
						+ "\"" );
			}
		}

		return returnVals;
	}

	private void initializeBinaryOutputVectors( final List<String> attributes, final String prefix ) throws Exception
	{
		for( final String attribute: attributes )
		{
			final String cap = capitalize( attribute );
			debug( "initializeBinaryOutputVectors: " + attribute );
			final List<String> values = config.getMetaUtil().getValues( attribute );
			final String field1 = prefix + cap + "_" + values.get( 0 );
			final String field2 = prefix + cap + "_" + values.get( 1 );
			addLine( INDENT + field1 + " = vector( mode=\"double\" )" );
			addLine( INDENT + field2 + " = vector( mode=\"double\" )" );
			data.add( field1 );
			data.add( field2 );
		}
	}

	private void initializeCategoryOutputVectors( final List<String> attributes, final String prefix ) throws Exception
	{
		for( final String attribute: attributes )
		{
			final String cap = capitalize( attribute );
			debug( "initializeCategoryOutputVectors: " + attribute );
			final List<String> values = config.getMetaUtil().getValues( attribute );
			for( final String val: values )
			{
				final String field = prefix + cap + "_" + val;
				addLine( INDENT + field + " = vector( mode=\"double\" )" );
				data.add( field );
			}
		}
	}

	private void initializeSummaryTableVectors( final String prefix ) throws Exception
	{
		final List<String> binaryAttributes = new ArrayList<>();
		final List<String> categoryAttributes = new ArrayList<>();

		for( final String attribute: reportAttributes )
		{
			if( config.getMetaUtil().getAttributeType( attribute ).equals( BINARY ) )
			{
				binaryAttributes.add( attribute );
			}
			else if( config.getMetaUtil().getAttributeType( attribute ).equals( CATEGORICAL ) )
			{
				categoryAttributes.add( attribute );
			}
		}

		initializeBinaryOutputVectors( binaryAttributes, prefix );
		initializeCategoryOutputVectors( categoryAttributes, prefix );
	}

	/**
	 * Build the query filters based on prop file values.
	 * @throws Exception
	 */
	private void initQueryFilters() throws Exception
	{
		final int attSize, opSize, valSize;

		attSize = filterAttributes.size();
		opSize = filterOperators.size();
		valSize = filterValues.size();

		final Iterator<String> nalIt = filterNaAttributes.iterator();
		final Iterator<String> attIt = filterAttributes.iterator();
		final Iterator<String> opIt = filterOperators.iterator();
		final Iterator<String> valIt = filterValues.iterator();

		if( ( attSize != opSize ) || ( attSize != valSize ) )
		{
			throw new Exception(
					"CONFIG FILE ERROR >>> THESE PROPERTIES MUST BE COMMA DELIMITED LISTS OF THE SAME LENGTH [ "
							+ R_FILTER_ATTRIBUTES + ", " + R_FILTER_OPERATORS + ", " + R_FILTER_VALUES + " ]"
							+ "CURRENT LENGTHS = [ " + attSize + ", " + " ]" + opSize + ", " + valSize );
		}

		while( attIt.hasNext() )
		{
			queryFilters.add( "(myT$" + attIt.next() + " " + opIt.next() + " " + valIt.next() + ")" );
		}

		while( nalIt.hasNext() )
		{
			queryFilters.add( "!is.na(myT$" + nalIt.next() + ")" );
		}

	}

	/**
	 * Create the R_SCRIPT_SUCCESS file here during initialization. Otherwise,
	 * if created at the end (as per usual) an error in R script processing
	 * could prevent the file from ever being created, leaving the program to
	 * loop indefinitely.
	 *
	 * @throws Exception
	 */
	private void initRScript() throws Exception
	{
		rScript = new File( getScriptDir() + File.separator + R_SCRIPT_NAME );
		addScriptFile( rScript );
		writer = new BufferedWriter( new FileWriter( rScript ) );
		addLine( "rm( list=ls() )" );
		addLine( "pdf( \"" + getScriptDir() + File.separator + R_SCRIPT_SUCCESS + "\" )" );
		// addLine( "install.packages( \"Kendall\", repos = \"http://cran.us.r-project.org\" ) " );
		// addLine( "install.packages( \"coin\", repos = \"http://cran.us.r-project.org\" ) " );
		addLine( "library( \"Kendall\" )" );
		addLine( "library( \"coin\" )" );
		addLine( "setwd(\"" + getOutputDir().getAbsolutePath() + "\")" );
		addLine( getTaxaLevels() );
		addLine( "sessionInfo()" );
		addLine( "wilcox_test.default <- function(x, y, ...) {" );
		addLine( "    data=data.frame(values = c(x, y), group = rep(c(\"x\", \"y\"), c(length(x), length(y))))" );
		addLine( "    wilcox_test(values ~ group, data = data, ...)" );
		addLine( "}" );
		addLine( "for( taxa in taxaLevels ) \n {" );
	}

	/**
	 * Initialize the R structures for all attributes.
	 * @throws Exception
	 */
	private void initRStructures() throws Exception
	{
		final String suffix = ( logNormal ? LOG_NORMAL: RAW_COUNT ) + META_MERGED_SUFFIX;
		addLine( INDENT + "pdf( paste( \"boxplots_\", taxa, \".pdf\", sep=\"\" ) )" );
		addLine( INDENT + "par( mfrow = c( 2, 2 ) )" );
		addLine( INDENT + "inFileName = paste( \"" + getInputDir().getAbsolutePath() + File.separator + "\", taxa, \""
				+ suffix + "\", sep =\"\" )" );
		addLine( INDENT + "myT = read.table( inFileName, header=TRUE, sep=\"\\t\" )" );
		if( ( getPreFilter() != null ) && ( getPreFilter().length() > 0 ) )
		{
			addLine( INDENT + getPreFilter() );
		}
		addLine( INDENT + "names = vector( mode=\"character\" )" );
		data.add( "names" );

		for( String attribute: reportAttributes )
		{
			if( config.getMetaUtil().getAttributeType( attribute ).equals( CATEGORICAL )
					|| config.getMetaUtil().getAttributeType( attribute ).equals( BINARY ) )
			{
				addLine( INDENT + attribute + " = factor( myT$" + attribute + " )" );
			}
			else
			{
				addLine( INDENT + attribute + " = myT$" + attribute );
			}
			attribute = capitalize( attribute );
			addLine( INDENT + P_VALS + attribute + " = vector( mode=\"double\" )" );
			addLine( INDENT + R_SQUARED + attribute + " = vector( mode=\"double\" )" );
			data.add( P_VALS + attribute );
			data.add( R_SQUARED + attribute );
		}

		initializeSummaryTableVectors( SAMPLE_SIZE );
		initializeSummaryTableVectors( MEAN );
	}

	/**
	 * Output lines for summary table for reportAttributes.
	 * @throws Exception
	 */
	private void outputSummaryTable() throws Exception
	{

		addLine( "" );
		addLine( INDENT + "dFrame = data.frame( " + getData() + " )" );

		for( final String attribute: reportAttributes )
		{
			final String att = "dFrame$" + P_VALS + "Adjusted_" + capitalize( attribute );
			addLine( INDENT + att + " = " + P_VALS + "Adjusted_" + capitalize( attribute ) );
		}

		addLine( INDENT
				+ "write.table( dFrame, file=paste( \"meta_pValuesFor_\", taxa, \".txt\", sep=\"\" ), sep=\"\\t\", row.names=FALSE )" );
	}

	/**
	 * Generate box plots for reportAttributes.
	 * @throws Exception
	 */
	private void plotBoxPlots() throws Exception
	{
		info( "Building Box Plots " );
		addLine( INDENT + "index = 1" );
		addLine( INDENT + "for( i in 2:lastTaxaCol )" );
		addLine( INDENT + "{" );
		final String rareTaxa = filterRareTaxa();
		String indent = INDENT;
		if( rareTaxa != null )
		{
			indent = indent + indent;
			addLine( indent + rareTaxa );
			addLine( indent + "{" );
		}

		addLine( MAIN_BODY_INDENT + "colVals = myT[,i]" );

		for( final String attribute: reportAttributes )
		{
			info( "Summary Attribute " + attribute + " (type=" + config.getMetaUtil().getAttributeType( attribute )
					+ ")" );
			final String cap = capitalize( attribute );
			final String adjPval = P_VALS + "Adjusted_" + cap;

			boolean isBinary = config.getMetaUtil().getAttributeType( attribute ).equals( BINARY );
			final boolean isCategorical = config.getMetaUtil().getAttributeType( attribute ).equals( CATEGORICAL );

			List<String> values = null;
			List<String> typedValues = null;
			if( isBinary )
			{
				values = config.getMetaUtil().getValues( attribute );
				if( values.size() < 2 )
				{
					isBinary = false;
					warn( "Binary attribute (" + attribute + ") has < 2 values.  Skipping summary plots." );
				}
				else
				{
					typedValues = getTypedBinaryValues( attribute );
				}
			}
			else if( isCategorical )
			{
				values = config.getMetaUtil().getValues( attribute );
				typedValues = getTypedCategoryValues( attribute );
			}

			if( isBinary || isCategorical )
			{
				addLine( MAIN_BODY_INDENT + "myFrame = data.frame( colVals, " + attribute + " )" );

				final StringBuffer colNames = new StringBuffer();
				colNames.append( "c( \"" + values.get( 0 ) + "\"" );
				for( int i = 1; i < values.size(); i++ )
				{
					colNames.append( ", \"" + values.get( i ) + "\"" );
				}

				colNames.append( ")" );

				final StringBuffer boxPlotColumns = new StringBuffer();
				boxPlotColumns.append( "myT[myT$" + attribute + "==" + typedValues.get( 0 ) + ",i]" );
				for( int i = 1; i < typedValues.size(); i++ )
				{
					boxPlotColumns.append( ", myT[myT$" + attribute + "==" + typedValues.get( i ) + ",i]" );
				}

				addLine( MAIN_BODY_INDENT + "boxplot( " + boxPlotColumns.toString() + ", main=paste( " + getTaxa()
						+ ", \"Adj P=\", " + getAdjP( adjPval ) + " ), names=" + colNames.toString()
						+ ", las=2, xlab=\"" + attribute + "\", ylab=\"" + ylab + "\" )" );

				addLine( MAIN_BODY_INDENT + "stripchart( colVals ~ " + attribute
						+ ", data=myFrame, vertical=TRUE, pch=21, add=TRUE )" );
			}
			else
			{
				addLine( MAIN_BODY_INDENT + "plot( " + attribute + ", colVals, main=paste( " + getTaxa()
						+ ", \"Adj P=\", " + getAdjP( adjPval ) + " ), xlab=\"" + attribute + "\", ylab=\"" + ylab
						+ "\" )" );
			}

			addLine( "" );
		}

		addLine( MAIN_BODY_INDENT + "index = index + 1" );

		if( rareTaxa != null )
		{
			addLine( indent + "}" );
		}

		addLine( INDENT + "}" );
	}

	/**
	 * Plot histograms of pvalues for reportAttributes
	 * @throws Exception
	 */
	private void plotSummaryHistograms() throws Exception
	{
		addLine( "" );
		final String numBreaks = getHistNumBreaks();
		for( final String attribute: reportAttributes )
		{
			final String pValVector = P_VALS + capitalize( attribute );
			addLine( INDENT + "if ( !is.nan( " + pValVector + " ) && !is.na( " + pValVector + " ) ) hist( " + pValVector
					+ ", breaks=" + numBreaks + ", xlab=\"P-Value\", main=\"" + attribute + "\" )" );
		}
	}

	private void populateBinaryOutputVectors( final List<String> attributes, final String prefix ) throws Exception
	{
		final String fun = prefix == MEAN ? "mean": "length";
		final String suffix2 = ", i] )";

		for( final String attribute: attributes )
		{
			final String cap = capitalize( attribute );
			final List<String> values = config.getMetaUtil().getValues( attribute );
			final List<String> typedValues = getTypedBinaryValues( attribute );

			final String field1 = prefix + cap + "_" + values.get( 0 );
			final String field2 = prefix + cap + "_" + values.get( 1 );
			final String suffix1 = fun + "( myT[myT$" + attribute + "==";

			addLine( MAIN_BODY_INDENT + field1 + "[index] = " + suffix1 + typedValues.get( 0 ) + suffix2 );
			addLine( MAIN_BODY_INDENT + field2 + "[index] = " + suffix1 + typedValues.get( 1 ) + suffix2 );
		}
	}

	private void populateCategoryOutputVectors( final List<String> attributes, final String prefix ) throws Exception
	{
		final String fun = prefix == MEAN ? "mean": "length";
		final String suffix2 = ", i] )";

		for( final String attribute: attributes )
		{
			final String cap = capitalize( attribute );
			final String prefix2 = fun + "( myT[myT$" + attribute + "==";

			final List<String> values = config.getMetaUtil().getValues( attribute );
			final List<String> typedValues = getTypedCategoryValues( attribute );
			int index = 0;
			for( final String val: values )
			{
				final String field = prefix + cap + "_" + val;
				addLine( MAIN_BODY_INDENT + field + "[index] = " + prefix2 + typedValues.get( index++ ) + suffix2 );
			}
		}
	}

	/**
	 * Populate output vectors for reportAttributes
	 * @throws Exception
	 */
	private void populateOutputVectors() throws Exception
	{
		final int numMetaAtts = config.getMetaUtil().getAttributeNames().size();
		addLine( INDENT + "lastTaxaCol = ncol(myT) - " + numMetaAtts );
		addLine( INDENT + "index = 1" );
		addLine( INDENT + "for( i in 2:lastTaxaCol )" );
		addLine( INDENT + "{" );
		final String rareTaxa = filterRareTaxa();
		String indent = INDENT;
		if( rareTaxa != null )
		{
			indent = indent + indent;
			addLine( indent + rareTaxa );
			addLine( indent + "{" );
		}

		addLine( MAIN_BODY_INDENT + "names[index] = names(myT)[i]" );
		addLine( MAIN_BODY_INDENT + "colVals = myT[,i]" );
		addLine( "" );
		for( final String attribute: reportAttributes )
		{
			addAttributeAnalysis( attribute );
			addLine( "" );
		}

		populateSummaryTableVectors( reportAttributes, SAMPLE_SIZE );
		populateSummaryTableVectors( reportAttributes, MEAN );

		addLine( MAIN_BODY_INDENT + "index = index + 1" );

		if( rareTaxa != null )
		{
			addLine( indent + "}" );
		}

		addLine( INDENT + "}" );
	}

	private void populateSummaryTableVectors( final List<String> attributes, final String prefix ) throws Exception
	{
		final List<String> binaryAttributes = new ArrayList<>();
		final List<String> categoryAttributes = new ArrayList<>();

		for( final String attribute: attributes )
		{
			if( config.getMetaUtil().getAttributeType( attribute ).equals( MetadataUtil.BINARY ) )
			{
				binaryAttributes.add( attribute );
			}
			else if( config.getMetaUtil().getAttributeType( attribute ).equals( MetadataUtil.CATEGORICAL ) )
			{
				categoryAttributes.add( attribute );
			}
		}

		populateBinaryOutputVectors( binaryAttributes, prefix );
		populateCategoryOutputVectors( categoryAttributes, prefix );
	}

}
