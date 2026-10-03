package decodes.tsdb.algo;

import decodes.db.Site;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DbCompException;
import decodes.tsdb.ParmRef;
import ilex.var.NamedVariable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Calculates daily Hamon evaporation from air temperature and site latitude.
 * The monthly properties provide the local adjustment coefficient.
 */
public class HamonMethod extends AW_AlgorithmBase {
   public double input;
   String[] _inputNames = new String[]{"input"};
   public NamedVariable output = new NamedVariable("output", 0);
   String[] _outputNames = new String[]{"output"};
   public double jan = (double)0.0F;
   public double feb = (double)0.0F;
   public double mar = (double)0.0F;
   public double apr = (double)0.0F;
   public double may = (double)0.0F;
   public double jun = (double)0.0F;
   public double jul = (double)0.0F;
   public double aug = (double)0.0F;
   public double sep = (double)0.0F;
   public double oct = (double)0.0F;
   public double nov = (double)0.0F;
   public double dec = (double)0.0F;
   String[] _propertyNames = new String[]{"jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"};

   public HamonMethod() {
    
   }

   protected void initAWAlgorithm() throws DbCompException {
      this._awAlgoType = AWAlgoType.TIME_SLICE;
   }

   protected void beforeTimeSlices() throws DbCompException {
   }

   protected void doAWTimeSlice() throws DbCompException {
      try {
         ParmRef inputParm = this.getParmRef("input");
         CTimeSeries inputTimeSeries = inputParm.timeSeries;
         Site site = inputTimeSeries.getTimeSeriesIdentifier().getSite();

         // Hamon uses the Julian day to estimate solar declination and daylight length.
         String dayOfYearText = (new SimpleDateFormat("DDD HH:MM:SS yyyy", Locale.US)).format(this._timeSliceBaseTime).substring(0, 3);
         Calendar calendar = Calendar.getInstance();
         calendar.setTime(this._timeSliceBaseTime);
         int month = calendar.get(2);
         int dayOfYear = Integer.parseInt(dayOfYearText);
         String latitudeText = site.latitude;
         double latitudeDegrees = (double)0.0F;

         try {
            latitudeDegrees = Double.parseDouble(latitudeText);
         } catch (NumberFormatException exception) {
            System.out.println("Latitude is not a decimal number. Please convert latitude for site " + site.getDisplayName() + " to a decimal number");
         }

         // Solar declination and sunset hour angle are expressed in radians.
         double solarDeclinationRadians = 0.4093 * Math.sin(0.01721420632103996 * (double)dayOfYear - 1.405);
         double sunsetHourAngleRadians = Math.acos((double)-1.0F * Math.tan(Math.toRadians(latitudeDegrees)) * Math.tan(solarDeclinationRadians));
         double daylightHours = 7.639437268410976 * sunsetHourAngleRadians;

         // Convert air temperature to saturation vapor pressure and density.
         double saturationVaporPressureKpa = 0.6108 * Math.pow(Math.E, 17.27 * this.input / (237.3 + this.input));
         double temperatureKelvin = this.input + 273.15;
         double saturationVaporDensity = 2166.74 * (saturationVaporPressureKpa / temperatureKelvin);

         // This is the unadjusted Hamon estimate before the monthly coefficient.
         double baseHamonEvaporation = 0.55 * Math.pow(daylightHours / (double)12.0F, (double)2.0F) * (saturationVaporDensity / (double)100.0F);
         double monthlyCoefficient = (double)0.0F;

         // Apply the coefficient configured for the time slice's calendar month.
         switch (month) {
            case 0:
               monthlyCoefficient = this.jan;
               break;
            case 1:
               monthlyCoefficient = this.feb;
               break;
            case 2:
               monthlyCoefficient = this.mar;
               break;
            case 3:
               monthlyCoefficient = this.apr;
               break;
            case 4:
               monthlyCoefficient = this.may;
               break;
            case 5:
               monthlyCoefficient = this.jun;
               break;
            case 6:
               monthlyCoefficient = this.jul;
               break;
            case 7:
               monthlyCoefficient = this.aug;
               break;
            case 8:
               monthlyCoefficient = this.sep;
               break;
            case 9:
               monthlyCoefficient = this.oct;
               break;
            case 10:
               monthlyCoefficient = this.nov;
               break;
            case 11:
               monthlyCoefficient = this.dec;
         }

         this.setOutput(this.output, baseHamonEvaporation * monthlyCoefficient);
      } catch (Exception exception) {
         exception.printStackTrace();
      }

   }

   protected void afterTimeSlices() throws DbCompException {
   }

   public String[] getInputNames() {
      return this._inputNames;
   }

   public String[] getOutputNames() {
      return this._outputNames;
   }

   public String[] getPropertyNames() {
      return this._propertyNames;
   }
}
 
