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
import org.opendcs.annotations.PropertySpec;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;

@Algorithm(description = "Computes the Powell inflow from the three upstream gages and a side inflow\n" +
"coefficient. If any gages are missing, estimates inflow using mass balance\n" + 
"and an assumed delta bankstorage coefficient.\n\n" +

"This algorithm does not actually write to delta bank storage, but the resulting\n" +
"estimated inflow will result in a delta bank storage that matches the value\n" +
"used here.\n\n" +

"Inputs:\n" +
"bffu = San Juan River at Bluff\n" +
"clru = Colorado River at Cisco\n" +
"grvu = Green River at Green River, UT\n" +
"evap = Powell Evaporation\n" +
"delta_storage = Powell change in storage\n" +
"total_release = Powell total release volume\n\n" +

"Output:\n" +
"inflow = Powell total inflow")
public class EstGLDAInflow extends decodes.tsdb.algo.AW_AlgorithmBase
{
	@Input
	public double bffu;
	@Input
	public double clru;
	@Input
	public double grvu;
	@Input
	public double evap;
	@Input
	public double delta_storage;
	@Input
	public double total_release;
	@Input
	public double inflowCoeff;

	@Output(type = Double.class)
	public NamedVariable inflow = new NamedVariable("inflow", 0);

	@PropertySpec(value = "ignore") 
	public String bffu_MISSING = "ignore";
	@PropertySpec(value = "ignore") 
	public String clru_MISSING = "ignore";
	@PropertySpec(value = "ignore") 
	public String grvu_MISSING = "ignore";
	@PropertySpec(value = "fail") 
	public String evap_MISSING = "fail";
	@PropertySpec(value = "fail") 
	public String delta_storage_MISSING = "fail";
	@PropertySpec(value = "fail") 
	public String total_release_MISSING = "fail";
	@PropertySpec(value = "ignore") 
	public String inflowCoeff_MISSING = "ignore";
	@PropertySpec(value = "0.04") 
	public double bscoeff = 0.04;
	

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
	 * @throws DbCompException (or subclass thereof) if execution of this
	 *        algorithm is to be aborted.
	 */
	@Override
	protected void doAWTimeSlice()
		throws DbCompException
	{
		if (!(isMissing(bffu) || isMissing(clru) || isMissing(grvu))) {
			// all inflow values are present, no estimate necessary
			inflowCoeff = getCoeff("inflowCoeff");
			double in = bffu + clru + grvu;
			in += in * inflowCoeff;
			
			debug3("doAWTimeSlice bffu=" + bffu + ", clru=" + clru +
			 ", gvru=" + grvu + ", inflowCoeff=" + inflowCoeff + ", inflow=" + in);
			setOutput(inflow, in);
			return;
		}
		else {
			//one or more of the gages is missing, do an estimate
			debug1("GLDA Estimated Inflow computation entered for " + _timeSliceBaseTime);
			double dBS = delta_storage * bscoeff;
			double invol = delta_storage + dBS + evap + total_release;
			double in = invol * 43560/86400; //convert to cfs
			
			debug3("doAWTimeSlice Estimated Inflow! dBS=" + dBS + ", invol=" + invol + ", in=" + in);
			setHdbDerivationFlag(inflow, "E");
			setOutput(inflow, in);
			return;
		}
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	@Override
	protected void afterTimeSlices()
	{
	}
}
