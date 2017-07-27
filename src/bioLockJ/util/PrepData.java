package bioLockJ.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import bioLockJ.module.classifier.ClassifierModule;

public class PrepData extends ClassifierModule
{
	private class Report
	{
		public String id = null;
		public String pool = null;
		public String project = null;
		public File r1 = null;
		public File r2 = null;
		public String runSeq = null;

		public Report( final File f ) throws Exception
		{
			final String path = f.getAbsolutePath();
			if( f.getName().endsWith( R1 ) )
			{
				r1 = f;
				path.replaceAll( R1, R2 );
				r2 = new File( path );
				if( !r2.exists() )
				{
					throw new Exception( "MISSING PAIRED FILE: " + r2.getAbsolutePath() );
				}
			}
			else
			{

				throw new Exception(
						"MISSING PAIRED FILE! Report should only be FW reads but found: " + r1.getAbsolutePath() );

			}

			final StringTokenizer st = new StringTokenizer( f.getName(), "_" );
			while( st.hasMoreTokens() )
			{
				final String token = st.nextToken().trim();
				if( id == null )
				{
					id = token;
				}
				else if( project == null )
				{
					project = token;
				}
				else if( runSeq == null )
				{
					runSeq = token;
				}
				else if( ( pool == null ) && !token.contains( "R" ) )
				{
					pool = token;
				}
				else if( !token.contains( "R" ) )
				{
					throw new Exception( "ERROR - File=" + f.getAbsolutePath() + " - bad token = \"" + token + "\"" );
				}
			}
		}
	}

	private static final Map<String, Report> bestReportMap = new HashMap<>();
	private static final Map<String, Set<File>> dupMap = new HashMap<>();
	private static final Map<File, String> fileMap = new HashMap<>();
	private static final Set<String> fileNames = new HashSet<>();
	private static final Set<String> ids = new HashSet<>();
	//private static final String OUTPUT_DIR = "/nobackup/afodor_research/datasets/cardia/all_noDups_noPrimers/";
	private static final String OUTPUT_DIR = "/users/msioda/cardiaMetadata.txt";
	private static final String R1 = "_R1.fastq";

	private static final String R2 = "_R2.fastq";

	private static final Map<String, Set<Report>> reportMap = new HashMap<>();

	private static void addReports() throws Exception
	{
		for( final String id: dupMap.keySet() )
		{
			final Set<Report> reports = new HashSet<>();
			final Set<File> files = dupMap.get( id );
			for( final File f: files )
			{
				//int numReads = intVal( id, NUM_READS );
				//int numHits = intVal( id, NUM_HITS );
				reports.add( new PrepData().new Report( f ) );
			}

			reportMap.put( id, reports );
		}
	}

	/** RANK
			4483-P2
			4483-P1
			4066-P1
			4216
			4066-P2
	*/
	private static void bestReports() throws Exception
	{
		for( final String id: reportMap.keySet() )
		{
			final Set<Report> reports = reportMap.get( id );
			Report best = null;
			int bestLevel = 6;

			for( final Report r: reports )
			{
				log.info( "best R1: " + ( ( best == null ) ? "null": best.r1.getName() ) );
				log.info( "bestLevel: " + bestLevel );
				log.info( "r.id: " + r.id );
				log.info( "r.r1: " + r.r1.getName() );
				log.info( "r.r2: " + r.r2.getName() );
				log.info( "r.project: " + r.project );
				log.info( "r.pool: " + r.pool );

				if( r.project.equals( "4483" ) && r.pool.equals( "P2" ) )
				{
					best = r;
					break;
				}
				else if( r.project.equals( "4483" ) && r.pool.equals( "P1" ) )
				{
					bestLevel = 2;
					best = r;
				}
				else if( r.project.equals( "4066" ) && r.pool.equals( "P1" ) )
				{
					if( 3 < bestLevel )
					{
						bestLevel = 3;
						best = r;
					}
				}
				else if( r.project.equals( "4216" ) )
				{
					if( 4 < bestLevel )
					{
						bestLevel = 4;
						best = r;
					}
				}
				else if( r.project.equals( "4066" ) && r.pool.equals( "P2" ) )
				{
					if( best == null )
					{
						bestLevel = 5;
						best = r;
					}
				}
				else
				{
					throw new Exception( "ERROR - no best found, missing proj/pool combo!" );
				}
			}

			if( best != null )
			{
				bestReportMap.put( id, best );
			}
			else
			{
				throw new Exception( "ERROR - NO BEST REPORT FOUND!" );
			}
		}
	}

	@Override
	public void executeProjectFile() throws Exception
	{
		final List<File> files = getInputFiles();
		for( final File f: files )
		{
			final String name = f.getName().substring( 0, 5 );
			info( "Found ID: " + name );
			fileNames.add( name );
		}

		final BufferedReader reader = new BufferedReader( new FileReader( config.getMetadata() ) );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( OUTPUT_DIR ) );
		String line = reader.readLine();
		writer.write( line + "\n" );
		for( line = reader.readLine(); line != null; line = reader.readLine() )
		{
			final String test = line.substring( 0, 5 );
			info( "checking ID: " + test );
			for( final String id: fileNames )
			{
				if( line.startsWith( id ) )
				{
					info( "FOUND ID: " + id );
					writer.write( line + "\n" );
					break;
				}
			}
		}

		reader.close();
		writer.flush();
		writer.close();

		info( "NEW META CREATED! " + OUTPUT_DIR );
	}

	//
	//	@Override
	//	public void executeProjectFile() throws Exception
	//	{
	//		popFileMap();
	//		popDupMap();	
	//		addReports();
	//		bestReports();
	//		final List<List<String>> data = buildScript( getInputFiles() );
	//		BashScriptUtil.buildScripts( this, data, "MEYER_CONVERT" );
	//	}

	/**
	 * Call RDP jar with specified params.
	 */
	@Override
	protected List<List<String>> buildScript( final List<File> files ) throws Exception
	{
		final Set<String> usedIds = new HashSet<>();

		final List<List<String>> data = new ArrayList<>();
		final Set<String> dupIds = bestReportMap.keySet();
		for( final File f: files )
		{
			final StringTokenizer st = new StringTokenizer( f.getName(), "_" );
			final String id = st.nextToken();

			if( usedIds.contains( id ) || !f.getName().endsWith( R1 ) )
			{
				continue;
			}

			String r1Path = null;
			String r2Path = null;

			if( dupIds.contains( id ) )
			{
				r1Path = bestReportMap.get( id ).r1.getAbsolutePath();
				r2Path = bestReportMap.get( id ).r2.getAbsolutePath();
			}
			else
			{
				r1Path = f.getAbsolutePath();
				r2Path = f.getAbsolutePath().replaceAll( R1, R2 );
			}

			usedIds.add( id );
			//final String fileId = trimSampleID( f.getName() );

			final String outR1 = OUTPUT_DIR + id + R1;
			final String outR2 = OUTPUT_DIR + id + R2;
			final ArrayList<String> lines = new ArrayList<>();
			lines.add( "cp " + r1Path + " " + outR1 );
			lines.add( "cp " + r2Path + " " + outR2 );
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
		warn( "Version unavailable for: PREP DATA" );
	}

	private void popDupMap() throws Exception
	{
		final List<File> input = getInputFiles();
		for( final File f: input )
		{
			if( !f.getName().endsWith( R1 ) )
			{
				continue;
			}

			System.out.print( "INSPECT FILE = " + f.getAbsolutePath() );
			final String id = f.getName().substring( 0, 5 );
			if( ids.contains( id ) )
			{
				Set<File> files = dupMap.get( id );
				if( files == null )
				{
					files = new HashSet<>();
				}

				if( files.contains( f ) )
				{
					throw new Exception( "File processed more than once! (Bad File = " + f.getAbsolutePath() + ")" );
				}

				files.add( f );
				dupMap.put( id, files );
			}
			ids.add( id );
		}
	}

	private void popFileMap() throws Exception
	{
		final List<File> files = getInputFiles();
		for( final File f: files )
		{
			if( !f.getName().endsWith( R1 ) )
			{
				continue;
			}
			final String id = f.getName().substring( 0, 5 );
			fileMap.put( f, id );
		}
	}
}