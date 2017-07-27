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
 * To see file format example: head 7A_1_reported.tsv
 *
 * FCC6MMAACXX:8:1101:1968:2100#GTATTCTC/1
 * d__Bacteria|p__Bacteroidetes|c__Bacteroidia|o__Bacteroidales|f__Bacteroidaceae|g__Bacteroides|s__Bacteroides_vulgatus
 *
 */
public class KrakenNode extends OtuNode
{

	/**
	 * Here is where we map out the taxa.
	 *
	 * @param fileLine
	 * @throws Exception
	 */
	public KrakenNode( final String line ) throws Exception
	{
		final StringTokenizer st = new StringTokenizer( line, DELIM );
		final Map<String, String> map = getDelimToTaxaLevelMap();
		if( st.countTokens() == 2 )
		{
			setId( st.nextToken() );
			final StringTokenizer taxaToken = new StringTokenizer( st.nextToken(), KRAKEN_DELIM );
			while( taxaToken.hasMoreTokens() )
			{
				String taxa = taxaToken.nextToken();
				for( final String levelDelim: map.keySet() )
				{
					if( taxa.contains( levelDelim ) )
					{
						taxa = taxa.replaceAll( levelDelim, "" );
						addCount( map.get( levelDelim ), buildName( taxa, levelDelim ), 1 );
						break;
					}
				}
			}

			setFullNameCount( 1 );
		}
		else
		{
			while( st.hasMoreTokens() )
			{
				warn( "Kraken token: " + st.nextToken() );
			}

			throw new Exception( "Invalid Record = (" + line + ")\n"
					+ "Kraken output must have exactly 2 tab delimited columns per line. " );
		}
	}
}