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
 * Represents a single sky condition element that appears in a METAR report
 * @author David Castro, dcastro@apu.edu
 * @version $Revision: 1.6 $
 */
public class SkyCondition {
	private String contraction = null;
  private String decodedContraction = null;
	private String modifier = null;
  private String decodedModifier = null;
	private int height = 0;

	private boolean isVerticalVisibility = false;
	private boolean isClear = false;
	private boolean isFewClouds = false;
	private boolean isScatteredClouds = false;
	private boolean isBrokenClouds = false;
	private boolean isOvercast = false;

	private boolean isNoSignificantClouds = false;

	private boolean isCumulonimbus = false;
	private boolean isToweringCumulus = false;

	public SkyCondition() {
	}

    /**
     *
     * @param contraction the part of a METAR sky condition token which represents
     * a contraction for the sky condition (e.g. 'FEW', 'SCT')
     */
	protected void setContraction(String contraction) {
		this.contraction = contraction;
		if (contraction.equals(MetarConstants.METAR_VERTICAL_VISIBILITY)) {
			isVerticalVisibility = true;
      decodedContraction = MetarConstants.METAR_DECODED_VERTICAL_VISIBILITY;
		} else if (contraction.equals(MetarConstants.METAR_SKY_CLEAR)) {
			isClear = true;
      decodedContraction = MetarConstants.METAR_DECODED_SKY_CLEAR;
		} else if (contraction.equals(MetarConstants.METAR_CLEAR)) {
			isClear = true;
      decodedContraction = MetarConstants.METAR_DECODED_CLEAR;
		} else if (contraction.equals(MetarConstants.METAR_FEW)) {
			isFewClouds = true;
      decodedContraction = MetarConstants.METAR_DECODED_FEW;
		} else if (contraction.equals(MetarConstants.METAR_SCATTERED)) {
			isScatteredClouds = true;
      decodedContraction = MetarConstants.METAR_DECODED_SCATTERED;
		} else if (contraction.equals(MetarConstants.METAR_BROKEN)) {
			isBrokenClouds = true;
      decodedContraction = MetarConstants.METAR_DECODED_BROKEN;
		} else if (contraction.equals(MetarConstants.METAR_OVERCAST)) {
			isOvercast = true;
      decodedContraction = MetarConstants.METAR_DECODED_OVERCAST;
		} else if (contraction.equals(MetarConstants.METAR_NO_SIGNIFICANT_CLOUDS)) {
			isNoSignificantClouds = true;
		}
	}

    /**
     *
     * @param modifier the part of a METAR sky condition token which represents
     * a modifier used to specify if the sky condition is of a certain type
     */
	protected void setModifier(String modifier) {
		this.modifier = modifier;
		if (modifier.equals(MetarConstants.METAR_CUMULONIMBUS)) {
			isCumulonimbus = true;
      decodedModifier = MetarConstants.METAR_DECODED_CUMULONIMBUS;
		} else if (modifier.equals(MetarConstants.METAR_TOWERING_CUMULUS)) {
			isToweringCumulus = true;
      decodedModifier = MetarConstants.METAR_DECODED_TOWERING_CUMULONIMBUS;
		}
	}

    /**
	 * get the part of a METAR sky condition token which represents a modifier
	 * used to specify if the sky condition is of a certain type
     */
	public String getModifier() {
		return this.modifier;
	}

    /**
     *
     * @param height the part of a METAR sky condition token which represents
     * the height of the sky condition (in hundreds of feet)
     */
	protected void setHeight(int height) {
		this.height = height * 100; // for hundreds of feet
	}

    /**
     * get the part of a METAR sky condition token which represents
     * the height of the sky condition (in hundreds of feet)
     */
	public int getHeight() {
		return height; // in hundreds of feet
	}

	public boolean isVerticalVisibility() {
		return isVerticalVisibility;
	}

	public boolean isClear() {
		return isClear;
	}

	public boolean isFewClouds() {
		return isFewClouds;
	}

	public boolean isScatteredClouds() {
		return isScatteredClouds;
	}

	public boolean isBrokenClouds() {
		return isBrokenClouds;
	}

	public boolean isOvercast() {
		return isOvercast;
	}

	public boolean isCumulonimbus() {
		return isCumulonimbus;
	}

	public boolean isToweringCumulus() {
		return isToweringCumulus;
	}

	public boolean isNoSignificantClouds() {
		return isNoSignificantClouds;
	}

    /**
     *
     * @return a string that represents the sky condition in natural language
     */
	public String getNaturalLanguageString() {
		String temp = "";

		if (isVerticalVisibility) {
			return "Vertical Visibility of " + height + "feet";
		} else if (isClear) {
			return "Clear skies";
		} else if (isClear) {
			return "Clear skies";
		} else if (isFewClouds) {
			temp += "Few clouds";
		} else if (isScatteredClouds) {
			temp += "Scattered clouds";
		} else if (isBrokenClouds) {
			temp += "Broken clouds";
		} else if (isOvercast) {
			temp += "Overcast";
		} else if (isNoSignificantClouds) {
			temp += "No Significant Clouds";
		} else {
			temp += contraction;
		}

		temp += " at " + height + " feet";

		if (isCumulonimbus) {
			temp += " (cumulonimbus)";
		} else if (isToweringCumulus) {
			temp += " (towering cumulus)";
		} else if ((modifier != null) && !modifier.equals("")) {
			temp += "("+modifier+")";
		}

		return temp;
	}
  
  /**
   * 
   * @return
   */
  public String getContraction() {
    return contraction;
  }

  /**
   * 
   * @return
   */
  public String getDecodedContraction() {
    return decodedContraction;
  }

  /**
   * 
   * @return
   */
  public String getDecodedModifier() {
    return decodedModifier;
  }

  /**
   * 
   * @param string
   */
  public void setDecodedContraction(String string) {
    decodedContraction = string;
  }

  /**
   * 
   * @param string
   */
  public void setDecodedModifier(String string) {
    decodedModifier = string;
  }

}
