/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Apr 9, 2017
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
package bioLockJ.module.parser.wgs;

import java.io.BufferedReader;
import java.io.File;
import bioLockJ.module.parser.ParserModule;
import bioLockJ.node.wgs.MetaphlanNode;

/**
 * To see file format: > head 7A_1_processed.txt
 *
 * #SampleID Metaphlan2_Analysis
 * #clade_name relative_abundance coverage average_genome_length_in_the_clade estimated_number_of_reads_from_the_clade
 * k__Bacteria|p__Bacteroidetes 14.68863 0.137144143537 4234739 580770
 *
 */
public class MetaphlanParser extends ParserModule
{

	/**
	 * Metaphlan doesn't support the demultiplex option
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		if( demultiplex )
		{
			throw new Exception( "METAPHLAN REQUIRES PROPERTY: " + INPUT_DEMULTIPLEX + "=TRUE" );
		}
	}

	@Override
	protected void createOtuNodes() throws Exception
	{
		int fileCount = 0;
		for( final File file: getInputFiles() )
		{
			info( "PARSE FILE # (" + String.valueOf( fileCount++ ) + ") = " + file.getName() );
			final BufferedReader reader = getFileReader( file );
			try
			{
				for( String line = reader.readLine(); line != null; line = reader.readLine() )
				{
					if( !line.startsWith( "#" ) )
					{
						final MetaphlanNode node = new MetaphlanNode( line );
						addOtuNode( getFileID( file ), node );
					}
				}
			}
			catch( final Exception ex )
			{
				throw new Exception( "Error occurred parsing file: " + file.getName(), ex );
			}
			finally
			{
				reader.close();
			}
		}
	}
}
