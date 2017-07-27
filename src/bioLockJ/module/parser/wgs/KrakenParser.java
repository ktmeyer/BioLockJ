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
package bioLockJ.module.parser.wgs;

import java.io.BufferedReader;
import java.io.File;
import bioLockJ.module.parser.ParserModule;
import bioLockJ.node.wgs.KrakenNode;

/**
 * To see file format: > head 7A_1_reported.tsv
 *
 * FCC6MMAACXX:8:1101:1968:2100#GTATTCTC/1
 * d__Bacteria|p__Bacteroidetes|c__Bacteroidia|o__Bacteroidales|f__Bacteroidaceae|g__Bacteroides|s__Bacteroides_vulgatus
 *
 */
public class KrakenParser extends ParserModule
{
	/**
	 * Kraken nodes may be multiplexed so determine ID based on demultiplex option.
	 */
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
					final KrakenNode node = new KrakenNode( line );
					final String id = ( demultiplex ? trimSampleID( node.getId() ): getFileID( file ) );
					addOtuNode( id, node );
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
