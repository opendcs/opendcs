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
package decodes.consumer;

import ilex.var.NoConversionException;
import ilex.var.Variable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import lrgs.common.DcpMsg;

import decodes.datasource.GoesPMParser;
import decodes.datasource.RawMessage;
import decodes.db.PresentationGroup;
import decodes.decoder.DecodedMessage;
import decodes.decoder.NosDecoder;

/**
 * Generates the NOS XXX.DCP Format
 */
public class NosDcpFormatter extends OutputFormatter
{
     private static final Logger log = OpenDcsLoggerFactory.getLogger();

     private SimpleDateFormat sdf = new SimpleDateFormat("MMM dd yyyy HH:mm:ss");
     public static final String module = "NosDcpFormatter";

     public NosDcpFormatter()
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
               log.warn("no raw message!");
               return;
          }
          try
          {
               char fc = rawmsg.getPM(GoesPMParser.FAILURE_CODE).getCharValue();
               if (fc != 'G' && fc != '?')
                    return;
          }
          catch (NoConversionException ex)
          {
               log.atWarn().setCause(ex).log("Unknown message type skipped!");
               return;
          }

          StringBuilder sb = new StringBuilder();
          try
          {
               sb.append(rawmsg.getTransportMedium().getMediumId());
          }
          catch(Exception ex)
          {
               log.atWarn().setCause(ex).log("Missing DCP Address!");
               sb.append("        ");
          }
          try
          {
               sb.append(sdf.format(
                    rawmsg.getPM(GoesPMParser.MESSAGE_TIME).getDateValue()).toUpperCase());
          }
          catch (NoConversionException ex)
          {
               log.atWarn().setCause(ex).log("Missing Time Stamp!");
               sb.append("MMM dd yyyy HH:mm:ss");
          }
          sb.append(rawmsg.getPM(GoesPMParser.FAILURE_CODE));
          sb.append(rawmsg.getPM(GoesPMParser.SIGNAL_STRENGTH));
          try
          {
               int freqOffset = rawmsg.getPM(GoesPMParser.FREQ_OFFSET).getIntValue();
               if (freqOffset < 0)
               {
                    sb.append('-');
                    freqOffset = -freqOffset;
               }
               else
                    sb.append('+');
               sb.append("" + (freqOffset % 10));
          }
          catch(Exception ex)
          {
               log.atWarn().setCause(ex).log("Missing or freq offset!");
               sb.append("-0"); // Note: -0 means couldn't parse from message.
          }
          sb.append(rawmsg.getPM(GoesPMParser.MOD_INDEX));
          sb.append(rawmsg.getPM(GoesPMParser.QUALITY));

          try
          {
               int chan = rawmsg.getPM(GoesPMParser.CHANNEL).getIntValue();
               sb.append((char)((int)'0' + ((chan/100)%10)));
               sb.append((char)((int)'0' + ((chan/10 )%10)));
               sb.append((char)((int)'0' + ( chan     %10)));
          }
          catch(Exception ex)
          {
               log.atWarn().setCause(ex).log("Missing or GOES Channel!");
               sb.append("   ");
          }

          sb.append(rawmsg.getPM(GoesPMParser.SPACECRAFT));
          sb.append(rawmsg.getPM(GoesPMParser.UPLINK_CARRIER));

          try
          {
               int chan = rawmsg.getPM(GoesPMParser.MESSAGE_LENGTH).getIntValue();
//               sb.append((char)((int)'0' + ((chan/10000)%10)));
            sb.append(' ');
               sb.append((char)((int)'0' + ((chan/1000 )%10)));
               sb.append((char)((int)'0' + ((chan/100  )%10)));
               sb.append((char)((int)'0' + ((chan/10   )%10)));
               sb.append((char)((int)'0' + ( chan       %10)));
          }
          catch(Exception ex)
          {
               log.atWarn().setCause(ex).log("Missing or message length!");
               sb.append("     ");
          }

          sb.append(rawmsg.getPM(NosDecoder.PM_STATION_ID));
          sb.append(rawmsg.getPM(NosDecoder.PM_DCP_NUM));

          try
          {
               int datumOffset = rawmsg.getPM(NosDecoder.PM_DATUM_OFFSET).getIntValue();
               sb.append(String.format("%7d", datumOffset));
          }
          catch(Exception ex)
          {
               log.atWarn().setCause(ex).log("Missing or bad Datum Offset!");
               sb.append("       ");
          }
          try
          {
               int sensorOffset = rawmsg.getPM(NosDecoder.PM_SENSOR_OFFSET).getIntValue();
               sb.append(String.format("%6d", sensorOffset));
          }
          catch(Exception ex)
          {
               log.atWarn().setCause(ex).log("Missing or bad Sensor Offset!");
               sb.append("      ");
          }

          // 11 obsolete flags to be zero-filled.
          sb.append("00000000000");
          // More obsolete filler
          sb.append(" 99  9999");
          try
          {
               Variable v = rawmsg.getPM(NosDecoder.PM_STATION_TIME);

               Date d = rawmsg.getPM(GoesPMParser.MESSAGE_TIME).getDateValue();
               if (v != null)
                    d = v.getDateValue();
               else
               {
                    log.warn("No {} in message.", NosDecoder.PM_STATION_TIME);
                    //return;
               }
               // lop of the seconds ":ss"
               String s = sdf.format(d);
               s = s.toUpperCase();
               s = s.substring(0, s.length()-3);
               sb.append(s);
          }
          catch (NoConversionException ex)
          {
               log.atWarn().setCause(ex).log("Missing or bad station time!");
          }

          // # of bytes - awaiting guidance from Sudha, right now constant 125
//          sb.append("125");
        try
        {
              int chan = rawmsg.getPM(GoesPMParser.MESSAGE_LENGTH).getIntValue();
              sb.append((char)((int)'0' + ((chan/100  )%10)));
              sb.append((char)((int)'0' + ((chan/10   )%10)));
              sb.append((char)((int)'0' + ( chan       %10)));
        }
          catch(Exception ex)
          {
               log.atWarn().setCause(ex).log("Message length not available!");
               sb.append("   ");
          }

        String capDate = sdf.format(new Date());
        String cd = capDate.substring(0,1);
        String lcd = capDate.substring(1,4);
        String theRest=capDate.substring(4,20);
        if (theRest.substring(0,1).equals("0"))
            theRest = " " + theRest.substring(1,16);
          sb.append(cd.toUpperCase());
        sb.append(lcd.toLowerCase());
        sb.append(theRest);
          sb.append(" ");

          // 2-digit parity count
          int parcnt = 0;
          byte [] data = rawmsg.getData();
          for(int idx = DcpMsg.IDX_DATA; idx < data.length; idx++)
               if ((char)data[idx] == '$')
                    parcnt++;

        sb.append(" ");
          sb.append((char)((int)'0' + (parcnt%10)));

          sb.append(" ");
          sb.append("S"); // source is always satellite for us.

          // These fields are not in my documentation. Sudha will provide
          // more info:
          sb.append(" P   14.00  99.999  99.999");

          consumer.println(sb.toString());
     }

     @Override
     public boolean usesTZ()
     {
          return false;
     }

}
