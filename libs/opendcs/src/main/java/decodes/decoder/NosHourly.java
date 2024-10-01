/*
* $Id$
* 
* $Log$
*
* 2023/02/21 Baoyu Yin
* Adding raw messages to the printout messages - ING 633
*
* 2023/02/15 Baoyu Yin
* Fixing issues with twos complement and undefined number - ING629 & ING 631
*
* 2023/02/08 Baoyu Yin
* Change L1 decoding (ING-625)
*
* 2023/01/09 Baoyu Yin
* Comment out 1 hour time shift for Redundant Paroscientific #2 (ING-615)
*
* Revision 1.2  2014/05/28 13:09:26  mmaloney
* dev
*
* Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
* OPENDCS 6.0 Initial Checkin
*
* Revision 1.3  2011/09/27 01:24:48  mmaloney
* Enhancements for SHEF and NOS Decoding.
*
* Revision 1.2  2011/09/21 18:27:08  mmaloney
* Updates to NOS decoding.
*
* Revision 1.1  2011/08/26 19:49:34  mmaloney
* Implemented the NOS decoders.
*
*/
package decodes.decoder;

import java.util.Calendar;
import java.util.Date;

import ilex.util.Logger;
import ilex.var.Variable;
import decodes.db.DecodesScript;
import decodes.db.Platform;
import decodes.db.PlatformConfig;
import decodes.decoder.DataOperations;
import decodes.decoder.DecodedMessage;
import decodes.decoder.DecodesFunction;
import decodes.datasource.RawMessage;

/** Handles NOS Hourly message format. */
public class NosHourly
        extends NosDecoder 
{
        public static final String module = "NosHourly";

        public NosHourly()
        {
                super();
        }

        public DecodesFunction makeCopy()
        {
                return new NosHourly();
        }

        public String getFuncName() 
        { 
                return module; 
        }

        /**
         * No arguments expected for NOS 6 Min
         */
        public void setArguments(String argString, DecodesScript script)
        {
        }

        public void execute(DataOperations dd, DecodedMessage msg)
                throws DecoderException
        {
                dataOps = dd;

                Platform platform = msg.getPlatform();
                if (platform == null)
                        throw new DecoderException(module + " function cannot be called with null platform.");
                PlatformConfig config = platform.getConfig();
                if (config == null)
                        throw new DecoderException(module + " function cannot be called with null config.");

                char msgType = (char)dd.curByte();
                if (msgType != 'N') 
                {
                        RawMessage ErrRawM = msg.getRawMessage();
                        byte[] ErrRawData = ErrRawM.getData();
                        String RawDataString = new String(ErrRawData);
                        System.out.println("The following Hourly Raw Message may have an issue: " + RawDataString);

                        throw new DecoderException(module + " requires 'N' in first char.");
                }
                dd.forwardspace();

                byte field[] = dd.getField(7, null);
                String stationId = new String(field);
                msg.getRawMessage().setPM(PM_STATION_ID, new Variable(stationId));

                dcpNum = (int)((char)dd.curByte()) - (int)'0';
                dd.forwardspace();
                msg.getRawMessage().setPM(PM_DCP_NUM, new Variable(dcpNum));

                Logger.instance().info(module + ": stationId=" + stationId + ", dcpNum=" + dcpNum);

                double datumOffset = getDouble(3, false);
                if ((int)datumOffset == 262143) datumOffset = 999999;
                msg.getRawMessage().setPM(PM_DATUM_OFFSET, new Variable (datumOffset));
                double sensorOffset = getDouble(2, true);
                if ((int)sensorOffset == 2047) sensorOffset = 999999;
                msg.getRawMessage().setPM(PM_SENSOR_OFFSET, new Variable (sensorOffset));
                int systemStatus = getInt(2, false);
                msg.getRawMessage().setPM(PM_SYSTEM_STATUS, new Variable(systemStatus));

                // Skip obsolete fields RSTS, CHKSM
                for(int i=0; i<3; i++) dd.forwardspace();

                // Time offset 1 byte
                int timeOffset = getInt(1, false);
                Logger.instance().info(module + ": dataumOffset=" + datumOffset + 
                        ", sensorOffset=" + sensorOffset + ", timeOffset=" + timeOffset);
                boolean haveBV = false;

                initSensorIndeces();

                int x, y, z, wl, sigma, outlier, offset;
                Date dataTime = null;
                Variable v;
                int sensorNum = -1; // Current water-level sensor num
                int ancSensor = -1; // Current ancillary sensor num
                char c2;
                boolean firstTime = true;

                while(dd.moreChars())
                {
                        char flag = (char)dd.curByte();
                        Logger.instance().debug3(module + " flag=" + flag);
                        dd.forwardspace();
                        switch(flag)
                        {
                        case '0': // time tag
                                x = getInt(2, false);
                                setDayNumber(x);
                                y = getInt(1, false);
                                cal.set(Calendar.HOUR_OF_DAY, y);
                                cal.set(Calendar.MINUTE, timeOffset);
                                cal.set(Calendar.SECOND, 0);
                                dataTime = cal.getTime();
                                if (firstTime)
                                {
                                        msg.getRawMessage().setPM(PM_STATION_TIME, new Variable(dataTime));
                                        firstTime = false;
                                        Logger.instance().info(module + " Set station time to " + cal.getTime());
                                }
                                Logger.instance().info(module + " Set calendar to " + cal.getTime());
                                break;
                        case '1': // PWL Aqua Track = Acousitic Water Level
                        case '(': // Air Gap
                                sensorNum = getSensorNumber(
                                        flag == '1' ? 'A' : 
                                        flag == '(' ? 'Q' : 'A', config);
                                if (sensorNum == -1)
                                        continue;
                                cal.setTime(dataTime);
                                offset = getInt(1, false);
                                for(int i=0; i<10; i++)
                                {
                                        wl = offset*250 + getInt(2, false);
                                        sigma = getInt(2, false);
                                        outlier = getInt(1, false);
                                        x = getInt(2, true); // AQT1
                                        y = getInt(2, true); // AQT2
                                        v = new Variable("" + wl + "," + sigma + "," + outlier
                                                + "," + x + "," + y);
                                        msg.addSampleWithTime(sensorNum, v, cal.getTime(), 1);
                                        cal.add(Calendar.MINUTE, -6);
                                }
                                cal.setTime(dataTime);
                                break;
                        case '2': // BWL
                                sensorNum = getSensorNumber('B', config);
                                if (sensorNum == -1)
                                        continue;
                                offset = getInt(1, false);
                                cal.setTime(dataTime);
                                for(int i=0; i<10; i++)
                                {
                                        msg.addSampleWithTime(sensorNum, 
                                                new Variable(offset*250 + getInt(2, false)), 
                                                cal.getTime(), 1);
                                        cal.add(Calendar.MINUTE, -6);
                                }
                                cal.setTime(dataTime);
                                break;

                        // The redundant blocks are all handled the same. We have
                        // a 3-byte WL sample for 6 minutes ago for the sensor we
                        // just parsed.
                        case '>': // RWL (redundant for Aqua Track)
                        case '"': // RBWL (redundant Backup
                        case '.': // Redundant SAE = value from 6 min ago.
                        case '#': // Redundant MWWL
                        case ')': // Redundant Air Gap
                        case '\'': // Redundant Paroscientific #1
                        case '*': // Redundant Paroscientific #2
                                offset = getInt(1, false);
                                cal.setTime(dataTime);
                                //cal.add(Calendar.HOUR_OF_DAY, -1);
                                for(int i=0; i<10; i++)
                                {
                                        v = new Variable(offset*250 + getInt(2, false));
                                        v.setFlags(v.getFlags() | NosDecoder.FLAG_REDUNDANT);
                                        msg.addSampleWithTime(sensorNum, v, cal.getTime(), 1);
                                        cal.add(Calendar.MINUTE, -6);
                                }
                                cal.setTime(dataTime);
                                break;
                        case '3': // Wind speed, dir, gust
                                ancSensor = getSensorNumber('C', config);
                                if (ancSensor == -1)
                                        continue;
                                cal.setTime(dataTime);

                                // Get the first wind value
                                x = getInt(2, false); // speed
                                y = getInt(2, false); // dir
                                z = getInt(2, false); // gust
/*
                                if (x == 4095) x = 999999;
                                if (y == 4095) y = 999999;
                                if (z == 4095) z = 999999;
*/
                                v = new Variable("" + x + "," + y + "," + z);
                                // Determine if 1 or 10 values are present by checking for a
                                // flag where the 2nd wind sample would start.
                                if (dd.curByte() < (int)'?') // all flags are below '?'
                                {
                                        // Only one sample present, time is top of hour
                                        cal.set(Calendar.MINUTE, 0);
                                        msg.addSampleWithTime(ancSensor, v, cal.getTime(), 1);
                                }
                                else // 10 samples, timetag as per WL
                                {
                                        msg.addSampleWithTime(ancSensor, v, cal.getTime(), 1);
                                        cal.add(Calendar.MINUTE, -6);
                                        for(int i=0; i<9; i++)
                                        {
                                                x = getInt(2, false); // speed
                                                y = getInt(2, false); // dir
                                                z = getInt(2, false); // gust
                                                v = new Variable("" + x + "," + y + "," + z);
                                                msg.addSampleWithTime(ancSensor, v, cal.getTime(), 1);
                                                cal.add(Calendar.MINUTE, -6);
                                        }
                                }
                                cal.setTime(dataTime);
                                break;
                        case '4': // Air temp
                                ancSensor = getSensorNumber('D', config);
                                if (ancSensor == -1)
                                        continue;
                                cal.setTime(dataTime);
                                for(int i=0; i<10; i++)
                                {
                                        v = getNumber(2, true);
                                        msg.addSampleWithTime(ancSensor, v, cal.getTime(), 1);
                                        cal.add(Calendar.MINUTE, -6);
                                }
                                break;
                        case '5': // Water Temp
                                ancSensor = getSensorNumber('E', config);
                                if (ancSensor == -1)
                                        continue;
                                getAncillary(ancSensor, cal, dataTime, true, msg, dd);
                                break;
                        case '6': // Barometric Pressure
                                ancSensor = getSensorNumber('F', config);
                                if (ancSensor == -1)
                                        continue;
                                getAncillary(ancSensor, cal, dataTime, false, msg, dd);
                                break;
//                      case '+': // Frequency #1 -- Ignore as per comment from Sudha
//                              break;
                        case '!': // Shaft Angle Encoder (SAE)
                                sensorNum = getSensorNumber('V', config);
                                if (sensorNum == -1)
                                        continue;
                                cal.setTime(dataTime);
                                offset = getInt(1, false);
                                for(int i=0; i<10; i++)
                                {
                                        wl = offset * 250 + getInt(2, false);
                                        sigma = getInt(2, false);
                                        outlier = getInt(1, false);
                                        v = new Variable("" + wl + "," + sigma + "," + outlier);
                                        msg.addSampleWithTime(sensorNum, v, cal.getTime(), 1);
                                        cal.add(Calendar.MINUTE, -6);
                                }
                                cal.setTime(dataTime);
                                break;
                        case '/': // unused flag
                                break;
                        case '7': // conductivity
                                ancSensor = getSensorNumber('G', config);
                                if (ancSensor == -1)
                                        continue;
                                getAncillary(ancSensor, cal, dataTime, false, msg, dd);
                                break;
                        case '8': // Microwave Water Level (MWWL)
                                sensorNum = getSensorNumber('Y', config);
                                if (sensorNum == -1)
                                        continue;
                                cal.setTime(dataTime);
                                offset = getInt(1, false);
                                for(int i=0; i<10; i++)
                                {
                                        wl = offset * 250 + getInt(2, false);
                                        sigma = getInt(2, false);
                                        outlier = getInt(1, false);
                                        v = new Variable("" + wl + "," + sigma + "," + outlier);
                                        msg.addSampleWithTime(sensorNum, v, cal.getTime(), 1);
                                        cal.add(Calendar.MINUTE, -6);
                                }
                                cal.setTime(dataTime);
                                break;
                        case '9': // Rel Hum
                                ancSensor = getSensorNumber('R', config);
                                if (ancSensor == -1)
                                        continue;
                                getAncillary(ancSensor, cal, dataTime, false, msg, dd);
                                break;
                        case ':': // Rainfall
                                ancSensor = getSensorNumber('J', config);
                                if (ancSensor == -1)
                                        continue;
                                getAncillary(ancSensor, cal, dataTime, false, msg, dd);
                                break;
                        case ';': // Solar Rad
                                ancSensor = getSensorNumber('K', config);
                                if (ancSensor == -1)
                                        continue;
                                getAncillary(ancSensor, cal, dataTime, false, msg, dd);
                                break;
                        case '<': // Analog #1 universally used for VB

                                ancSensor = getSensorNumber('L', config);
                                if (ancSensor == -1)
                                    continue;
                               
                                getAncillary(ancSensor, cal, dataTime, false, msg, dd);
                                // Phil says that after VB, increment DCP number and reset sensor indeces

                                dcpNum++;
                                initSensorIndeces();
                                break;
                        case '=': // Analog #2
                                ancSensor = getSensorNumber('M', config);
                                if (ancSensor == -1)
                                        continue;
                                getAncillary(ancSensor, cal, dataTime, false, msg, dd);
                                break;
                        case '%': // Paroscientific #1 - Pressure WL (N1,N2)
                                sensorNum = getSensorNumber('N', config);
                                if (sensorNum == -1)
                                        continue;
                                cal.setTime(dataTime);
                                offset = getInt(1, false);
                                for(int i=0; i<10; i++)
                                {
                                        wl = offset * 250 + getInt(2, false);
                                        sigma = getInt(2, false);
                                        outlier = getInt(1, false);
                                        v = new Variable("" + wl + "," + sigma + "," + outlier);
                                        msg.addSampleWithTime(sensorNum, v, cal.getTime(), 1);
                                        cal.add(Calendar.MINUTE, -6);
                                }
                                cal.setTime(dataTime);
                                break;
                        case '&': // Paroscientific #2 - Pressure WL (T1, T2, T3)
                                sensorNum = getSensorNumber('T', config);
                                if (sensorNum == -1)
                                        continue;
                                cal.setTime(dataTime);
                                offset = getInt(1, false);
                                for(int i=0; i<10; i++)
                                {
                                        wl = offset * 250 + getInt(2, false);
                                        sigma = getInt(2, false);
                                        outlier = getInt(1, false);
                                        v = new Variable("" + wl + "," + sigma + "," + outlier);
                                        msg.addSampleWithTime(sensorNum, v, cal.getTime(), 1);
                                        cal.add(Calendar.MINUTE, -6);
                                }
                                cal.setTime(dataTime);
                                break;
                        case ',': // Unused flag
                                break;
                        case ' ': // Delimiter for single-char VB at end of message
                                // Only try to get BV once to avoid reading past end of data.
                                if (!haveBV)
                                {
                                        v = new Variable(getDouble(1, false)+9.5);
                                        msg.getRawMessage().setPM(PM_NOS_BATTERY, v);
                                        haveBV = true;
                                }
                                break;
                        case '-': // Start of 2-char flag
                                c2 = (char)dd.curByte();
                                dd.forwardspace();
                                if (c2 == 'O') // Visibility
                                {
                                        x = getInt(3, false);
                                        y = getInt(1, false);
                                        ancSensor = getSensorNumber('O', config);
                                        if (ancSensor == -1)
                                                continue;
                                        msg.addSampleWithTime(ancSensor, 
                                                new Variable("" + x + "," + y), dataTime, 1);
                                }
                                break;
                        default:
                                if (!Character.isWhitespace(flag))
                                        Logger.instance().warning(module + " Unrecognized flag char '" + flag + "'");
                        }
                }
                Logger.instance().debug3(module + " end of message");
        }


        /**
         * Helper method to get one or 10 ancillary values.
         * @param sensorNum
         * @param cal
         * @param dataTime
         * @param signed
         * @param msg
         * @param dd
         * @throws DecoderException
         */
        private void getAncillary(int sensorNum, Calendar cal, Date dataTime,
                boolean signed, DecodedMessage msg, DataOperations dd)
                throws DecoderException
        {
                cal.setTime(dataTime);

                // Get the ancillary value
                Variable v = getNumber(2, signed);

                // Determine if 1 or 10 values are present by checking for a
                // flag where the 2nd sample would start.
                if (dd.curByte() < (int)'?') // all flags are below '?'
                {
                        // Only one sample present, time is top of hour
                        cal.set(Calendar.MINUTE, 0);
                        msg.addSampleWithTime(sensorNum, v, cal.getTime(), 1);
                }
                else // 10 samples, timetag as per WL
                {
                        msg.addSampleWithTime(sensorNum, v, cal.getTime(), 1);
                        cal.add(Calendar.MINUTE, -6);
                        for(int i=0; i<9; i++)
                        {
                                v = getNumber(2, signed);
                                msg.addSampleWithTime(sensorNum, v, cal.getTime(), 1);
                                cal.add(Calendar.MINUTE, -6);
                        }
                }
                cal.setTime(dataTime);
        }

}
