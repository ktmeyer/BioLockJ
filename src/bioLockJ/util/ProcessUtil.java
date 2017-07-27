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
package bioLockJ.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import bioLockJ.BioLockJ;

/**
 * ProcessUtil enables the Java program to execute scripts on thos host OS.
 */
public class ProcessUtil extends BioLockJ
{
	private static Logger log = LoggerFactory.getLogger( ProcessUtil.class );

	private ProcessUtil( final String[] args ) throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		for( final String cmdArg: args )
		{
			sb.append( cmdArg + " " );
		}

		log.info( "[ProcessUtil] EXECUTE: " + sb.toString() );

		final Runtime r = Runtime.getRuntime();
		final Process p = r.exec( args );
		final BufferedReader br = new BufferedReader( new InputStreamReader( p.getInputStream() ) );
		String s;
		while( ( s = br.readLine() ) != null )
		{
			log.info( "[ProcessUtil] " + s );
		}

		p.waitFor();
		p.destroy();
	}

	public static void submit( final String[] args ) throws Exception
	{
		new ProcessUtil( args );
	}
}
