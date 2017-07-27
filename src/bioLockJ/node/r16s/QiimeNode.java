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
package bioLockJ.node.r16s;

import java.util.Map;
import java.util.StringTokenizer;
import bioLockJ.node.OtuNode;

/**
 * To see file format example: head otu_table_L2.txt
 *
 * # Constructed from biom file
 * #OTU ID 3A.1 6A.1 120A.1 7A.1
 * k__Bacteria;p__Actinobacteria 419.0 26.0 90.0 70.0
 *
 */
public class QiimeNode extends OtuNode
{
	private static Map<String, String> taxaLevelMap = null;

	static
	{
		taxaLevelMap = getDelimToTaxaLevelMap();

		if( taxonomyLevels.contains( DOMAIN ) )
		{
			taxaLevelMap.remove( DOMAIN_DELIM );
			taxaLevelMap.put( QIIME_DOMAIN_DELIM, DOMAIN );
		}
	}

	public QiimeNode( final String line, final int count ) throws Exception
	{
		final StringTokenizer st = new StringTokenizer( line, QIIME_DELIM );
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

		setFullNameCount( count );
	}

}