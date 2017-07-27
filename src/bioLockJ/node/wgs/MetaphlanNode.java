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
package bioLockJ.node.wgs;

import java.util.Map;
import java.util.StringTokenizer;
import bioLockJ.node.OtuNode;

/**
 * To see file format example: head 7A_1_processed.txt
 *
 * #SampleID Metaphlan2_Analysis
 * #clade_name relative_abundance coverage average_genome_length_in_the_clade estimated_number_of_reads_from_the_clade
 * k__Bacteria|p__Bacteroidetes 14.68863 0.137144143537 4234739 580770
 *
 */
public class MetaphlanNode extends OtuNode
{
	private static Map<String, String> taxaLevelMap = null;

	static
	{
		taxaLevelMap = getDelimToTaxaLevelMap();

		if( taxonomyLevels.contains( DOMAIN ) )
		{
			taxaLevelMap.remove( DOMAIN_DELIM );
			taxaLevelMap.put( METAPHLAN_DOMAIN_DELIM, DOMAIN );
		}
	}

	/**
	 * Here is where we map out the taxa.
	 *
	 * @param fileLine
	 * @throws Exception
	 */
	public MetaphlanNode( final String line ) throws Exception
	{
		final String[] parts = line.split( "\\s" );
		if( parts.length != 5 )
		{
			throw new Exception( "INVALID FILE FORMAT.  Line should have 5 parts.  LINE =  (" + line
					+ ") METAPHLAN CLASSIFICATION NOT RUN WITH SWITCH: -t (ANALYSIS_TYPE) rel_ab_w_read_stats.  Set property "
					+ EXE_CLASSIFIER_PARAMS + "=t rel_ab_w_read_stats in config file." );
		}

		final String allTaxa = parts[ 0 ];
		final int count = Integer.valueOf( parts[ 4 ] );

		final StringTokenizer st = new StringTokenizer( allTaxa, METAPHLAN_DELIM );
		while( st.hasMoreTokens() )
		{
			String taxa = st.nextToken();
			for( final String levelDelim: taxaLevelMap.keySet() )
			{
				if( taxa.contains( levelDelim ) )
				{
					taxa = taxa.replaceAll( levelDelim, "" );
					addCount( taxaLevelMap.get( levelDelim ), buildName( taxa, levelDelim ), count );
					break;
				}
			}
		}

		setFullNameCount( 1 );
	}

}