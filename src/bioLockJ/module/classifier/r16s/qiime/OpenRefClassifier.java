/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Apr 5, 2017
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
package bioLockJ.module.classifier.r16s.qiime;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.io.filefilter.TrueFileFilter;
import bioLockJ.module.classifier.r16s.QiimeClassifier;
import bioLockJ.util.BashScriptUtil;

/**
 * Used for Open Ref OTU picking in which all samples must be processed
 * as a single batch.
 */
public class OpenRefClassifier extends QiimeClassifier
{
	private static final String repSet = REP_SET + ".fna";
	private boolean removeChimeras = false;
	private String vsearch = null;
	private String vsearchParams = null;

	/**
	 * If chimeras must be removed, verify vsearch params.
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		checkOtuPickingDependencies();
		removeChimeras = requireBoolean( QIIME_REMOVE_CHIMERAS );
		if( removeChimeras )
		{
			vsearch = requireString( EXE_VSEARCH );
			vsearchParams = getVsearchParams();
			if( !moduleExists( vsearch ) )
			{
				requireExistingFile( EXE_VSEARCH );
			}
		}
	}

	/**
	 * Call build scripts.
	 */
	@Override
	public void executeProjectFile() throws Exception
	{
		BashScriptUtil.buildScripts( this, buildScript( getInputFiles() ), "QIIME.OpenRefClassifier" );
	}

	/**
	 * Dir top level holds only fasta files and it's subdirectory holds the corrected
	 * QIIME specific mapping file as output by QiimePreprocessor.java
	 *
	 */
	@Override
	public void initInputFiles( File dir ) throws Exception
	{
		if( dir == null )
		{
			dir = getInputDirsFromPropFile().get( 0 );
		}
		setModuleInput( dir, TrueFileFilter.INSTANCE, null );
		initMappingFile( dir );
	}

	/**
	 * Pick OTUs with open ref QIIME script and filter chimeras if qiime.removeChimeras=Y
	 */
	@Override
	protected List<List<String>> buildScript( final List<File> files ) throws Exception
	{
		final List<List<String>> data = new ArrayList<>();
		final List<String> lines = new ArrayList<>();
		final String inDir = getInputDir().getAbsolutePath();
		final String outputDir = getOutputDir().getAbsolutePath() + File.separator;
		final String biomFile = "$(ls -t " + outputDir + "*.biom | head -1)";

		addPickOtuLines( lines, inDir, config.getMetaPath(), getOutputDir().getAbsolutePath() );

		if( removeChimeras )
		{
			final String otusToFilter = outputDir + "chimeras.fasta";
			final String line1 = vsearch + vsearchParams + "--uchime_ref " + outputDir + repSet + " --chimeras "
					+ otusToFilter + " --nonchimeras " + outputDir + "nochimeras.fasta";

			final String line2 = SCRIPT_FILTER_OTUS + biomFile + " -e " + otusToFilter + " -o " + outputDir + OTU_TABLE;

			lines.add( line1 );
			lines.add( line2 );
		}
		else
		{
			lines.add( "cp " + biomFile + " " + outputDir + OTU_TABLE );
		}

		data.add( lines );
		return data;
	}

	/**
	 * Get Vsearch params from the prop file and format switches for the bash script.
	 * @return
	 * @throws Exception
	 */
	private String getVsearchParams() throws Exception
	{
		String formattedSwitches = " ";
		final List<String> params = requireList( EXE_VSEARCH_PARAMS );
		final Iterator<String> it = params.iterator();
		while( it.hasNext() )
		{
			formattedSwitches += "--" + it.next() + " ";
		}

		return formattedSwitches;
	}
}