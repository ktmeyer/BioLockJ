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

import java.util.StringTokenizer;
import bioLockJ.node.OtuNode;

/**
 * To see file format example: head 7A_1_reported.tsv
 *
 * FCABK7W:1:2105:21787:12788#/1 Root rootrank 1.0 Bacteria domain 1.0
 * Firmicutes phylum 1.0 Clostridia class 1.0 Clostridiales order 1.0
 * Ruminococcaceae family 1.0 Faecalibacterium genus 1.0
 *
 */
public class RdpNode extends OtuNode
{
	private int score;

	public RdpNode( final String line ) throws Exception
	{
		final StringTokenizer st = new StringTokenizer( line, DELIM );
		setId( st.nextToken() );
		while( st.hasMoreTokens() )
		{
			String taxa = stripQuotes( st.nextToken() );

			if( taxa.trim().equals( "-" ) )
			{
				taxa = st.nextToken();
			}

			final String level = st.nextToken();
			if( taxonomyLevels.contains( level ) )
			{
				addCount( level, buildName( taxa, level ), 1 );
			}

			setScore( st.nextToken() );
		}

		setFullNameCount( 1 );
	}

	/*
	 * Should be between 1 and 100
	 */
	public int getScore()
	{
		return score;
	}

	private void setScore( String scoreString ) throws Exception
	{
		scoreString = scoreString.trim();

		if( scoreString.equals( "1" ) || scoreString.equals( "1.0" ) )
		{
			score = 100;
		}
		else if( scoreString.equals( "0" ) )
		{
			score = 0;
		}
		else
		{
			if( !scoreString.startsWith( "0." ) )
			{
				throw new Exception( "Unexpected score string: " + scoreString );
			}

			if( scoreString.length() == 3 )
			{
				scoreString = scoreString + "0";
			}

			score = Integer.parseInt( scoreString.substring( 2 ) );
		}

		if( ( score < 0 ) || ( score > 100 ) )
		{
			throw new Exception( "Unexpected score: " + score );
		}
	}

}