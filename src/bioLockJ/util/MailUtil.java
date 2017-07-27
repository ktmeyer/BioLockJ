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
package bioLockJ.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.PropertiesConfigurationLayout;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import bioLockJ.ApplicationManager;
import bioLockJ.BioLockJ;

/**
 * A simple Mail utility to send notifications when job is complete with a status message
 * and summary details about failures and run time.
 */
public class MailUtil extends BioLockJ
{
	private static int emailMaxAttachmentMB = 0;
	private static MailUtil mailUtil = null;
	private static final char[] PASSWORD = "enfldsgbnlsngdlksdsgm".toCharArray();
	private static final byte[] SALT = { (byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12, (byte) 0xde, (byte) 0x33,
			(byte) 0x10, (byte) 0x12, };

	/**
	 * Get a static reference to a MailUtil instance.
	 * @return
	 */
	public static MailUtil getMailUtil()
	{
		if( mailUtil == null )
		{
			mailUtil = new MailUtil();
		}

		return mailUtil;
	}

	/**
	 * The default message.
	 * @param fileMsg
	 * @return
	 * @throws Exception
	 */
	private static String getMessage( final String fileMsg ) throws Exception
	{
		return " BioLockJ job complete.  " + fileMsg + "\n\n Regards, \n BioLockJ Admin";
	}

	/**
	 * Used to obtain a new encrypted password hash when the admin email password is set.
	 * @param password
	 * @throws Exception
	 */
	public void encryptAndStoreEmailPassword( final String password ) throws Exception
	{
		final String encryptedPassword = encrypt( password );
		final PropertiesConfiguration props = new PropertiesConfiguration();
		final PropertiesConfigurationLayout layout = new PropertiesConfigurationLayout( props );
		layout.load( new InputStreamReader( new FileInputStream( config.getPropertiesFile() ) ) );
		config.setProperty( EMAIL_ENCRYPTED_PASSWORD, encryptedPassword );
		layout.save( new FileWriter( config.getPropertiesFile().getAbsolutePath(), false ) );
		System.out.println( "CONFIG FILE UPDATED WITH ENCRYPTED PASSWORD: " + encryptedPassword );
	}

	/**
	 * Send notification from admin email address (emailFrom) to all emailTo recipients
	 * @param summary
	 * @throws Exception
	 */
	public void sendEmailNotification( final String summary ) throws Exception
	{
		final List<String> emailTos = requireList( EMAIL_TO );
		final String emailFrom = requireString( EMAIL_FROM );
		final String emailEncryptedPassword = requireString( EMAIL_ENCRYPTED_PASSWORD );
		final boolean includeQsub = requireBoolean( EMAIL_SEND_QSUB );
		emailMaxAttachmentMB = requirePositiveInteger( EMAIL_ATTACHMENT_MAX_MB );

		final Properties props = new Properties();
		props.put( "mail.smtp.auth", "true" );
		props.put( "mail.smtp.starttls.enable", "true" );
		props.put( "mail.smtp.host", "smtp.gmail.com" );
		props.put( "mail.smtp.port", "25" );

		// Get the Session object.
		final Session session = Session.getInstance( props, new javax.mail.Authenticator()
		{
			@Override
			protected PasswordAuthentication getPasswordAuthentication()
			{
				return new PasswordAuthentication( emailFrom, decrypt( emailEncryptedPassword ) );
			}
		} );

		final StringBuffer addys = new StringBuffer();
		for( final String to: emailTos )
		{
			if( addys.length() != 0 )
			{
				addys.append( "," );
			}
			addys.append( to );
		}

		// Create a default MimeMessage object.
		final Message message = new MimeMessage( session );
		message.setFrom( new InternetAddress( emailFrom ) );
		message.addRecipients( Message.RecipientType.TO, InternetAddress.parse( addys.toString() ) );
		message.setSubject( "BioLockJ " + classifierType + " Job Complete" );

		// MESSAGE BODY
		final BodyPart messageBodyPart = new MimeBodyPart();

		final Multipart multipart = new MimeMultipart();
		multipart.addBodyPart( messageBodyPart );

		String msg = null;

		final String logFilePath = requireString( LOG_FILE );
		final BodyPart logAttachment = getAttachment( logFilePath );
		if( logAttachment != null )
		{
			msg = "Review attached log file for details.\n";
			multipart.addBodyPart( logAttachment );
		}

		final String noLogMsg = ( ( msg == null )
				? "Log file exceded max size set in property: " + EMAIL_ATTACHMENT_MAX_MB + ".": "" ) + "\n";

		if( includeQsub )
		{
			if( msg == null )
			{
				msg = "Review attached qsub files for details.\n" + noLogMsg;
			}
			else
			{
				msg = "Review attached qsub/log files for details.\n" + noLogMsg;
			}

			final List<BodyPart> attachments = getAttachments();
			for( final BodyPart bp: attachments )
			{
				multipart.addBodyPart( bp );
			}
		}

		if( msg == null )
		{
			msg = noLogMsg;
		}

		int i = 0;
		for( final File f: ApplicationManager.getFailures() )
		{
			msg += "Failure[" + i++ + "] = " + f.getAbsolutePath() + "\n";
		}

		msg += "\n" + summary + "\n";

		// Send message
		messageBodyPart.setText( getMessage( msg ) );
		message.setContent( multipart );
		Transport.send( message );
		info( "EMAIL SENT!" );
	}

	private byte[] base64Decode( final String encodedPassword ) throws IOException
	{
		return Base64.getDecoder().decode( encodedPassword );
	}

	private String base64Encode( final byte[] bytes )
	{
		return Base64.getEncoder().encodeToString( bytes );
	}

	private String decrypt( final String property )
	{
		String decryptedPassword = null;
		try
		{
			final SecretKeyFactory keyFactory = SecretKeyFactory.getInstance( "PBEWithMD5AndDES" );
			final SecretKey key = keyFactory.generateSecret( new PBEKeySpec( PASSWORD ) );
			final Cipher pbeCipher = Cipher.getInstance( "PBEWithMD5AndDES" );
			pbeCipher.init( Cipher.DECRYPT_MODE, key, new PBEParameterSpec( SALT, 20 ) );
			decryptedPassword = new String( pbeCipher.doFinal( base64Decode( property ) ), "UTF-8" );
		}
		catch( final Exception ex )
		{
			error( ex.getMessage(), ex );
		}

		return decryptedPassword;

	}

	private String encrypt( final String property ) throws GeneralSecurityException, UnsupportedEncodingException
	{
		final SecretKeyFactory keyFactory = SecretKeyFactory.getInstance( "PBEWithMD5AndDES" );
		final SecretKey key = keyFactory.generateSecret( new PBEKeySpec( PASSWORD ) );
		final Cipher pbeCipher = Cipher.getInstance( "PBEWithMD5AndDES" );
		pbeCipher.init( Cipher.ENCRYPT_MODE, key, new PBEParameterSpec( SALT, 20 ) );
		return base64Encode( pbeCipher.doFinal( property.getBytes( "UTF-8" ) ) );
	}

	private BodyPart getAttachment( final String filePath ) throws Exception
	{
		try
		{
			final DataSource source = new FileDataSource( filePath );
			final File logFile = new File( filePath );
			info( "Email attachement file: " + filePath );
			info( "Email attachement size: " + getFileSize( logFile ) );
			final double fileSize = logFile.length() / 1000000;
			if( fileSize < emailMaxAttachmentMB )
			{
				final BodyPart attachPart = new MimeBodyPart();
				attachPart.setDataHandler( new DataHandler( source ) );
				attachPart.setFileName( filePath.substring( filePath.lastIndexOf( File.separator ) + 1 ) );
				return attachPart;
			}
			else
			{
				warn( "File too large to attach.  Max file size configured in prop file set to = "
						+ emailMaxAttachmentMB + " MB" );

			}
		}
		catch( final Exception ex )
		{
			error( "UNABLE TO ATTACH FILE", ex );
		}

		return null;
	}

	private List<BodyPart> getAttachments() throws Exception
	{
		final List<BodyPart> attachments = new ArrayList<>();
		final List<String> qsubDirs = getList( QSUBS );
		for( final String qsub: qsubDirs )
		{
			final List<File> files = (List<File>) FileUtils.listFiles( new File( qsub ), TrueFileFilter.INSTANCE,
					null );
			for( final File file: files )
			{
				final BodyPart qsubAttachment = getAttachment( file.getAbsolutePath() );

				if( qsubAttachment != null )
				{
					attachments.add( qsubAttachment );
				}
				else
				{
					warn( "UNABLE TO ATTACH QSUB OUTPUT: " + file.getAbsolutePath() );
				}

			}
		}

		return attachments;
	}

	private String getFileSize( final File f ) throws Exception
	{
		final double fileSize = f.length();
		info( "Raw file size = " + fileSize );

		final double fileSizeMB = fileSize / 1000000;
		final double fileSizeKB = fileSize / 1000;
		if( fileSizeMB > 0 )
		{
			return fileSizeMB + " MB";
		}
		if( fileSizeKB > 0 )
		{
			return fileSizeKB + " KB";
		}

		return fileSize + " B";

	}

}
