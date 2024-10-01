package decodes.tsdb.compedit;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ilex.util.EnvExpander;
import ilex.util.ErrorException;
import ilex.util.Logger;
import ilex.xml.DomHelper;

/**
 * Holds a python function list read from a file.
 */
public class PyFuncList
{
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
		try
		{
			doc = DomHelper.readFile(module, EnvExpander.expand(filename));
		}
		catch (ErrorException ex)
		{
			warning("Cannot read file: " + ex);
			return;
		}
        Node element = doc.getDocumentElement();
        if (!element.getNodeName().equalsIgnoreCase("funclist"))
        {
            warning("Wrong type of xml file -- Cannot initialize. "
                + "Root element is not 'funclist'.");
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
                    		warning("func without name attribute, ignored.");
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
						Logger.instance().warning(module + "File '" 
							+ filename + "': Unexpected node name '" + nn 
							+ " -- ignored.");
                }
            }
		}
	}
	
	private void warning(String msg)
	{
		Logger.instance().warning(module + " file=" + filename + ": " + msg);
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
