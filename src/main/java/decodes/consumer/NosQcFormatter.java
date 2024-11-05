/*
*
* 2023/02/15 Baoyu Yin
* Fixing issues with twos complement and undefined number - ING629 & ING 631
*
* 2023/02/08 Baoyu Yin
* Fixing First Temperature and Second Temperature in WL sensors - ING622
*
*/
package decodes.consumer;

import ilex.util.Logger;
import ilex.var.NoConversionException;
import ilex.var.TimedVariable;

import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.ArrayList; 

import lrgs.common.DcpMsg;

import decodes.datasource.GoesPMParser;
import decodes.datasource.RawMessage;
import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.PresentationGroup;
import decodes.decoder.DecodedMessage;
import decodes.decoder.NosDecoder;
import decodes.decoder.TimeSeries;
import java.util.Arrays;

/**
 * Generates the NOS XXX.QC Format
 */
public class NosQcFormatter extends OutputFormatter
{
        private SimpleDateFormat sdf = new SimpleDateFormat("MMM dd yyyy HH:mm");
        public static final String module = "NosQcFormatter";

        public NosQcFormatter()
        {
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        }

        @Override
        protected void initFormatter(String type, TimeZone tz, PresentationGroup presGrp,
                        Properties rsProps) throws OutputFormatterException
        {
        }

        @Override
        public void shutdown()
        {
        }

        @Override
        public void formatMessage(DecodedMessage msg, DataConsumer consumer)
                        throws DataConsumerException, OutputFormatterException
        {
                RawMessage rawmsg = msg.getRawMessage();
                if (rawmsg == null)
                {
                        Logger.instance().warning(module + " no raw message!");
                        return;
                }

                try
                {
                        char fc = rawmsg.getPM(GoesPMParser.FAILURE_CODE).getCharValue();
                        if (fc != 'G' && fc != '?')
                                return;
                }
                catch (NoConversionException e2)
                {
                        return;
                }

                StringBuilder sb = new StringBuilder();

                // Sudha says that airtemps persist, and should be output for redundant
                // and tsunami sensors.
                int saved_airtemp1      = 999999;
                int saved_airtemp2      = 999999;

                // Go through each sensor. If ancillary output a line.
        String msgString = msg.getRawMessage().toString();
        ArrayList<String> NTArray=new ArrayList<>(10); 
        for (int i = 0; i < 10; i++) 
        {
            NTArray.add(" ");
        }
        int NTIndex = 1;
        for(Iterator<TimeSeries> tsit = msg.getAllTimeSeries(); tsit.hasNext(); )
                {
                        TimeSeries ts = tsit.next();
                        if (ts.size() == 0)
                                continue;
                        // MJM Don't sort, use the order in which data was decoded.
                        ts.sort(true); // sort in descending order
                        DataType dt = ts.getSensor().getDataType(Constants.datatype_NOS);
                        if (dt == null)
                                continue;
                        if (!NosConsumer.isWaterLevel(dt.getCode()))
                                continue;
                        int sensorDcpNum = ts.getSensorNumber() / 100 + 1;

            String holdStationDCP = rawmsg.getPM(NosDecoder.PM_STATION_ID).getStringValue() + sensorDcpNum;  

                        for(int varidx = 0; varidx < ts.size(); varidx++)
                        {
                                TimedVariable tv = ts.sampleAt(varidx);
                                sb.append(rawmsg.getPM(NosDecoder.PM_STATION_ID));
                                sb.append("" + sensorDcpNum);
                                sb.append("  ");
                                sb.append(sdf.format(tv.getTime()).toUpperCase());
                                sb.append(" ");
                                sb.append(dt.getCode());
                                sb.append(" S "); // this is all satellite data
                                sb.append((tv.getFlags() & NosDecoder.FLAG_REDUNDANT) == 0
                                        ? "P" : "R");

                                int pressure      = 999999;
                                int prim_wl_value = 999999;
                                int prim_wl_sigma = 999999;
                                int prim_wl_outli = 999999;
                                int back_wl_value = 999999;
                                int back_wl_sigma = 999999;
                                int back_wl_outli = 999999;
                                int back_wl_temp  = 999999;
                                int airtemp1      = 999999;
                                int airtemp2      = 999999;
                                int datum_offset  = 999999;
                                int sensor_offset = 999999;
                                int back_wl_gain  = 999999;
                                int back_wl_offset= 999999;
                                try
                                {
// Sudha says that even B sensors go into the PWL columns
//                                      if (dt.getCode().charAt(0) != 'B') // Not backup = primary
//                                      {
                                                if (!tv.isNumeric())
                                                {
                                                        // should be value,sigma,outliers[,t1,t2]
                                                        // parse and put data in the prim_wl fields
                                                        // If aquatrack(A) or airgap(Q), then the
                                                        // two temperature fields are also present.
                                                        StringTokenizer strtok = 
                                                                new StringTokenizer(tv.getStringValue(),",");
                                                        if (strtok.hasMoreTokens())
                                                                prim_wl_value = Integer.parseInt(strtok.nextToken());
                                                                if (prim_wl_value == 262143) prim_wl_value = 999999;
                                                        if (strtok.hasMoreTokens())
                                                                prim_wl_sigma = Integer.parseInt(strtok.nextToken());
                                                        if (strtok.hasMoreTokens())
                                                                prim_wl_outli = Integer.parseInt(strtok.nextToken());
                                                        if (strtok.hasMoreTokens())
                                                                saved_airtemp1 = airtemp1 = Integer.parseInt(strtok.nextToken());
                                                        if (strtok.hasMoreTokens())
                                                                saved_airtemp2 = airtemp2 = Integer.parseInt(strtok.nextToken());

                                                        if (airtemp1 == 2047 || airtemp1 == -2048) saved_airtemp1 = airtemp1 = 99999;
                                                        if (airtemp2 == 2047 || airtemp2 == -2048) saved_airtemp2 = airtemp2 = 99999;

                                                        if (prim_wl_sigma == 4095) prim_wl_sigma = 999999;
                                                        if (prim_wl_outli == 63) prim_wl_outli = 999999;

                                                }
                                                else // is numeric -- means redundant: just prim_wl_value
                                                {
                                                        prim_wl_value = tv.getIntValue();
                                                        if (prim_wl_value == 262143) prim_wl_value = 999999;
                                                }
//                                      }
//                                      else
//                                      {
//                                              if (!tv.isNumeric())
//                                              {
//                                                      // should be value,sigma,outliers[,t1,t2]
//                                                      // parse and put data in the back_wl fields
//                                                      // If aquatrack(A) or airgap(Q), then the
//                                                      // two temperature fields are also present.
//                                                      StringTokenizer strtok = 
//                                                              new StringTokenizer(tv.getStringValue(),",");
//                                                      if (strtok.hasMoreTokens())
//                                                              back_wl_value = Integer.parseInt(strtok.nextToken());
//                                                      if (strtok.hasMoreTokens())
//                                                              back_wl_sigma = Integer.parseInt(strtok.nextToken());
//                                                      if (strtok.hasMoreTokens())
//                                                              back_wl_outli = Integer.parseInt(strtok.nextToken());
//                                                      if (strtok.hasMoreTokens())
//                                                              airtemp1 = Integer.parseInt(strtok.nextToken());
//                                                      if (strtok.hasMoreTokens())
//                                                              airtemp2 = Integer.parseInt(strtok.nextToken());
//                                              }
//                                              else // is numeric -- means redundant.
//                                              {
//                                                      back_wl_value = tv.getIntValue();
//                                              }
//                                      }
                                        // Datum Offset
                                        datum_offset = 
                                                msg.getRawMessage().getPM(
                                                        NosDecoder.PM_DATUM_OFFSET).getIntValue();
                                        sensor_offset = 
                                                msg.getRawMessage().getPM(
                                                        NosDecoder.PM_SENSOR_OFFSET).getIntValue();
                                }
                                catch(Exception ex)
                                {
                                        Logger.instance().warning("NosQcFormatter bad value '"
                                                + tv.getStringValue() + "' -- cannot parse.");
                                        continue;
                                }

                                if (dt.getCode().equals("N1") ||
                        dt.getCode().equals("NT") ||
                        dt.getCode().equals("T1"))
                {
                                sb.append(" " + String.format("%6d", prim_wl_value));
                    sb.append(" " + String.format("%6d", 999999));
                }
                else
                {
                    sb.append(" " + String.format("%6d", pressure));
                                sb.append(" " + String.format("%6d", prim_wl_value));
                }
                
                                sb.append(" " + String.format("%6d", prim_wl_sigma));
                                sb.append(" " + String.format("%6d", prim_wl_outli));
                                sb.append(" " + String.format("%6d", back_wl_value));
                                sb.append(" " + String.format("%6d", back_wl_sigma));
                                sb.append(" " + String.format("%6d", back_wl_outli));
                                sb.append(" " + String.format("%6d", back_wl_temp));

                                // Sudha says to copy airtemps for redundant and tsunami records:
                // ING-569
                                if (airtemp1 == 999999
                                 && dt.getCode().charAt(0) != 'B'
                                 && dt.getCode().charAt(0) != 'Y'
                                 && (dt.getCode().charAt(0) == 'U'
                                         || (tv.getFlags() & NosDecoder.FLAG_REDUNDANT) != 0))
                                {
                                        sb.append(" " + String.format("%6d", saved_airtemp1));
                                        sb.append(" " + String.format("%6d", saved_airtemp2));
                                }
                                else
                                {
                                        sb.append(" " + String.format("%6d", airtemp1));
                                        sb.append(" " + String.format("%6d", airtemp2));
                                }

                                sb.append(" " + String.format("%6d", datum_offset));
                                sb.append(" " + String.format("%6d", sensor_offset));
                                sb.append(" " + String.format("%6d", back_wl_gain));
                                sb.append(" " + String.format("%6d", back_wl_offset));
                consumer.println(sb.toString());
                
                                if (dt.getCode().equals("N1") || dt.getCode().equals("T1"))
                {
                    try
                    {
                    for (int i = 0; i < NTArray.size(); i++) 
                    {
                       // ING-569 To write an NT record, the times have to match, and there has to be both
                       //   an N1 and T1 record present.  Therefore, the times and dcp must match, but the sensor
                       //   types must not match.
                       if (NTArray.get(i).substring(22,27).equals(sb.toString().substring(22,27)) &&
                               NTArray.get(i).substring(7,8).equals(sb.toString().substring(7,8)) &&
                               !dt.getCode().equals(NTArray.get(i).substring(28,30)))
                       {
                           if (dt.getCode().equals("N1"))
                           {
                               consumer.println(sb.toString().substring(0,28) + "NT" + sb.toString().substring(30,132)); 
                           }
                           else
                           {
                               consumer.println(NTArray.get(i).substring(0,28) + "NT" + NTArray.get(i).substring(30,132));                               
                           }
                           NTArray.remove(i);
                       }
                       else
                       {
                           NTArray.set(NTIndex, sb.toString());
                           NTIndex++;
                           break;
                       }
                    }
                    }
                    catch (Exception e)
                    {
                        Logger.instance().warning(module + ": Exception: Array is empty");
                    }
                    if (NTArray.get(0).equals(" "))
                    {
                           NTArray.set(0, sb.toString());
                           NTIndex = 1;
                    }
                }        
                    
                                sb.setLength(0);
                        }
                }
        }

        @Override
        public boolean usesTZ() 
        { 
             return false; 
        }

}
