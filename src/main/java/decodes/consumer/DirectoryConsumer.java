/*
*  $Id$
*/
package decodes.consumer;

import java.io.File;
import java.io.OutputStream;
import java.util.Properties;
import java.util.Date;

import ilex.util.EnvExpander;
import ilex.util.FileUtil;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import ilex.var.Variable;
import decodes.datasource.RawMessage;
import decodes.datasource.UnknownPlatformException;
import decodes.datasource.GoesPMParser;
import decodes.decoder.DecodedMessage;
import decodes.db.*;
import decodes.util.PropertySpec;

/**
DirectoryConsumer sends data to files in a named directory. 
The 'argument' is the name of the directory.
Properties are used to specify file name templates, etc.
<ul>
  <li>filename - contains a template used to construct individual
      file names, which may contain the site ID and/or time/date stamps.</li>
  <li>tmpdir - option to build file in a temporary directory. After file
      is complete it is moved to the actual directory.</li>
</ul>
@see FileConsumer for explanation on how file names are constructed.
*/
public class DirectoryConsumer extends DataConsumer
{
	/** Set by argument */
	private String directoryName;

	/** The directory as a File object. */
	private File directory;
 
	/** Local copy of properties */
	private Properties props;

	/** current file being written */
	private FileConsumer curFileConsumer;

	/** Temporary file being written */
	private File outFile;

	/** Current file name */
	private String currentFileName;

	/** file name template */
	private String filenameTemplate;

	/** Temporary directory for building files. */
	private File tmpdir;
	
	/** So apps like poll and PollGUI can retrieve last file written */
	private File lastOutFile = null;
	
	private int sequenceNum = 1;
	
	PropertySpec[] myspecs = new PropertySpec[]
	{
		new PropertySpec("filenameTemplate", PropertySpec.STRING, 
			"Template for building filename."),
		new PropertySpec("tmpdir", PropertySpec.STRING, 
			"Temporary directory for building file before moving to final location."),

	};

	/** No-args constructor required */
	public DirectoryConsumer()
	{
		super();
		currentFileName = null;
		curFileConsumer = null;
		directory = null;
		tmpdir = null;
	}

	/**
	  Opens and initializes the consumer.
	  @param consumerArg The directory name
	  @param props The properties.
	  @throws DataConsumerException if the consumer could not be initialized.
	*/
	public void open(String consumerArg, Properties props)
		throws DataConsumerException
	{
		this.props = props;

		directoryName = EnvExpander.expand(consumerArg, props);
		directoryName = EnvExpander.expand(directoryName);
		directory = new File(directoryName);
		if (!directory.isDirectory())
			directory.mkdirs();

		filenameTemplate = PropertiesUtil.getIgnoreCase(props, "filenameTemplate");
		if (filenameTemplate == null)
		{
			filenameTemplate = PropertiesUtil.getIgnoreCase(props, "filename");
			if (filenameTemplate == null)
				filenameTemplate = "$SITENAME-$DATE(" + Constants.suffixDateFormat_fmt + ")";
		}
		Logger.instance().debug3("DirectoryConsumer filenameTemplate='" + filenameTemplate + "'");

		String tmpdirname = PropertiesUtil.getIgnoreCase(props, "tmpdir");
		if (tmpdirname != null)
		{
			tmpdir = new File(EnvExpander.expand(tmpdirname));
			if (!tmpdir.isDirectory())
				if (!tmpdir.mkdirs())
					tmpdir = null;
		}
	}

	/**
	  Closes the data consumer.
	  This method is called by the routing specification when the data
	  consumer is no longer needed.
	*/
	public void close()
	{
		endMessage();
	}

	public void startMessage(DecodedMessage msg)
		throws DataConsumerException
	{
		try { prepareConsumer(msg, false); }
		catch(DataConsumerException e ) { throw (e); }
	}	

	public void prepareConsumer(DecodedMessage msg, boolean appendToCurrentFile)
		throws DataConsumerException
	{
		if (curFileConsumer != null)
			endMessage(); // shouldn't happen!

		RawMessage rm = msg.getRawMessage();
		Variable v = rm.getPM(GoesPMParser.DCP_ADDRESS);
		if (v != null)
		{
			String s = v.toString();
			props.setProperty("TRANSPORTID", s);
			props.setProperty("DCP_ADDRESS", s);
		}
		try
		{
			Platform p = rm.getPlatform();
			if (p != null)
			{
				String n = p.getSiteName(false);
				if (n != null && n.length() > 0)
					props.setProperty("SITENAME", n);
			}
			TransportMedium tm = rm.getTransportMedium();
			Logger.instance().log(Logger.E_DEBUG3, 
                              "Transport Id  = " + tm.getMediumId() );
			if (tm != null)
				props.setProperty("TRANSPORTID", tm.getMediumId());
		}
		catch(UnknownPlatformException e) 
		{
		}
		
		props.setProperty("SEQUENCE", "" + (sequenceNum++));

		try
		{
			Logger.instance().debug2( 
            	"FileNameTemplate = " + filenameTemplate );
			Logger.instance().debug2( 
                "TRANSPORTID = " + props.getProperty("TRANSPORTID") );
			if ( currentFileName == null || !appendToCurrentFile )
			{
				Date d = rm.getTimeStamp();
//Logger.instance().info("DirConsumer: template='" + filenameTemplate + "', props=" + PropertiesUtil.props2string(props));
				currentFileName = EnvExpander.expand(filenameTemplate, props, d);
			}

			Logger.instance().debug2( 
                "CurrentFileName = " + currentFileName );

			if (tmpdir != null)
				outFile = new File(tmpdir, currentFileName);
			else
				outFile = new File(directory, currentFileName);

			Logger.instance().debug2(
				"Opening file '" + outFile.getPath() + "'");

			curFileConsumer = new FileConsumer();
			if ( appendToCurrentFile )
					props.setProperty("file.overwrite", "false");		 
			curFileConsumer.open(outFile.getPath(), props);
			curFileConsumer.startMessage(msg);
		}
		catch(DataConsumerException e)
		{
			Logger.instance().log(Logger.E_FAILURE,
				"Cannot create output file: " + e);
			curFileConsumer = null;
		}
	}

	/*
	  Output a single line by delegating to current FileConsumer.
	  @param line the line to be written.
	*/
	public void println(String line)
	{
		if (curFileConsumer != null)
			curFileConsumer.println(line);
	}

	/**
	  Closes the current file consumer.
	*/
	public void endMessage()
	{
		if (curFileConsumer != null)
		{
			curFileConsumer.endMessage();
			curFileConsumer.close();
			curFileConsumer = null;
			if (tmpdir != null && outFile.exists() && outFile.length() > 0L)
			{
				File permFile = new File(directory, outFile.getName());
				try { FileUtil.moveFile(outFile, permFile); }
				catch(Exception ex)
				{
					Logger.instance().failure(
						"Cannot move '" + outFile.getPath() + "' to '"
						+ permFile.getPath() + "': " + ex);
				}
			}
			lastOutFile = outFile;
			outFile = null;
			//Don't set curfile to null, so getActiveOutput will
			//return the last file written. Otherwise routmon always
			//says (no file)
			//currentFileName = null;
		}
	}

	public OutputStream getOutputStream()
		throws DataConsumerException
	{
		if (curFileConsumer == null)
			throw new DataConsumerException("No current file in directory.");
		return curFileConsumer.getOutputStream();
	}

	public String getActiveOutput()
	{
		return currentFileName != null ? currentFileName : "(no file)";
	}
	
	@Override
	public String getArgLabel()
	{
		return "Directory Name";
	}

	public File getLastOutFile()
	{
		return lastOutFile;
	}
	
	@Override
	public PropertySpec[] getSupportedProps()
	{
		return myspecs;
	}


}

