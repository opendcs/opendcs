/*
 * $Id$
 * 
 * Copyright 2017 Cove Software, LLC. All rights reserved.
 * 
 * $Log$
 * Revision 1.2  2019/03/05 20:47:42  mmaloney
 * Support new table names for ALARM
 *
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
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

import ilex.util.Logger;
import ilex.util.TextUtil;
import ilex.xml.DomHelper;
import ilex.xml.XmlOutputStream;
import decodes.db.DataType;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.sql.DbKey;
import decodes.tsdb.alarm.AlarmEvent;
import decodes.tsdb.alarm.AlarmGroup;
import decodes.tsdb.alarm.AlarmScreening;
import decodes.tsdb.alarm.AlarmLimitSet;
import decodes.tsdb.alarm.EmailAddr;
import decodes.tsdb.alarm.FileMonitor;
import decodes.tsdb.alarm.ProcessMonitor;
import decodes.tsdb.xml.CompXioTags;
import decodes.tsdb.xml.DbXmlException;

/**
XML Input/Output for Alarm Meta Data.
*/
public class AlarmXio
{
	private String module = "AlarmXio";
	private String filename;
	public static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	static { sdf.setTimeZone(TimeZone.getTimeZone("UTC")); }


	public AlarmXio()
	{
	}
	
	/**
	 * Reads an openDCS 6.6 alarm xml file and returns it in an AlarmFile object
	 * @param filename
	 * @return
	 * @throws DbXmlException
	 */
	public AlarmFile readAlarmFile(String filename)
		throws DbXmlException
	{
		AlarmFile ret = new AlarmFile();
		
		Logger.instance().debug1(module + " reading alarm file '" + filename + "'");
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
		if (rootel.getNodeName().equalsIgnoreCase(AlarmXioTags.AlarmGroup))
		{
			// Legacy file contains a single group definition
			AlarmGroup ag = readAlarmGroup(rootel);
			ret.getGroups().add(ag);
		}
		else if (rootel.getNodeName().equalsIgnoreCase(AlarmXioTags.AlarmDefinitions))
		{
			// Walk the tree, which can contain multiple AlarmGroup or AlarmScreening elements
			NodeList children = rootel.getChildNodes();
			for(int i=0; children != null && i<children.getLength(); i++)
			{
				Node node = children.item(i);
				String nn = node.getNodeName();
				if (node.getNodeType() == Node.ELEMENT_NODE)
				{
					if (nn.equalsIgnoreCase(AlarmXioTags.AlarmGroup))
						ret.getGroups().add(readAlarmGroup(node));
					else if (nn.equalsIgnoreCase(AlarmXioTags.AlarmScreening))
						ret.getScreenings().add(readScreening(node));
				}
			}
		}
		
		return ret;
	}

//	/**
//	 * Reads a file containing a single group definintion and returns the
//	 * group object. 
//	 * Alarm meta data objects read from XML files will have NullKey for
//	 * all surrogate keys.
//	 * @param filename
//	 * @return
//	 * @throws DbXmlException
//	 */
//	public AlarmGroup readFile(String filename)
//		throws DbXmlException
//	{
//		Logger.instance().debug1(module + " reading file '" + filename + "'");
//		this.filename = filename;
//		Document doc;
//		try
//		{
//			doc = DomHelper.readFile(module, filename);
//		}
//		catch(ilex.util.ErrorException ex)
//		{
//			throw new DbXmlException(ex.toString());
//		}
//
//		Node alarmGroupNode = doc.getDocumentElement();
//		if (!alarmGroupNode.getNodeName().equalsIgnoreCase(AlarmXioTags.AlarmGroup))
//		{
//			String s = module 
//				+ ": Wrong type of configuration file -- Cannot initialize. "
//				+ "Root element is not '" + AlarmXioTags.AlarmGroup + "'.";
//			Logger.instance().failure(s);
//			throw new DbXmlException(s);
//		}
//		
//		
//		return readAlarmGroup(alarmGroupNode);
//	}
	
	public AlarmGroup readAlarmGroup(Node alarmGroupNode)
		throws DbXmlException
	{
		AlarmGroup grp = new AlarmGroup(DbKey.NullKey);
	
		grp.setName(DomHelper.findAttr((Element)alarmGroupNode, AlarmXioTags.name));
		if (grp.getName() == null)
		{
			String s = module + " file '" + filename 
				+ "' AlarmGroup missing required 'name' attribute.";
			Logger.instance().failure(s);
			throw new DbXmlException(s);
		}
		
		NodeList children = alarmGroupNode.getChildNodes();
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
	
	public AlarmScreening readScreening(Node screeningNode)
		throws DbXmlException
	{
		AlarmScreening scrn = new AlarmScreening();
		scrn.setScreeningName(DomHelper.findAttr((Element)screeningNode, AlarmXioTags.name));
		if (scrn.getScreeningName() == null)
		{
			String s = module + " file '" + filename 
				+ "' AlarmScreening missing required 'name' attribute.";
			Logger.instance().failure(s);
			throw new DbXmlException(s);
		}
		
Logger.instance().debug3(module + " reading screening '" + scrn.getScreeningName() + "'");
		// Walk the DOM tree and fill in screening and limit sets.
		NodeList children = screeningNode.getChildNodes();
		for(int i=0; children != null && i<children.getLength(); i++)
		{
			Node node = children.item(i);
			String nn = node.getNodeName();
Logger.instance().debug3(module + "     " + nn + " iselement=" + (node.getNodeType() == Node.ELEMENT_NODE));
			if (node.getNodeType() != Node.ELEMENT_NODE)
				continue;
			
			Element childElem = (Element)node;

			if (nn.equalsIgnoreCase(CompXioTags.siteName))
			{
				SiteName siteName = new SiteName(null, 
					DomHelper.findAttr(childElem, CompXioTags.nameType),
					DomHelper.getTextContent(node));
				scrn.getSiteNames().add(siteName);
			}
			else if (nn.equalsIgnoreCase(CompXioTags.dataType))
			{
				String std = DomHelper.findAttr(childElem, CompXioTags.standard);
				String cod = DomHelper.findAttr(childElem,	CompXioTags.code);
				if (cod == null)
					cod = DomHelper.getTextContent(node);
				if (std == null || cod == null)
					Logger.instance().warning(module + " Invalid datatype in screening '"
						+ scrn.getScreeningName() + "' std=" + std + ", code=" + cod);
				else
				{
					DataType dataType = DataType.getDataType(std, cod);
					scrn.setDataType(dataType);
Logger.instance().debug3(module + "     assigned datatype = " + dataType);
				}
			}
			else if (nn.equalsIgnoreCase(AlarmXioTags.startDateTime))
			{
				try
				{
					scrn.setStartDateTime(sdf.parse(node.getTextContent().trim()));
				}
				catch (Exception ex)
				{
					Logger.instance().warning(module + " Error parsing startDateTime with text content '"
						+ node.getTextContent() + "': " + ex + " -- ignored.");
				}
			}
			else if (nn.equalsIgnoreCase(AlarmXioTags.lastModified))
			{
				try
				{
					scrn.setLastModified(sdf.parse(node.getTextContent().trim()));
				}
				catch (Exception ex)
				{
					Logger.instance().warning(module + " Error parsing lastModified with text content '"
						+ node.getTextContent() + "': " + ex + " -- ignored.");
				}
			}
			else if (nn.equalsIgnoreCase(AlarmXioTags.lastModified))
				scrn.setEnabled(TextUtil.str2boolean(node.getTextContent().trim()));
			else if (nn.equalsIgnoreCase(AlarmXioTags.alarmGroupName))
				scrn.setGroupName(node.getTextContent().trim());
			else if (nn.equalsIgnoreCase(AlarmXioTags.desc))
				scrn.setDescription(node.getTextContent().trim());
			else if (nn.equalsIgnoreCase(AlarmXioTags.AlarmLimitSet))
			{
				AlarmLimitSet als = new AlarmLimitSet();
				als.setSeasonName(DomHelper.findAttr(childElem, AlarmXioTags.seasonName));
				scrn.addLimitSet(als);
Logger.instance().debug3(module + "    added AlarmLimitSet with season=" + als.getSeasonName());
				NodeList limSetChild = childElem.getChildNodes();
				for(int lsi=0; limSetChild != null && lsi<limSetChild.getLength(); lsi++)
				{
					Node lsNode = limSetChild.item(lsi);
					String lsnn = lsNode.getNodeName();
					if (lsNode.getNodeType() != Node.ELEMENT_NODE)
						continue;
					
					String content = lsNode.getTextContent().trim();
Logger.instance().debug3(module + "        " + lsnn + " content='" + content + "'");
					
					if (lsnn.equalsIgnoreCase(AlarmXioTags.rejectHigh))
					{
						try { als.setRejectHigh(Double.parseDouble(content)); }
						catch(Exception ex)
						{
							Logger.instance().warning(module + " Error parsing " + AlarmXioTags.rejectHigh
								+ " '" + content + "': must be a number -- ignored.");
						}
					}
					else if (lsnn.equalsIgnoreCase(AlarmXioTags.criticalHigh))
					{
						try { als.setCriticalHigh(Double.parseDouble(content)); }
						catch(Exception ex)
						{
							Logger.instance().warning(module + " Error parsing " + AlarmXioTags.criticalHigh
								+ " '" + content + "': must be a number -- ignored.");
						}
					}
					else if (lsnn.equalsIgnoreCase(AlarmXioTags.warningHigh))
					{
						try { als.setWarningHigh(Double.parseDouble(content)); }
						catch(Exception ex)
						{
							Logger.instance().warning(module + " Error parsing " + AlarmXioTags.warningHigh
								+ " '" + content + "': must be a number -- ignored.");
						}
					}
					else if (lsnn.equalsIgnoreCase(AlarmXioTags.warningLow))
					{
						try { als.setWarningLow(Double.parseDouble(content)); }
						catch(Exception ex)
						{
							Logger.instance().warning(module + " Error parsing " + AlarmXioTags.warningLow
								+ " '" + content + "': must be a number -- ignored.");
						}
					}
					else if (lsnn.equalsIgnoreCase(AlarmXioTags.criticalLow))
					{
						try { als.setCriticalLow(Double.parseDouble(content)); }
						catch(Exception ex)
						{
							Logger.instance().warning(module + " Error parsing " + AlarmXioTags.criticalLow
								+ " '" + content + "': must be a number -- ignored.");
						}
					}
					else if (lsnn.equalsIgnoreCase(AlarmXioTags.rejectLow))
					{
						try { als.setRejectLow(Double.parseDouble(content)); }
						catch(Exception ex)
						{
							Logger.instance().warning(module + " Error parsing " + AlarmXioTags.rejectLow
								+ " '" + content + "': must be a number -- ignored.");
						}
					}
					else if (lsnn.equalsIgnoreCase(AlarmXioTags.stuckDuration))
						als.setStuckDuration(content);
					else if (lsnn.equalsIgnoreCase(AlarmXioTags.stuckTolerance))
					{
						try { als.setStuckTolerance(Double.parseDouble(content)); }
						catch(Exception ex)
						{
							Logger.instance().warning(module + " Error parsing " + AlarmXioTags.stuckTolerance
								+ " '" + content + "': must be a number -- ignored.");
						}
					}
					else if (lsnn.equalsIgnoreCase(AlarmXioTags.stuckMinToCheck))
					{
						try { als.setMinToCheck(Double.parseDouble(content)); }
						catch(Exception ex)
						{
							Logger.instance().warning(module + " Error parsing " + AlarmXioTags.stuckMinToCheck
								+ " '" + content + "': must be a number -- ignored.");
						}
					}
					else if (lsnn.equalsIgnoreCase(AlarmXioTags.stuckMaxGap))
						als.setMaxGap(content);
					else if (lsnn.equalsIgnoreCase(AlarmXioTags.rocInterval))
						als.setRocInterval(content);
					if (lsnn.equalsIgnoreCase(AlarmXioTags.rejectRocHigh))
					{
						try { als.setRejectRocHigh(Double.parseDouble(content)); }
						catch(Exception ex)
						{
							Logger.instance().warning(module + " Error parsing " + AlarmXioTags.rejectRocHigh
								+ " '" + content + "': must be a number -- ignored.");
						}
					}
					else if (lsnn.equalsIgnoreCase(AlarmXioTags.criticalRocHigh))
					{
						try { als.setCriticalRocHigh(Double.parseDouble(content)); }
						catch(Exception ex)
						{
							Logger.instance().warning(module + " Error parsing " + AlarmXioTags.criticalRocHigh
								+ " '" + content + "': must be a number -- ignored.");
						}
					}
					else if (lsnn.equalsIgnoreCase(AlarmXioTags.warningRocHigh))
					{
						try { als.setWarningRocHigh(Double.parseDouble(content)); }
						catch(Exception ex)
						{
							Logger.instance().warning(module + " Error parsing " + AlarmXioTags.warningRocHigh
								+ " '" + content + "': must be a number -- ignored.");
						}
					}
					else if (lsnn.equalsIgnoreCase(AlarmXioTags.warningRocLow))
					{
						try { als.setWarningRocLow(Double.parseDouble(content)); }
						catch(Exception ex)
						{
							Logger.instance().warning(module + " Error parsing " + AlarmXioTags.warningRocLow
								+ " '" + content + "': must be a number -- ignored.");
						}
					}
					else if (lsnn.equalsIgnoreCase(AlarmXioTags.criticalRocLow))
					{
						try { als.setCriticalRocLow(Double.parseDouble(content)); }
						catch(Exception ex)
						{
							Logger.instance().warning(module + " Error parsing " + AlarmXioTags.criticalRocLow
								+ " '" + content + "': must be a number -- ignored.");
						}
					}
					else if (lsnn.equalsIgnoreCase(AlarmXioTags.rejectRocLow))
					{
						try { als.setRejectRocLow(Double.parseDouble(content)); }
						catch(Exception ex)
						{
							Logger.instance().warning(module + " Error parsing " + AlarmXioTags.rejectRocLow
								+ " '" + content + "': must be a number -- ignored.");
						}
					}
					else if (lsnn.equalsIgnoreCase(AlarmXioTags.missingPeriod))
						als.setMissingPeriod(content);
					else if (lsnn.equalsIgnoreCase(AlarmXioTags.missingInterval))
						als.setMissingInterval(content);
					else if (lsnn.equalsIgnoreCase(AlarmXioTags.missingMaxValues))
					{
						try { als.setMaxMissingValues(Integer.parseInt(content)); }
						catch(Exception ex)
						{
							Logger.instance().warning(module + " Error parsing " + AlarmXioTags.rejectRocLow
								+ " '" + content + "': must be a integer -- ignored.");
						}
					}
					else if (lsnn.equalsIgnoreCase(AlarmXioTags.hint))
						als.setHintText(content);
				}
				
Logger.instance().debug3(module + " after parsing limitSet.stuckDuration=" + als.getStuckDuration()
+ ", rocInterval=" + als.getRocInterval());
				
			}
		}
		
		return scrn;
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
		
		writeXML(grp, xos);
	}

	public void writeXML(AlarmGroup grp, XmlOutputStream xos)
		throws IOException
	{
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
	 * Writes to an output stream. Does not close stream after fininshing.
	 * @param grp
	 * @param os
	 * @throws IOException
	 */
	public void writeXML(ArrayList<AlarmScreening> screenings, ArrayList<AlarmGroup> groups, OutputStream os)
		throws IOException
	{
		XmlOutputStream xos = new XmlOutputStream(os, AlarmXioTags.AlarmDefinitions);
		xos.writeXmlHeader();

		xos.startElement(AlarmXioTags.AlarmDefinitions);
		
		decodes.db.Database decDb = decodes.db.Database.getDb();
		
		// Write the alarm groups first.
		for(AlarmGroup grp : groups)
			writeXML(grp, xos);
		
		for(AlarmScreening as : screenings)
		{
			xos.startElement(AlarmXioTags.AlarmScreening, AlarmXioTags.name, as.getScreeningName());
			
			if (!DbKey.isNull(as.getSiteId()))
			{
				Site site = decDb.siteList.getSiteById(as.getSiteId());
				if (site != null)
					for(SiteName sn : site.getNameArray())
						xos.writeElement(CompXioTags.siteName, CompXioTags.nameType,  sn.getNameType(), 
							sn.getNameValue());
				else
					Logger.instance().warning("Alarm Screening with id=" + as.getScreeningId()
						+ " and name='" + as.getScreeningName() + "' has an invalid site with id=" 
						+ as.getSiteId() + " -- ignored.");
			}
			if (!DbKey.isNull(as.getDatatypeId()))
			{
				DataType dt = decDb.dataTypeSet.getById(as.getDatatypeId());
				if (dt != null)
				{
					xos.writeElement(CompXioTags.dataType,
						CompXioTags.standard, dt.getStandard(), 
						CompXioTags.code, dt.getCode(), null);
				}
				else
					Logger.instance().warning("Alarm Screening with id=" + as.getScreeningId()
						+ " and name='" + as.getScreeningName() + "' has an invalid datatype with id=" 
						+ as.getDatatypeId() + " -- ignored.");
			}
			if (as.getStartDateTime() != null)
				xos.writeElement(AlarmXioTags.startDateTime, sdf.format(as.getStartDateTime()));
			Date lmt = as.getLastModified();
			xos.writeElement(AlarmXioTags.lastModified, sdf.format(lmt != null ? lmt : new Date()));
			xos.writeElement(AlarmXioTags.enabled, "" + as.isEnabled());
			
			if (!DbKey.isNull(as.getAlarmGroupId()))
			{
				boolean found = false;
				for(AlarmGroup grp : groups)
					if (as.getAlarmGroupId().equals(grp.getAlarmGroupId()))
					{
						xos.writeElement(AlarmXioTags.alarmGroupName, grp.getName());
						found = true;
					}
				if (!found)
					Logger.instance().warning("Alarm Screening with id=" + as.getScreeningId()
						+ " and name='" + as.getScreeningName() + "' has an invalid AlarmGroup with id=" 
						+ as.getAlarmGroupId() + " -- ignored.");
			}
			
			if (as.getDescription() != null)
				xos.writeElement(AlarmXioTags.desc, as.getDescription());
			
			NumberFormat nf = NumberFormat.getNumberInstance();
			nf.setGroupingUsed(false);
			nf.setMaximumFractionDigits(6);
			for(AlarmLimitSet als : as.getLimitSets())
			{
				if (als.getSeasonName() == null)
					xos.startElement(AlarmXioTags.AlarmLimitSet);
				else
					xos.startElement(AlarmXioTags.AlarmLimitSet, AlarmXioTags.seasonName, als.getSeasonName());
				
				if (als.getRejectHigh() != AlarmLimitSet.UNASSIGNED_LIMIT)
					xos.writeElement(AlarmXioTags.rejectHigh, nf.format(als.getRejectHigh()));
				if (als.getCriticalHigh() != AlarmLimitSet.UNASSIGNED_LIMIT)
					xos.writeElement(AlarmXioTags.criticalHigh, nf.format(als.getCriticalHigh()));
				if (als.getWarningHigh() != AlarmLimitSet.UNASSIGNED_LIMIT)
					xos.writeElement(AlarmXioTags.warningHigh, nf.format(als.getWarningHigh()));
				if (als.getWarningLow() != AlarmLimitSet.UNASSIGNED_LIMIT)
					xos.writeElement(AlarmXioTags.warningLow, nf.format(als.getWarningLow()));
				if (als.getCriticalLow() != AlarmLimitSet.UNASSIGNED_LIMIT)
					xos.writeElement(AlarmXioTags.criticalLow, nf.format(als.getCriticalLow()));
				if (als.getRejectLow() != AlarmLimitSet.UNASSIGNED_LIMIT)
					xos.writeElement(AlarmXioTags.rejectLow, nf.format(als.getRejectLow()));
				
				if (als.getStuckDuration() != null)
				{
					xos.writeElement(AlarmXioTags.stuckDuration, als.getStuckDuration());
					xos.writeElement(AlarmXioTags.stuckTolerance,
						nf.format(als.getStuckTolerance() != AlarmLimitSet.UNASSIGNED_LIMIT ?
							als.getStuckTolerance() : 0.0));
					if (als.getMinToCheck() != AlarmLimitSet.UNASSIGNED_LIMIT)
						xos.writeElement(AlarmXioTags.stuckMinToCheck, nf.format(als.getMinToCheck()));
					if (als.getMaxGap() != null)
						xos.writeElement(AlarmXioTags.stuckMaxGap, als.getMaxGap());
				}
				
				if (als.getRocInterval() != null)
				{
					xos.writeElement(AlarmXioTags.rocInterval, als.getRocInterval());
					if (als.getRejectRocHigh() != AlarmLimitSet.UNASSIGNED_LIMIT)
						xos.writeElement(AlarmXioTags.rejectRocHigh, nf.format(als.getRejectRocHigh()));
					if (als.getCriticalRocHigh() != AlarmLimitSet.UNASSIGNED_LIMIT)
						xos.writeElement(AlarmXioTags.criticalRocHigh, nf.format(als.getCriticalRocHigh()));
					if (als.getWarningRocHigh() != AlarmLimitSet.UNASSIGNED_LIMIT)
						xos.writeElement(AlarmXioTags.warningRocHigh, nf.format(als.getWarningRocHigh()));
					if (als.getWarningRocLow() != AlarmLimitSet.UNASSIGNED_LIMIT)
						xos.writeElement(AlarmXioTags.warningRocLow, nf.format(als.getWarningRocLow()));
					if (als.getCriticalRocLow() != AlarmLimitSet.UNASSIGNED_LIMIT)
						xos.writeElement(AlarmXioTags.criticalRocLow, nf.format(als.getCriticalRocLow()));
					if (als.getRejectRocLow() != AlarmLimitSet.UNASSIGNED_LIMIT)
						xos.writeElement(AlarmXioTags.rejectRocLow, nf.format(als.getRejectRocLow()));
				}
				
				if (als.getMissingPeriod() != null && als.getMissingInterval() != null)
				{
					xos.writeElement(AlarmXioTags.missingPeriod, als.getMissingPeriod());
					xos.writeElement(AlarmXioTags.missingInterval, als.getMissingInterval());
					xos.writeElement(AlarmXioTags.missingMaxValues, "" + als.getMaxMissingValues());
				}
				
				if (als.getHintText() != null)
					xos.writeElement(AlarmXioTags.hint, als.getHintText());
				
				xos.endElement(AlarmXioTags.AlarmLimitSet);
			}
			
			xos.endElement(AlarmXioTags.AlarmScreening);
		}

		xos.endElement(AlarmXioTags.AlarmDefinitions);
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
		AlarmFile alarmFile = xio.readAlarmFile(args[0]);
		xio.writeXML(alarmFile.getScreenings(), alarmFile.getGroups(), System.out);
	}
}


