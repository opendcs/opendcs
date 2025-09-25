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
package decodes.tsdb.xml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;

import opendcs.dai.TsGroupDAI;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import ilex.util.HasProperties;
import ilex.util.TextUtil;
import ilex.xml.DomHelper;
import ilex.xml.TaggedStringOwner;
import ilex.xml.TaggedStringSetter;
import ilex.xml.XmlHierarchyParser;
import ilex.xml.XmlObjectParser;
import ilex.xml.XmlOutputStream;
import decodes.sql.DbKey;
import decodes.tsdb.*;
import decodes.db.Constants;
import decodes.db.SiteName;
import decodes.db.DataType;

/**
XML Input/Output for Computational Meta Data.
*/
public class CompXio implements XmlObjectParser, TaggedStringOwner
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private String module;
	private String filename;
	private TimeSeriesDb theDb = null;
	private static final int propertyTag = 0;
	private static final int commentTag = 1;

	private CompAppInfo workingObject = null;
	private String propName = null;

	/** Default constructor. */
	public CompXio(String module, TimeSeriesDb theDb)
	{
		this.module = module;
		this.theDb = theDb;
	}

	public ArrayList<CompMetaData> readStream(InputStream stream) throws DbXmlException
	{
		this.filename = "Provided Stream";
		Document doc;
		try
		{
			doc = DomHelper.readStream(module, stream);
		}
		catch(ilex.util.ErrorException ex)
		{
			throw new DbXmlException("Unable to read XML file", ex);
		}
		ArrayList<CompMetaData> metadata = new ArrayList<CompMetaData>();

		Node rootel = doc.getDocumentElement();
		if (rootel.getNodeName().equalsIgnoreCase(CompXioTags.loadingApplication))
		{
			addLoadingApplication(metadata, rootel);
			return metadata;
		}
		else if (!rootel.getNodeName().equalsIgnoreCase(CompXioTags.compMetaData))
		{
			String s = "Wrong type of configuration file -- Cannot initialize. "
				+ "Root element is not '" + CompXioTags.compMetaData + "'.";
			throw new DbXmlException(s);
		}

		NodeList children = rootel.getChildNodes();
		for(int i=0; children != null && i<children.getLength(); i++)
		{
			Node node = children.item(i);
			String nn = node.getNodeName();
			if (node.getNodeType() == Node.ELEMENT_NODE)
			{
				if (nn.equalsIgnoreCase(CompXioTags.algorithm))
					addAlgorithm(metadata, node);
				else if (nn.equalsIgnoreCase(CompXioTags.computation))
					addComputation(metadata, node);
				else if (nn.equalsIgnoreCase(CompXioTags.loadingApplication))
					addLoadingApplication(metadata, node);
				else if (nn.equalsIgnoreCase(CompXioTags.tsGroup))
					addTsGroup(metadata, node);
			}
		}
		if (log.isTraceEnabled())
		{
			for (CompMetaData cmd: metadata)
			{
				log.trace("Loaded {}", cmd.typeString());
			}
		}
		return metadata;
	}

	/**
	 * Reads the specified file and returns a collection of the CompMetaData
	 * objects found therein.
	 * @param filename the file name
	 * @throws DbXmlException
	 */
	public ArrayList<CompMetaData> readFile(String filename) throws DbXmlException
	{
		log.info("CompXio.readFile({})", filename);
		this.filename = filename;
		File file = new File(filename);
		try (InputStream in = new FileInputStream(file))
		{
			return this.readStream(in);
		}
		catch(IOException ex)
		{
			throw new DbXmlException("Unable to process " + filename, ex);
		}
	}

	private void addAlgorithm(ArrayList<CompMetaData> metadata, Node node)
	{
		// Get number and host attributes.
		Element elem = (Element)node;
		String name = DomHelper.findAttr(elem, CompXioTags.name);
		if (name == null)
		{
			log.warn("{} Algorithm element without name attribute -- ignored.", filename);
			return;
		}
		DbCompAlgorithm algo = new DbCompAlgorithm(Constants.undefinedId,
			name, null, null);

		NodeList children = node.getChildNodes();
		for(int i=0; children != null && i<children.getLength(); i++)
		{
			Node childNode = children.item(i);
			if (childNode.getNodeType() == Node.ELEMENT_NODE)
			{
				String nn = childNode.getNodeName();
				if (nn.equalsIgnoreCase(CompXioTags.execClass))
					algo.setExecClass(DomHelper.getTextContent(childNode));
				else if (nn.equalsIgnoreCase(CompXioTags.comment))
					algo.setComment(
						TextUtil.collapseWhitespace(
							DomHelper.getTextContent(childNode)));
				else if (nn.equalsIgnoreCase(CompXioTags.algoProperty))
					setProperty(CompXioTags.algoProperty, algo,
						(Element)childNode);
				else if (nn.equalsIgnoreCase(CompXioTags.algoParm))
				{
					Element childElem = (Element)childNode;
					String roleName =
						DomHelper.findAttr(childElem, CompXioTags.roleName);
					if (roleName == null)
					{
						log.warn("Algorithm '{}' has an {} element with no {} attribute -- ignored.",
								 name, CompXioTags.algoParm, CompXioTags.roleName);
						continue;
					}
					Node pt = DomHelper.findNode(childElem, CompXioTags.parmType,
						Node.ELEMENT_NODE);
					if (pt == null)
					{
						log.warn("Algorithm '{}' has an {} element with no {} element -- ignored.",
								 name, CompXioTags.algoParm, CompXioTags.parmType);
						continue;
					}
					String pts = DomHelper.getTextContent(pt);
					algo.addParm(new DbAlgoParm(roleName, pts));
				}
				else if (nn.equalsIgnoreCase(CompXioTags.algoScript))
				{
					Element scriptElem = (Element)childNode;
					String scriptTypeStr = DomHelper.findAttr(scriptElem, CompXioTags.algoScriptType);
					if (scriptTypeStr == null || scriptTypeStr.trim().length() == 0)
					{
						log.warn("Algorithm '{}' has an {} element with no {} element -- ignored.",
								 name, CompXioTags.algoScript, CompXioTags.algoScriptType);
						continue;
					}
					scriptTypeStr = scriptTypeStr.trim();

					StringBuilder scriptBuilder = new StringBuilder();

					NodeList scriptLines = scriptElem.getChildNodes();
					for(int si=0; scriptLines != null && si<scriptLines.getLength(); si++)
					{
						Node scriptLine = scriptLines.item(si);
						if (scriptLine.getNodeName().equalsIgnoreCase(CompXioTags.algoScriptLine))
						{
							// Get indent attribute
							Element scriptLineElem = (Element)scriptLine;
							String indents = DomHelper.findAttr(scriptLineElem, CompXioTags.indent);
							int indent = -1;
							if (indents != null)
								try { indent = Integer.parseInt(indents.trim()); }
								catch(NumberFormatException ex) { indent = -1; }

							String line = scriptLine.getTextContent();
							if (indent != -1)
								line = line.trim();
							if (line != null)
							{
								// Remove any line breaks that xml encoding added.
								StringBuilder lineb = new StringBuilder(line);
								for(int lineidx = 0; lineidx < lineb.length(); )
								{
									char c = lineb.charAt(lineidx);
									if (c == '\n' || c == '\r')
										lineb.deleteCharAt(lineidx);
									else
										lineidx++;
								}
								while(indent-- > 0)
									lineb.insert(0, ' ');
								scriptBuilder.append(lineb);
								scriptBuilder.append("\n");
								// Add the indent
							}
							else
							{
								log.warn("Algorithm '{}' has an {} element with null content -- ignored.",
										 name, CompXioTags.algoScriptLine);
							}
						}
					}
					DbCompAlgorithmScript newScript = new DbCompAlgorithmScript(algo,
						ScriptType.fromDbChar(scriptTypeStr.charAt(0)));
					newScript.addToText(scriptBuilder.toString());
					algo.putScript(newScript);
				}
				else
				{
					log.warn("Unrecognized element '{}' ignored DRGS Config.", nn);
				}
			}
		}

		log.trace("Adding algorithm {}", name);
		metadata.add(algo);
	}

	private void addComputation(ArrayList<CompMetaData> metadata, Node node)
	{
		// Get number and host attributes.
		Element elem = (Element)node;
		String name = DomHelper.findAttr(elem, CompXioTags.name);
		if (name == null)
		{
			log.warn("{} Computation element without name attribute -- ignored.", filename);
			return;
		}
		DbComputation comp = new DbComputation(Constants.undefinedId, name);

		NodeList children = node.getChildNodes();
		for(int i=0; children != null && i<children.getLength(); i++)
		{
			Node childNode = children.item(i);
			if (childNode.getNodeType() == Node.ELEMENT_NODE)
			{
				String nn = childNode.getNodeName();
				if (nn.equalsIgnoreCase(CompXioTags.algorithmName))
				{
					String algoName = DomHelper.getTextContent(childNode);
					DbCompAlgorithm algo = findAlgorithm(metadata, algoName);
					if (algo != null)
						comp.setAlgorithm(algo);
					else
						comp.setAlgorithmName(algoName);
				}
				else if (nn.equalsIgnoreCase(CompXioTags.comment))
					comp.setComment(
						TextUtil.collapseWhitespace(
							DomHelper.getTextContent(childNode)));
				else if (nn.equalsIgnoreCase(CompXioTags.compProcName))
				{
					String appName = DomHelper.getTextContent(childNode);
					comp.setApplicationName(appName);
				}
				else if (nn.equalsIgnoreCase(CompXioTags.enabled))
					comp.setEnabled(DomHelper.getBooleanContent(childNode,
						true, "Computation:" + name));
				else if (nn.equalsIgnoreCase(CompXioTags.validStart))
					comp.setValidStart(DomHelper.getDateContent(childNode,
						"Computation:" + name));
				else if (nn.equalsIgnoreCase(CompXioTags.validEnd))
					comp.setValidEnd(DomHelper.getDateContent(childNode,
						"Computation:" + name));
				else if (nn.equalsIgnoreCase(CompXioTags.lastModified))
					comp.setLastModified(DomHelper.getDateContent(childNode,
						"Computation:" + name));
				else if (nn.equalsIgnoreCase(CompXioTags.compProperty))
					setProperty(CompXioTags.compProperty, comp,
						(Element)childNode);
				else if (nn.equalsIgnoreCase(CompXioTags.compParm))
					addCompParm(comp, (Element)childNode);
				else if (nn.equalsIgnoreCase(CompXioTags.groupName))
				{
					TsGroup g = new TsGroup();
					g.setGroupName(DomHelper.getTextContent(childNode));
					comp.setGroup(g);
				}
			}
		}
		log.trace("Adding computation {}", name);
		metadata.add(comp);
	}

	protected void addCompParm(DbComputation comp, Element elem)
	{
		// Role Name
		String roleName = DomHelper.findAttr(elem, CompXioTags.roleName);
		if (roleName == null)
		{
			log.warn("{} {} element without {} attribute -- ignored.",
					 filename, CompXioTags.compParm, CompXioTags.roleName);
			return;
		}

		// Interval
		Node node =
			DomHelper.findNode(elem, CompXioTags.interval, Node.ELEMENT_NODE);
		String interval = null;
		if (node != null)
			interval = DomHelper.getTextContent(node);

		// Table Selector
		node =
			DomHelper.findNode(elem, CompXioTags.tableSelector,
			Node.ELEMENT_NODE);
		String tableSelector = "Other";
		if (node != null)
			tableSelector = DomHelper.getTextContent(node);

		// Optional delta-T
		node = DomHelper.findNode(elem, CompXioTags.deltaT, Node.ELEMENT_NODE);
		int deltaT = 0;
		if (node != null)
		{
			deltaT = DomHelper.getIntegerContent(node, deltaT,
				module + ":" + filename);
		}
		// Delta T Units
		node = DomHelper.findNode(elem, CompXioTags.deltaTUnits, Node.ELEMENT_NODE);
		String deltaTUnits = null;
		if (node != null)
		{
			deltaTUnits = DomHelper.getTextContent(node);
		}

		// Optional Model ID
		node = DomHelper.findNode(elem, CompXioTags.modelId, Node.ELEMENT_NODE);
		int modelId = Constants.undefinedIntKey;
		if (node != null)
		{
			modelId = DomHelper.getIntegerContent(node, modelId,
				module + ":" + filename);
		}

		// Construct the parm object & add to the computation.
		DbCompParm compParm = new DbCompParm(roleName, Constants.undefinedId,
			interval, tableSelector, deltaT);
		compParm.setModelId(modelId);
		compParm.setDeltaTUnits(deltaTUnits);
		comp.addParm(compParm);

		node = DomHelper.findNode(elem, CompXioTags.groupName,
			Node.ELEMENT_NODE);
		if (node != null)
			setGroup(comp, DomHelper.getTextContent(node));

		// Find SiteDataType complex element.
		// Then walk the tree and parse all DataType and SiteName elements.
		node = DomHelper.findNode(elem, CompXioTags.siteDataType,
			Node.ELEMENT_NODE);
		if (node != null)
		{
			NodeList sndts = node.getChildNodes();
			for(int i=0; sndts != null && i<sndts.getLength(); i++)
			{
				Node cn = sndts.item(i);
				if (cn.getNodeType() == Node.ELEMENT_NODE)
				{
					Element ce = (Element)cn;
					String nn = cn.getNodeName();
					if (nn.equalsIgnoreCase(CompXioTags.siteName))
					{
						SiteName siteName = new SiteName(null,
							DomHelper.findAttr(ce, CompXioTags.nameType),
							DomHelper.getTextContent(cn));
						compParm.addSiteName(siteName);
					}
					else if (nn.equalsIgnoreCase(CompXioTags.dataType))
					{
						String std = DomHelper.findAttr(ce,
							CompXioTags.standard);
						String cod = DomHelper.findAttr(ce,
							CompXioTags.code);
						if (cod == null)
							cod = DomHelper.getTextContent(cn);

						DataType dataType = DataType.getDataType(std, cod);
						compParm.setDataType(dataType);
					}
					else
						log.trace("Unknown element '{}' in compparm", nn);
				}
			}
		}
		node = DomHelper.findNode(elem, CompXioTags.dataType,
			Node.ELEMENT_NODE);
		if (node != null)
		{
			Element ce = (Element)node;
			String std = DomHelper.findAttr(ce, CompXioTags.standard);
			String cod = DomHelper.getTextContent(ce);
			DataType dataType = DataType.getDataType(std, cod);
			compParm.setDataType(dataType);
			compParm.setDataTypeId(dataType.getId());
		}
	}

	/**
	 * @param metadata
	 * @param node
	 */
	private void addTsGroup(ArrayList<CompMetaData> metadata, Node node)
	{
		// Find the Group Name
		Element elem = (Element)node;
		String groupName = DomHelper.findAttr(elem, CompXioTags.name);
		if (groupName == null)
		{
			log.warn(module + "{} {} element without {} attribute -- ignored.",
					 filename, CompXioTags.tsGroup, CompXioTags.name);
			return;
		}

		// Declare the tsGrp object
		TsGroup tsGrp = new TsGroup();
		tsGrp.setGroupName(groupName);

		// Reset the parameters of the tsGrp
		String nodeName;
		Node childNode;
		NodeList children = elem.getChildNodes();
		for(int i=0; children != null && i<children.getLength(); i++)
		{
			childNode = children.item(i);
			if (childNode.getNodeType() == Node.ELEMENT_NODE) {
				nodeName = childNode.getNodeName();

				if (nodeName.equalsIgnoreCase(CompXioTags.groupType))
					tsGrp.setGroupType(DomHelper.getTextContent(childNode));
				else if (nodeName.equalsIgnoreCase(CompXioTags.description))
					tsGrp.setDescription(DomHelper.getTextContent(childNode));
				else if (nodeName.equalsIgnoreCase(CompXioTags.timeSeries))
				{
					String tsIdStr = DomHelper.getTextContent(childNode);
					if (tsIdStr != null)
					{
						tsGrp.addTsMemberID(tsIdStr);
					}
				}
				else if (nodeName.equalsIgnoreCase(CompXioTags.siteName))
				{
					String siteName = DomHelper.getTextContent(childNode);
					tsGrp.addSiteName(siteName);
				}
				else if (nodeName.equalsIgnoreCase(CompXioTags.dataType))
				{
					String dataTypeStd = DomHelper.findAttr((Element)childNode, CompXioTags.standard);
					String dataTypeCod = DomHelper.findAttr((Element)childNode, CompXioTags.code);
					if (dataTypeCod == null)
						dataTypeCod = DomHelper.getTextContent((Element)childNode);
					DataType dataType = DataType.getDataType(dataTypeStd, dataTypeCod);

					tsGrp.addDataTypeId(dataType.getId());
				}
				else if (nodeName.equalsIgnoreCase(CompXioTags.member))
				{
					String memberTyp = DomHelper.findAttr((Element)childNode, CompXioTags.type);
					String memberVal = DomHelper.findAttr((Element)childNode, CompXioTags.value);
					if (memberVal == null)
						memberVal = DomHelper.getTextContent((Element)childNode);
					tsGrp.addOtherMember(memberTyp, memberVal);
				}
				else if (nodeName.equalsIgnoreCase(CompXioTags.subGroup))
				{
					String combine = DomHelper.findAttr((Element)childNode, CompXioTags.combine);
					String include = DomHelper.findAttr((Element)childNode, CompXioTags.include);
					TsGroup tsSubgrp = new TsGroup();
					tsSubgrp.setGroupName(DomHelper.getTextContent(childNode));
					if ((combine != null && combine.equalsIgnoreCase(CompXioTags.add))
					 || (include != null && TextUtil.str2boolean(include)))
						tsGrp.addSubGroup(tsSubgrp, 'A');
					else if ((combine != null && combine.equalsIgnoreCase(CompXioTags.subtract))
						  || (include != null && !TextUtil.str2boolean(include)))
						tsGrp.addSubGroup(tsSubgrp, 'S');
					else if (combine != null && combine.equalsIgnoreCase(CompXioTags.intersect))
						tsGrp.addSubGroup(tsSubgrp, 'I');
				}
			}
		}

		//Add the TS group to the metadata
		metadata.add(tsGrp);
	}

	private void setGroup(DbComputation comp, String groupName)
	{
		TsGroupDAI groupDAO = theDb.makeTsGroupDAO();
		try
		{
			comp.setGroup(groupDAO.getTsGroupByName(groupName));
			if (comp.getGroup() == null)
			{
				log.warn("Unknown group '{}' in computation '{}'", groupName, comp.getName());
			}
		}
		catch (DbIoException ex)
		{
			log.atWarn().setCause(ex).log("Database IO Error reading group {}", groupName);
			comp.setGroup(null);
		}
		finally
		{
			groupDAO.close();
		}

	}

	private void addLoadingApplication(
		ArrayList<CompMetaData> metadata, Node node)
	{
		// Get name attribute.
		Element elem = (Element)node;
		String name = DomHelper.findAttr(elem, CompXioTags.name);
		if (name == null)
		{
			log.warn("{} {} element without name attribute -- ignored.", filename, CompXioTags.loadingApplication);
			return;
		}
		CompAppInfo info = new CompAppInfo(Constants.undefinedId);
		info.setAppName(name);
		Node commentNode = DomHelper.findNode(elem, CompXioTags.comment,
			Node.ELEMENT_NODE);
		if (commentNode != null)
			info.setComment(
				TextUtil.collapseWhitespace(
					DomHelper.getTextContent(commentNode)));

		NodeList children = node.getChildNodes();
		for(int i=0; children != null && i<children.getLength(); i++)
		{
			Node childNode = children.item(i);
			if (childNode.getNodeType() == Node.ELEMENT_NODE)
			{
				String nn = childNode.getNodeName();
				if (nn.equalsIgnoreCase(CompXioTags.appProperty))
					setProperty(CompXioTags.appProperty, info,
						(Element)childNode);
			}
		}

		metadata.add(info);
	}

	private void setProperty(String tag, HasProperties obj, Element elem)
	{
		String name = DomHelper.findAttr(elem, CompXioTags.name);
		if (name == null)
		{
			log.warn("{} {} without name attribute -- ignored.", filename, tag);
			return;
		}
		obj.setProperty(name, DomHelper.getTextContent(elem));
	}

	private DbCompAlgorithm findAlgorithm(ArrayList<CompMetaData> metadata,
		String algoName)
	{
		for(CompMetaData obj : metadata)
		{
			if (obj instanceof DbCompAlgorithm)
			{
				DbCompAlgorithm algo = (DbCompAlgorithm)obj;
				if (algoName.equalsIgnoreCase(algo.getName()))
					return algo;
			}
		}
		return null;
	}

	/**
	 * Creates or overwrites the specified file, writing all of the objects
	 * found in 'metadata'.
	 * @param metadata the computational meta-data objects to write.
	 * @param filename the file name to write to.
	 */
	public void writeFile(ArrayList<CompMetaData> metadata, String filename)
		throws IOException
	{
		// Open an output stream wrapped by an XmlOutputStream
		try (FileOutputStream fos = new FileOutputStream(filename))
		{	
			XmlOutputStream xos = new XmlOutputStream(fos, CompXioTags.compMetaData);
			xos.writeXmlHeader();

			xos.startElement(CompXioTags.compMetaData);
			for(CompMetaData mdObject : metadata)
			{
				if (mdObject instanceof DbComputation)
					writeComp(xos, (DbComputation)mdObject);
				else if (mdObject instanceof DbCompAlgorithm)
					writeAlgo(xos, (DbCompAlgorithm)mdObject);
				else if (mdObject instanceof CompAppInfo)
					writeApp(xos, (CompAppInfo)mdObject);
				else if (mdObject instanceof TsGroup)
					writeTsGrp(xos, (TsGroup)mdObject);
			}
			xos.endElement(CompXioTags.compMetaData);
		}
	}

	private void writeAlgo(XmlOutputStream xos, DbCompAlgorithm algo)
		throws IOException
	{
		xos.startElement(CompXioTags.algorithm,
			CompXioTags.name, algo.getName());

		String s = algo.getComment();
		if (s != null)
			xos.writeElement(CompXioTags.comment, s);

		s = algo.getExecClass();
		if (s != null)
			xos.writeElement(CompXioTags.execClass, s);

		for(Enumeration en = algo.getPropertyNames(); en.hasMoreElements(); )
		{
			String propname = (String)en.nextElement();
			xos.writeElement(CompXioTags.algoProperty,
				CompXioTags.name, propname, algo.getProperty(propname));
		}

		for(Iterator<DbAlgoParm> it = algo.getParms(); it.hasNext(); )
		{
			DbAlgoParm dap = it.next();
			xos.startElement(CompXioTags.algoParm,
				CompXioTags.roleName, dap.getRoleName());
			xos.writeElement(CompXioTags.parmType, dap.getParmType());
			xos.endElement(CompXioTags.algoParm);
		}

		for(ScriptType st : ScriptType.values())
		{
			DbCompAlgorithmScript script = algo.getScript(st);
			// Temporarily set width very wide to prevent script lines from wrapping
			// and inserting awkward spacing when read back in.
			int w = xos.width;
			xos.width = 200;
			if (script != null)
			{
				xos.startElement(CompXioTags.algoScript, CompXioTags.algoScriptType, ""+st.getDbChar());
				String scriptText = script.getText();
				BufferedReader bufReader = new BufferedReader(new StringReader(scriptText));
				String line = null;
				while((line = bufReader.readLine()) != null)
				{
					int indent = 0;
					for(int idx=0; idx<line.length(); idx++)
					{
						char c = line.charAt(idx);
						if (c == ' ')
							indent++;
						else if (c == '\t')
							indent += 4;
						else
							break;
					}
					line = line.trim();
					xos.writeElement(CompXioTags.algoScriptLine, CompXioTags.indent, ""+indent, line);
				}
				xos.endElement(CompXioTags.algoScript);
			}
			xos.width = w;
		}

		xos.endElement(CompXioTags.algorithm);
	}

	public void writeApp(XmlOutputStream xos, CompAppInfo appInfo)
		throws IOException
	{
		xos.startElement(CompXioTags.loadingApplication,
			CompXioTags.name, appInfo.getAppName());
		String s = appInfo.getComment();
		if (s != null)
			xos.writeElement(CompXioTags.comment, s);
		for(Enumeration en = appInfo.getPropertyNames(); en.hasMoreElements(); )
		{
			String nm = (String)en.nextElement();
			String v = appInfo.getProperty(nm);
			xos.writeElement(CompXioTags.appProperty, CompXioTags.name, nm, v);
		}

		xos.endElement(CompXioTags.loadingApplication);
	}

	private void writeComp(XmlOutputStream xos, DbComputation comp)
		throws IOException
	{
		xos.startElement(CompXioTags.computation,
			CompXioTags.name, comp.getName());

		String s = comp.getComment();
		if (s != null)
			xos.writeElement(CompXioTags.comment, s);

		xos.writeElement(CompXioTags.enabled, "" + comp.isEnabled());

		s = comp.getApplicationName();
		if (s != null)
			xos.writeElement(CompXioTags.compProcName, s);

		s = comp.getAlgorithmName();
		if (s != null)
			xos.writeElement(CompXioTags.algorithmName, s);

		Date d = comp.getLastModified();
		if (d != null)
			xos.writeElement(CompXioTags.lastModified,
				DomHelper.dateFormat.format(d));

		d = comp.getValidStart();
		if (d != null)
			xos.writeElement(CompXioTags.validStart,
				DomHelper.dateFormat.format(d));

		d = comp.getValidEnd();
		if (d != null)
			xos.writeElement(CompXioTags.validEnd,
				DomHelper.dateFormat.format(d));

		s = comp.getGroupName();
		if (s != null)
			xos.writeElement(CompXioTags.groupName, s);

		for(Enumeration en = comp.getPropertyNames(); en.hasMoreElements(); )
		{
			String propname = (String)en.nextElement();
			xos.writeElement(CompXioTags.compProperty,
				CompXioTags.name, propname, comp.getProperty(propname));
		}

		for(Iterator<DbCompParm> it = comp.getParms(); it.hasNext(); )
		{
			DbCompParm dcp = it.next();
			xos.startElement(CompXioTags.compParm,
				CompXioTags.roleName, dcp.getRoleName());

			xos.startElement(CompXioTags.siteDataType);
			for(SiteName sn : dcp.getSiteNames())
				xos.writeElement(CompXioTags.siteName,
					CompXioTags.nameType,  sn.getNameType(),
					sn.getNameValue());
			DataType dataType = dcp.getDataType();
			if (dataType != null)
			{
				xos.writeElement(CompXioTags.dataType,
					CompXioTags.standard, dataType.getStandard(),
					CompXioTags.code, dataType.getCode(), null);
			}
			else
			{
				log.warn("Role '{}' has no data type.", dcp.getRoleName());
			}
			xos.endElement(CompXioTags.siteDataType);

			s = dcp.getInterval();
			if (s != null)
				xos.writeElement(CompXioTags.interval, s);

			s = dcp.getTableSelector();
			if (s != null)
				xos.writeElement(CompXioTags.tableSelector, s);

			int dt = dcp.getDeltaT();
			xos.writeElement(CompXioTags.deltaT, "" + dt);
			String dtu = dcp.getDeltaTUnits();
			if (dtu != null && dtu.trim().length() > 0)
				xos.writeElement(CompXioTags.deltaTUnits, dtu);

			int mid = dcp.getModelId();
			if (mid != Constants.undefinedIntKey)
				xos.writeElement(CompXioTags.modelId, "" + mid);

			xos.endElement(CompXioTags.compParm);
		}

		xos.endElement(CompXioTags.computation);
	}

	/**
	 * Write the TsGroup block into XML file if it exists
	 *
	 * @param xos
	 * @param tsgrp
	 */
	private void writeTsGrp(XmlOutputStream xos, TsGroup tsgrp)
		throws IOException
	{
		if (tsgrp == null) { return; }

		//Write the group elements for each TS group
		xos.startElement(CompXioTags.tsGroup, CompXioTags.name, tsgrp.getGroupName());
		xos.writeElement(CompXioTags.groupType, tsgrp.getGroupType());
		xos.writeElement(CompXioTags.description, tsgrp.getDescription());

		for (TimeSeriesIdentifier tsId: tsgrp.getTsMemberList())
			xos.writeElement(CompXioTags.timeSeries, tsId.getUniqueString());
		for (DbKey i: tsgrp.getSiteIdList())
		{
			try
			{
			  xos.writeElement(CompXioTags.siteName, (theDb.getSiteById(i)).getUniqueName());
			}
			catch (Exception ex)
			{
				log.atError().setCause(ex).log("Unable to write Ts Group element.");
			}
		}
		DataType dataType;
		for (DbKey j: tsgrp.getDataTypeIdList())
		{
			dataType = DataType.getDataType(j);
			xos.writeElement(CompXioTags.dataType,
					CompXioTags.standard, dataType.getStandard(),
					CompXioTags.code, dataType.getCode(), null);
		}
		for (TsGroupMember mem: tsgrp.getOtherMembers())
		{
			xos.writeElement(CompXioTags.member,
		  		CompXioTags.type, mem.getMemberType(),
		  		CompXioTags.value, mem.getMemberValue(), null);
		}
		for (TsGroup tsGrpId: tsgrp.getIncludedSubGroups())
		  xos.writeElement(CompXioTags.subGroup,
		  		CompXioTags.combine, "add", tsGrpId.getGroupName());
		for (TsGroup tsGrpId: tsgrp.getExcludedSubGroups())
		  xos.writeElement(CompXioTags.subGroup,
		  		CompXioTags.combine, "subtract", tsGrpId.getGroupName());
		for (TsGroup tsGrpId: tsgrp.getIntersectedGroups())
			  xos.writeElement(CompXioTags.subGroup,
			  		CompXioTags.combine, "intersect", tsGrpId.getGroupName());
		xos.endElement(CompXioTags.tsGroup);
	}

	@Override
	public void characters(char[] ch, int start, int length)
		throws SAXException
	{
		if (!TextUtil.isAllWhitespace(new String(ch, start, length)))
			throw new SAXException(
				"No character data expected within LoadingApplication");
	}

	@Override
	public void startElement(XmlHierarchyParser hier, String namespaceURI,
		String localName, String qname, Attributes atts) throws SAXException
	{
		if (localName.equalsIgnoreCase(CompXioTags.comment))
			hier.pushObjectParser(new TaggedStringSetter(this, commentTag));
		else if (localName.equalsIgnoreCase(CompXioTags.appProperty))
		{
			propName = atts.getValue(CompXioTags.name);
			if (propName == null)
				throw new SAXException(CompXioTags.appProperty
					+ " without " + CompXioTags.name +" attribute");
			hier.pushObjectParser(new TaggedStringSetter(this, propertyTag));
		}
	}

	@Override
	public void endElement(XmlHierarchyParser hier, String namespaceURI,
		String localName, String qname) throws SAXException
	{
		if (localName.equalsIgnoreCase(CompXioTags.loadingApplication))
			hier.popObjectParser();
		else
			throw new SAXException(
				"Parse stack corrupted: got end tag for " + localName
				+ ", expected " + CompXioTags.loadingApplication);
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length)
		throws SAXException
	{
	}

	@Override
	public void set(int tag, String value) throws SAXException
	{
		switch(tag)
		{
		case propertyTag:
			workingObject.getProperties().setProperty(propName, value);
			break;
		case commentTag:
			workingObject.setComment(value);
			break;
		}
	}

	/**
	 * When parsing a decodes database image with embedded LoadingApplication elements,
	 * the caller must set a working object for the SAX methods above.
	 * @param workingObject
	 */
	public void setWorkingObject(CompAppInfo workingObject)
	{
		this.workingObject = workingObject;
	}
}
