package decodes.hdb.algo;

import java.util.Date;

import ilex.var.NamedVariableList;
import ilex.var.NamedVariable;
import decodes.tsdb.DbAlgorithmExecutive;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.VarFlags;
// this new import was added by M. Bogner Aug 2012 for the 3.0 CP upgrade project
import decodes.tsdb.algo.AWAlgoType;


import decodes.hdb.dbutils.*;
import decodes.hdb.HdbFlags;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import org.opendcs.annotations.PropertySpec;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;

@Algorithm(description = "Takes up to 5 input values labeled input1 ... input5.\n" +
"The callproc property will have all the proper procedure call elements already established, \n" +
"so all this program has to do is call the procedure.\n" +
 "The <input1> ... <input5> and <tsbt> can be used if you want to use the procedure call dynamically.")
public class CallProcAlg extends decodes.tsdb.algo.AW_AlgorithmBase
{
	@Input
	public double input1;
	@Input
	public double input2;
	@Input
	public double input3;
	@Input
	public double input4;
	@Input
	public double input5;

    boolean do_setoutput = true;

	@Output(type = Double.class)
	public NamedVariable output = new NamedVariable("output", 0);
	
	@PropertySpec(value = "ignore")
	public String input1_MISSING = "ignore";
	@PropertySpec(value = "ignore")
	public String input2_MISSING = "ignore";
	@PropertySpec(value = "ignore")
	public String input3_MISSING = "ignore";
	@PropertySpec(value = "ignore")
	public String input4_MISSING = "ignore";
	@PropertySpec(value = "ignore")
	public String input5_MISSING = "ignore";
	@PropertySpec(value = "")
    public String proccall = "";

	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	@Override
	protected void initAWAlgorithm( )
	{
		_awAlgoType = AWAlgoType.TIME_SLICE;
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
	@Override
	protected void beforeTimeSlices()
	{
	}

	/**
	 * Do the algorithm for a single time slice.
	 * AW will fill in user-supplied code here.
	 * Base class will set inputs prior to calling this method.
	 * User code should call one of the setOutput methods for a time-slice
	 * output variable.
	 *
	 * @throw DbCompException (or subclass thereof) if execution of this
	 *        algorithm is to be aborted.
	 */
	@Override
	protected void doAWTimeSlice()
		throws DbCompException
	{
	   SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm");
           String new_proccall = proccall.replaceAll("<<INPUT","<<input");
           String tsbt_time = "to_date('" + sdf.format(_timeSliceBaseTime) + "','dd-mon-yyyy HH24:mi')";
		new_proccall = new_proccall.replaceAll("<<tsbt>>",tsbt_time);
		if (!isMissing(input1))
		{
			new_proccall = new_proccall.replaceAll("<<input1>>",(Double.valueOf(input1)).toString());
		}
		if (!isMissing(input2))
		{
			new_proccall = new_proccall.replaceAll("<<input2>>",(Double.valueOf(input2)).toString());
		}
		if (!isMissing(input3))
		{
			new_proccall = new_proccall.replaceAll("<<input3>>",(Double.valueOf(input3)).toString());
		}
		if (!isMissing(input4))
		{
			new_proccall = new_proccall.replaceAll("<<input4>>",(Double.valueOf(input4)).toString());
		}
		if (!isMissing(input5))
		{
			new_proccall = new_proccall.replaceAll("<<input5>>",(Double.valueOf(input5)).toString());
		}
		debug3("doAWTimeSlice input1=" + input1 +", input2=" + input2);

		do_setoutput = true;
		if (do_setoutput)
		//           Then continue with the calling of the Procedure
        {
             // get the connection and a few other classes so we can do some sql
             try(Connection conn = tsdb.getConnection())
			 {
             	DBAccess db = new DBAccess(conn);
				DataObject dbobj = new DataObject();
				String dt_fmt = "dd-MMM-yyyy HH:mm";
				// now do the procedure call with all the needed data
				db.callProc(new_proccall,dbobj);
				debug3(" Proc Call  STRING:" + new_proccall + "   DBOBJ: " + dbobj.toString() );
			 }
			 catch (SQLException ex)
			 {
				throw new DbCompException("Unable to get connection.", ex);
			 }
	    }

// 	    procedure call is expected to do everything so just return
            return;
//
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
        @Override
	protected void afterTimeSlices()
	{
	}
}
