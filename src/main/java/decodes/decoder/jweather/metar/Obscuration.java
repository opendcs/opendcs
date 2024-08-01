/*
jWeather(TM) is a Java library for parsing raw weather data
Copyright (C) 2004 David Castro

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

For more information, please email arimus@users.sourceforge.net
*/
package decodes.decoder.jweather.metar;

/**
 * Represents a single obscuration element that appears in a METAR report
 * @author David Castro, dcastro@apu.edu
 * @version $Revision: 1.3 $
 */
public class Obscuration {
	private String phenomena = null;
  protected String decodedPhenomena = null;
	private String contraction = null;
  protected String decodedContraction = null;
	private int height = 0;

	// intensity flags
	private boolean isSlight = false;
	private boolean isModerate = false;
	private boolean isHeavy = false;
	private boolean isSevere = false;

	// obscuration type flags
	private boolean isMist  = false;
	private boolean isFog   = false;
	private boolean isSmoke = false;
	private boolean isVolcanicAsh = false;
	private boolean isDust  = false;
	private boolean isSand  = false;
	private boolean isHaze  = false;
	private boolean isSpray = false;

	public Obscuration() {
	}

    /**
     *
     * @param phenomena the part of a METAR obscuration token which represents
     * a specific type of phenomena (e.g. 'FG', 'HZ')
     */
	protected void setPhenomena(String phenomena) {
		this.phenomena = phenomena;

		if (phenomena.equals(MetarConstants.METAR_MIST)) {
			isMist  = true;
      decodedPhenomena = MetarConstants.METAR_DECODED_MIST;
		} else if (phenomena.equals(MetarConstants.METAR_FOG)) {
			isFog   = true;
      decodedPhenomena = MetarConstants.METAR_DECODED_FOG;
		} else if (phenomena.equals(MetarConstants.METAR_SMOKE)) {
			isSmoke = true;
      decodedPhenomena = MetarConstants.METAR_DECODED_SMOKE;
		} else if (phenomena.equals(MetarConstants.METAR_VOLCANIC_ASH)) {
			isVolcanicAsh = true;
      decodedPhenomena = MetarConstants.METAR_DECODED_VOLCANIC_ASH;
		} else if (phenomena.equals(MetarConstants.METAR_DUST_STORM)) {
			isDust  = true;
      decodedPhenomena = MetarConstants.METAR_DECODED_DUST_STORM;
		} else if (phenomena.equals(MetarConstants.METAR_SAND)) {
			isSand  = true;
      decodedPhenomena = MetarConstants.METAR_DECODED_SAND;
		} else if (phenomena.equals(MetarConstants.METAR_HAZE)) {
			isHaze  = true;
      decodedPhenomena = MetarConstants.METAR_DECODED_HAZE;
		} else if (phenomena.equals(MetarConstants.METAR_SPRAY)) {
			isSpray = true;
      decodedPhenomena = MetarConstants.METAR_DECODED_SPRAY;
		}
	}

    /**
     *
     * @param contraction the part of a METAR obscuration token which represents
     * a contraction for the phenomena (e.g. 'FEW', 'SCT')
     */
	protected void setContraction(String contraction) {
		this.contraction = contraction;

		if (contraction.equals(MetarConstants.METAR_FEW)) {
			isSlight = true;
      decodedContraction = MetarConstants.METAR_DECODED_SLIGHT;
		} else if (contraction.equals(MetarConstants.METAR_SCATTERED)) {
			isModerate = true;
      decodedContraction = MetarConstants.METAR_DECODED_MODERATE;
		} else if (contraction.equals(MetarConstants.METAR_BROKEN)) {
			isHeavy = true;
      decodedContraction = MetarConstants.METAR_DECODED_HEAVY;
		} else if (contraction.equals(MetarConstants.METAR_OVERCAST)) {
			isSevere = true;
      decodedContraction = MetarConstants.METAR_DECODED_SEVERE;
		}
	}

	public boolean isSlight() {
		return isSlight;
	}

	public boolean isModerate () {
		return isModerate;
	}

	public boolean isHeavy () {
		return isHeavy;
	}

	public boolean isSevere () {
		return isSevere;
	}

	public boolean isMist () {
		return isMist;
	}

	public boolean isFog () {
		return isFog;
	}

	public boolean isSmoke () {
		return isSmoke;
	}

	public boolean isVolcanicAsh () {
		return isVolcanicAsh;
	}

	public boolean isDust () {
		return isDust;
	}

	public boolean isSand () {
		return isSand;
	}

	public boolean isHaze () {
		return isHaze;
	}

	public boolean isSpray () {
		return isSpray;
	}

    /**
     *
     * @param height the part of a METAR obscuration token which represents
     * the height of the phenomena (in hundreds of feet)
     */
	protected void setHeight(int height) {
		this.height = height * 100; // for hundreds of feet
	}

    /**
     *
     * @return a string that represents the obscuration in natural language
     */
	public String getNaturalLanguageString() {
		String temp = "";

		if (isSlight) {
			temp += "Slight";
		} else if (isModerate) {
			temp += "Moderate";
		} else if (isHeavy) {
			temp += "Heavy";
		} else if (isSevere) {
			temp += "Severe";
		}

		if (isMist) {
			temp += " Mist";
		} else if (isFog) {
			temp += " Fog";
		} else if (isSmoke) {
			temp += " Smoke";
		} else if (isVolcanicAsh) {
			temp += " Volcanic Ash";
		} else if (isDust) {
			temp += " Dust";
		} else if (isSand) {
			temp += " Sand";
		} else if (isHaze) {
			temp += " Haze";
		} else if (isSpray) {
			temp += " Spray";
		}

		if (height != 0) {
			temp += " at " + height + " feet";
		} else {
			temp += " at ground level";
		}

		return temp;
	}
}
