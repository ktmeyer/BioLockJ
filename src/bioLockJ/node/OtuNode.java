/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Jun 2, 2017
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
package bioLockJ.node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * OtuNode holds taxonomy info representing one line of output from the classifier output file.
 */
public abstract class OtuNode extends bioLockJ.BioLockJ
{
	private static final Map<String, String> delimToTaxaLevelMap = new HashMap<>();
	private static final Set<Integer> levels = new HashSet<>();
	private static final List<String> taxaLevels = new ArrayList<>();
	private final TreeMap<String, Integer> classMap3 = new TreeMap<>();
	private final TreeMap<String, Integer> domainMap1 = new TreeMap<>();
	private final TreeMap<String, Integer> familyMap5 = new TreeMap<>();
	private final StringBuffer fullName = new StringBuffer();
	private final TreeMap<String, Integer> fullNameMap = new TreeMap<>();
	private final TreeMap<String, Integer> genusMap6 = new TreeMap<>();
	private String id = "";
	private String oneLevelUp = "";
	private final TreeMap<String, Integer> orderMap4 = new TreeMap<>();
	private final TreeMap<String, Integer> phylumMap2 = new TreeMap<>();
	private final TreeMap<String, Integer> speciesMap7 = new TreeMap<>();

	private Map<String, Integer> topMap = null;

	static
	{
		if( taxonomyLevels.contains( DOMAIN ) )
		{
			delimToTaxaLevelMap.put( DOMAIN_DELIM, DOMAIN );
			taxaLevels.add( DOMAIN );
			levels.add( 1 );
		}
		if( taxonomyLevels.contains( PHYLUM ) )
		{
			delimToTaxaLevelMap.put( PHYLUM_DELIM, PHYLUM );
			taxaLevels.add( PHYLUM );
			levels.add( 2 );
		}
		if( taxonomyLevels.contains( CLASS ) )
		{
			delimToTaxaLevelMap.put( CLASS_DELIM, CLASS );
			taxaLevels.add( CLASS );
			levels.add( 3 );
		}
		if( taxonomyLevels.contains( ORDER ) )
		{
			delimToTaxaLevelMap.put( ORDER_DELIM, ORDER );
			taxaLevels.add( ORDER );
			levels.add( 4 );
		}
		if( taxonomyLevels.contains( FAMILY ) )
		{
			delimToTaxaLevelMap.put( FAMILY_DELIM, FAMILY );
			taxaLevels.add( FAMILY );
			levels.add( 5 );
		}
		if( taxonomyLevels.contains( GENUS ) )
		{
			delimToTaxaLevelMap.put( GENUS_DELIM, GENUS );
			taxaLevels.add( GENUS );
			levels.add( 6 );
		}
		if( taxonomyLevels.contains( SPECIES ) )
		{
			delimToTaxaLevelMap.put( SPECIES_DELIM, SPECIES );
			taxaLevels.add( SPECIES );
			levels.add( 7 );
		}

		log.info( "OtuNode static maps initialilzed" );
	}

	/**
	 * Get taxonomyLevel based on levelDelim found in node taxa name.
	 * @param nodeTaxa
	 * @return
	 */
	public static String getTaxaLevel( final String nodeTaxa )
	{
		if( nodeTaxa.contains( DOMAIN_DELIM ) )
		{
			return DOMAIN;
		}
		if( nodeTaxa.contains( PHYLUM_DELIM ) )
		{
			return PHYLUM;
		}
		if( nodeTaxa.contains( CLASS_DELIM ) )
		{
			return CLASS;
		}
		if( nodeTaxa.contains( ORDER_DELIM ) )
		{
			return ORDER;
		}
		if( nodeTaxa.contains( FAMILY_DELIM ) )
		{
			return FAMILY;
		}
		if( nodeTaxa.contains( GENUS_DELIM ) )
		{
			return GENUS;
		}
		if( nodeTaxa.contains( SPECIES_DELIM ) )
		{
			return SPECIES;
		}
		return "[" + nodeTaxa + " TAXA LEVEL NOT FOUND]";
	}

	/**
	 * Get the map relating levelDelim to taxaLevel.
	 * @return
	 */
	protected static Map<String, String> getDelimToTaxaLevelMap()
	{
		return delimToTaxaLevelMap;
	}

	/**
	 * Add count to node, if a taxa has been reported for the same sample ID, increase the count,
	 * otherwise create a new node with inital count value = count method param.
	 * Example: addCount( "domain", "Bacteria", 487 );
	 * @param level
	 * @param name
	 * @param count
	 * @throws Exception
	 */
	public void addCount( final String level, final String name, final int count ) throws Exception
	{
		final int newCount = count + getCount( level, name );
		getMap( level ).put( name, newCount );
	}

	/**
	 * Get the sampleID (or possibly seq Id for RDP or Kraken)
	 * @return
	 */
	public String getId()
	{
		return id;
	}

	/**
	 * Get the level specific map for the level provided.
	 * Example: getMap( "domain" ).put( "Bacteria", 487 );
	 * @param level
	 * @return
	 * @throws Exception
	 */
	public TreeMap<String, Integer> getMap( final String level ) throws Exception
	{
		if( level.equals( DOMAIN ) )
		{
			return domainMap1;
		}
		if( level.equals( PHYLUM ) )
		{
			return phylumMap2;
		}
		if( level.equals( CLASS ) )
		{
			return classMap3;
		}
		if( level.equals( ORDER ) )
		{
			return orderMap4;
		}
		if( level.equals( FAMILY ) )
		{
			return familyMap5;
		}
		if( level.equals( GENUS ) )
		{
			return genusMap6;
		}
		if( level.equals( SPECIES ) )
		{
			return speciesMap7;
		}
		throw new Exception( level + " is not a valid taxonomy level" );
	}

	/**
	 * Get numHits found by checking how many hits found at the top level reported,
	 * typically phylum by default.
	 * @return
	 * @throws Exception
	 */
	public int getNumHits() throws Exception
	{
		int total = 0;
		final Map<String, Integer> map = getTopMap();
		for( final String taxa: map.keySet() )
		{

			total += map.get( taxa );
		}
		return total;
	}

	/**
	 * This method is called if a sample has already reported counts for a given taxa.
	 * @param node
	 * @throws Exception
	 */
	public void mergeNode( final OtuNode node ) throws Exception
	{
		for( final String level: taxaLevels )
		{
			final Map<String, Integer> map = node.getMap( level );
			if( map != null )
			{
				for( final String taxa: map.keySet() )
				{
					addCount( level, taxa, map.get( taxa ) );
				}
			}
		}
	}

	/**
	 * setId is called when there is a line by line ID provided in classifier output.
	 * @param sampleId
	 */
	public void setId( final String sampleId )
	{
		id = sampleId;
	}

	/**
	 * A critical method used to populate the name of a taxa level.
	 * Species may be formatted as Genus_Species or just first initial G. species.
	 * Users can also configure to always use full taxonomy names for all reported nodes.
	 * @param name
	 * @param levelDelim
	 * @return
	 * @throws Exception
	 */
	protected String buildName( String name, final String levelDelim ) throws Exception
	{
		if( name.isEmpty() )
		{
			return oneLevelUp;
		}

		final String currentOneLevelUp = oneLevelUp;
		oneLevelUp = name;

		if( useFullNames )
		{
			oneLevelUp = fullName.toString();
			if( fullName.length() != 0 )
			{
				fullName.append( "_" );
			}
			fullName.append( name );
			return fullName.toString();
		}

		if( getTaxaLevel( levelDelim ).equals( SPECIES ) )
		{
			if( addGenusFirstInitialToSpecies )
			{
				name = currentOneLevelUp.substring( 0, 1 ) + emptySpaceDelim + name;
			}
			else if( addGenusNameToSpecies )
			{
				name = currentOneLevelUp + emptySpaceDelim + name;
			}

		}
		return name;
	}

	/**
	 * Currently not used, returns a handle to a map of every fully quallified taxa
	 * with it's related count.
	 * @return
	 */
	protected TreeMap<String, Integer> getFullNameMap()
	{
		return fullNameMap;
	}

	/**
	 * Set the fullNameMap count & reset the oneLEvelUp & fullName OtuNode variables.
	 * @param count
	 */
	protected void setFullNameCount( final int count )
	{
		fullNameMap.put( fullName.toString(), count );
		oneLevelUp = "";
		fullName.setLength( 0 );
	}

	/**
	 * Return the count for a given taxaLevel for this node ID.
	 * @param level
	 * @param name
	 * @return
	 * @throws Exception
	 */
	private int getCount( final String level, final String name ) throws Exception
	{
		Integer count = getMap( level ).get( name );
		if( count == null )
		{
			count = 0;
		}
		return count;
	}

	/**
	 * Return the top level map which can be used to find numHits once all nodes are populated.
	 * @return
	 * @throws Exception
	 */
	private Map<String, Integer> getTopMap() throws Exception
	{
		if( topMap != null )
		{
			return topMap;
		}

		int topLevel = 8;
		for( final int x: levels )
		{
			if( x < topLevel )
			{
				topLevel = x;
			}
		}

		if( topLevel == 1 )
		{
			info( "Num_Hits calculated based on DOMAIN hits." );
			topMap = domainMap1;
		}
		if( topLevel == 2 )
		{
			info( "Num_Hits calculated based on PHYLUM hits." );
			topMap = phylumMap2;
		}
		if( topLevel == 3 )
		{
			info( "Num_Hits calculated based on CLASS hits." );
			topMap = classMap3;
		}
		if( topLevel == 4 )
		{
			info( "Num_Hits calculated based on ORDER hits." );
			topMap = orderMap4;
		}
		if( topLevel == 5 )
		{
			info( "Num_Hits calculated based on FAMILY hits." );
			topMap = familyMap5;
		}
		if( topLevel == 6 )
		{
			info( "Num_Hits calculated based on GENUS hits." );
			topMap = genusMap6;
		}
		if( topLevel == 7 )
		{
			info( "Num_Hits calculated based on SPECIES hits." );
			topMap = speciesMap7;
		}

		if( topMap != null )
		{
			return topMap;
		}

		throw new Exception( "OTU Node top level taxonomy not found!" );
	}

}