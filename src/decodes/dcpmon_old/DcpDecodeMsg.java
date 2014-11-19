//package decodes.dcpmon_old;
///**
// * $Id$
// * 
// * Open Source Software
// * 
// * $Log
// */
//
//
//import java.io.OutputStream;
//import java.util.Date;
//import java.util.Iterator;
//import java.util.Properties;
//import java.util.TimeZone;
//
//import ilex.var.Variable;
//import ilex.util.Logger;
//import lrgs.common.DcpAddress;
//
//import decodes.comp.ComputationProcessor;
//import decodes.consumer.DataConsumerException;
//import decodes.consumer.OutputFormatter;
//import decodes.consumer.OutputFormatterException;
//import decodes.datasource.GoesPMParser;
//import decodes.datasource.HeaderParseException;
//import decodes.datasource.PMParser;
//import decodes.datasource.RawMessage;
//import decodes.datasource.UnknownPlatformException;
//import decodes.db.Constants;
//import decodes.db.Database;
//import decodes.db.DatabaseException;
//import decodes.db.DecodesScript;
//import decodes.db.IncompleteDatabaseException;
//import decodes.db.InvalidDatabaseException;
//import decodes.db.Platform;
//import decodes.db.PresentationGroup;
//import decodes.db.TransportMedium;
//import decodes.dcpmon_old.ByteArrayConsumer;
//import decodes.decoder.DecodedMessage;
//import decodes.decoder.DecoderException;
//import decodes.util.DecodesException;
//
///**
// * Class to decode a Xmit record raw message data when the user
// * request to see the message.
// *
// */
//public class DcpDecodeMsg
//{
//	/**
//	 * Given the rawMsg - decode the data and generate an output
//	 * using the HtmlFormatter.
//	 * 
//	 * @param rawMsg read from SQL Table
//	 * @return decoded data using HtmlFormatter - this output will be passed
//	 * to the cgi
//	 */
//	public static String decodeData(XmitTimeWindow xr, Date timestamp,
//			DcpAddress dcpAddress)
//	{
//		boolean testing = true;
//		if (testing)
//		{
//			String decMsg = doDecoding(xr, timestamp, dcpAddress);
//			if (decMsg == null || decMsg.equals(""))
//				return wrapString(new String(xr.getData()));
//			else
//				return decMsg;
//		}
//		else //just display raw message
//			return wrapString(new String(xr.getData()));
//	}
//	
//	//---------------- TO DELETE for testing purposes only
//	private static String wrapString(String ins)
//	{
//		StringBuffer sb = new StringBuffer(ins);
//		int length = ins.length();
//
//		int wordlen = 0;
//		for(int i = 0; i<length; i++)
//		{
//			if (Character.isWhitespace(sb.charAt(i)))
//				wordlen = 0;
//			else if (++wordlen >= 80)
//			{
//				sb.insert(i, ' ');
//				length++;
//				wordlen = 0;
//			}
//		}
//		return sb.toString();
//	}
//	
//	//Decode the rawMsg using the HtmlFormatter
//	private static synchronized String doDecoding(
//		XmitTimeWindow xr, Date timestamp, DcpAddress dcpAddress)
//	{
//		String module = "DcpDecodeMsg";
//		String errorMsg = "Can not display message ";
//
//		byte []msgData = xr.getData();
//		RawMessage rawMsg = new RawMessage(msgData, msgData.length);
//		DecodedMessage dm = null;
//		DecodesScript ds = null;
//		PMParser pmp;
//		try 
//		{ 
//			pmp = PMParser.getPMParser(Constants.medium_Goes);
//			pmp.parsePerformanceMeasurements(rawMsg);
//		}
//		catch(HeaderParseException e) 
//		{
//			Logger.instance().log(Logger.E_FAILURE, module +
//					":decodeData Could not parse message header for '"
//					+ new String(msgData, 0, 20) + "': " + e);	
//		} // shouldn't happen.
//		rawMsg.setTimeStamp(timestamp);
//
//		rawMsg.setPM(GoesPMParser.CARRIER_START, 
//			new Variable(xr.getCarrierStart()));
//		rawMsg.setPM(GoesPMParser.CARRIER_STOP, 
//			new Variable(xr.getCarrierEnd()));
//		rawMsg.setPM(GoesPMParser.FAILURE_CODE, 
//			new Variable(xr.failureCodes()));
//
//		int chan = xr.getGoesChannel();
//		
//		//Attempt to get platform record using type Goes
//		// Note: Platform list will find any matching GOES TM type (ST or RD).
//		Platform p = null;
//		try
//		{
//			p = Database.getDb().platformList.getPlatform(
//				Constants.medium_Goes, dcpAddress.toString());
//		}
//		catch(DatabaseException e)
//		{
//			Logger.instance().log(Logger.E_WARNING, module + 
//				":decodeData Cannot read platform record for message '"
//				+ new String(rawMsg.getData(), 0, 
//					(rawMsg.getData().length > 19 ? 19 : rawMsg.getData().length))
//				+ "': " + e);
//			p = null;
//		}
//
//		if (p != null)
//		{
//			rawMsg.setPlatform(p);  // Set platform reference in the raw message.
//			
//			//Get transport medium
//			TransportMedium tm = resolveTransportMedium(p, 
//				dcpAddress.toString(), chan, false);
//			if (tm == null)
//			{
//				Logger.instance().warning(module + 
//					":decodeData Cannot find transport medium for" +
//					"platform with addr='" + dcpAddress + "', chan=" + chan);
//			}
//			else
//			{
//				rawMsg.setTransportMedium(tm);
//				try
//				{
//					if (!p.isPrepared())
//						p.prepareForExec();
//					if (!tm.isPrepared())
//						tm.prepareForExec();
//				
//					// Get decodes script & use it to decode message.
//					ds = tm.getDecodesScript();
//					if (ds != null)
//					{
//						dm = ds.decodeMessage(rawMsg);
//						dm.applyScaleAndOffset();
//						if (DcpMonitorConfig.instance().enableComputations)
//						{
////TODO: Figure out how to get comp proc instance without DcpMonitor
////							DcpMonitor dcpmon = DcpMonitor.instance();
////							ComputationProcessor compProc = dcpmon.getCompProcessor();
////							if (compProc != null)
////								compProc.applyComputations(dm);
//						}
//					}
//					else
//					{
//						Logger.instance().warning(module + 
//						":decodeData Cannot find decodes script for" +
//						"platform with addr='" + dcpAddress + "', chan=" 
//						+ chan);
//					}
//				} catch (IncompleteDatabaseException e)
//				{
//					dm = null;
//					Logger.instance().warning(module + ":decodeData " +
//					"Incomplete Database error in platform with addr='"
//							+ dcpAddress + "', chan=" + chan);
//				} catch (InvalidDatabaseException e)
//				{
//					dm = null;
//					Logger.instance().warning(module + ":decodeData " +
//					"Invalid Database error in platform with addr='"
//							+ dcpAddress + "', chan=" + chan);
//				} catch (UnknownPlatformException e)
//				{
//					dm = null;
//					Logger.instance().warning(module + ":decodeData " +
//					"UnknownPlatformException error in platform with addr='"
//									+ dcpAddress + "', chan=" + chan);
//				} catch (DecoderException e)
//				{
//					dm = null;
//					Logger.instance().warning(module + ":decodeData " +
//							"Failed to decode message from platform "
//							+ p.makeFileName() + ", transport="
//							+ tm.makeFileName() + ": " + e);
//				}
//				catch(Exception ex)
//				{
//					dm = null;
//					String platname = "(null)";
//					if (p != null)
//						platname = p.makeFileName();
//					String es = module + ":decodeData " +
//						"Exception processing data from platform '"
//						+ platname + "': " + ex.toString();
//					Logger.instance().warning(es);
//					System.err.println(es);
//					System.out.println(ex.toString());
//					ex.printStackTrace(System.err);
//				}
//			}
//		}
//		else
//		{
//			//Platform was null
//			Logger.instance().warning(module + 
//					":decodeData Cannot find Platform record for " +
//					"dcp addr='" + dcpAddress + "', chan=" + chan);
//		}
//		
//		try 
//		{ 
//			if (dm == null)
//			{
//				dm = new DecodedMessage(rawMsg, false);
//				dm.setPlatform(p);
//			}
//		}
//		catch(Exception ex)
//		{
//	 		String msg = module + ":decodeData Unexpected Error: " +
//	 					"Cannot create DecodedMessage "
//	 					+ "wrapper for raw message: " + ex;
//	 		Logger.instance().warning(msg);
//			System.err.println(msg);
//			System.out.println(ex.toString());
//			ex.printStackTrace(System.err);
////			return errorMsg + "Cannot create DecodedMessage";
//		}
//		
//		ByteArrayConsumer consumer = new ByteArrayConsumer();
//		OutputFormatter formatter;
//		TimeZone tz = java.util.TimeZone.getTimeZone("UTC");
//		
//		PresentationGroup presentationGroup = null;
////		presentationGroup =
////			Database.getDb().presentationGroupList.find(
////				rs.presentationGroupName);
////		
////		if (presentationGroup == null)
////		{
////			log(Logger.E_FAILURE,
////				"Cannot find presentation group '" +
////				rs.presentationGroupName + "'");
////			done = true;
////			currentStatus = "ERROR-PresGrpInit";
////			return;
////		}
//		
//		consumer.setTimeZone(tz);
//		try
//		{
//			formatter = OutputFormatter.makeOutputFormatter(
//					"HtmlFormatter", tz,
//				presentationGroup, new Properties());
//			formatter.formatMessage(dm, consumer); 
//			OutputStream os = consumer.getOutputStream();
//			if (os != null)
//				return os.toString();
//			else
//				return errorMsg + " Error on Consumer output";
//		} catch (OutputFormatterException e)
//		{
//			Logger.instance().warning(module + 
//			":decodeData Error on output formatter '"
//					+ "': " + e);
//			return errorMsg + "Error on output formatter";
//		} catch (DataConsumerException e)
//		{
//			Logger.instance().warning(module + 
//			":decodeData Error on data consumer '" + "': " + e);
//			return errorMsg + "Error on data consumer";
//		}
//		catch(Exception ex)
//		{
//			String msg = module + ":decodeData " +
//			"Unexpected exception in formatter: " + ex;
//			Logger.instance().warning(msg);
//			System.err.println(msg);
//			System.out.println(ex.toString());
//			ex.printStackTrace(System.err);
//			return errorMsg + "Unexpected exception in formatter";
//		}
//	}
//	
//	/**
//	  Find the matching transport medium for this platform.
//	  This logic is a bit messy because GOES can have different TM
//	  types (ST, RD, or simply 'GOES'), and because we want to support
//	  the old logic of channels < 100 being self-timed.
//	  <p>
//	  The logic is implemented here in the base class so that it can
//	  be used consistently for all data sources that return GOES messages.
//	  If channel is not -1, this indicates GOES message. This function
//	  should work for non-goes-data also.
//	  <p>
//	  @param p the platform resolved from the message.
//	  @param tmid the transport medium ID (e.g. DCP address, Site ID)
//	  @param chan the channel number from the message or -1 if undefined.
//	  @param oldChannelRanges deprecated flag indicating channels < 100 are ST
//	  @return TransportMedium matching the arguments.
//	*/
//	private static TransportMedium resolveTransportMedium(Platform p,
//		String tmid, int chan, boolean oldChannelRanges)
//	{
//		for(Iterator<TransportMedium> tmit = p.transportMedia.iterator(); tmit.hasNext(); )
//		{
//			TransportMedium tm = tmit.next();
//
//			// If this is a GOES msg, but not a GOES TM, skip it.
//			if (chan != -1
//			 && !(tm.getMediumType().equalsIgnoreCase(Constants.medium_GoesST)
//			     || tm.getMediumType().equalsIgnoreCase(Constants.medium_GoesRD)
//			     || tm.getMediumType().equalsIgnoreCase(Constants.medium_Goes)))
//				continue;
//
//			if (!tmid.equalsIgnoreCase(tm.getMediumId()))
//				continue;
//
//			/*
//			  IF
//				no channel to check,
//			  	or channel matches,
//			  	or using old ranges && this channel falls into ST or RD range,
//			  THEN
//				we have a match!
//			*/
//			if (chan == -1
//			 || chan == tm.channelNum
//		     || (oldChannelRanges 
//				 && tm.getMediumType() == Constants.medium_GoesST 
//				 && chan < 100)
//		     || (oldChannelRanges 
//				 && tm.getMediumType() == Constants.medium_GoesRD
//				 && chan > 100))
//			{
//				return tm;
//			}
//		}
//		return null;
//	}
//}
