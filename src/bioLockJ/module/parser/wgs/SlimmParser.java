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
import java.util.ArrayList;
import java.util.List;
import bioLockJ.module.parser.ParserModule;
import bioLockJ.node.wgs.SlimmNode;

/**
 * To see file format: > head 7A_1_phylum_reported.tsv
 *
 * No. Name Taxid NoOfReads RelativeAbundance Contributers Coverage 1
 * Bacteroidetes 976 1137994 29.7589 17 24.7204
 */
public class SlimmParser extends ParserModule
{

	private static List<String> reportNameSuffixList = new ArrayList<>();

	static
	{
		reportNameSuffixList.add( DOMAIN_REPORT );
		reportNameSuffixList.add( PHYLUM_REPORT );
		reportNameSuffixList.add( CLASS_REPORT );
		reportNameSuffixList.add( ORDER_REPORT );
		reportNameSuffixList.add( FAMILY_REPORT );
		reportNameSuffixList.add( GENUS_REPORT );
		reportNameSuffixList.add( SPECIES_REPORT );
	}

	/**
	 * SLIMM doesn't support demultiplex option
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		if( demultiplex )
		{
			throw new Exception( "SLIMM REQUIRES PROPERTY: " + INPUT_DEMULTIPLEX + "=TRUE" );
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
				String line = reader.readLine(); // skip header
				for( line = reader.readLine(); line != null; line = reader.readLine() )
				{
					final SlimmNode node = new SlimmNode( file.getName(), line );
					addOtuNode( getFileID( file ), node );
				}
			}
			finally
			{
				reader.close();
			}
		}
	}

	/**
	 * Slimm provides one file/sample so pull SampleID from the classifier output file name.
	 */
	@Override
	protected String getFileID( final File file ) throws Exception
	{
		for( final String suffix: reportNameSuffixList )
		{
			if( file.getName().contains( suffix ) )
			{
				return file.getName().replace( suffix, "" );
			}
		}

		throw new Exception(
				"SLIMM REPORT NAME INVALID - REQUIRED FORMAT = <Sample_ID>_<Taxonomy_Level>_reported.tsv" );
	}

}
