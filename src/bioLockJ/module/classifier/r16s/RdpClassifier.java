/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
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
package bioLockJ.module.classifier.r16s;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import bioLockJ.module.classifier.ClassifierModule;

/**
 * RdpClassifier is used to build RDP classifier bash scripts
 */
public class RdpClassifier extends ClassifierModule
{
	private String javaExe;

	/**
	 * The only unique RDP dependency is on Java.
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		javaExe = requireString( EXE_JAVA );

	}

	/**
	 * Call RDP jar with specified params.
	 */
	@Override
	protected List<List<String>> buildScript( final List<File> files ) throws Exception
	{
		final List<List<String>> data = new ArrayList<>();
		for( final File file: files )
		{
			final String fileId = trimSampleID( file.getName() );
			final String outputFile = getOutputDir().getAbsolutePath() + File.separator + fileId + PROCESSED;
			final ArrayList<String> lines = new ArrayList<>();
			lines.add( javaExe + " -jar " + classifierExe + getProgramSwitches() + "-o " + outputFile + " "
					+ file.getAbsolutePath() );
			data.add( lines );
		}

		return data;
	}

	/**
	 * Paired reads must use mergeUtil & then call standard buildScripts() method.
	 */
	@Override
	protected List<List<String>> buildScriptForPairedReads( final List<File> files ) throws Exception
	{
		return buildScript( files );
	}

	/**
	 * RDP does not supply a version call.
	 */
	@Override
	protected void logVersion()
	{
		warn( "Version unavailable for: " + classifierExe );
	}

}
