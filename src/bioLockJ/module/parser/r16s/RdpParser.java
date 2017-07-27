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
package bioLockJ.module.parser.r16s;

import java.io.BufferedReader;
import java.io.File;
import bioLockJ.module.parser.ParserModule;
import bioLockJ.node.r16s.RdpNode;

/**
 * To see file format: > head 7A_1_reported.tsv
 *
 * FCABK7W:1:2105:21787:12788#/1 Root rootrank 1.0 Bacteria domain 1.0
 * Firmicutes phylum 1.0 Clostridia class 1.0 Clostridiales order 1.0
 * Ruminococcaceae family 1.0 Faecalibacterium genus 1.0
 *
 */
public class RdpParser extends ParserModule
{

	private int thresholdScore = 0;

	/**
	 * RDP must have a thrshold score configured.
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		thresholdScore = requirePositiveInteger( RDP_THRESHOLD_SCORE );
	}

	/**
	 * RDP nodes may be multiplexed so determine ID based on demultiplex option.
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
					final RdpNode node = new RdpNode( line );
					if( node.getScore() >= thresholdScore )
					{
						final String id = ( demultiplex ? trimSampleID( node.getId() ): getFileID( file ) );
						addOtuNode( id, node );
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
