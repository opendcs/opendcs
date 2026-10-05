// Source code is decompiled from a .class file using FernFlower decompiler (from Intellij IDEA).
package decodes.tsdb.algo;

import decodes.db.Site;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DbCompException;
import decodes.tsdb.ParmRef;
import ilex.var.NamedVariable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

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
         ParmRef var1 = this.getParmRef("input");
         CTimeSeries var2 = var1.timeSeries;
         Site var3 = var2.getTimeSeriesIdentifier().getSite();
         String var4 = (new SimpleDateFormat("DDD HH:MM:SS yyyy", Locale.US)).format(this._timeSliceBaseTime).substring(0, 3);
         Calendar var5 = Calendar.getInstance();
         var5.setTime(this._timeSliceBaseTime);
         int var6 = var5.get(2);
         int var7 = Integer.parseInt(var4);
         String var8 = var3.latitude;
         double var9 = (double)0.0F;

         try {
            var9 = Double.parseDouble(var8);
         } catch (NumberFormatException var27) {
            System.out.println("Latitude is not a decimal number. Please convert latitude for site " + var3.getDisplayName() + " to a decimal number");
         }

         double var11 = 0.4093 * Math.sin(0.01721420632103996 * (double)var7 - 1.405);
         double var13 = Math.acos((double)-1.0F * Math.tan(Math.toRadians(var9)) * Math.tan(var11));
         double var15 = 7.639437268410976 * var13;
         double var17 = 0.6108 * Math.pow(Math.E, 17.27 * this.input / (237.3 + this.input));
         double var19 = this.input + 273.15;
         double var21 = 2166.74 * (var17 / var19);
         double var23 = 0.55 * Math.pow(var15 / (double)12.0F, (double)2.0F) * (var21 / (double)100.0F);
         double var25 = (double)0.0F;
         switch (var6) {
            case 0:
               var25 = this.jan;
            case 1:
               var25 = this.feb;
               break;
            case 2:
               var25 = this.mar;
               break;
            case 3:
               var25 = this.apr;
               break;
            case 4:
               var25 = this.may;
               break;
            case 5:
               var25 = this.jun;
               break;
            case 6:
               var25 = this.jul;
               break;
            case 7:
               var25 = this.aug;
               break;
            case 8:
               var25 = this.sep;
               break;
            case 9:
               var25 = this.oct;
               break;
            case 10:
               var25 = this.nov;
               break;
            case 11:
               var25 = this.dec;
         }

         this.setOutput(this.output, var23 * var25);
      } catch (Exception var28) {
         var28.printStackTrace();
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
 