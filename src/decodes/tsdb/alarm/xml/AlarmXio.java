/*
 * $Id$
 * 
 * Copyright 2017 Cove Software, LLC. All rights reserved.
 * 
 * $Log$
 * Revision 1.1  2019/03/05 14:53:01  mmaloney
 * Checked in partial implementation of Alarm classes.
 *
 * Revision 1.5  2018/03/23 20:12:20  mmaloney
 * Added 'Enabled' flag for process and file monitors.
 *
 * Revision 1.4  2017/05/17 20:37:38  mmaloney
 * First working version.
 *
 * Revision 1.3  2017/03/30 20:55:20  mmaloney
 * Alarm and Event monitoring capabilities for 6.4 added.
 *
 * Revision 1.2  2017/03/21 12:17:11  mmaloney
 * First working XML and SQL I/O.
 *
 */
package decodes.tsdb.alarm.xml;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

import ilex.util.Logger;
import ilex.util.TextUtil;
import ilex.xml.DomHelper;
import ilex.xml.XmlOutputStream;
import decodes.sql.DbKey;
import decodes.tsdb.alarm.AlarmEvent;
import decodes.tsdb.alarm.AlarmGroup;
import decodes.tsdb.alarm.EmailAddr;
import decodes.tsdb.alarm.FileMonitor;
import decodes.tsdb.alarm.ProcessMonitor;
import decodes.tsdb.xml.DbXmlException;

/**
XML Input/Output for Alarm Meta Data.
*/
public class AlarmXio
{
	private String module = "AlarmXio";
	private String filename;

	public AlarmXio()
	{
	}

	/**
	 * Reads a file containing a single group definintion and returns the
	 * group object. 
	 * Alarm meta data objects read from XML files will have NullKey for
	 * all surrogate keys.
	 * @param filename
	 * @return
	 * @throws DbXmlException
	 */
	public AlarmGroup readFile(String filename)
		throws DbXmlException
	{
		Logger.instance().debug1(module + " reading file '" + filename + "'");
		this.filename = filename;
		Document doc;
		try
		{
			doc = DomHelper.readFile(module, filename);
		}
		catch(ilex.util.ErrorException ex)
		{
			throw new DbXmlException(ex.toString());
		}

		Node rootel = doc.getDocumentElement();
		if (!rootel.getNodeName().equalsIgnoreCase(AlarmXioTags.AlarmGroup))
		{
			String s = module 
				+ ": Wrong type of configuration file -- Cannot initialize. "
				+ "Root element is not '" + AlarmXioTags.AlarmGroup + "'.";
			Logger.instance().failure(s);
			throw new DbXmlException(s);
		}
		
		AlarmGroup grp = new AlarmGroup(DbKey.NullKey);
		
		grp.setName(DomHelper.findAttr((Element)rootel, AlarmXioTags.name));
		if (grp.getName() == null)
		{
			String s = module + " file '" + filename 
				+ "' AlarmGroup missing required 'name' attribute.";
			Logger.instance().failure(s);
			throw new DbXmlException(s);
		}
		
		NodeList children = rootel.getChildNodes();
		for(int i=0; children != null && i<children.getLength(); i++)
		{
			Node node = children.item(i);
			String nn = node.getNodeName();
			if (node.getNodeType() == Node.ELEMENT_NODE)
			{
				if (nn.equalsIgnoreCase(AlarmXioTags.Email))
					grp.getEmailAddrs().add(new EmailAddr(DomHelper.getTextContent(node).trim()));
				else if (nn.equalsIgnoreCase(AlarmXioTags.ProcessMonitor))
					addProcessMonitor(grp, node);
				else if (nn.equalsIgnoreCase(AlarmXioTags.FileMonitor))
					addFileMonitor(grp, node);
				else
				{
					Logger.instance().warning(module + " In file '" + filename
						+ "' unrecognized node '" + nn + "' skipped.");
				}
			}
		}
		return grp;
	}

	private void addProcessMonitor(AlarmGroup grp, Node node)
	{
		// Get number and host attributes.
		Element elem = (Element)node;
//System.out.println("addAlgorithm nodename=" + node.getNodeName());
		String name = DomHelper.findAttr(elem, AlarmXioTags.name);
		if (name == null)
		{
			Logger.instance().warning(module + ": " + filename
				+ " " + AlarmXioTags.ProcessMonitor + " element without name attribute -- ignored.");
			return;
		}
		ProcessMonitor pm = new ProcessMonitor(DbKey.NullKey);
		pm.setXmlProcName(name);
		
		String s = DomHelper.findAttr(elem, AlarmXioTags.enabled);
		if (s != null)
			pm.setEnabled(TextUtil.str2boolean(s));
		
//System.out.println(">>> process monitor " + name);
		
		NodeList children = node.getChildNodes();
		for(int i=0; children != null && i<children.getLength(); i++)
		{
			Node childNode = children.item(i);
			String nn = childNode.getNodeName();
			if (nn.equalsIgnoreCase("#text"))
				continue;
			else if (nn.equalsIgnoreCase(AlarmXioTags.enabled))
			{
				pm.setEnabled(DomHelper.getBooleanContent(childNode, true, module));
			}
			else if (nn.equalsIgnoreCase(AlarmXioTags.AlarmDef))
			{
				if (childNode.getNodeType() == Node.ELEMENT_NODE)
				{
					Element alarmDefElem = (Element)childNode;
	
					if (!nn.equalsIgnoreCase(AlarmXioTags.AlarmDef))
					{
						Logger.instance().warning(module + " In file '" + filename + "'"
							+ " ProcessMonitor with name=" + name + " has a child node "
							+ " that is not an AlarmDef. Skipped.");
						continue;
					}
					
					AlarmEvent def = new AlarmEvent(DbKey.NullKey);
					
					def.setPriority(getPriorityElement(alarmDefElem, AlarmXioTags.AlarmDef));
					String patt = DomHelper.getTextContent(alarmDefElem);
					if (patt != null && patt.trim().length() == 0)
						patt = null;
					def.setPattern(patt);
					
					pm.getDefs().add(def);
				}
			}
		}

		Logger.instance().debug2(module + " Adding ProcessMonitor " + name);
		grp.getProcessMonitors().add(pm);
	}
	
	/**
	 * Finds a priority attribute and returns the Logger E_ constant
	 * associated with it. Returns -1 if no attribute or unrecognized string.
	 * @param elem the Element
	 * @return the Logger E_ constant or -1.
	 */
	private int getPriorityElement(Element elem, String elemName)
	{
		String sPri = DomHelper.findAttr(elem, AlarmXioTags.priority);
		if (sPri == null)
			return -1;
		else if (sPri.trim().equalsIgnoreCase("ANY"))
			return -1;
		else if (TextUtil.startsWithIgnoreCase(sPri.trim(), "INFO"))
			return Logger.E_INFORMATION;
		else if (sPri.trim().equalsIgnoreCase("WARNING"))
			return Logger.E_WARNING;
		else if (sPri.trim().equalsIgnoreCase("FAILURE"))
			return Logger.E_FAILURE;
		else if (sPri.trim().equalsIgnoreCase("FATAL"))
			return Logger.E_FATAL;
		else
		{
			Logger.instance().warning(module + " In file '" + filename + "' "
				+ elemName + " has a unrecognized priority '"
				+ sPri + " -- defaulting to any-priority.");
			return -1;
		}

	}

	private void addFileMonitor(AlarmGroup grp, Node node)
	{
		// Get number and host attributes.
		Element elem = (Element)node;
		String path = DomHelper.findAttr(elem, AlarmXioTags.path);
		if (path == null)
		{
			Logger.instance().warning(module + ": " + filename
				+ " " + AlarmXioTags.FileMonitor 
				+ " element without " + AlarmXioTags.path + " attribute -- ignored.");
			return;
		}
		
		FileMonitor fm = new FileMonitor(path);
		fm.setPriority(getPriorityElement(elem, AlarmXioTags.FileMonitor));
		
		
		NodeList children = node.getChildNodes();
		for(int i=0; children != null && i<children.getLength(); i++)
		{
			Node childNode = children.item(i);
			if (childNode.getNodeType() == Node.ELEMENT_NODE)
			{
				Element childElem = (Element)childNode;
				
				String nn = childNode.getNodeName();
				if (nn.equalsIgnoreCase(AlarmXioTags.MaxFiles))
				{
					fm.setMaxFilesHint(DomHelper.findAttr(childElem, AlarmXioTags.hint));
					
					int v = DomHelper.getIntegerContent(childNode, -1, nn);
					if (v <= 0)
					{
						Logger.instance().warning(module + ": " + filename
							+ " " + AlarmXioTags.FileMonitor + " with invalid "
							+ AlarmXioTags.MaxFiles + " value. Requires positive integer. Ignored.");
						continue;
					}
					fm.setMaxFiles(v);
				}
				else if (nn.equalsIgnoreCase(AlarmXioTags.MaxSize))
				{
					fm.setMaxSizeHint(DomHelper.findAttr(childElem, AlarmXioTags.hint));
					
					long v = DomHelper.getLongIntContent(childNode, -1L, nn);
					if (v <= 0)
					{
						Logger.instance().warning(module + ": " + filename
							+ " " + AlarmXioTags.FileMonitor + " with invalid "
							+ AlarmXioTags.MaxSize + " value. Requires positive integer. Ignored.");
						continue;
					}
					fm.setMaxSize(v);
				}
				else if (nn.equalsIgnoreCase(AlarmXioTags.MaxLMT))
				{
					fm.setMaxLMTHint(DomHelper.findAttr(childElem, AlarmXioTags.hint));
					
					String v = DomHelper.getTextContent(childNode);
					if (v == null || v.trim().length() == 0)
					{
						Logger.instance().warning(module + ": " + filename
							+ " " + AlarmXioTags.FileMonitor + " with invalid "
							+ AlarmXioTags.MaxLMT + " value. Requires valid interval. Ignored.");
						continue;
					}
					fm.setMaxLMT(v.trim());
				}
				else if (nn.equalsIgnoreCase(AlarmXioTags.OnDelete))
				{
					fm.setAlarmOnDeleteHint(DomHelper.findAttr(childElem, AlarmXioTags.hint));
					fm.setAlarmOnDelete(true);
				}
				else if (nn.equalsIgnoreCase(AlarmXioTags.OnExists))
				{
					fm.setAlarmOnExistsHint(DomHelper.findAttr(childElem, AlarmXioTags.hint));
					fm.setAlarmOnExists(true);
				}
				else if (nn.equalsIgnoreCase(AlarmXioTags.enabled))
				{
					fm.setEnabled(DomHelper.getBooleanContent(childNode, true, module));
				}
				else
				{
					Logger.instance().warning(module + " In file '" + filename + "' "
						+ AlarmXioTags.FileMonitor + " with path=" + path 
						+ " has unrecognized a child node '" + nn + "' -- skipped.");
					continue;
				}
			}
		}
		grp.getFileMonitors().add(fm);
	}
	
	/**
	 * Write the alarm group to the named XML file.
	 * @param grp
	 * @throws IOException
	 */
	public void writeXML(AlarmGroup grp, String filename)
		throws IOException
	{
		// Open an output stream wrapped by an XmlOutputStream
		FileOutputStream fos = new FileOutputStream(filename);
		writeXML(grp, fos);
		fos.close();
	}
	
	/**
	 * Writes to an output stream. Does not close stream after fininshing.
	 * @param grp
	 * @param os
	 * @throws IOException
	 */
	public void writeXML(AlarmGroup grp, OutputStream os)
		throws IOException
	{
		XmlOutputStream xos = new XmlOutputStream(os, AlarmXioTags.AlarmGroup);
		xos.writeXmlHeader();

		xos.startElement(AlarmXioTags.AlarmGroup, AlarmXioTags.name, grp.getName());
		
		for(EmailAddr addr : grp.getEmailAddrs())
			xos.writeElement(AlarmXioTags.Email, addr.getAddr());

		for(FileMonitor fm : grp.getFileMonitors())
		{
			xos.startElement(AlarmXioTags.FileMonitor, AlarmXioTags.path, fm.getPath(),
				AlarmXioTags.priority, priority2string(fm.getPriority()));
			if (fm.isAlarmOnDelete())
				xos.writeElement(AlarmXioTags.OnDelete, AlarmXioTags.hint,
					(fm.getAlarmOnDeleteHint() == null ? "" : fm.getAlarmOnDeleteHint()), null);
			if (fm.isAlarmOnExists())
				xos.writeElement(AlarmXioTags.OnExists, AlarmXioTags.hint,
					(fm.getAlarmOnExistsHint() == null ? "" : fm.getAlarmOnExistsHint()), null);
			if (fm.getMaxFiles() > 0)
				xos.writeElement(AlarmXioTags.MaxFiles, AlarmXioTags.hint, 
					(fm.getMaxFilesHint() == null ? "" : fm.getMaxFilesHint()),
					"" + fm.getMaxFiles());
			if (fm.getMaxSize() > 0)
				xos.writeElement(AlarmXioTags.MaxSize, AlarmXioTags.hint, 
					(fm.getMaxSizeHint() == null ? "" : fm.getMaxSizeHint()),
					"" + fm.getMaxSize());
			if (fm.getMaxLMT() != null && fm.getMaxLMT().trim().length() > 0)
				xos.writeElement(AlarmXioTags.MaxLMT, AlarmXioTags.hint, 
					(fm.getMaxLMTHint() == null ? "" : fm.getMaxLMTHint()),
					fm.getMaxLMT());
			xos.writeElement(AlarmXioTags.enabled, "" + fm.isEnabled());
			xos.endElement(AlarmXioTags.FileMonitor);
		}
		
		for(ProcessMonitor pm : grp.getProcessMonitors())
		{
			xos.startElement(AlarmXioTags.ProcessMonitor, AlarmXioTags.name, pm.getProcName());
			xos.writeElement(AlarmXioTags.enabled, "" + pm.isEnabled());
			for(AlarmEvent def : pm.getDefs())
			{
				xos.writeElement(AlarmXioTags.AlarmDef, AlarmXioTags.priority, 
					priority2string(def.getPriority()), def.getPattern());
			}
			
			xos.endElement(AlarmXioTags.ProcessMonitor);
		}

		xos.endElement(AlarmXioTags.AlarmGroup);
	}
	
	private String priority2string(int pri)
	{
		switch(pri)
		{
		case Logger.E_INFORMATION: return "INFO";
		case Logger.E_WARNING: return "WARNING";
		case Logger.E_FAILURE: return "FAILURE";
		case Logger.E_FATAL: return "FATAL";
		default: return "ANY";
		}
	}
	
	/**
	 * Test main. Reads XML into objects and then converts back to XML to stdout.
	 * @param args 1 arg filename
	 */
	public static void main(String args[])
		throws Exception
	{
		if (args.length == 0)
		{
			System.err.println("Usage: java ... AlarmXio <filename>");
			System.exit(1);
		}
		
		AlarmXio xio = new AlarmXio();
		AlarmGroup grp = xio.readFile(args[0]);
		xio.writeXML(grp, System.out);
	}
}


