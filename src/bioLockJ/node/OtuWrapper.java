/**
 * @UNCC Fodor Lab
 * @author Anthony Fodor
 * @email anthony.fodor@gmail.com
 * @date Feb 9, 2017
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import bioLockJ.Constants;

/**
 * This method is a helper class to output log_normalized tables built from OtuNodes.
 */
public class OtuWrapper
{

	protected static final Logger log = LoggerFactory.getLogger( OtuWrapper.class );
	private static final String TAB = "\t";
	private static final double threshold = -1000;

	/*
	 * All of these lists will be made unmodifiable (and hence thread safe) in
	 * the constructor
	 */

	private List<List<Double>> dataPointsNormalized = new ArrayList<>();
	private List<List<Double>> dataPointsNormalizedThenLogged = new ArrayList<>();
	private List<List<Double>> dataPointsUnnormalized = new ArrayList<>();

	private List<String> otuNames = new ArrayList<>();
	private List<String> sampleNames = new ArrayList<>();

	/**
	 * Instantiating OtuWrapper based on the input file which contains the raw counts.
	 * @param f
	 * @throws Exception
	 */
	public OtuWrapper( final File f, final String logBase ) throws Exception
	{
		final BufferedReader reader = new BufferedReader( new FileReader( f ) );
		String nextLine = reader.readLine();
		StringTokenizer tr = new StringTokenizer( nextLine, TAB );
		tr.nextToken();
		int x = 0;
		while( tr.hasMoreTokens() )
		{
			String taxaName = tr.nextToken();
			if( taxaName.startsWith( "\"" ) && taxaName.endsWith( "\"" ) )
			{
				taxaName = taxaName.substring( 1, taxaName.length() - 1 );
			}
			otuNames.add( taxaName );
			x++;
		}

		nextLine = reader.readLine();
		int totalCounts = 0;
		while( nextLine != null )
		{
			tr = new StringTokenizer( nextLine, TAB );
			final String sampleName = tr.nextToken();
			final List<Double> innerList = new ArrayList<>();
			sampleNames.add( sampleName );
			dataPointsUnnormalized.add( innerList );
			dataPointsNormalized.add( new ArrayList<Double>() );
			dataPointsNormalizedThenLogged.add( new ArrayList<Double>() );

			while( tr.hasMoreTokens() )
			{
				final String nextToken = tr.nextToken();
				double d = 0;
				if( nextToken.length() > 0 )
				{
					d = Double.parseDouble( nextToken );
				}

				innerList.add( d );
				totalCounts += d;
			}

			nextLine = reader.readLine();
		}

		reader.close();

		assertNum( totalCounts, dataPointsUnnormalized );
		removeThreshold( otuNames, dataPointsUnnormalized, threshold );

		if( threshold < 0.1 )
		{
			assertNoZeros( dataPointsUnnormalized );
			assertNum( totalCounts, dataPointsUnnormalized );
		}

		final double avgNumber = totalCounts / dataPointsNormalized.size();

		for( x = 0; x < dataPointsUnnormalized.size(); x++ )
		{
			final List<Double> unnormalizedInnerList = dataPointsUnnormalized.get( x );
			double sum = 0;

			for( final Double d: unnormalizedInnerList )
			{
				sum += d;
			}

			final List<Double> normalizedInnerList = dataPointsNormalized.get( x );
			final List<Double> loggedInnerList = dataPointsNormalizedThenLogged.get( x );

			for( int y = 0; y < unnormalizedInnerList.size(); y++ )
			{
				final double val = ( avgNumber * unnormalizedInnerList.get( y ) ) / sum;
				normalizedInnerList.add( val );

				if( logBase.equals( Constants.LOG_E ) )
				{
					loggedInnerList.add( Math.log( val + 1 ) );
				}
				else
				{
					loggedInnerList.add( Math.log10( val + 1 ) );
				}

			}
		}

		dataPointsNormalized = Collections.unmodifiableList( dataPointsNormalized );
		dataPointsNormalizedThenLogged = Collections.unmodifiableList( dataPointsNormalizedThenLogged );
		dataPointsUnnormalized = Collections.unmodifiableList( dataPointsUnnormalized );
		otuNames = Collections.unmodifiableList( otuNames );
		sampleNames = Collections.unmodifiableList( sampleNames );
	}

	public static double crank( final List<Double> w )
	{
		double s;

		int j = 1, ji, jt;
		double t, rank;

		final int n = w.size();
		s = 0.0f;
		while( j < n )
		{
			if( !w.get( j ).equals( w.get( j - 1 ) ) )
			{
				w.set( j - 1, j + 0.0 );
				++j;
			}
			else
			{
				for( jt = j + 1; ( jt <= n ) && w.get( jt - 1 ).equals( w.get( j - 1 ) ); jt++ )
				{
					;
				}
				rank = 0.5f * ( ( j + jt ) - 1 );
				for( ji = j; ji <= ( jt - 1 ); ji++ )
				{
					w.set( ji - 1, rank );
				}
				t = jt - j;
				s += ( ( t * t * t ) - t );
				j = jt;
			}
		}
		if( j == n )
		{
			w.set( n - 1, n + 0.0 );
		}

		return s;
	}

	private static void assertNoZeros( final List<List<Double>> dataPointsUnnormalized ) throws Exception
	{
		for( int x = 0; x < dataPointsUnnormalized.size(); x++ )
		{
			for( int y = 0; y < dataPointsUnnormalized.get( x ).size(); y++ )
			{
				double sum = 0;

				for( final Double d: dataPointsUnnormalized.get( x ) )
				{
					sum += d;
				}

				if( sum == 0 )
				{
					throw new Exception( "Logic error" );
				}

			}
		}
	}

	private static void assertNum( final int totalCounts, final List<List<Double>> dataPointsUnnormalized )
			throws Exception
	{
		int sum = 0;

		for( int x = 0; x < dataPointsUnnormalized.size(); x++ )
		{
			for( int y = 0; y < dataPointsUnnormalized.get( x ).size(); y++ )
			{
				sum += dataPointsUnnormalized.get( x ).get( y );
			}
		}

		if( totalCounts != sum )
		{
			throw new Exception( "Logic error " + totalCounts + " " + sum );
		}

		if( dataPointsUnnormalized.size() > 0 )
		{
			final int length = dataPointsUnnormalized.get( 0 ).size();

			for( int x = 0; x < dataPointsUnnormalized.size(); x++ )
			{
				if( length != dataPointsUnnormalized.get( x ).size() )
				{
					throw new Exception( "Jagged array" );
				}
			}
		}
	}

	private static void removeThreshold( final List<String> otuNames, final List<List<Double>> dataPointsUnNormalized,
			final double threshold )
	{
		final List<Boolean> removeList = new ArrayList<>();

		for( int x = 0; x < otuNames.size(); x++ )
		{
			int sum = 0;

			for( int y = 0; y < dataPointsUnNormalized.size(); y++ )
			{
				sum += dataPointsUnNormalized.get( y ).get( x );
			}

			if( sum <= threshold )
			{
				removeList.add( true );
			}
			else
			{
				removeList.add( false );
			}
		}

		for( int y = 0; y < dataPointsUnNormalized.size(); y++ )
		{
			int x = 0;

			for( final Iterator<Double> i = dataPointsUnNormalized.get( y ).iterator(); i.hasNext(); )
			{
				i.next();
				if( removeList.get( x ) )
				{
					i.remove();
				}

				x++;
			}
		}

		int x = 0;

		for( final Iterator<String> i = otuNames.iterator(); i.hasNext(); )
		{
			i.next();
			if( removeList.get( x ) )
			{
				i.remove();
			}

			x++;
		}
	}

	public void writeNormalizedLoggedDataToFile( final String id, final String filePath ) throws Exception
	{
		writeNormalizedLoggedDataToFile( id, new File( filePath ) );
	}

	private void writeNormalizedLoggedDataToFile( final String id, final File file ) throws Exception
	{
		final BufferedWriter writer = new BufferedWriter( new FileWriter( file ) );

		writer.write( id );

		for( final String s: otuNames )
		{
			writer.write( "\t" + s );
		}

		writer.write( "\n" );

		final int size = sampleNames.size();
		for( int x = 0; x < size; x++ )
		{
			writer.write( sampleNames.get( x ) );

			for( int y = 0; y < otuNames.size(); y++ )
			{
				writer.write( "\t" + dataPointsNormalizedThenLogged.get( x ).get( y ) );
			}

			if( ( x + 1 ) != size )
			{
				writer.write( "\n" );
			}

		}

		writer.flush();
		writer.close();
	}

}
