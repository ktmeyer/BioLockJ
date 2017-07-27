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

import java.util.HashMap;
import java.util.Map;
import bioLockJ.Module;
import bioLockJ.node.OtuNode;

/**
 * To see file format example: head 7A_1_phylum_reported.tsv
 *
 * No. Name Taxid NoOfReads RelativeAbundance Contributers Coverage
 * 1 Bacteroidetes 976 1137994 29.7589 17 24.7204
 */
public class SlimmNode extends OtuNode
{

	private static Map<String, String> taxaLevelMap = new HashMap<>();

	static
	{
		if( taxonomyLevels.contains( Module.DOMAIN ) )
		{
			taxaLevelMap.put( SLIMM_DOMAIN_DELIM, Module.DOMAIN );
		}
		if( taxonomyLevels.contains( Module.PHYLUM ) )
		{
			taxaLevelMap.put( PHYLUM, PHYLUM );
		}
		if( taxonomyLevels.contains( CLASS ) )
		{
			taxaLevelMap.put( CLASS, CLASS );
		}
		if( taxonomyLevels.contains( ORDER ) )
		{
			taxaLevelMap.put( ORDER, ORDER );
		}
		if( taxonomyLevels.contains( FAMILY ) )
		{
			taxaLevelMap.put( FAMILY, FAMILY );
		}
		if( taxonomyLevels.contains( GENUS ) )
		{
			taxaLevelMap.put( GENUS, GENUS );
		}
		if( taxonomyLevels.contains( SPECIES ) )
		{
			taxaLevelMap.put( SPECIES, SPECIES );
		}
	}

	/**
	 * Here is where we map out the taxa.
	 *
	 * @param fileLine
	 * @throws Exception
	 */
	public SlimmNode( final String fileName, final String line ) throws Exception
	{
		final String[] parts = line.split( DELIM );
		String taxa = parts[ 1 ];
		final int count = ( Integer.valueOf( parts[ 3 ] ) );
		for( final String levelDelim: taxaLevelMap.keySet() )
		{
			if( fileName.contains( levelDelim ) )
			{
				taxa = taxa.replaceAll( levelDelim, "" );
				addCount( taxaLevelMap.get( levelDelim ), taxa, count );
				break;
			}
		}
	}
}