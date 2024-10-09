/*
*  $Id$
*
*  $Log$
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.2  2009/06/18 18:01:56  mjmaloney
*  Run computations from HTML display in dcp monitor
*
*  Revision 1.1  2008/04/04 18:20:59  cvs
*  Added legacy code to repository
*
*  Revision 1.6  2007/06/27 20:57:36  mmaloney
*  dev
*
*  Revision 1.5  2004/08/24 14:31:28  mjmaloney
*  Added javadocs
*
*  Revision 1.4  2004/08/11 21:40:57  mjmaloney
*  Improved javadocs
*
*  Revision 1.3  2004/08/11 21:17:17  mjmaloney
*  dev
*
*  Revision 1.2  2004/06/24 18:36:06  mjmaloney
*  Preliminary working version.
*
*  Revision 1.1  2004/06/24 14:29:53  mjmaloney
*  Created.
*
*/
/**
 * @(#) ComputationProcessor.java
 */
package decodes.comp;

import decodes.comp.CompResolver;
import decodes.db.RoutingSpec;
//import decodes.decoder.DecodedMessage;

import java.util.Vector;
import java.util.ArrayList;
import java.util.Iterator;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Element;

import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.util.ErrorException;
import ilex.xml.DomHelper;

/**
* Handles the set of classes used for performing computations on
* decoded messages. An application should have one instance of this
* class.
*/
public class ComputationProcessor
{
	/**
	* Set of known resolvers, populated from configuration file.
	*/
	private ArrayList<CompResolver> compResolvers;
	
	/**
	* Module name required by DOM reader.
	*/
	public static final String module = "ComputationProcessor";

	
	/**
	* Configuration file being read, used in warning messages.
	*/
	private String configFile;
	
	private RoutingSpec routingSpec = null;
	
	/**
	* Resolve and execute all relavent computations for the passed
	* DecodedMessage.
	* @param msg the data collection
	*/
	public synchronized void applyComputations( IDataCollection msg )
	{
		for(CompResolver cr : compResolvers)
		{
			Computation comps[] = cr.resolve(msg);
			if (comps != null)
				for(int i=0; i< comps.length; i++)
					comps[i].apply(msg);
		}
	}
	
	/**
	* Construct new ComputationProcessor.
	*/
	public ComputationProcessor( )
	{
		compResolvers = new ArrayList<CompResolver>();
	}
	
	/**
	 * Initialize the computation processor from the named configuraiton file.
	 * @param configFile name of configuration file to use.
	 * @param routingSpec The routing spec that we're running under, or null
	 * if this is for the DCP Monitor.
	 */
	public synchronized void init( String configFile, RoutingSpec routingSpec )
		throws BadConfigException
	{
		Logger.instance().debug1(module + " initializing with config file '"
			+ configFile + "' for routing spec '" + routingSpec.getName() + "'");
		this.configFile = configFile;
		this.routingSpec = routingSpec;
		
		// MJM 6/18/2009 - Allow init to be called multiple times. Each time
		// It throws away anything it had before.
		compResolvers = new ArrayList<CompResolver>();

        Document doc;
		try
		{
			doc = DomHelper.readFile(module, EnvExpander.expand(configFile));
		}
		catch(ilex.util.ErrorException ex)
		{
			throw new BadConfigException(ex.toString());
		}
                                                                                
        Node element = doc.getDocumentElement();
        if (!element.getNodeName().equalsIgnoreCase("ComputationProcessor"))
        {
            String s = module
                + ": Wrong type of configuration file -- Cannot initialize. "
                + "Root element is not 'ComputationProcessor'.";
            Logger.instance().failure(s);
            throw new BadConfigException(s);
        }

        NodeList children = element.getChildNodes();
        if (children != null)
		{
			int length = children.getLength();
            for(int i=0; i<length; i++)
            {
                Node node = children.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE)
                {
                    String nn = node.getNodeName();
                    if (nn.equalsIgnoreCase("CompResolver"))
						parseCRElem((Element)node);
					else
						parseWarning("Unexpected node name '" + nn 
							+ " -- ignored.");
                }
            }
		}
	}

	/**
	* Called from init (or externally), adds a CompResolver to the internal
	* collection.
	* @param r the resolver
	*/
	public void addCompResolver( CompResolver r )
	{
		compResolvers.add(r);
	}

	/**
	* Parses a CompResolver element from the DOM.
	* @param crelem the comp resolver Element from the DOM.
	*/
	private void parseCRElem( Element crelem )
	{
		String clsname = crelem.getAttribute("class");
		if (clsname == null)
		{
			parseWarning(
				"CompResolver element with no class attribute ignored.");
			return;
		}
		Logger.instance().debug1("Parsing CompResolver element for class '"
			+ clsname + "'");
		try
		{
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			Class cls = cl.loadClass(clsname);
			CompResolver cr = (CompResolver)cls.newInstance();
        	NodeList crkids = crelem.getChildNodes();
        	if (crkids != null)
			{
				int length = crkids.getLength();
            	for(int i=0; i<length; i++)
            	{
                	Node node = crkids.item(i);
                    String nn = node.getNodeName();
                	if (node.getNodeType() == Node.ELEMENT_NODE
                     && nn.equalsIgnoreCase("Property"))
                	{
						Element pelem = (Element)node;
						String attr = pelem.getAttribute("name");
						if (attr == null)
						{
							parseWarning("CompResolver for class '"
								+ clsname + " contains Property with no "
								+ "'name' attribute -- ignored.");
							continue;
						}
						String val = DomHelper.getTextContent(pelem).trim();
						cr.setProperty(attr, val);
						Logger.instance().debug1("CR Property '" + attr
							+ "=" + val);
					}
                }
            }

			cr.init(routingSpec);
			addCompResolver(cr);
		}
		catch(ClassNotFoundException ex)
		{
			parseWarning("CompResolver references class '" + clsname
				+ "' which cannot be found -- ignored.");
			return;
		}
		catch(InstantiationException ex)
		{
			parseWarning("CompResolver references class '" + clsname
				+ "' which cannot be instantiated -- ignored.");
			return;
		}
		catch(IllegalAccessException ex)
		{
			parseWarning("CompResolver references class '" + clsname
				+ "' which cannot be accessed -- ignored.");
			return;
		}
		catch(ClassCastException ex)
		{
			parseWarning("CompResolver element uses class '" + clsname
				+ " which does not extend the CompResolver base class. "
				+ "-- ignored.");
			return;
		}
	}
	
	
	/**
	* Print a warning message with the config file name.
	* @param msg a message to print
	*/
	private void parseWarning( String msg )
	{
		Logger.instance().warning(module + "Config File '" + configFile
			+ "': " + msg);
	}
	
	/**
	 * Called before routing spec exits. Shut down all resolvers.
	 */
	public void shutdown()
	{
		for(CompResolver cr : compResolvers)
			cr.shutdown();
	}
	
	
}
