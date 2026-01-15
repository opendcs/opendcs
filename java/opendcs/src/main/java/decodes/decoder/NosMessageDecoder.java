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
package decodes.decoder;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import ilex.util.PseudoBinary;
import ilex.util.TextUtil;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

public final class NosMessageDecoder
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
    private NosMessageDecoder() {
        // prevent instantiation
    }

    /**	Formats */
    private static final NumberFormat DP1 = nfFrac(1);
    private static final NumberFormat DP2 = nfFrac(2);
    private static final NumberFormat DP3 = nfFrac(3);

    private static final NumberFormat I1  = nfInt(1);
    private static final NumberFormat I2  = nfInt(2);
    private static final NumberFormat I6  = nfInt(6);
    private static final NumberFormat I8  = nfInt(8);

    private static final DecimalFormat D1 = new DecimalFormat("0.0");
    private static final DecimalFormat D3 = new DecimalFormat("0.000");

    /**	NOS sensor flags */
    private static final String NORTEK_LEGACY = "CA";
    private static final String NORTEK_GEN2   = "CN";
    private static final String RESERVED_VAR  = "CX";
    

    /** Helpers to build NumberFormats */
    private static NumberFormat nfFrac(int minFrac)
    {
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setGroupingUsed(false);
        nf.setMinimumFractionDigits(minFrac);
        return nf;
    }
    
    private static NumberFormat nfInt(int minDigits)
    {
        NumberFormat nf = NumberFormat.getIntegerInstance();
        nf.setGroupingUsed(false);
        nf.setMinimumIntegerDigits(minDigits);
        return nf;
    }

    public static String updateSensortype(byte[] field)
    {
        String snsType = RESERVED_VAR;
        if (field.length >= 3)
        {
            String firstThree = new String(field, 0, 3);
            if (firstThree.contains(NORTEK_LEGACY))     snsType = NORTEK_LEGACY;
            else if (firstThree.contains(NORTEK_GEN2))  snsType = NORTEK_GEN2;
        }
        return snsType;
    }

    public static byte[] appendHeaderByte(byte[] field)
    {
        /** If first char is 'C', prepend '*' */
        if (field.length > 0 && field[0] == 'C')
        {
            byte[] updatedField = new byte[field.length + 1];
            updatedField[0] = '*';
            System.arraycopy(field, 0, updatedField, 1, field.length);
            field = updatedField;
        }
        return field;
    }

    /** Nortek Legacy (CA) Header. */
    public static String decodeLegacyHeader(byte[] field)
    {
        log.debug("Legacy Nortek field='" + new String(field) + "', len=" + field.length);
        StringBuilder header = new StringBuilder();

        // first 8 chars after signature (idx 3..10)
        for (int idx = 3; idx < 11; idx++)
        {
            header.append((char) field[idx]);
        }
        header.append('\n');

        int x; double d;

        x = PseudoBinary.decodePB(new String(field, 12, 2), false); // MM
        header.append(I2.format(x)).append(' ');
        x = PseudoBinary.decodePB(new String(field, 14, 2), false); // DD
        header.append(I2.format(x)).append(' ');
        x = PseudoBinary.decodePB(new String(field, 16, 2), false); // YYYY (upstream formatting)
        header.append(I2.format(x)).append(' ');
        x = PseudoBinary.decodePB(new String(field, 18, 2), false); // HR
        header.append(I2.format(x)).append(' ');
        x = PseudoBinary.decodePB(new String(field, 20, 2), false); // MN
        header.append(I2.format(x)).append(' ');
        x = PseudoBinary.decodePB(new String(field, 22, 2), false); // SS
        header.append(I2.format(x)).append(' ');

        x = PseudoBinary.decodePB(new String(field, 24, 3), false); // 8-digit binary #
        header.append(I8.format(x)).append(' ');
        x = PseudoBinary.decodePB(new String(field, 27, 3), false); // 8-digit binary #
        header.append(I8.format(x)).append(' ');

        d = PseudoBinary.decodePB(new String(field, 30, 2), false) * 0.1;
        header.append(TextUtil.setLengthRightJustify(DP1.format(d), 5)).append(' ');
        d = PseudoBinary.decodePB(new String(field, 32, 3), false) * 0.1;
        header.append(TextUtil.setLengthRightJustify(DP1.format(d), 6)).append(' ');
        d = PseudoBinary.decodePB(new String(field, 35, 2), false) * 0.1;
        header.append(TextUtil.setLengthRightJustify(DP1.format(d), 5)).append(' ');
        d = PseudoBinary.decodePB(new String(field, 37, 2), true) * 0.1;
        header.append(TextUtil.setLengthRightJustify(DP1.format(d), 5)).append(' ');
        d = PseudoBinary.decodePB(new String(field, 39, 2), true) * 0.1;
        header.append(TextUtil.setLengthRightJustify(DP1.format(d), 5)).append(' ');
        d = PseudoBinary.decodePB(new String(field, 41, 2), false) * 0.001;
        header.append(TextUtil.setLengthRightJustify(DP3.format(d), 7)).append(' ');
        d = PseudoBinary.decodePB(new String(field, 43, 2), false) * 0.01;
        header.append(TextUtil.setLengthRightJustify(DP2.format(d), 6)).append(' ');

        x = PseudoBinary.decodePB(new String(field, 45, 3), false);
        header.append(TextUtil.setLengthRightJustify(I2.format(x), 5)).append(' ');
        x = PseudoBinary.decodePB(new String(field, 48, 3), false);
        header.append(TextUtil.setLengthRightJustify(I2.format(x), 5)).append('\n');
        log.debug("Legacy Nortek header '" + header + "'");
        return header.toString();
    }

    /** Nortek Legacy (CA) Body. */
    public static String decodeLegacyBins(byte[] field)
    {
        StringBuilder result = new StringBuilder();
        String strResult;

        int fieldIndex;
        for (fieldIndex = 52; fieldIndex < field.length - 1; fieldIndex++)
        {
            for (int i = 0; i < 6; i++)
            {
                if (fieldIndex > field.length - 2) {
                    break;
                }

                String temp = "";
                temp += (char) field[fieldIndex++];
                temp += (char) field[fieldIndex++];

                // First three values per bin are 3-byte pseudo-binary (only if a byte remains)
                if ((i == 0 || i == 1 || i == 2) && fieldIndex < field.length - 1)
                {
                    temp += (char) field[fieldIndex++];
                }

                int number = PseudoBinary.decodePB(temp, true);
                double dnum;
                String pad = "";

                if (i == 0 || i == 1 || i == 2)
                {
                    dnum = number * 0.001;
                    strResult = D3.format(dnum);
                    while (strResult.length() + pad.length() < 8) pad += " ";
                }
                else
                {
                    dnum = number;
                    strResult = String.valueOf(dnum);
                    if (strResult.equalsIgnoreCase("0.0")) strResult = "0.000";
                    while (strResult.length() + pad.length() < 6) pad += " ";
                }
                result.append(pad).append(strResult);
            }
            result.append('\n');
        }
        return result.toString();
    }

    /** Nortek Gen-2 (CN) Header. */
    public static String decodeGen2Header(byte[] field) throws FieldParseException
    {
        log.debug("Nortek Generation 2 field='" + new String(field) + "', len=" + field.length);
        StringBuilder header = new StringBuilder();

        // first 8 chars after signature (idx 3..10)
        for (int idx = 3; idx < 11; idx++)
        {
            header.append((char) field[idx]);
        }
        header.append('\n');

        int x;
        x = PseudoBinary.decodePB(new String(field, 12, 1), false); // Instrument type
        header.append(I1.format(x)).append(", ");
        x = PseudoBinary.decodePB(new String(field, 13, 4), false); // Head ID
        header.append(I6.format(x)).append(", ");
        x = PseudoBinary.decodePB(new String(field, 17, 1), false); // No of beams
        int beamNo = x;
        header.append(I1.format(x)).append(", ");
        x = PseudoBinary.decodePB(new String(field, 18, 2), false); // No of cells
        header.append(I2.format(x)).append(", ");

        double blankingDistance = PseudoBinary.decodePB(new String(field, 20, 2), false) * .01; // m
        header.append(TextUtil.setLengthRightJustify(DP2.format(blankingDistance), 5)).append(", ");
        double cellSize = PseudoBinary.decodePB(new String(field, 22, 2), false) * .01; // m
        header.append(TextUtil.setLengthRightJustify(DP2.format(cellSize), 5)).append(", ");

        x = PseudoBinary.decodePB(new String(field, 24, 1), false); // Coordinate System
        header.append(I1.format(x)).append('\n');

        x = PseudoBinary.decodePB(new String(field, 25, 3), false); // Date
        header.append(I6.format(x)).append(", ");
        x = PseudoBinary.decodePB(new String(field, 28, 3), false); // Time
        header.append(I6.format(x)).append(", ");
        x = PseudoBinary.decodePB(new String(field, 31, 3), false); // Error code
        header.append(I6.format(x)).append(", ");

        x = PseudoBinary.decodePB(new String(field, 34, 4), false); // Status code (hex reshape)
        String y = Integer.toHexString(x);
        String firstFour = y.length() >= 4 ? y.substring(0, 4) : String.format("%-4s", y).replace(' ', '0');
        char lastChar = y.charAt(y.length() - 1);
        y = (firstFour + "000" + lastChar).toUpperCase();
        header.append(y).append(", ");

        double d;
        d = PseudoBinary.decodePB(new String(field, 38, 2), false) * .1;  // Battery Voltage
        header.append(TextUtil.setLengthRightJustify(DP1.format(d), 4)).append(", ");
        d = PseudoBinary.decodePB(new String(field, 40, 3), false) * .1;   // Sound Speed
        header.append(TextUtil.setLengthRightJustify(DP1.format(d), 6)).append(", ");
        d = PseudoBinary.decodePB(new String(field, 43, 2), false) * .01;  // Heading Std Dev
        header.append(TextUtil.setLengthRightJustify(DP2.format(d), 5)).append(", ");
        d = PseudoBinary.decodePB(new String(field, 45, 2), false) * .1;   // Heading
        header.append(TextUtil.setLengthRightJustify(DP1.format(d), 5)).append(", ");
        d = PseudoBinary.decodePB(new String(field, 47, 2), true) * .1;    // Pitch
        header.append(TextUtil.setLengthRightJustify(DP1.format(d), 5)).append(", ");
        d = PseudoBinary.decodePB(new String(field, 49, 2), false) * .01;  // Pitch Std Dev
        header.append(TextUtil.setLengthRightJustify(DP2.format(d), 5)).append(", ");
        d = PseudoBinary.decodePB(new String(field, 51, 2), true) * .1;    // Roll
        header.append(TextUtil.setLengthRightJustify(DP1.format(d), 5)).append(", ");
        d = PseudoBinary.decodePB(new String(field, 53, 2), false) * .01;  // Roll Std Dev
        header.append(TextUtil.setLengthRightJustify(DP2.format(d), 5)).append(", ");
        d = PseudoBinary.decodePB(new String(field, 55, 3), false) * .001; // Pressure
        header.append(TextUtil.setLengthRightJustify(DP3.format(d), 7)).append(", ");
        d = PseudoBinary.decodePB(new String(field, 58, 2), false) * .01;  // Pressure Std Dev
        header.append(TextUtil.setLengthRightJustify(DP2.format(d), 5)).append(", ");
        d = PseudoBinary.decodePB(new String(field, 60, 3), true) * .01;   // Temperature
        header.append(TextUtil.setLengthRightJustify(DP2.format(d), 6)).append('\n');
        log.debug("Nortek Generation 2 header '" + header + "'");
        return header.toString();
    }

    /** Nortek Gen-2 (CN) Body. */
    public static String decodeGen2Body(byte[] field) throws FieldParseException
    {
        // Read beams
        int beamNo = PseudoBinary.decodePB(new String(field, 17, 1), false);

        // Check for profile presence
        if (field.length < 64)
        {
            throw new FieldParseException("No profile message in data");
        }

        // Find '+' marker and compute countProfile
        int plusAt = -1;
        for (int i = 64; i < field.length; i++)
        {
            if (field[i] == '+')
            {
                plusAt = i; break;
            }
        }
        int countProfile = (plusAt - 64) / beamNo;
        if (countProfile != 5 && countProfile != 7)
        {
            throw new FieldParseException("Cannot identify message type");
        }

        // Distances
        double blankingDistance = PseudoBinary.decodePB(new String(field, 20, 2), false) * .01;
        double cellSize         = PseudoBinary.decodePB(new String(field, 22, 2), false) * .01;

        // Determine loopLimit from beamNo & countProfile
        int loopLimit = 0;
        if ((beamNo == 2 && countProfile == 7) || (beamNo == 3 && countProfile == 5))       loopLimit = 6;
        else if (beamNo == 2 && countProfile == 5)                                          loopLimit = 4;
        else if (beamNo == 3 && countProfile == 7)                                          loopLimit = 9;

        StringBuilder result = new StringBuilder();
        String strResult;
        int fieldIndex;
        int cellNumber = 1;

        for (fieldIndex = 64; fieldIndex < field.length - 1; fieldIndex++)
        {
            result.append(String.format("%2d", cellNumber)).append(",");
            result.append(" ").append(TextUtil.setLengthRightJustify(
                    DP1.format(blankingDistance + cellSize * cellNumber++), 6)).append(",");

            for (int i = 0; i < loopLimit; i++)
            {
                if (fieldIndex > field.length - 2) break;

                String temp = "";
                temp += (char) field[fieldIndex++];
                temp += (char) field[fieldIndex++];
                if (i < beamNo) temp += (char) field[fieldIndex++];

                int number = (i <= beamNo - 1) ? PseudoBinary.decodePB(temp, true)
                                               : PseudoBinary.decodePB(temp, false);

                double dnum = (i <= beamNo - 1) ? number * 0.001
                              : (i > beamNo - 1 && i <= 2 * beamNo - 1) ? number * 0.1
                              : number;

                // Format selection based on position and message type
                if (i <= beamNo - 1)
                {
                    strResult = D3.format(dnum);
                }
                else if (i > beamNo - 1 && i <= 2 * beamNo - 1 && countProfile == 7)
                {
                    strResult = D1.format(dnum);
                }
                else if (countProfile == 7)
                {
                    strResult = String.valueOf((int) dnum);
                }
                else
                {
                    strResult = D1.format(dnum);
                }

                String pad = "";
                int padLen = (i <= beamNo - 1) ? 10 : 8;
                while (strResult.length() + pad.length() < padLen) pad += " ";

                result.append(pad).append(strResult).append(',');
            }

            // replace trailing comma with newline
            result.setLength(result.length() - 1);
            result.append('\n');
        }
        return result.toString();
    }
}
