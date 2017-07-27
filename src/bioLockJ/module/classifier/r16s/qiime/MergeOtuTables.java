/**
 * @UNCC BINF 8380
 *
 * @author Michael Sioda
 * @date Jun 8, 2017
 */
package bioLockJ.module.classifier.r16s.qiime;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import bioLockJ.module.classifier.r16s.QiimeClassifier;
import bioLockJ.util.BashScriptUtil;

/**
 *
 */
public class MergeOtuTables extends QiimeClassifier
{
	private static final String SCRIPT_MERGE_OTU_TABLES = "merge_otu_tables.py -i ";

	/**
	 * Call build scripts to merge OTU tables via QIIME script: merge_otu_tables.py
	 */
	@Override
	public void executeProjectFile() throws Exception
	{
		BashScriptUtil.buildScripts( this, buildScript( getInputFiles() ), SCRIPT_MERGE_OTU_TABLES );
	}

	/**
	 * Input files are all of the otu_table.biom files output by the ClosedRefClassifier.
	 * The QiimeClassifer superclass will be called next & wil expect a single otu_table.biom file.
	 */
	@Override
	protected List<List<String>> buildScript( final List<File> files ) throws Exception
	{
		String tables = "";
		for( final File f: files )
		{
			tables += ( tables.isEmpty() ? "": "," ) + f.getAbsolutePath();
		}

		final List<List<String>> data = new ArrayList<>();
		final List<String> lines = new ArrayList<>();
		final String outputFile = getOutputDir().getAbsolutePath() + File.separator + OTU_TABLE;
		lines.add( SCRIPT_MERGE_OTU_TABLES + tables + " -o " + outputFile );
		data.add( lines );

		return data;
	}
}
