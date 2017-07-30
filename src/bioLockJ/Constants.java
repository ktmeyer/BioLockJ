/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Jun 23, 2017
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
package bioLockJ;

/**
 * This is the root class for BioLockJ containing all constant values.  The purpose
 * of this class is to improve readability of code within it's subcalsses.
 */
public abstract class Constants
{
	public static final String DEFAULT_META_ID = "SampleId"; 
	public static final String ALPHA_DIVERSITY_TABLE = "alphaDiversity.txt";
	public static final int ATT_TYPE_INDEX = 0;

	public static final String BARCODE_SEQUENCE = "BarcodeSequence";
	public static final String BATCH_MAPPING = "batchMapping.txt";
	public static final String BINARY = "BINARY";

	public static final String CATEGORICAL = "CATEGORICAL";
	public static final String CLASS = "class";
	public static final String CLASS_DELIM = "c__";
	public static final String CLASS_REPORT = "_class_reported.tsv";
	public static final String CLUSTER_BATCH_COMMAND = "cluster.batchCommand";
	public static final String CLUSTER_MODULES = "cluster.modules";
	public static final String CLUSTER_NUM_PROCESSORS = "procs";
	public static final String CLUSTER_PARAMS = "cluster.params";
	public static final String CLUSTER_VALIDATE_PARAMS = "cluster.validateParams";
	public static final String COMBINED_FNA = "combined_seqs.fna";
	public static final String CONTINUOUS = "CONTINUOUS";
	public static final String CONTROL_MERGE_PAIRS = "control.mergePairs";
	public static final String CONTROL_RAREFY_SEQS = "control.rarefySeqs";
	public static final String CONTROL_RUN_CLASSIFIER = "control.runClassifier";
	public static final String CONTROL_RUN_ON_CLUSTER = "control.runOnCluster";
	public static final String CONTROL_RUN_PARSER = "control.runParser";
	public static final String CONTROL_RUN_R_SCRIPT = "control.run_rScript";
	public static final String CONTROL_TRIM_PRIMERS = "control.trimSeqs";

	public static final String DELIM = "\t";
	public static final String DEMUX_COLUMN = "InputFileName";
	public static final String DESCRIPTION = "Description";
	public static final String DOMAIN = "domain";
	public static final String DOMAIN_DELIM = "d__";
	public static final String DOMAIN_REPORT = "_superkingdom_reported.tsv";

	public static final String EMAIL_ATTACHMENT_MAX_MB = "email.maxAttachmentSizeMB";
	public static final String EMAIL_ENCRYPTED_PASSWORD = "email.encryptedPassword";
	public static final String EMAIL_FROM = "email.from";
	public static final String EMAIL_SEND_NOTIFICATION = "email.sendNotification";
	public static final String EMAIL_SEND_QSUB = "email.sendQsub";
	public static final String EMAIL_TO = "email.to";
	public static final String ERROR_DETECTED = "errorDetected";
	public static final String ERROR_ON_PREVIOUS_LINE = "errorOnPreviousLine";
	public static final String EXE_AWK = "exe.awk";
	public static final String EXE_BOWTIE = "exe.bowtie";
	public static final String EXE_BOWTIE_PARAMS = "exe.bowtie_params";
	public static final String EXE_CLASSIFIER = "exe.classifier";
	public static final String EXE_CLASSIFIER_PARAMS = "exe.classifierParams";
	public static final String EXE_GZIP = "exe.gzip";
	public static final String EXE_JAVA = "exe.java";
	public static final String EXE_PEAR = "exe.pear";
	public static final String EXE_PEAR_PARAMS = "exe.pear_params";
	public static final String EXE_PYTHON = "exe.python";
	public static final String EXE_RSCRIPT = "exe.rScript";
	public static final String EXE_SAMTOOLS = "exe.samtools";
	public static final String EXE_VSEARCH = "exe.vsearch";
	public static final String EXE_VSEARCH_PARAMS = "exe.vsearchParams";
	public static final String EXIT_CODE = "exitCode";

	public static final String FAILURE_CODE = "failureCode";
	public static final String FALSE = "N";
	public static final String FAMILY = "family";
	public static final String FAMILY_DELIM = "f__";
	public static final String FAMILY_REPORT = "_family_reported.tsv";
	public static final String FASTA = "fasta";
	public static final String FASTQ = "fastq";

	public static final String GENUS = "genus";
	public static final String GENUS_DELIM = "g__";
	public static final String GENUS_REPORT = "_genus_reported.tsv";

	public static final String INDENT = "    ";
	public static final String INPUT_DEMULTIPLEX = "input.demultiplex";
	public static final String INPUT_DIRS = "input.dirs";
	public static final String INPUT_FORWARD_READ_SUFFIX = "input.forwardFileSuffix";
	public static final String INPUT_IGNORE_FILES = "input.ignoreFiles";
	public static final String INPUT_PAIRED_READS = "input.pairedReads";
	public static final String INPUT_RAREFYING_MAX = "input.rarefyMaxNumSeqs";
	public static final String INPUT_RAREFYING_MIN = "input.rarefyMinNumSeqs";
	public static final String INPUT_REVERSE_READ_SUFFIX = "input.reverseFileSuffix";
	public static final String INPUT_TRIM_PREFIX = "input.trimPrefix";
	public static final String INPUT_TRIM_SEQ_PATH = "input.trimSeqPath";
	public static final String INPUT_TRIM_SUFFIX = "input.trimSuffix";
	public static final String INPUT_KEEP_SEQS_MISSING_PRIMER = "input.keepSeqsMissingPrimer";

	public static final String KRAKEN = "KRAKEN";
	public static final String KRAKEN_DATABASE = "kraken.db";
	public static final String KRAKEN_DELIM = "\\|";
	public static final String KRAKEN_FILE = "_kraken.txt";

	public static final String LINKER_PRIMER_SEQUENCE = "LinkerPrimerSequence";
	public static final String LOG_E = "e";
	public static final String LOG_FILE = "LOG_FILE";
	public static final String LOG_NORMAL = "_LogNormal";
	public static final String LOG_NORMAL_SUFFIX = "_AsLogNormalColumns.txt";
	public static final String LOG_SPACER = "========================================================================";

	public static final String MAIN_SCRIPT = "main";
	public static final String MAP_TYPE_DESCRIPTOR = "DESCRIPTOR";
	public static final String MAP_TYPE_METADATA = "METADATA";
	public static final String MEAN = "mean";
	public static final String MERGE_SUFFIX = ".assembled." + FASTQ;
	public static final String META_MERGED_SUFFIX = "_metaMerged.txt";
	public static final String METADATA_COMMENT = "metadata.commentChar";
	public static final String METADATA_DESCRIPTOR = "metadata.descriptor";
	public static final String METADATA_FILE = "metadata.file";
	public static final String METADATA_NULL_VALUE = "metadata.nullValue";
	public static final String METAPHLAN = "METAPHLAN";
	public static final String METAPHLAN_CLASS = "c";
	public static final String METAPHLAN_DELIM = "\\|";
	public static final String METAPHLAN_DOMAIN = "k";
	public static final String METAPHLAN_DOMAIN_DELIM = "k__";
	public static final String METAPHLAN_FAMILY = "f";
	public static final String METAPHLAN_GENUS = "g";
	public static final String METAPHLAN_ORDER = "o";
	public static final String METAPHLAN_PHYLUM = "p";
	public static final String METAPHLAN_SPECIES = "s";

	public static final String NUM_HITS = "Num_Hits";
	public static final String NUM_READS = "Num_Reads";

	public static final String ORDER = "order";
	public static final String ORDER_DELIM = "o__";
	public static final String ORDER_REPORT = "_order_reported.tsv";
	public static final String ORDERED_MAPPING = "orderedMapping.txt";
	public static final String ORDINAL = "ORDINAL";
	public static final String OTU_DIR = "otu_by_taxa_level";
	public static final String OTU_ID = "#OTU ID";
	public static final String OTU_SUMMARY_FILE = "otuSummary.txt";
	public static final String OTU_TABLE = "otu_table.biom";
	public static final String OTU_TABLE_PREFIX = "otu_table_L";

	public static final String P_VALS = "pValues";
	public static final String PHYLUM = "phylum";
	public static final String PHYLUM_DELIM = "p__";
	public static final String PHYLUM_REPORT = "_phylum_reported.tsv";
	public static final String PROCESSED = "_reported.tsv";
	public static final String PROJECT_CLASSIFIER_TYPE = "project.classifierType";
	public static final String PROJECT_COPY_FILES = "project.copyInputFiles";
	public static final String PROJECT_DELETE_TEMP_FILES = "project.deleteTempFiles";
	public static final String PROJECT_NAME = "project.name";
	public static final String PROJECTS_DIR = "project.rootDir";

	public static final String QIIME = "QIIME";
	public static final String QIIME_ALPHA_DIVERSITY_METRICS = "qiime.alphaDiversityMetrics";
	public static final String QIIME_COMMENT = "BioLockJ Generated Mapping";
	public static final String QIIME_DELIM = ";";
	public static final String QIIME_DOMAIN_DELIM = "k__";
	public static final String QIIME_FORMAT_METADATA = "qiime.formatMetadata";
	public static final String QIIME_ID = "#SampleID";
	public static final String QIIME_MAPPING = "qiimeMapping.txt";
	public static final String QIIME_MERGE_OTU_TABLES = "qiime.mergeOtuTables";
	public static final String QIIME_PICK_OTU_SCRIPT = "qiime.pickOtuScript";
	public static final String QIIME_PICK_OTUS = "qiime.pickOtus";
	public static final String QIIME_PREPROCESS = "qiime.preprocessInput";
	public static final String QIIME_REMOVE_CHIMERAS = "qiime.removeChimeras";
	public static final String QSUBS = "qsubs";

	public static final String R_FILTER_ATTRIBUTES = "r.filterAttributes";
	public static final String R_FILTER_NA_ATTRIBUTES = "r.filterNaAttributes";
	public static final String R_FILTER_OPERATORS = "r.filterOperators";
	public static final String R_FILTER_VALUES = "r.filterValues";
	public static final String R_LOG_BASE = "r.logBase";
	public static final String R_LOG_NORMALIZED = "r.logNormal";
	public static final String R_MAX_TITLE_SIZE = "r.maxTitleSize";
	public static final String R_NUM_HISTAGRAM_BREAKS = "r.numHistogramBreaks";
	public static final String R_RARE_OTU_THRESHOLD = "r.rareOtuThreshold";
	public static final String R_SCRIPT_NAME = "report.r";
	public static final String R_SQUARED = "rSquared";
	public static final String RAW_COUNT = "_RawCount";
	public static final String RDP = "RDP";
	public static final String RDP_THRESHOLD_SCORE = "rdp.minThresholdScore";
	public static final String RELATIVE_ABUNDANCE = "Relative Abundance";
	public static final String REP_SET = "rep_set";
	public static final String REPORT_ADD_GENUS_NAME_TO_SPECIES = "report.addGenusToSpeciesName";
	public static final String REPORT_ATTRIBUTES = "report.attributes";
	public static final String REPORT_EMPTY_SPACE_DELIM = "report.emptySpaceDelim";
	public static final String REPORT_FULL_TAXONOMY_NAMES = "report.fullTaxonomyNames";
	public static final String REPORT_MINIMUM_OTU_COUNT = "report.minOtuCount";
	public static final String REPORT_NUM_HITS = "report.numHits";
	public static final String REPORT_NUM_READS = "report.numReads";
	public static final String REPORT_TAXONOMY_LEVELS = "report.taxonomyLevels";
	public static final String REPORT_USE_GENUS_FIRST_INITIAL = "report.useGenusFirstInitial";
	public static final String ROOT_DIR = "ROOT_DIR";

	public static final String SAMPLE_SIZE = "sampleSize";
	public static final String SCRIPT_ADD_ALPHA_DIVERSITY = "add_alpha_to_mapping_file.py -m ";
	public static final String SCRIPT_ADD_LABELS = "add_qiime_labels.py -n 1 -i ";
	public static final String SCRIPT_BATCH_SIZE = "script.batchSize";
	public static final String SCRIPT_CALC_ALPHA_DIVERSITY = "alpha_diversity.py -i ";
	public static final String SCRIPT_CHMOD_COMMAND = "script.chmodCommand";
	public static final String SCRIPT_EXIT_ON_ERROR = "script.exitOnError";
	public static final String SCRIPT_FAILED = "_FAIL";
	public static final String SCRIPT_FILTER_OTUS = "filter_otus_from_otu_table.py -i ";
	public static final String SCRIPT_NUM_THREADS = "script.numThreads";
	public static final String SCRIPT_PICK_CLOSED_REF_OTUS = "pick_closed_reference_otus.py";
	public static final String SCRIPT_PICK_DE_NOVO_OTUS = "pick_de_novo_otus.py";
	public static final String SCRIPT_PICK_OPEN_REF_OTUS = "pick_open_reference_otus.py";
	public static final String SCRIPT_PRINT_CONFIG = "print_qiime_config.py -t";
	public static final String SCRIPT_SUCCEEDED = "_SUCCESS";
	public static final String SCRIPT_SUMMARIZE_BIOM = "biom summarize-table -i ";
	public static final String SCRIPT_SUMMARIZE_TAXA = "summarize_taxa.py -a -i ";
	public static final String SCRIPT_VALIDATE_MAPPING = "validate_mapping_file.py -p -b -m ";
	public static final String SLIMM = "SLIMM";
	public static final String SLIMM_CLASS_DELIM = "class";
	public static final String SLIMM_DATABASE = "slimm.db";
	public static final String SLIMM_DOMAIN_DELIM = "superkingdom";
	public static final String SLIMM_FAMILY_DELIM = "family";
	public static final String SLIMM_GENUS_DELIM = "genus";
	public static final String SLIMM_ORDER_DELIM = "order";
	public static final String SLIMM_PHYLUM_DELIM = "phylum";
	public static final String SLIMM_REF_GENOME_INDEX = "slimm.refGenomeIndex";
	public static final String SLIMM_SPECIES_DELIM = "species";
	public static final String SPECIES = "species";
	public static final String SPECIES_DELIM = "s__";
	public static final String SPECIES_REPORT = "_species_reported.tsv";
	public static final String STRAIN_DELIM = "t__";

	public static final char TAB = '\t';
	public static final String TRUE = "Y";

	public static final String VALIDATED_MAPPING = "_corrected.txt";

	// private static final String OTUS_TREE_97 = "97_otus.tree";
	// private static final String TAXA_TREE = "taxa.tre";
	// private static final String SCRIPT_FILTER_TREE = "filter_tree.py -i ";
	// private static final String DIFF_OTU_SUMMARY = "differential_otu_summary.txt";
	// private static final String SCRIPT_DIFF_ABUNDANCE = "differential_abundance.py -i ";
	// private static final String SCRIPT_COMP_CORE_MB = "compute_core_microbiome.py ";
}
