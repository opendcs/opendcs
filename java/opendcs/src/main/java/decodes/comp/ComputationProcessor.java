/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
* 
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software 
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations 
* under the License.
*/

package decodes.comp;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.comp.CompResolver;
import decodes.db.RoutingSpec;

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
import ilex.util.ErrorException;
import ilex.xml.DomHelper;

/**
* Handles the set of classes used for performing computations on
* decoded messages. An application should have one instance of this
* class.
*/
public class ComputationProcessor
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
		log.atInfo()
		.log("{} initializing with config file '{}' for routing spec '{}'",
			module,configFile,routingSpec.getName());
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
			log.atError().log(s);
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
						log.warn("Unexpected node name '{}'  -- ignored.", nn );
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
			log.warn("CompResolver element with no class attribute ignored.");
			return;
		}
		log.atInfo().log("Parsing CompResolver element for class '{}'",clsname);
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
							log.warn("CompResolver for class '{}'"
								+ " contains Property with no "
								+ "'name' attribute -- ignored.",clsname);
							continue;
						}
						String val = DomHelper.getTextContent(pelem).trim();
						cr.setProperty(attr, val);
						log.debug("CR Property '{}' =", attr, val);
					}
                }
            }

			cr.init(routingSpec);
			addCompResolver(cr);
		}
		catch(ClassNotFoundException ex)
		{
			log.atWarn().setCause(ex).log("CompResolver references class '{}'" 
				+ " which cannot be found -- ignored.",clsname);
			return;
		}
		catch(InstantiationException ex)
		{
			log.atWarn().setCause(ex).log("CompResolver references class '{}'"
				+ " which cannot be instantiated -- ignored.",clsname);
			return;
		}
		catch(IllegalAccessException ex)
		{
			log.atWarn().setCause(ex).log("CompResolver references class '{}'" 
				+ " which cannot be accessed -- ignored.",clsname);
			return;
		}
		catch(ClassCastException ex)
		{
			log.atWarn().setCause(ex)
			.log("CompResolver element uses class '{}'"  
				+ " which does not extend the CompResolver base class. "
				+ "-- ignored.",clsname);
			return;
		}
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
