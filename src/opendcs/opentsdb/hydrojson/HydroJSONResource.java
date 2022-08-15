package opendcs.opentsdb.hydrojson;

import ilex.util.IDateFormat;
import ilex.util.Location;
import ilex.util.Logger;
import ilex.util.StringPair;
import ilex.util.TextUtil;
import lrgs.common.NetworkListItem;

import java.io.LineNumberReader;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.*;

//import javax.json.stream.JsonGenerator;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jaxrs.annotation.JacksonFeatures;

import decodes.cwms.CwmsTsId;
import decodes.db.DbEnum;
import decodes.db.NetworkList;
import decodes.db.Site;
import decodes.db.SiteList;
import decodes.sql.DbKey;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DbIoException;
import decodes.tsdb.IntervalIncrement;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.util.PropertiesOwner;
import decodes.util.PropertySpec;
import decodes.util.TSUtil;
import opendcs.dai.AlgorithmDAI;
import opendcs.dai.SiteDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.opentsdb.hydrojson.beans.AlgorithmRef;
import opendcs.opentsdb.hydrojson.beans.ApiDecodedMessage;
import opendcs.opentsdb.hydrojson.beans.AppRef;
import opendcs.opentsdb.hydrojson.beans.Computation;
import opendcs.opentsdb.hydrojson.beans.ComputationRef;
import opendcs.opentsdb.hydrojson.beans.Credentials;
import opendcs.opentsdb.hydrojson.beans.DataSourceRef;
import opendcs.opentsdb.hydrojson.beans.DecodeRequest;
import opendcs.opentsdb.hydrojson.beans.DecodesAlgorithm;
import opendcs.opentsdb.hydrojson.beans.ApiConfigRef;
import opendcs.opentsdb.hydrojson.beans.ApiConfigScript;
import opendcs.opentsdb.hydrojson.beans.ApiConfigScriptSensor;
import opendcs.opentsdb.hydrojson.beans.DecodesDataSource;
import opendcs.opentsdb.hydrojson.beans.DecodesDataType;
import opendcs.opentsdb.hydrojson.beans.DecodesPlatform;
import opendcs.opentsdb.hydrojson.beans.ApiPlatformConfig;
import opendcs.opentsdb.hydrojson.beans.DecodesPresentationGroup;
import opendcs.opentsdb.hydrojson.beans.DecodesRouting;
import opendcs.opentsdb.hydrojson.beans.DecodesScheduleEntry;
import opendcs.opentsdb.hydrojson.beans.DecodesUnit;
import opendcs.opentsdb.hydrojson.beans.LoadingApp;
import opendcs.opentsdb.hydrojson.beans.ApiRawMessage;
import opendcs.opentsdb.hydrojson.beans.NetList;
import opendcs.opentsdb.hydrojson.beans.NetListItem;
import opendcs.opentsdb.hydrojson.beans.NetlistRef;
import opendcs.opentsdb.hydrojson.beans.PlatformRef;
import opendcs.opentsdb.hydrojson.beans.PresentationRef;
import opendcs.opentsdb.hydrojson.beans.RefList;
import opendcs.opentsdb.hydrojson.beans.RoutingRef;
import opendcs.opentsdb.hydrojson.beans.ScheduleEntryRef;
import opendcs.opentsdb.hydrojson.beans.SiteData;
import opendcs.opentsdb.hydrojson.beans.SiteFull;
import opendcs.opentsdb.hydrojson.beans.SiteRef;
import opendcs.opentsdb.hydrojson.beans.TestBean;
import opendcs.opentsdb.hydrojson.beans.TimeSeries;
import opendcs.opentsdb.hydrojson.dao.AlgorithmDaoWrapper;
import opendcs.opentsdb.hydrojson.dao.AppDAO;
import opendcs.opentsdb.hydrojson.dao.ComputationDaoWrapper;
import opendcs.opentsdb.hydrojson.dao.ConfigDAO;
import opendcs.opentsdb.hydrojson.dao.DataSourceDAO;
import opendcs.opentsdb.hydrojson.dao.NetlistDAO;
import opendcs.opentsdb.hydrojson.dao.PlatformDAO;
import opendcs.opentsdb.hydrojson.dao.PresentationDAO;
import opendcs.opentsdb.hydrojson.dao.RoutingDAO;
import opendcs.opentsdb.hydrojson.dao.TsDAO;
import opendcs.opentsdb.hydrojson.dao.UnitDAO;
import opendcs.opentsdb.hydrojson.errorhandling.WebAppException;
import opendcs.opentsdb.hydrojson.util.PythonDate;
import opendcs.opentsdb.hydrojson.util.Token;

@Path("/")
public class HydroJSONResource
{
	public static final String defaultDateFmt = "%Y-%m-%dT%H:%M:%S";
	private static final long MSEC_PER_DAY = 1000L * 3600 * 24;
	private static final long TOKEN_MAX_AGE = 1000L * 3600 * 3; // A session is only valid for 3 hrs.
	private static ArrayList<Token> activeTokens = new ArrayList<Token>();
	
	public HydroJSONResource()
		throws DbIoException
	{
	}
	
	@GET
	@Path("tscatalog")
	@Produces(MediaType.APPLICATION_JSON)
	public HashMap<String, SiteData> getTsCatalog(@QueryParam("site") String site,
		@QueryParam("param") String param,
		@QueryParam("interval") String interval,
		@QueryParam("active") String active,
		@QueryParam("token") String token,
		@QueryParam("tsid") String tsid
		)
		throws WebAppException
	{
		System.out.println("tscatalog site='" + site + "', param='" + param 
			+ "', interval=" + interval + ", active=" + active + ", token=" + token
			+ ", tsid=");

		getToken(token);
		
		// As opposed to getjson method, default date format and UTC are always used.
		HashMap<String, SiteData> ret = new HashMap<String, SiteData>();
		SimpleDateFormat sdf = new SimpleDateFormat(PythonDate.pyFmt2sdf(defaultDateFmt));
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

		doTsCatalog(ret, site, sdf, param, interval, active, false, null, null, tsid);
		return ret;
	}
	
	@GET
	@Path("tsdata")
	@Produces(MediaType.APPLICATION_JSON)
	public HashMap<String, SiteData> getTsData(
		@QueryParam("tsid") String tsid,
		@QueryParam("site") String site,
		@QueryParam("param") String param,
		@QueryParam("interval") String interval,
		@QueryParam("active") String active,
		@QueryParam("since") String since,
		@QueryParam("until") String until,
		@QueryParam("token") String token
		)
		throws WebAppException
	{
		System.out.println("tsdata tsid='" + tsid + "', site='" + site + "', param='" + param 
			+ "', interval=" + interval + ", active=" + active + ", token=" + token);

		getToken(token);
		
		// As opposed to getjson method, default date format and UTC are always used.
		HashMap<String, SiteData> ret = new HashMap<String, SiteData>();
		SimpleDateFormat sdf = new SimpleDateFormat(PythonDate.pyFmt2sdf(defaultDateFmt));
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

		doTsCatalog(ret, site, sdf, param, interval, active, true, since, until, tsid);
		return ret;
	}
	

	
	/**
	 * Retrieve and update lastUsed date on a cached token.
	 * @param tokstr the token string
	 * @return The token if the passed tokstr is not null and a token exists, null otherwise.
	 */
	private Token getToken(String tokstr)
	{
		if (tokstr == null)
			return null;

		// Search for requested token, and cull the list of expired tokens.
		Token ret = null;
		long now = System.currentTimeMillis();
		for(Iterator<Token> tokit = activeTokens.iterator(); tokit.hasNext(); )
		{
			Token t = tokit.next();
			if (now - t.getLastUsed() > TOKEN_MAX_AGE)
				tokit.remove();
			else if (tokstr.equals(t.getToken()))
				ret = t;
		}
		if (ret == null)
			return null;
		
		ret.touch();
		return ret;
	}
	
	@GET
	@Path("getjson")
	@Produces(MediaType.APPLICATION_JSON)
	public HashMap<String, SiteData> getJsonQuery(@QueryParam("query") String query,
		@QueryParam("backward") String backward,
		@QueryParam("time_format") String time_format,
		@QueryParam("timeseries") String timeseries,
		@QueryParam("tscatalog") String tscatalog,
		@QueryParam("catalog") String catalog,
		@QueryParam("tz") String tz,
		@QueryParam("token") String token
		) 
		throws WebAppException
	{
		System.out.println("getjson query='" + query + "', backward='" + backward 
			+ "', time_format='" + time_format + "', tscatalog='" + tscatalog + "', catalog='"
			+ catalog + "', tz='" + tz + "'");
		
		getToken(token);
		
		// Build SimpleDateFormat for all date/times embedded in the JSON output.
		if (time_format == null || time_format.trim().length() == 0)
			time_format = defaultDateFmt;
		SimpleDateFormat sdf = new SimpleDateFormat(PythonDate.pyFmt2sdf(time_format));
		if (tz == null || tz.trim().length() == 0)
			tz = "UTC";
		sdf.setTimeZone(TimeZone.getTimeZone(tz));
		
		HashMap<String, SiteData> siteDataMap = new HashMap<String, SiteData>();
		
		if (catalog != null)
			doCatalog(siteDataMap, catalog);
		else if (tscatalog != null)
			doTsCatalog(siteDataMap, tscatalog, sdf, null, null, null, false, null, null, null);
		else if (timeseries != null)
			doTimeSeries(siteDataMap, timeseries, backward, sdf);
		else if (query != null)
			doGeneralQuery(siteDataMap, query, backward, sdf);

		return siteDataMap;
	}
	
	private static TestBean testBean = new TestBean();
	
	@GET
	@Path("testbean")
	@Produces(MediaType.APPLICATION_JSON)
	public TestBean getTestBean()
		throws WebAppException
	{
		System.out.println("GET testbean called.");
		return testBean;
	}
	
	@POST
	@Path("testbean")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postTestBean(TestBean tb)
		throws WebAppException
	{
		System.out.println("POST testbean(" + tb.getName() + ", " + tb.getValue() + ")");
		
		testBean.setName(tb.getName());
		testBean.setValue(tb.getValue());
		
		return Response.status(HttpServletResponse.SC_OK).entity(testBean).build();
	}

	@GET
	@Path("reflists")
	@Produces(MediaType.APPLICATION_JSON)
    @JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public HashMap<String, RefList> getRefLists(
		@QueryParam("name") String listNames,
		@QueryParam("token") String token
		)
		throws WebAppException
	{
		getToken(token);
		
//		Map<String, Object> map = new HashMap<>();
//	         map.put(JsonGenerator.PRETTY_PRINTING, true);
	 
		HashMap<String, RefList> ret = new HashMap<String, RefList>();
		TsdbInterface dbi = null;
		
		ArrayList<String> searches = getSearchTerms(listNames);

		try
		{
			dbi = new TsdbInterface();
			for(DbEnum dbEnum : decodes.db.Database.getDb().enumList.getEnumList())
			{
				if (searches.size() > 0)
				{
					boolean found = false;
					for(String search : searches)
						if (dbEnum.enumName.equalsIgnoreCase(search))
						{
							found = true;
							break;
						}
					if (!found)
						continue;
				}
System.out.println("Adding enum '" + dbEnum.enumName);
				ret.put(dbEnum.enumName, new RefList(dbEnum));
			}
		}
		catch(Exception ex)
		{
			String msg = "Error in getRefLists(" + listNames + "): " + ex;
			System.err.println(msg);
			ex.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR, msg);
		}
		finally
		{
			dbi.close();
		}
System.out.println("getRefLists returning " + ret.size() + " objects.");
		
		return ret;
	}
	
	
	
	@GET
	@Path("netlistrefs")
	@Produces(MediaType.APPLICATION_JSON)
    @JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public List<NetlistRef> getNetlistRefs(
		@QueryParam("token") String token,
		@QueryParam("tmtype") String tmtype
		)
		throws WebAppException
	{
		getToken(token);
		
System.out.println("getNetlistRefs tmtype=" + tmtype);
		try (
			TsdbInterface dbi = new TsdbInterface(); 
			NetlistDAO netlistDAO = new NetlistDAO(dbi.getTheDb()))
		{
			return netlistDAO.readNetlistRefs(tmtype);
		}
		catch (Exception e)
		{
			// Auto-generated catch block
			e.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR, "Internal exception:" + e);
		}
	}

	
	
	@GET
	@Path("netlist")
	@Produces(MediaType.APPLICATION_JSON)
    @JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public NetList getNetList(
		@QueryParam("netlistid") Long netlistId,
		@QueryParam("token") String token
		)
		throws WebAppException
	{
		getToken(token);
		
		if (netlistId == null)
			throw new WebAppException(ErrorCodes.MISSING_ID, "Missing required netlistid parameter.");
		
System.out.println("getNetList netlistid=" + netlistId);
		
		try (
			TsdbInterface dbi = new TsdbInterface(); 
			NetlistDAO netlistDAO = new NetlistDAO(dbi.getTheDb()))
		{
			NetList ret = netlistDAO.readNetworkList(netlistId);
			if (ret == null)
				throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT, 
					"No such network list with id=" + netlistId + ".");
			return ret;
		}
		catch (DbIoException e)
		{
			// Auto-generated catch block
			e.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR, "Internal exception:" + e);
		}
	}

	@POST
	@Path("netlist")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public NetList  postNetlist(@QueryParam("token") String token, NetList netList)
		throws WebAppException
	{
		System.out.println(
			"post netlist received netlist " + netList.getName() 
			+ " with tm type " + netList.getTransportMediumType() + " containing "
			+ netList.getItems().size() + " TMs, token=" + token);
		
		if (getToken(token) == null)
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		// Use username and password to attempt to connect to the database
		try (TsdbInterface dbi = new TsdbInterface();
			NetlistDAO netlistDAO = new NetlistDAO(dbi.getTheDb()))
		{
			netlistDAO.checkNameClash(netList);
			
			// Convert the list to a decodes netlist object.
			NetworkList decodesNL = netList.toNetworkList();
			
			// If we have an existing list, use it's ID before writing.
			for(NetworkList nl : decodes.db.Database.getDb().networkListList.getList())
				if (nl.name.equalsIgnoreCase(netList.getName()))
				{
					decodesNL.forceSetId(nl.getId());
					decodes.db.Database.getDb().networkListList.remove(nl);
					break;
				}
			
			// Now write the list to DECODES.
			decodes.db.Database.getDb().networkListList.add(decodesNL);
			decodesNL.write();
			netList.setLastModifyTime(decodesNL.lastModifyTime);

			return netList;
		}
		catch(WebAppException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR,
				"Internal DbIoException: " + e);
		}
	}
	
	@DELETE
	@Path("netlist")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteNetlist(
		@QueryParam("token") String token, 
		@QueryParam("netlistid") Long netlistId)
		throws WebAppException
	{
		System.out.println(
			"DELETE netlist received netlistid=" + netlistId
			+ ", token=" + token);
		
		if (getToken(token) == null)
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		// Use username and password to attempt to connect to the database
		try (TsdbInterface dbi = new TsdbInterface();
			NetlistDAO netlistDAO = new NetlistDAO(dbi.getTheDb()))
		{
			String errmsg = netlistDAO.netlistUsedByRs(netlistId);
			if (errmsg != null)
				return Response.status(ErrorCodes.NOT_ALLOWED).entity(
					" Cannot delete network list with ID " + netlistId 
					+ " because it is used by the following routing specs: "
					+ errmsg).build();

			netlistDAO.deleteNetlist(netlistId);
			return Response.status(HttpServletResponse.SC_OK).entity(
				"ID " + netlistId + " deleted").build();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return Response.status(ErrorCodes.DATABASE_ERROR).entity(
				"Error deleting netlistid " + netlistId + ": " + e).build();
		}
	}

	
	
	
	@POST
	@Path("cnvtnl")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	public NetList  cnvtNL(
		@QueryParam("token") String token,
		String nldata
		)
		throws WebAppException
	{
		System.out.println("cnvtnl nldata='" + nldata + "'");
		
		getToken(token);
		
		NetList ret = new NetList();
		
   		LineNumberReader rdr = new LineNumberReader(new StringReader(nldata));
   	   	String ln = null;
   	   	try
   	   	{
	   	   	while( (ln = rdr.readLine()) != null)
	   	   	{
				if (ln.length() <= 0
	   			 || ln.charAt(0) == '#') // skip comment lines.
	   				continue;
	   			else
	   			{
	   				NetworkListItem nli = new NetworkListItem(ln);
   					ret.getItems().put(nli.addr.toString(),
   						new NetListItem(nli.addr.toString(), nli.name, nli.description));
	   			}
	   		}
   	   	}
		catch(Exception ex)
		{
			String msg = 
				"NL File Parsing Failed on line " + rdr.getLineNumber() + ": " + ex
				+ (ln == null ? "" : (" -- " + ln));
			throw new WebAppException(HttpServletResponse.SC_NOT_ACCEPTABLE, msg);
		}
	
		try { rdr.close(); } catch (Exception e) {}

		return ret;
	}
	
	

	
	
	@GET
	@Path("check")
	@Produces(MediaType.APPLICATION_JSON)
	public Response checkToken(@QueryParam("token") String token)
		throws WebAppException
	{
		if (token == null)
			throw new WebAppException(HttpServletResponse.SC_NOT_ACCEPTABLE, 
				"Missing required token argument.");

		Token ret = getToken(token);
		if (ret == null)
			throw new WebAppException(HttpServletResponse.SC_GONE, 
					"Token '" + token + "' does not exist.");
		return Response.status(HttpServletResponse.SC_OK).entity(ret).build();
	}

	
	@POST
	@Path("credentials")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postCredentials(Credentials credentials)
		throws WebAppException
	{
		System.out.println(
			"Received credentials(" + credentials.getUsername() + ", " + credentials.getPassword()+")");
		String u = credentials.getUsername();
		String p = credentials.getPassword();
		if (u == null || u.trim().length() == 0 || p == null || p.trim().length() == 0)
		{
			throw new WebAppException(HttpServletResponse.SC_NOT_ACCEPTABLE, 
				"Neither username nor password may be null.");
//			return Response.status(500).entity("Neither username nor password may be null.").build();
		}
		
		// Use username and password to attempt to connect to the database
		TsdbInterface dbi = null;
		try
		{
			dbi = new TsdbInterface();
			if (dbi.isUserValid(u, p))
			{
				Random rand = new Random(System.currentTimeMillis());
				String tokstr = Long.toHexString(rand.nextLong());
				Token ret = new Token(tokstr, credentials.getUsername());
				activeTokens.add(ret);
System.out.println("Added new token for user '" + credentials.getUsername() + "'=" + tokstr);
				return Response.status(HttpServletResponse.SC_OK).entity(ret).build();
			}
			else
			{
System.out.println("Returning 401: " + dbi.getReason());
				return Response.status(401).entity(dbi.getReason()).build();
			}
		}
		catch (DbIoException e)
		{
			e.printStackTrace();
			return Response.status(ErrorCodes.DATABASE_ERROR).entity("Internal DbIoException: " + e).build();
		}
		finally
		{
			dbi.close();
		}
	}
	
	@GET
	@Path("platformrefs")
	@Produces(MediaType.APPLICATION_JSON)
    @JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public HashMap<String, PlatformRef> gePlatformRefs(
		@QueryParam("token") String token,
		@QueryParam("tmtype") String tmtype
		)
		throws WebAppException
	{
		getToken(token);
		
		HashMap<String, PlatformRef> ret = new HashMap<String, PlatformRef>();
		
System.out.println("getPlatforms, tmtype=" + tmtype);
		try (TsdbInterface dbi = new TsdbInterface();
			PlatformDAO platformDAO = new PlatformDAO(dbi.getTheDb()))
		{
			ArrayList<PlatformRef> platSpecs = platformDAO.readPlatformSpecs(tmtype);
			for(PlatformRef ps : platSpecs)
				ret.put(ps.getName(), ps);
		}
		catch (DbIoException e)
		{
			e.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR, "Internal exception:" + e);
		}
		
System.out.println("getPlatforms returning " + ret.size() + " objects.");
		
		return ret;
	}
	
	@GET
	@Path("platform")
	@Produces(MediaType.APPLICATION_JSON)
    @JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public DecodesPlatform getPlatform(
		@QueryParam("platformid") Long platformId,
		@QueryParam("token") String token
		)
		throws WebAppException
	{
		getToken(token);
		
		if (platformId == null)
			throw new WebAppException(ErrorCodes.MISSING_ID, 
				"Missing required platformid parameter.");
		
System.out.println("getPlatform id=" + platformId);
		try (TsdbInterface dbi = new TsdbInterface();
			PlatformDAO dao = new PlatformDAO(dbi.getTheDb()))
		{
			return dao.readPlatform(platformId);
		}
		catch (DbIoException e)
		{
			e.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR, "Internal exception:" + e);
		}
	}
	
	@POST
	@Path("platform")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public DecodesPlatform postPlatform(@QueryParam("token") String token, 
		DecodesPlatform platform)
		throws WebAppException
	{
		System.out.println(
			"post platform received platformId=" + platform.getPlatformId());
Logger.instance().setMinLogPriority(Logger.E_DEBUG3);
		
		if (getToken(token) == null)
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		try (TsdbInterface dbi = new TsdbInterface();
			PlatformDAO dao = new PlatformDAO(dbi.getTheDb()))
		{
			dao.writePlatform(platform);
Logger.instance().setMinLogPriority(Logger.E_INFORMATION);
			return platform;
		}
		catch(WebAppException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			e.printStackTrace();
Logger.instance().setMinLogPriority(Logger.E_INFORMATION);
			throw new WebAppException(ErrorCodes.DATABASE_ERROR,
				"Internal DbIoException: " + e);
		}
	}
	
	@DELETE
	@Path("platform")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response deletePlatform(
		@QueryParam("token") String token, 
		@QueryParam("platformid") Long platformId)
		throws WebAppException
	{
		System.out.println(
			"DELETE platform received id=" + platformId
			+ ", token=" + token);
		
		if (getToken(token) == null)
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		// Use username and password to attempt to connect to the database
		try (TsdbInterface dbi = new TsdbInterface();
			PlatformDAO platDao = new PlatformDAO(dbi.getTheDb()))
		{
			platDao.deletePlatform(platformId);
			return Response.status(HttpServletResponse.SC_OK).entity(
				"Platform with ID " + platformId + " deleted").build();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return Response.status(ErrorCodes.DATABASE_ERROR).entity(
				"Error deleting platform id " + platformId + ": " + e).build();
		}
	}



	@GET
	@Path("datasourcerefs")
	@Produces(MediaType.APPLICATION_JSON)
    @JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public HashMap<String, DataSourceRef> getDataSourceRefs(
		@QueryParam("token") String token
		)
		throws WebAppException
	{
		getToken(token);
		
		HashMap<String, DataSourceRef> ret = new HashMap<String, DataSourceRef>();
		
System.out.println("getDataSourceRefs");
		try (TsdbInterface dbi = new TsdbInterface();
			DataSourceDAO dao = new DataSourceDAO(dbi.getTheDb()))
		{
			ArrayList<DataSourceRef> refs = dao.readDataSourceRefs();

			for(DataSourceRef ref : refs)
				ret.put(ref.getName(), ref);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR, "Internal exception:" + e);
		}
		
System.out.println("getDataSourceRefs returning " + ret.size() + " objects.");
		
		return ret;
	}

	@GET
	@Path("datasource")
	@Produces(MediaType.APPLICATION_JSON)
    @JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public DecodesDataSource geDataSource(
		@QueryParam("datasourceid") Long dataSourceId,
		@QueryParam("token") String token
		)
		throws WebAppException
	{
		if (getToken(token) == null)
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		if (dataSourceId == null)
			throw new WebAppException(ErrorCodes.MISSING_ID, 
				"Missing required datasourceid parameter.");
		
System.out.println("getDataSource id=" + dataSourceId);
		try (TsdbInterface dbi = new TsdbInterface();
			DataSourceDAO dao = new DataSourceDAO(dbi.getTheDb()))
		{
			DecodesDataSource ret = dao.readDataSource(dataSourceId);
			if (ret == null)
				throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT, 
					"No such DECODES data source with id=" + dataSourceId + ".");
			return ret;
		}
		catch (DbIoException e)
		{
			e.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR, "Internal exception:" + e);
		}
	}
	
	@POST
	@Path("datasource")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public DecodesDataSource postDatasource(@QueryParam("token") String token, 
			DecodesDataSource datasource)
		throws WebAppException
	{
		System.out.println(
			"post datasource received datasource " + datasource.getName() 
			+ " with ID=" + datasource.getDataSourceId());
		
		if (getToken(token) == null)
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		try (TsdbInterface dbi = new TsdbInterface();
			DataSourceDAO dsDao = new DataSourceDAO(dbi.getTheDb()))
		{
			dsDao.writedDataSource(datasource);
			return datasource;
		}
		catch(WebAppException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR,
				"Internal DbIoException: " + e);
		}
	}

	@DELETE
	@Path("datasource")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteDatasource(
		@QueryParam("token") String token, 
		@QueryParam("datasourceid") Long datasourceId)
		throws WebAppException
	{
		System.out.println(
			"DELETE datasource received datasourceid=" + datasourceId
			+ ", token=" + token);
		
		if (getToken(token) == null)
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		// Use username and password to attempt to connect to the database
		try (TsdbInterface dbi = new TsdbInterface();
			DataSourceDAO dsDao = new DataSourceDAO(dbi.getTheDb()))
		{
			String errmsg = dsDao.datasourceUsedByRs(datasourceId);
			if (errmsg != null)
				return Response.status(ErrorCodes.NOT_ALLOWED).entity(
					" Cannot delete datasource with ID " + datasourceId 
					+ " because it is used by the following routing specs: "
					+ errmsg).build();

			dsDao.deleteDatasource(datasourceId);
			return Response.status(HttpServletResponse.SC_OK).entity(
				"Datasource with ID " + datasourceId + " deleted").build();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return Response.status(ErrorCodes.DATABASE_ERROR).entity(
				"Error deleting datasourceid " + datasourceId + ": " + e).build();
		}
	}

	@GET
	@Path("siterefs")
	@Produces(MediaType.APPLICATION_JSON)
    @JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public ArrayList<SiteRef> geSiteRefs(
		@QueryParam("token") String token
		)
		throws WebAppException
	{
		getToken(token);
		
		ArrayList<SiteRef> ret = new ArrayList<SiteRef>();
		
System.out.println("getSiteRefs");
		try (TsdbInterface dbi = new TsdbInterface();
			SiteDAI dao = dbi.getTheDb().makeSiteDAO())
		{
			SiteList siteList = new SiteList();
			dao.read(siteList);
			for(Iterator<Site> si = siteList.iterator(); si.hasNext(); )
				ret.add(SiteRef.fromSite(si.next()));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR, "Internal exception:" + e);
		}
		
System.out.println("getSiteRefs returning " + ret.size() + " objects.");
		
		return ret;
	}

	@GET
	@Path("site")
	@Produces(MediaType.APPLICATION_JSON)
    @JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public SiteFull geSiteFull(
		@QueryParam("token") String token,
		@QueryParam("siteid") Long siteId
		)
		throws WebAppException
	{
		getToken(token);
		
		if (siteId == null)
			throw new WebAppException(ErrorCodes.MISSING_ID, 
				"Missing required siteid parameter.");

System.out.println("getSite, id=" + siteId);
		try (TsdbInterface dbi = new TsdbInterface();
			SiteDAI dao = dbi.getTheDb().makeSiteDAO())
		{
			Site site = new Site();
			site.forceSetId(DbKey.createDbKey(siteId));
			dao.readSite(site);
			return SiteFull.fromSite(site);
		}
		catch(NoSuchObjectException ex)
		{
			throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT,
				"No such site with siteid=" + siteId);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR, "Internal exception:" + e);
		}
	}

	@POST
	@Path("site")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public SiteFull postSite(@QueryParam("token") String token, SiteFull sf)
		throws WebAppException
	{
		System.out.println(
			"POST site received site id=" + sf.getSiteId()
			+ ", token=" + token);
		
		if (getToken(token) == null)
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		// Use username and password to attempt to connect to the database
		try (TsdbInterface dbi = new TsdbInterface();
			SiteDAI dao = dbi.getTheDb().makeSiteDAO())
		{
			Site decodesSite = sf.toSite();
			dao.writeSite(decodesSite);
			
			sf.setSiteId(decodesSite.getId().getValue());
			sf.setLastModified(decodesSite.getLastModifyTime());
				
			return sf;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR,
				"Internal DbIoException: " + e);
		}
	}

	@DELETE
	@Path("site")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteSite(
		@QueryParam("token") String token, 
		@QueryParam("siteid") Long siteId)
		throws WebAppException
	{
		System.out.println(
			"DELETE site received site id=" + siteId
			+ ", token=" + token);
		
		if (getToken(token) == null)
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		// Use username and password to attempt to connect to the database
		try (TsdbInterface dbi = new TsdbInterface();
			SiteDAI siteDAO = dbi.getTheDb().makeSiteDAO();
			PlatformDAO platformDAO = new PlatformDAO(dbi.getTheDb()))
		{
			DbKey siteKey = DbKey.createDbKey(siteId);
			if (platformDAO.isSiteUsed(siteKey))
				return Response.status(ErrorCodes.NOT_ALLOWED).entity(
					" Cannot delete site with ID " + siteId 
					+ " because it is used by one or more platforms.").build();
			siteDAO.deleteSite(siteKey);
			return Response.status(HttpServletResponse.SC_OK).entity(
				"ID " + siteId + " deleted").build();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return Response.status(ErrorCodes.DATABASE_ERROR).entity(
				"Error deleting siteid " + siteId + ": " + e).build();
		}
	}
	
	@GET
	@Path("configrefs")
	@Produces(MediaType.APPLICATION_JSON)
    @JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public ArrayList<ApiConfigRef> getConfigRefs(@QueryParam("token") String token)
		throws WebAppException
	{
		getToken(token);
		
System.out.println("geConfigRefs");
		try (TsdbInterface dbi = new TsdbInterface();
			ConfigDAO configDAO = new ConfigDAO(dbi.getTheDb()))
		{
			return configDAO.getConfigRefs();
		}
		catch (DbIoException e)
		{
			e.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR, "Internal exception:" + e);
		}
	}

	@GET
	@Path("config")
	@Produces(MediaType.APPLICATION_JSON)
    @JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public ApiPlatformConfig getConfig(
		@QueryParam("configid") Long configId,
		@QueryParam("token") String token
		)
		throws WebAppException
	{
		getToken(token);
		
		if (configId == null)
			throw new WebAppException(ErrorCodes.MISSING_ID, 
				"Missing required configid parameter.");
		
System.out.println("getConfig id=" + configId);
		try (TsdbInterface dbi = new TsdbInterface();
			ConfigDAO configDAO = new ConfigDAO(dbi.getTheDb()))
		{
			return configDAO.getConfig(configId);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR, "Internal exception:" + e);
		}
	}

	@POST
	@Path("config")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public ApiPlatformConfig postConfig(@QueryParam("token") String token, 
			ApiPlatformConfig config)
		throws WebAppException
	{
		System.out.println("post config received config " + config.getName() 
			+ " with ID=" + config.getConfigId());
		
		if (getToken(token) == null)
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
System.out.println("POST config script sensors: ");
for(ApiConfigScript acs : config.getScripts())
{
	System.out.println("\tscript " + acs.getName());
	for(ApiConfigScriptSensor acss : acs.getScriptSensors())
	{
		System.out.println("\t\t" + acss.prettyPrint());
	}
}
		try (TsdbInterface dbi = new TsdbInterface();
			ConfigDAO dao = new ConfigDAO(dbi.getTheDb()))
		{
			dao.writeConfig(config);
			return config;
		}
		catch(WebAppException ex)
		{
			throw ex;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR,
				"Internal DbIoException: " + e);
		}
	}
	
	@DELETE
	@Path("config")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteConfig(
		@QueryParam("token") String token, 
		@QueryParam("configid") Long configId)
		throws WebAppException
	{
		System.out.println(
			"DELETE config received configid=" + configId
			+ ", token=" + token);
		
		if (getToken(token) == null)
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		// Use username and password to attempt to connect to the database
		try (TsdbInterface dbi = new TsdbInterface();
			ConfigDAO cfgDao = new ConfigDAO(dbi.getTheDb());
			PlatformDAO platDao = new PlatformDAO(dbi.getTheDb()))
		{
			if (platDao.isConfigUsed(configId))
				return Response.status(ErrorCodes.NOT_ALLOWED).entity(
					" Cannot delete config with ID " + configId 
					+ " because it is used by one or more platforms.").build();
				
			cfgDao.deleteConfig(configId);
			return Response.status(HttpServletResponse.SC_OK).entity(
				"Config with ID " + configId + " deleted").build();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return Response.status(ErrorCodes.DATABASE_ERROR).entity(
				"Error deleting config id " + configId + ": " + e).build();
		}
	}

	@GET
	@Path("presentationrefs")
	@Produces(MediaType.APPLICATION_JSON)
    @JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public ArrayList<PresentationRef> getPresentationRefs(@QueryParam("token") String token)
		throws WebAppException
	{
		getToken(token);
		
System.out.println("getPresentationRefs");
		try (TsdbInterface dbi = new TsdbInterface();
			PresentationDAO dao = new PresentationDAO(dbi.getTheDb()))
		{
			return dao.getPresentationRefs();
		}
		catch (DbIoException e)
		{
			e.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR, "Internal exception:" + e);
		}
	}

	@GET
	@Path("presentation")
	@Produces(MediaType.APPLICATION_JSON)
    @JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public DecodesPresentationGroup getPresentation(
		@QueryParam("groupid") Long groupId,
		@QueryParam("token") String token
		)
		throws WebAppException
	{
		getToken(token);
		
		if (groupId == null)
			throw new WebAppException(ErrorCodes.MISSING_ID, 
				"Missing required groupid parameter.");
		
System.out.println("getPresentation id=" + groupId);
		try (TsdbInterface dbi = new TsdbInterface();
			PresentationDAO dao = new PresentationDAO(dbi.getTheDb()))
		{
			return dao.getPresentation(groupId);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR, "Internal exception:" + e);
		}
	}

	@POST
	@Path("presentation")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public DecodesPresentationGroup postPresentation(@QueryParam("token") String token, 
			DecodesPresentationGroup presGrp)
		throws WebAppException
	{
		System.out.println("post presentation received presentation " + presGrp.getName() 
			+ " with ID=" + presGrp.getGroupId());
		
		if (getToken(token) == null)
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		try (TsdbInterface dbi = new TsdbInterface();
			PresentationDAO dao = new PresentationDAO(dbi.getTheDb()))
		{
			dao.writePresentation(presGrp);
			return presGrp;
		}
		catch (DbIoException e)
		{
			e.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR,
				"Internal DbIoException: " + e);
		}
	}

	@DELETE
	@Path("presentation")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response deletePresentation(
		@QueryParam("token") String token, 
		@QueryParam("groupid") Long groupId)
		throws WebAppException
	{
		System.out.println(
			"DELETE presentation received groupid=" + groupId
			+ ", token=" + token);
		
		if (getToken(token) == null)
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		// Use username and password to attempt to connect to the database
		try (TsdbInterface dbi = new TsdbInterface();
			PresentationDAO dao = new PresentationDAO(dbi.getTheDb()))
		{
			String s = dao.routSpecsUsing(groupId);
			if (s != null)
				return Response.status(ErrorCodes.NOT_ALLOWED).entity(
					"Cannot delete presentation group " + groupId 
					+ " because it is used by the following routing specs: " 
					+ s).build();

			dao.deletePresentation(groupId);
			return Response.status(HttpServletResponse.SC_OK).entity(
				"Presentation Group with ID " + groupId + " deleted").build();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return Response.status(ErrorCodes.DATABASE_ERROR).entity(
				"Error deleting Presentation Group id=" + groupId + ": " + e).build();
		}
	}

	@GET
	@Path("routingrefs")
	@Produces(MediaType.APPLICATION_JSON)
    @JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public ArrayList<RoutingRef> getRoutingRefs(@QueryParam("token") String token)
		throws WebAppException
	{
		getToken(token);
		
System.out.println("getRoutingRefs");
		try (TsdbInterface dbi = new TsdbInterface();
			RoutingDAO dao = new RoutingDAO(dbi.getTheDb()))
		{
			return dao.getRoutingRefs();
		}
		catch (DbIoException e)
		{
			e.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR, "Internal exception:" + e);
		}
	}

	@GET
	@Path("routing")
	@Produces(MediaType.APPLICATION_JSON)
    @JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public DecodesRouting getRouting(
		@QueryParam("routingid") Long routingId,
		@QueryParam("token") String token
		)
		throws WebAppException
	{
		getToken(token);
		
		if (routingId == null)
			throw new WebAppException(ErrorCodes.MISSING_ID, 
				"Missing required routingid parameter.");
		
System.out.println("getRouting id=" + routingId);
		try (TsdbInterface dbi = new TsdbInterface();
			RoutingDAO dao = new RoutingDAO(dbi.getTheDb()))
		{
			return dao.getRouting(routingId);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR, "Internal exception:" + e);
		}
	}

	@POST
	@Path("routing")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public DecodesRouting postRouting(@QueryParam("token") String token, 
		DecodesRouting routing)
		throws WebAppException
	{
		System.out.println("post routing received routing " + routing.getName() 
			+ " with ID=" + routing.getRoutingId());
		
		if (getToken(token) == null)
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		try (TsdbInterface dbi = new TsdbInterface();
				RoutingDAO dao = new RoutingDAO(dbi.getTheDb()))
		{
			dao.writeRouting(routing);
			return routing;
		}
		catch(WebAppException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR,
				"Internal DbIoException: " + e);
		}
	}

	@DELETE
	@Path("routing")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteRouting(
		@QueryParam("token") String token, 
		@QueryParam("routingid") Long routingId)
		throws WebAppException
	{
		System.out.println(
			"DELETE routing received routingId=" + routingId
			+ ", token=" + token);
		
		if (getToken(token) == null)
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		// Use username and password to attempt to connect to the database
		try (TsdbInterface dbi = new TsdbInterface();
			RoutingDAO dao = new RoutingDAO(dbi.getTheDb()))
		{
			dao.deleteRouting(routingId);
			return Response.status(HttpServletResponse.SC_OK).entity(
				"RoutingSpec with ID " + routingId + " deleted").build();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return Response.status(ErrorCodes.DATABASE_ERROR).entity(
				"Error deleting routing id=" + routingId + ": " + e).build();
		}
	}

	@GET
	@Path("schedulerefs")
	@Produces(MediaType.APPLICATION_JSON)
    @JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public ArrayList<ScheduleEntryRef> getScheduleRefs(@QueryParam("token") String token)
		throws WebAppException
	{
		getToken(token);
		
System.out.println("getScheduleRefs");
		try (TsdbInterface dbi = new TsdbInterface();
			RoutingDAO dao = new RoutingDAO(dbi.getTheDb()))
		{
			return dao.getScheduleRefs();
		}
		catch (DbIoException e)
		{
			e.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR, "Internal exception:" + e);
		}
	}

	@GET
	@Path("schedule")
	@Produces(MediaType.APPLICATION_JSON)
    @JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public DecodesScheduleEntry getSchedule(
		@QueryParam("scheduleid") Long scheduleId,
		@QueryParam("token") String token
		)
		throws WebAppException
	{
		getToken(token);
		
		if (scheduleId == null)
			throw new WebAppException(ErrorCodes.MISSING_ID, 
				"Missing required scheduleid parameter.");
		
		System.out.println("getSchedule id=" + scheduleId);
		try (TsdbInterface dbi = new TsdbInterface();
			RoutingDAO dao = new RoutingDAO(dbi.getTheDb()))
		{
			return dao.getSchedule(scheduleId);
		}
		catch (DbIoException e)
		{
			e.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR, "Internal exception:" + e);
		}
	}

	@POST
	@Path("schedule")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public DecodesScheduleEntry postSchedule(@QueryParam("token") String token, 
		DecodesScheduleEntry schedule)
		throws WebAppException
	{
		System.out.println("post schedule received sched " + schedule.getName() 
			+ " with ID=" + schedule.getSchedEntryId());
		
		if (getToken(token) == null)
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		try (TsdbInterface dbi = new TsdbInterface();
				RoutingDAO dao = new RoutingDAO(dbi.getTheDb()))
		{
			dao.writeSchedule(schedule);
			return schedule;
		}
		catch(WebAppException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR,
				"Internal DbIoException: " + e);
		}
	}

	@DELETE
	@Path("schedule")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteSchedule(
		@QueryParam("token") String token, 
		@QueryParam("scheduleid") Long scheduleId)
		throws WebAppException
	{
		System.out.println(
			"DELETE schedule received scheduleId=" + scheduleId
			+ ", token=" + token);
		
		if (getToken(token) == null)
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		// Use username and password to attempt to connect to the database
		try (TsdbInterface dbi = new TsdbInterface();
			RoutingDAO dao = new RoutingDAO(dbi.getTheDb()))
		{
			dao.deleteSchedule(scheduleId);
			return Response.status(HttpServletResponse.SC_OK).entity(
				"schedulec with ID " + scheduleId + " deleted").build();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return Response.status(ErrorCodes.DATABASE_ERROR).entity(
				"Error deleting schedule id=" + scheduleId + ": " + e).build();
		}
	}

	@GET
	@Path("apprefs")
	@Produces(MediaType.APPLICATION_JSON)
    @JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public ArrayList<AppRef> getAppRefs(@QueryParam("token") String token)
		throws WebAppException
	{
		getToken(token);
		
System.out.println("getAppRefs");
		try (TsdbInterface dbi = new TsdbInterface();
			AppDAO dao = new AppDAO(dbi.getTheDb()))
		{
			ArrayList<AppRef> ret = dao.getAppRefs();
System.out.println("Returning " + ret.size() + " apps.");
			return ret;
		}
		catch (DbIoException e)
		{
			e.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR, "Internal exception:" + e);
		}
	}

	@GET
	@Path("app")
	@Produces(MediaType.APPLICATION_JSON)
    @JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public LoadingApp getApp(
		@QueryParam("appid") Long appId,
		@QueryParam("token") String token
		)
		throws WebAppException
	{
		getToken(token);
		
		if (appId == null)
			throw new WebAppException(ErrorCodes.MISSING_ID, 
				"Missing required appid parameter.");
		
		System.out.println("getApp id=" + appId);
		try (TsdbInterface dbi = new TsdbInterface();
			AppDAO dao = new AppDAO(dbi.getTheDb()))
		{
			return dao.getApp(appId);
		}
		catch (DbIoException e)
		{
			e.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR, "Internal exception:" + e);
		}
	}

	@POST
	@Path("app")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public LoadingApp postApp(@QueryParam("token") String token, 
			LoadingApp app)
		throws WebAppException
	{
		System.out.println("post app received app " + app.getAppName() 
			+ " with ID=" + app.getAppId());
		
		if (getToken(token) == null)
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		try (TsdbInterface dbi = new TsdbInterface();
			AppDAO dao = new AppDAO(dbi.getTheDb()))
		{
			dao.writeApp(app);
			return app;
		}
		catch(WebAppException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR,
				"Internal DbIoException: " + e);
		}
	}

	@DELETE
	@Path("app")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response deletApp(
		@QueryParam("token") String token, 
		@QueryParam("appid") Long appId)
		throws WebAppException
	{
		System.out.println(
			"DELETE app received appId=" + appId + ", token=" + token);
		
		if (getToken(token) == null)
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		// Use username and password to attempt to connect to the database
		try (TsdbInterface dbi = new TsdbInterface();
			AppDAO dao = new AppDAO(dbi.getTheDb()))
		{
			dao.deleteApp(appId);
			return Response.status(HttpServletResponse.SC_OK).entity(
				"appId with ID " + appId + " deleted").build();
		}
		catch(WebAppException ex)
		{
			return Response.status(ex.getStatus()).entity(ex.getErrMessage()).build();
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return Response.status(ErrorCodes.DATABASE_ERROR).entity(
				"Error deleting app id=" + appId + ": " + e).build();
		}
	}
	
	@GET
	@Path("tsdb_properties")
	@Produces(MediaType.APPLICATION_JSON)
    @JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public Properties getTsdbProperties(@QueryParam("token") String token)
		throws WebAppException
	{
		getToken(token);
		
System.out.println("getTsdbProperties");
		try (TsdbInterface dbi = new TsdbInterface();
			TsDAO dao = new TsDAO(dbi.getTheDb()))
		{
			return dao.getTsdbProperties();
		}
		catch (DbIoException e)
		{
			e.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR, "Internal exception:" + e);
		}
	}

	@POST
	@Path("tsdb_properties")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Properties postTsdbProperties(@QueryParam("token") String token, 
		Properties props)
		throws WebAppException
	{
		System.out.println("post tsdb_properties");
		
		if (getToken(token) == null)
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		try (TsdbInterface dbi = new TsdbInterface();
			TsDAO dao = new TsDAO(dbi.getTheDb()))
		{
			dao.setTsdbProperties(props);;
			return props;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR,
				"Internal DbIoException: " + e);
		}
	}

	@GET
	@Path("algorithmrefs")
	@Produces(MediaType.APPLICATION_JSON)
    @JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public ArrayList<AlgorithmRef> getAlgorithmRefs(@QueryParam("token") String token)
		throws WebAppException
	{
		getToken(token);
		
System.out.println("getAlgorithmRefs");
		try (TsdbInterface dbi = new TsdbInterface();
			AlgorithmDAI dao = dbi.getTheDb().makeAlgorithmDAO())
		{
			AlgorithmDaoWrapper daoWrap = new AlgorithmDaoWrapper(dao);
			return daoWrap.getAlgorithmRefs();
		}
		catch (DbIoException e)
		{
			e.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR, "Internal exception:" + e);
		}
	}

	@GET
	@Path("algorithm")
	@Produces(MediaType.APPLICATION_JSON)
    @JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public DecodesAlgorithm getAlgorithm(
		@QueryParam("algorithmid") Long algoId,
		@QueryParam("token") String token
		)
		throws WebAppException
	{
		getToken(token);
		
		if (algoId == null)
			throw new WebAppException(ErrorCodes.MISSING_ID, 
				"Missing required algorithmid parameter.");
		
		System.out.println("getAlgorithm algorithmid=" + algoId);

Logger.instance().setMinLogPriority(Logger.E_DEBUG3);
		try (TsdbInterface dbi = new TsdbInterface();
			AlgorithmDAI dao = dbi.getTheDb().makeAlgorithmDAO())
		{
			AlgorithmDaoWrapper daoWrap = new AlgorithmDaoWrapper(dao);
			return daoWrap.getAlgorithm(algoId);
		}
		catch (DbIoException e)
		{
			e.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR, "Internal exception:" + e);
		}
	}

	
	@POST
	@Path("algorithm")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public DecodesAlgorithm postAlgorithm(@QueryParam("token") String token, 
		DecodesAlgorithm algo)
		throws WebAppException
	{
		System.out.println("post algo received algo " + algo.getName()
			+ " with ID=" + algo.getAlgorithmId());
		
		if (getToken(token) == null)
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		try (TsdbInterface dbi = new TsdbInterface();
			AlgorithmDAI dao = dbi.getTheDb().makeAlgorithmDAO())
		{
			AlgorithmDaoWrapper daoWrap = new AlgorithmDaoWrapper(dao);
			return daoWrap.writeAlgorithm(algo);
		}
		catch(WebAppException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR,
				"Internal DbIoException: " + e);
		}
	}

	@DELETE
	@Path("algorithm")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response deletAlgorithm(
		@QueryParam("token") String token, 
		@QueryParam("algorithmid") Long algorithmId)
		throws WebAppException
	{
		System.out.println(
			"DELETE algorithm received algorithmId=" + algorithmId + ", token=" + token);
		
		if (getToken(token) == null)
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		// Use username and password to attempt to connect to the database
		try (TsdbInterface dbi = new TsdbInterface();
			AlgorithmDAI dao = dbi.getTheDb().makeAlgorithmDAO())
		{
			dao.deleteAlgorithm(DbKey.createDbKey(algorithmId));
			return Response.status(HttpServletResponse.SC_OK).entity(
				"Algorithm with ID " + algorithmId + " deleted").build();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return Response.status(ErrorCodes.DATABASE_ERROR).entity(
				"Error deleting algorithm id=" + algorithmId + ": " + e).build();
		}
	}
	
	@GET
	@Path("unitlist")
	@Produces(MediaType.APPLICATION_JSON)
    @JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public ArrayList<DecodesUnit> getUnitList(@QueryParam("token") String token)
		throws WebAppException
	{
		getToken(token);
		
System.out.println("getUnitList");
		try (TsdbInterface dbi = new TsdbInterface();
			UnitDAO dao = new UnitDAO(dbi.getTheDb()))
		{
			return dao.getUnitList();
		}
		catch (DbIoException e)
		{
			e.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR, "Internal exception:" + e);
		}
	}

	@GET
	@Path("computationrefs")
	@Produces(MediaType.APPLICATION_JSON)
    @JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public ArrayList<ComputationRef> getComputationRefs(@QueryParam("token") String token,
		@QueryParam("site") String site,
		@QueryParam("algorithm") String algorithm,
		@QueryParam("datatype") String datatype,
		@QueryParam("group") String group,
		@QueryParam("process") String process,
		@QueryParam("enabled") Boolean enabled,
		@QueryParam("interval") String interval)
		throws WebAppException
	{
		getToken(token);
		
System.out.println("getComputationRefs");
		try (TsdbInterface dbi = new TsdbInterface();
			ComputationDaoWrapper dao = new ComputationDaoWrapper(dbi.getTheDb()))
		{
			return dao.getComputationRefs(site, algorithm, datatype, group,
				process, enabled, interval);
		}
		catch (DbIoException e)
		{
			e.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR, "Internal exception:" + e);
		}
	}

	@GET
	@Path("computation")
	@Produces(MediaType.APPLICATION_JSON)
    @JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public Computation getComputation(
		@QueryParam("computationid") Long compId,
		@QueryParam("token") String token
		)
		throws WebAppException
	{
		getToken(token);
		
		if (compId == null)
			throw new WebAppException(ErrorCodes.MISSING_ID, 
				"Missing required computationid parameter.");
		
		System.out.println("getComputation computationid=" + compId);

		try (TsdbInterface dbi = new TsdbInterface();
			ComputationDaoWrapper dao = new ComputationDaoWrapper(dbi.getTheDb()))
		{
			return dao.getComputation(compId);
		}
		catch (DbIoException e)
		{
			e.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR, "Internal exception:" + e);
		}
	}

	
	@POST
	@Path("computation")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Computation postComputation(@QueryParam("token") String token, 
		Computation comp)
		throws WebAppException
	{
		System.out.println("post comp received comp " + comp.getName()
			+ " with ID=" + comp.getComputationId());
		
		if (getToken(token) == null)
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		try (TsdbInterface dbi = new TsdbInterface();
			ComputationDaoWrapper dao = new ComputationDaoWrapper(dbi.getTheDb()))
		{
			return dao.writeComputation(comp);
		}
		catch(WebAppException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR,
				"Internal DbIoException: " + e);
		}
	}

	@DELETE
	@Path("computation")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteComputation(
		@QueryParam("token") String token, 
		@QueryParam("computationid") Long computationId)
		throws WebAppException
	{
		System.out.println(
			"DELETE computation received computationId=" + computationId + ", token=" + token);
		
		if (getToken(token) == null)
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		// Use username and password to attempt to connect to the database
		try (TsdbInterface dbi = new TsdbInterface();
			ComputationDaoWrapper dao = new ComputationDaoWrapper(dbi.getTheDb()))
		{
			dao.deleteComputation(DbKey.createDbKey(computationId));
			return Response.status(HttpServletResponse.SC_OK).entity(
					"Computation with ID " + computationId + " deleted").build();

		}
		catch (Exception e)
		{
			e.printStackTrace();
			return Response.status(ErrorCodes.DATABASE_ERROR).entity(
				"Error deleting computation id=" + computationId + ": " + e).build();
		}
	}


	@GET
	@Path("datatypelist")
	@Produces(MediaType.APPLICATION_JSON)
    @JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public ArrayList<DecodesDataType> getDataTypeList(@QueryParam("token") String token,
		@QueryParam("standard") String std)
		throws WebAppException
	{
		getToken(token);
		
System.out.println("getDataTypeList");
		try (TsdbInterface dbi = new TsdbInterface();
			UnitDAO dao = new UnitDAO(dbi.getTheDb()))
		{
			return dao.getDataTypeList(std);
		}
		catch (DbIoException e)
		{
			e.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR, "Internal exception:" + e);
		}
	}

	@GET
	@Path("propspecs")
	@Produces(MediaType.APPLICATION_JSON)
    @JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public PropertySpec[] getPropSpecs(@QueryParam("token") String token,
		@QueryParam("class") String className)
		throws WebAppException
	{
		getToken(token);
		
		if (className == null)
			throw new WebAppException(ErrorCodes.MISSING_ID, "Missing required class argument.");
		
System.out.println("getPropSpecs class='" + className + "'");
		try
		{
			Class c = Class.forName(className);
			PropertiesOwner pw = (PropertiesOwner)c.newInstance();
			return pw.getSupportedProps();
		}
		catch (ClassCastException ex)
		{
			ex.printStackTrace();
			throw new WebAppException(ErrorCodes.NOT_ALLOWED, "Class '"
				+ className + "' does not have property specs.");

		}
		catch (ClassNotFoundException ex)
		{
			ex.printStackTrace();
			throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT, "No class named '"
				+ className + "'");
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR, "Internal exception:" + ex);
		}
	}

	@GET
	@Path("message")
	@Produces(MediaType.APPLICATION_JSON)
    @JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public ApiRawMessage getMessage(@QueryParam("token") String token,
		@QueryParam("tmid") String tmid, @QueryParam("tmtype") String tmtype)
		throws WebAppException
	{
		getToken(token);
		
		if (tmid == null)
			throw new WebAppException(ErrorCodes.MISSING_ID, "Missing required tmid argument.");
		
System.out.println("getMessage tmid='" + tmid + "', tmtype='" + tmtype + "'");
		try (TsdbInterface dbi = new TsdbInterface())
		{
			return DecodesRunner.getRawMessage2(tmtype, tmid, dbi);
		}
		catch (DbIoException e)
		{
			e.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR, "Internal exception:" + e);
		}
	}
	
	@POST
	@Path("decode")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public ApiDecodedMessage postDecode(@QueryParam("token") String token,
		@QueryParam("script") String scriptName, DecodeRequest request)
		throws WebAppException
	{
		System.out.println("decode message");
		
		getToken(token);
		
		try (TsdbInterface dbi = new TsdbInterface())
		{
			return DecodesRunner.decodeMessage(request.getRawmsg(), request.getConfig(), 
				scriptName);
		}
		catch(WebAppException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR,
				"Internal DbIoException: " + e);
		}
	}


	
	private void doCatalog(HashMap<String, SiteData> siteDataMap, String arg)
		throws WebAppException
	{
		TsdbInterface dbi = null;
		
		System.out.println("doCatalog arg='" + arg + "'");
		
		ArrayList<String> searchTerms = null;
		arg = arg.trim();
		if (arg.charAt(0) == '[' || arg.charAt(0) == '(')
			searchTerms = getSearchTerms(arg);
		System.out.println("doCatalog with " + 
			(searchTerms == null ? 0 : searchTerms.size()) + " search terms.");
		if (searchTerms != null)
			for(String st : searchTerms)
				System.out.println("    " + st);

		SiteDAI siteDAO = null;
		try
		{
			dbi = new TsdbInterface();
			siteDAO = dbi.getTheDb().makeSiteDAO();
			SiteList siteList = new SiteList();
			siteDAO.read(siteList);
		  nextSite:
			for(Iterator<Site> sit = siteList.iterator(); sit.hasNext(); )
			{
				Site site = sit.next();
				String prefName = site.getPreferredName().getNameValue().toLowerCase();
				String dispName = site.getDisplayName();
				if (dispName.length() == 0)
					dispName = null;
				else
					dispName = dispName.toLowerCase();
System.out.println("Checking site name='" + prefName + "', dispname='" + dispName + "'");
				if (searchTerms != null && searchTerms.size() > 0)
				{
					for(String st : searchTerms)
						if (!prefName.contains(st)
						 && (dispName == null || !dispName.contains(st)))
							continue nextSite;
				}
				siteDataMap.put(prefName, site2siteData(site));
			}
		}
		catch(Exception ex)
		{
			String msg = "Error in doCatalog: " + ex;
			System.err.println(msg);
			ex.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR, msg);
		}
		finally
		{
			if (siteDAO != null)
				siteDAO.close();
			if (dbi != null)
				dbi.close();
		}
	}
	
	private SiteData site2siteData(Site site)
	{
		SiteData ret = new SiteData();
		ret.setLocation_id(site.getPreferredName().getNameValue());
		String s = site.getProperty("HUC");
		if (s != null)
			ret.setHUC(s);
		ret.setActive_flag(site.isActive() ? 'T' : 'F');
		try
		{
			ret.getCoordinates().setLatitude(Location.parseLatitude(site.latitude));
			ret.getCoordinates().setLongitude(Location.parseLongitude(site.longitude));
		}
		catch(Exception ex)
		{
			Logger.instance().warning("In site '" + ret.getLocation_id() + "', error processing lat/lon "
				+ "'" + site.latitude + "', '" + site.longitude + "': " + ex
				+ " -- set to 0/0");
		}
		ret.getElevation().setValue(site.getElevation());
		s = site.getProperty("locationType");
		if (s != null)
			ret.setLocation_type(s);
		if (site.getPublicName() != null)
			ret.setName(site.getPublicName());
		else
			ret.setName(ret.getLocation_id());
		s = site.getProperty("responsibility");
		if (s != null)
			ret.setResponsibility(s);
		
		if (site.timeZoneAbbr != null && site.timeZoneAbbr.length() > 0)
			ret.setTimezone(site.timeZoneAbbr);

		return ret;
	}

	/**
	 * The arg should be a single list of site names.
	 * @param siteDataMap
	 * @param sites Null for no filter or comma separated list of sites
	 * @param param Null for no filter or comma separated list of data types
	 * @param interval Null for no filter or comma separated list of intervals
	 * @param active Either null (meaning no filter) or a boolean string indicating to retrieve
	 * 		  only active or only inactive
	 * @param getData true to get time series data, false for catalog only
	 * @param since null for no lower date limit, IDateFormat since time
	 * @param until null for "now", IDateFormat until time
	 * @param tsidStr the most fine-grained query, if present overrides sites, param, interval, active.
	 */
	private void doTsCatalog(HashMap<String, SiteData> siteDataMap, String sites, 
		SimpleDateFormat sdf, String param, String interval, String active, boolean getData, 
		String since, String until, String tsidStr)
		throws WebAppException
	{
		TsdbInterface dbi = null;
		
		ArrayList<String> siteSearches = getSearchTerms(sites);
		ArrayList<String> paramSearches = getSearchTerms(param);
		ArrayList<String> intervalSearches = getSearchTerms(interval);
		ArrayList<String> tsidSearches = getSearchTerms(tsidStr);
		
		System.out.println("doTsCatalog with " + siteSearches.size() + " site searches:");
		if (siteSearches.size() > 0)
			for(String s : siteSearches)
				System.out.println("    " + s);
		
		System.out.println("doTsCatalog with " + paramSearches.size() + " param searches:");
		if (paramSearches.size() > 0)
			for(String s : paramSearches)
				System.out.println("    " + s);
		
		System.out.println("doTsCatalog with " + intervalSearches.size() + " interval searches:");
		if (intervalSearches.size() > 0)
			for(String s : intervalSearches)
				System.out.println("    " + s);
		
		boolean activeFilter = TextUtil.str2boolean(active);

		SiteDAI siteDAO = null;
		TimeSeriesDAI tsDAO = null;
		try
		{
			dbi = new TsdbInterface();
			siteDAO = dbi.getTheDb().makeSiteDAO();
			SiteList siteList = new SiteList();
			siteDAO.read(siteList);
			
			// Disable active checks for HDB. HdbTsids don't have an active flag.
			boolean doActiveCheck = active != null && (dbi.isCwms || dbi.isOpenTsdb);
			
			tsDAO = dbi.getTheDb().makeTimeSeriesDAO();
			ArrayList<TimeSeriesIdentifier> tsids = tsDAO.listTimeSeries();
			
			for(TimeSeriesIdentifier tsid : tsids)
			{
				String siteName = tsid.getSiteName();

				// Specified specific time series IDs?
				if (tsidSearches.size() > 0)
				{
					boolean found = false;
					String thisTsid = tsid.getUniqueString();
					for(String ts : tsidSearches)
						if (ts.equalsIgnoreCase(thisTsid))
						{
							found = true;
							break;
						}
					if (!found)
						continue;
					
				}
				else // If not, then use site,param,interval,active searches
				{
					if (siteSearches.size() > 0)
					{
						// This site name must match at least one of the search terms
						boolean found = false;
						for(String st : siteSearches)
							if (siteName.equalsIgnoreCase(st))
							{
								found = true;
								break;
							}
						if (!found)
							continue;
					}
					
					if (paramSearches.size() > 0)
					{
						String tsdt = tsid.getDataType().getCode();
						boolean found = false;
						for(String dt : paramSearches)
							if (tsdt.equalsIgnoreCase(dt))
							{
								found = true;
								break;
							}
						if (!found)
							continue;
					}
	
					if (doActiveCheck && activeFilter != ((CwmsTsId)tsid).isActive())
						continue;
	
					if (intervalSearches.size() > 0)
					{
						String tsint = tsid.getInterval();
						boolean found = false;
						for(String intv : intervalSearches)
							if (tsint.equalsIgnoreCase(intv))
							{
								found = true;
								break;
							}
						if (!found)
							continue;
					}
				}

				// Fell through -- DO add this time series:
				SiteData siteData = siteDataMap.get(siteName);
				if (siteData == null)
				{
					// First TS for this site, create SiteData
					Site site = tsid.getSite();
					if (site == null)
					{
						System.err.println("TSID '" + tsid.getUniqueString() + "' with no site!");
						continue;
					}
					siteData = site2siteData(site);
					siteDataMap.put(siteName, siteData);
				}
				
				// Add this time series to siteData
				try
				{
					CTimeSeries cts = dbi.getTheDb().makeTimeSeries(tsid);
					if (getData)
					{
						Date dSince = since==null ? null : IDateFormat.parse(since);
						Date dUntil = until==null ? null : IDateFormat.parse(until);
						dbi.getTheDb().fillTimeSeries(cts, dSince, dUntil);
					}
					TimeSeries ts = new TimeSeries();
					ts.fillFromCTimeSeries(cts, sdf);
					siteData.getTimeseries().put(tsid.getUniqueString(), ts);
				}
				catch (Exception ex)
				{
					Logger.instance().warning("Cannot build time series for '" + tsid.getUniqueString()
						+ "': " + ex);
					ex.printStackTrace(System.err);
				}
			}
		}
		catch(Exception ex)
		{
			String msg = "Error in doTsCatalog: " + ex;
			System.err.println(msg);
			ex.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR, msg);

		}
		finally
		{
			if (tsDAO != null)
				tsDAO.close();
			if (siteDAO != null)
				siteDAO.close();
			if (dbi != null)
				dbi.close();
		}

	}

	/**
	 * Argument is [["tsid1", "units1"], ["tsid2", "units2"], ...]
	 * Where the units specifier is optional.
	 * The "tsid" should be a fully qualified time series identifier as returned in the tscatalog.
	 * @param siteDataMap
	 * @param timeseries
	 * @param backward
	 * @param time_format
	 */
	private void doTimeSeries(HashMap<String, SiteData> siteDataMap, String timeseries, 
		String backward, SimpleDateFormat sdf)
		throws WebAppException
	{
		timeseries = timeseries.trim();
		if (!(timeseries.startsWith("[[") || timeseries.startsWith("(("))
		 || !(timeseries.endsWith("]]")   || timeseries.endsWith("))"))
		   )
			throw new WebAppException(400, "Malformed timeseries request, "
				+ "expected [[\"tsid1\", \"units1\"], [\"tsid2\", \"units2\"], ...]");
		
		// Determine the start time.
		// Parse the backward string and compute the "since" time. If not present set to default.
		if (backward == null)
			backward = "1D";
		IntervalIncrement period[] = IntervalIncrement.parseIso8601(backward);
		Calendar cal = Calendar.getInstance();
		for(IntervalIncrement ii : period)
			cal.add(ii.getCalConstant(), -ii.getCount());
		Date since = 
			period.length == 0 ? new Date(System.currentTimeMillis() - MSEC_PER_DAY)
			: cal.getTime();
			
		// For now, until is always current time. Perhaps in the future an arg will be implemented for it.
		Date until = new Date();
		
		ArrayList<StringPair> requestedTsidUnits = new ArrayList<StringPair>();
		timeseries = timeseries.substring(1, timeseries.length()-1);
		int openBracket = -1;
		while((openBracket = findOpenBracket(timeseries)) >= 0)
		{
			timeseries = timeseries.substring(openBracket+1);
			ArrayList<String> tsidSpec = getSearchTerms(timeseries);
			if (tsidSpec.size() == 0)
				continue;
			
			StringPair tu = new StringPair(tsidSpec.get(0).toLowerCase(),
				tsidSpec.size() >= 2 ? tsidSpec.get(1) : null);
			
			if (tu.first == null || tu.first.trim().length() == 0)
				continue;
			
			requestedTsidUnits.add(tu);
			
			Logger.instance().debug1("tsid=" + tu.first + ", units=" + tu.second);
			
			int closeBracket = timeseries.indexOf(']');
			if (closeBracket < 0)
				break;
			timeseries = timeseries.substring(closeBracket+1);
		}
		Logger.instance().debug1("Number of TSIDs=" + requestedTsidUnits.size());
		for(StringPair tu : requestedTsidUnits)
			Logger.instance().debug1("    " + tu.first + ", units=" + tu.second);
		
		TsdbInterface dbi = null;
		SiteDAI siteDAO = null;
		TimeSeriesDAI tsDAO = null;
		try
		{
			dbi = new TsdbInterface();
			siteDAO = dbi.getTheDb().makeSiteDAO();
			SiteList siteList = new SiteList();
			siteDAO.read(siteList);
			tsDAO = dbi.getTheDb().makeTimeSeriesDAO();
			ArrayList<TimeSeriesIdentifier> tsids = tsDAO.listTimeSeries();
			
			for(TimeSeriesIdentifier tsid : tsids)
			{
				boolean found = false;
				String units = null;
				for(StringPair tu : requestedTsidUnits)
					if (tsid.getUniqueString().toLowerCase().contains(tu.first))
					{
Logger.instance().debug1("Found match tsid='" + tsid.getUniqueString() + "' search string='" + tu.first + "'");
						found = true;
						units = tu.second;
						break;
					}
					
				if (!found)
					continue;
				
				// Fell through -- DO add this time series:
				String siteName = tsid.getSiteName();
				SiteData siteData = siteDataMap.get(siteName);
				if (siteData == null)
				{
					// First TS for this site, create SiteData
					Site site = tsid.getSite();
					if (site == null)
					{
						System.err.println("TSID '" + tsid.getUniqueString() + "' with no site!");
						continue;
					}
					siteData = site2siteData(site);
					siteDataMap.put(siteName, siteData);
				}
				
				// Add this time series to siteData
				try
				{
					CTimeSeries cts = dbi.getTheDb().makeTimeSeries(tsid);
					tsDAO.fillTimeSeries(cts, since, until);
					
					// If units specified above is not null, user wants the TS in those
					// units, so try to build a converter to apply to the CTimeSeries.
					if (units != null
					 && !TextUtil.strEqualIgnoreCase(cts.getUnitsAbbr(), units))
						TSUtil.convertUnits(cts, units);

					TimeSeries ts = new TimeSeries();
					ts.fillFromCTimeSeries(cts, sdf);
					siteData.getTimeseries().put(tsid.getUniqueString(), ts);
				}
				catch (Exception ex)
				{
					Logger.instance().warning("Cannot build time series for '" + tsid.getUniqueString()
						+ "': " + ex);
					ex.printStackTrace(System.err);
				}
			}
		}
		catch(Exception ex)
		{
			String msg = "Error in doTimeSeries: " + ex;
			System.err.println(msg);
			ex.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR, msg);
		}
		finally
		{
			if (tsDAO != null)
				tsDAO.close();
			if (siteDAO != null)
				siteDAO.close();
			if (dbi != null)
				dbi.close();
		}
	}

	/**
	 * The General Query contains a number of search terms inside a single set of
	 * square brackets. Return Time Series that contain all of the search terms in
	 * the TSID.
	 * @param siteDataMap
	 * @param query
	 * @param backward
	 * @param time_format
	 */
	private void doGeneralQuery(HashMap<String, SiteData> siteDataMap, String arg, 
		String backward, SimpleDateFormat sdf)
		throws WebAppException
	{
		TsdbInterface dbi = null;
		
		System.out.println("query arg='" + arg + "'");
		
		ArrayList<String> searchTerms = null;
		arg = arg.trim();
		if (arg.charAt(0) == '[' || arg.charAt(0) == '(')
			searchTerms = getSearchTerms(arg);
		if (searchTerms == null || searchTerms.size() == 0)
			throw new WebAppException(400, "Bad Query: Please be more descriptive");
		
		System.out.println("query with " + searchTerms.size() + " search terms.");
		for(String st : searchTerms)
			System.out.println("    " + st);
		
		// Determine the start time.
		// Parse the backward string and compute the "since" time. If not present set to default.
		if (backward == null)
			backward = "1D";
		IntervalIncrement period[] = IntervalIncrement.parseIso8601(backward);
		Calendar cal = Calendar.getInstance();
		for(IntervalIncrement ii : period)
			cal.add(ii.getCalConstant(), -ii.getCount());
		Date since = 
			period.length == 0 ? new Date(System.currentTimeMillis() - MSEC_PER_DAY)
			: cal.getTime();
			
		// For now, until is always current time. Perhaps in the future an arg will be implemented for it.
		Date until = new Date();


		SiteDAI siteDAO = null;
		TimeSeriesDAI tsDAO = null;
		try
		{
			dbi = new TsdbInterface();
			siteDAO = dbi.getTheDb().makeSiteDAO();
			SiteList siteList = new SiteList();
			siteDAO.read(siteList);
			
			tsDAO = dbi.getTheDb().makeTimeSeriesDAO();
			ArrayList<TimeSeriesIdentifier> tsids = tsDAO.listTimeSeries();
			
			for(TimeSeriesIdentifier tsid : tsids)
			{
				String siteName = tsid.getSiteName();
				if (searchTerms != null && searchTerms.size() > 0)
				{
					// ALL search terms must be present in the TSID.
					boolean found = true;
					for(String st : searchTerms)
						if (!tsid.getUniqueString().toLowerCase().contains(st))
						{
							found = false;
							break;
						}
					if (!found)
						continue;
				}
				
				// Fell through -- DO add this time series:
				SiteData siteData = siteDataMap.get(siteName);
				if (siteData == null)
				{
					// First TS for this site, create SiteData
					Site site = tsid.getSite();
					if (site == null)
					{
						System.err.println("TSID '" + tsid.getUniqueString() + "' with no site!");
						continue;
					}
					siteData = site2siteData(site);
					siteDataMap.put(siteName, siteData);
				}
				
				// Add this time series to siteData
				try
				{
					CTimeSeries cts = dbi.getTheDb().makeTimeSeries(tsid);
					tsDAO.fillTimeSeries(cts, since, until);
					TimeSeries ts = new TimeSeries();
					ts.fillFromCTimeSeries(cts, sdf);
					siteData.getTimeseries().put(tsid.getUniqueString(), ts);
				}
				catch (Exception ex)
				{
					Logger.instance().warning("Cannot build time series for '" + tsid.getUniqueString()
						+ "': " + ex);
					ex.printStackTrace(System.err);
				}
			}
		}
		catch(Exception ex)
		{
			String msg = "Error in doGeneralQuery: " + ex;
			System.err.println(msg);
			ex.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR, msg);
		}
		finally
		{
			if (tsDAO != null)
				tsDAO.close();
			if (siteDAO != null)
				siteDAO.close();
			if (dbi != null)
				dbi.close();
		}
	}


	
	/**
	 * Passed a list like ("one", "two", "three"), return the quoted strings as an array list.
	 * Parans may also be square brackets.
	 * Parens may be omitted. E.g. one,two,three
	 * Terms are optionally enclosed in quotes.
	 * @param arg
	 * @return ArrayList of search terms. Empty if empty string.
	 */
	private ArrayList<String> getSearchTerms(String arg)
	{
		ArrayList<String> ret = new ArrayList<String>();
		
		if (arg == null)
			return ret;
		arg = arg.trim();
		if (arg.length() == 0)
			return ret;
		
		if (arg.charAt(0) == '[' || arg.charAt(0) == '(')
		{
			arg = arg.substring(1);
			int idx = findCloseBracket(arg);
			if (idx < arg.length())
				arg = arg.substring(0, idx);
		}
		
		// Now parse into quoted strings
		int start = 0;
		while(start < arg.length())
		{
			char c = arg.charAt(start);
			if (c == '"')
			{
				int end = start+1;
				boolean escaped = false;
				while(end < arg.length() && 
					(arg.charAt(end) != '"' || escaped))
				{
					if (!escaped && arg.charAt(end) == '\\')
						escaped = true;
					else
						escaped = false;
					end++;
				}
				ret.add(arg.substring(start+1, end).toLowerCase());
				start = end+1;
			}
			else if (Character.isLetterOrDigit(c)||c=='-'||c=='+')
			{
				int end = start+1;
				boolean escaped = false;
				while(end < arg.length() && (arg.charAt(end) != ',' || escaped))
				{
					if (!escaped && arg.charAt(end) == '\\')
						escaped = true;
					else
						escaped = false;
					end++;
				}
				ret.add(arg.substring(start, end).toLowerCase());
				start = end;
			}
			else
			{
				if (c == ',' || c == ' ' || c == '\t')
					start++;
				else
				{
					System.err.println("Parse error in argument '" + arg + "' at position " + start);
					return ret;
				}
			}
		}
		
		return ret;
	}
	
	/**
	 * Pass string that has been trimmed to start just AFTER the open bracket.
	 * @param s
	 * @return the index of the close bracket or length of string if not found.
	 */
	private int findCloseBracket(String arg)
	{
		int level = 1;
		int idx = 0;
		for(; idx < arg.length() && level > 0; idx++)
		{
			char c = arg.charAt(idx);
			if (c == '[' || c == '(')
				level++;
			else if (c == ']' || c == ')')
			{
				if (--level == 0)
					return idx;
			}
		}
		return idx;
	}
	
	private int findOpenBracket(String arg)
	{
		int idx = 0;
		for(; idx < arg.length(); idx++)
		{
			char c = arg.charAt(idx);
			if (c == '[' || c == '(')
				return idx;
		}
		return -1;
	}
	
	
	
}
