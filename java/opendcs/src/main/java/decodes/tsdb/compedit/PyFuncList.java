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
package decodes.tsdb.compedit;

import java.util.Collection;
import java.util.HashMap;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ilex.util.EnvExpander;
import ilex.util.ErrorException;
import ilex.xml.DomHelper;

/**
 * Holds a python function list read from a file.
 */
public class PyFuncList
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	public static final String module = "PyFuncList";
	private String filename = null;
	private String listName = null;

	private HashMap<String, PyFunction> funcs = new HashMap<String, PyFunction>();

	public PyFuncList(String listName, String filename)
	{
		this.listName = listName;
		this.filename = filename;
	}

	public void readFile()
	{
		Document doc;
		final String fileName = EnvExpander.expand(filename);
		try
		{
			doc = DomHelper.readFile(module, fileName);
		}
		catch (ErrorException ex)
		{
			log.atWarn().setCause(ex).log("Cannot read file: '{}'", fileName);
			return;
		}
        Node element = doc.getDocumentElement();
        if (!element.getNodeName().equalsIgnoreCase("funclist"))
        {
            log.warn("Wrong type of xml file -- Cannot initialize. Root element is not 'funclist'.");
            return;
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
                    if (nn.equalsIgnoreCase("func"))
                    {
                    	String nm = DomHelper.findAttr((Element)node, "name");
                    	if (nm == null)
                    	{
                    		log.warn("func without name attribute, ignored.");
                    		continue;
                    	}
                    	String sig = DomHelper.findAttr((Element)node, "sig");
                    	if (sig == null)
                    	{
                    		sig = nm + "()";
                    	}
                    	String desc = DomHelper.getTextContent(node);
                    	funcs.put(nm, new PyFunction(nm, sig, desc));
                    }
					else
					{
						log.warn("File '{}': Unexpected node name '{} -- ignored.", fileName, nn);
					}
                }
            }
		}
	}

	/**
	 * Return the function with the given name, or null if none found.
	 * @param name the function name
	 * @return the PyFunction or null if not found.
	 */
	public PyFunction get(String name)
	{
		return funcs.get(name);
	}

	public void dump()
	{
		for(String nm : funcs.keySet())
		{
			PyFunction func = funcs.get(nm);
			System.out.println();
			System.out.println("============================================");
			System.out.println("name=" + nm + ", sig=" + func.getSignature()
				+ "\n" + func.getDesc());
		}
	}

	public String getListName()
	{
		return listName;
	}

	public Collection<PyFunction> getList()
	{
		return funcs.values();
	}

}