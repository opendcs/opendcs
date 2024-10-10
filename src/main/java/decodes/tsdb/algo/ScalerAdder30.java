package decodes.tsdb.algo;

import org.opendcs.annotations.PropertySpec;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;

import ilex.var.NamedVariable;
import decodes.tsdb.DbCompException;

@Algorithm(description = "Takes up to 30 input values labeled input1 ... input30. Multiplies\n" + //
        "them by coefficients supplied in properties coeff1 ... coeff30.\n" + //
        "Adds them together and produces a single output labeled 'output'.\n" + //
        "Values not assigned by computation are ignored.\n" + //
        "All coefficients default to 1.0 if not supplied.\n" + //
        "\n" + //
        "")
public class ScalerAdder30 extends AW_AlgorithmBase
{
    @Input public double input1;
    @Input public double input2;
    @Input public double input3;
    @Input public double input4;
    @Input public double input5;
    @Input public double input6;
    @Input public double input7;
    @Input public double input8;
    @Input public double input9;
    @Input public double input10;
    @Input public double input11;
    @Input public double input12;
    @Input public double input13;
    @Input public double input14;
    @Input public double input15;
    @Input public double input16;
    @Input public double input17;
    @Input public double input18;
    @Input public double input19;
    @Input public double input20;
    @Input public double input21;
    @Input public double input22;
    @Input public double input23;
    @Input public double input24;
    @Input public double input25;
    @Input public double input26;
    @Input public double input27;
    @Input public double input28;
    @Input public double input29;
    @Input public double input30;

    @Output(type = Double.class)
    public NamedVariable output = new NamedVariable("output", 0);

//AW:PROPERTIES
    @PropertySpec(value = "1.0") public double coeff1 = 1.0;
    @PropertySpec(value = "1.0") public double coeff2 = 1.0;
    @PropertySpec(value = "1.0") public double coeff3 = 1.0;
    @PropertySpec(value = "1.0") public double coeff4 = 1.0;
    @PropertySpec(value = "1.0") public double coeff5 = 1.0;
    @PropertySpec(value = "1.0") public double coeff6 = 1.0;
    @PropertySpec(value = "1.0") public double coeff7 = 1.0;
    @PropertySpec(value = "1.0") public double coeff8 = 1.0;
    @PropertySpec(value = "1.0") public double coeff9 = 1.0;
    @PropertySpec(value = "1.0") public double coeff10 = 1.0;
    @PropertySpec(value = "1.0") public double coeff11 = 1.0;
    @PropertySpec(value = "1.0") public double coeff12 = 1.0;
    @PropertySpec(value = "1.0") public double coeff13 = 1.0;
    @PropertySpec(value = "1.0") public double coeff14 = 1.0;
    @PropertySpec(value = "1.0") public double coeff15 = 1.0;
    @PropertySpec(value = "1.0") public double coeff16 = 1.0;
    @PropertySpec(value = "1.0") public double coeff17 = 1.0;
    @PropertySpec(value = "1.0") public double coeff18 = 1.0;
    @PropertySpec(value = "1.0") public double coeff19 = 1.0;
    @PropertySpec(value = "1.0") public double coeff20 = 1.0;
    @PropertySpec(value = "1.0") public double coeff21 = 1.0;
    @PropertySpec(value = "1.0") public double coeff22 = 1.0;
    @PropertySpec(value = "1.0") public double coeff23 = 1.0;
    @PropertySpec(value = "1.0") public double coeff24 = 1.0;
    @PropertySpec(value = "1.0") public double coeff25 = 1.0;
    @PropertySpec(value = "1.0") public double coeff26 = 1.0;
    @PropertySpec(value = "1.0") public double coeff27 = 1.0;
    @PropertySpec(value = "1.0") public double coeff28 = 1.0;
    @PropertySpec(value = "1.0") public double coeff29 = 1.0;
    @PropertySpec(value = "1.0") public double coeff30 = 1.0;
    @PropertySpec(value = "ignore") public String input1_MISSING = "ignore";
    @PropertySpec(value = "ignore") public String input2_MISSING = "ignore";
    @PropertySpec(value = "ignore") public String input3_MISSING = "ignore";
    @PropertySpec(value = "ignore") public String input4_MISSING = "ignore";
    @PropertySpec(value = "ignore") public String input5_MISSING = "ignore";
    @PropertySpec(value = "ignore") public String input6_MISSING = "ignore";
    @PropertySpec(value = "ignore") public String input7_MISSING = "ignore";
    @PropertySpec(value = "ignore") public String input8_MISSING = "ignore";
    @PropertySpec(value = "ignore") public String input9_MISSING = "ignore";
    @PropertySpec(value = "ignore") public String input10_MISSING = "ignore";
    @PropertySpec(value = "ignore") public String input11_MISSING = "ignore";
    @PropertySpec(value = "ignore") public String input12_MISSING = "ignore";
    @PropertySpec(value = "ignore") public String input13_MISSING = "ignore";
    @PropertySpec(value = "ignore") public String input14_MISSING = "ignore";
    @PropertySpec(value = "ignore") public String input15_MISSING = "ignore";
    @PropertySpec(value = "ignore") public String input16_MISSING = "ignore";
    @PropertySpec(value = "ignore") public String input17_MISSING = "ignore";
    @PropertySpec(value = "ignore") public String input18_MISSING = "ignore";
    @PropertySpec(value = "ignore") public String input19_MISSING = "ignore";
    @PropertySpec(value = "ignore") public String input20_MISSING = "ignore";
    @PropertySpec(value = "ignore") public String input21_MISSING = "ignore";
    @PropertySpec(value = "ignore") public String input22_MISSING = "ignore";
    @PropertySpec(value = "ignore") public String input23_MISSING = "ignore";
    @PropertySpec(value = "ignore") public String input24_MISSING = "ignore";
    @PropertySpec(value = "ignore") public String input25_MISSING = "ignore";
    @PropertySpec(value = "ignore") public String input26_MISSING = "ignore";
    @PropertySpec(value = "ignore") public String input27_MISSING = "ignore";
    @PropertySpec(value = "ignore") public String input28_MISSING = "ignore";
    @PropertySpec(value = "ignore") public String input29_MISSING = "ignore";
    @PropertySpec(value = "ignore") public String input30_MISSING = "ignore";

    @Override
    protected void initAWAlgorithm( ) throws DbCompException
    {
        _awAlgoType = AWAlgoType.TIME_SLICE;
    }

    /**
     * This method is called once before iterating all time slices.
     */
    @Override
    protected void beforeTimeSlices() throws DbCompException
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
    protected void doAWTimeSlice() throws DbCompException
    {
        double tally = 0.0;
        if (!isMissing(input1))
            tally += (input1 * coeff1);
        if (!isMissing(input2))
            tally += (input2 * coeff2);
        if (!isMissing(input3))
            tally += (input3 * coeff3);
        if (!isMissing(input4))
            tally += (input4 * coeff4);
        if (!isMissing(input5))
            tally += (input5 * coeff5);
        if (!isMissing(input6))
            tally += (input6 * coeff6);
        if (!isMissing(input7))
            tally += (input7 * coeff7);
        if (!isMissing(input8))
            tally += (input8 * coeff8);
        if (!isMissing(input9))
            tally += (input9 * coeff9);
        if (!isMissing(input10))
            tally += (input10 * coeff10);
        if (!isMissing(input11))
            tally += (input11 * coeff11);
        if (!isMissing(input12))
            tally += (input12 * coeff12);
        if (!isMissing(input13))
            tally += (input13 * coeff13);
        if (!isMissing(input14))
            tally += (input14 * coeff14);
        if (!isMissing(input15))
            tally += (input15 * coeff15);
        if (!isMissing(input16))
            tally += (input16 * coeff16);
        if (!isMissing(input17))
            tally += (input17 * coeff17);
        if (!isMissing(input18))
            tally += (input18 * coeff18);
        if (!isMissing(input19))
            tally += (input19 * coeff19);
        if (!isMissing(input20))
            tally += (input20 * coeff20);
        if (!isMissing(input21))
            tally += (input21 * coeff21);
        if (!isMissing(input22))
            tally += (input22 * coeff22);
        if (!isMissing(input23))
            tally += (input23 * coeff23);
        if (!isMissing(input24))
            tally += (input24 * coeff24);
        if (!isMissing(input25))
            tally += (input25 * coeff25);
        if (!isMissing(input26))
            tally += (input26 * coeff26);
        if (!isMissing(input27))
            tally += (input27 * coeff27);
        if (!isMissing(input28))
            tally += (input28 * coeff28);
        if (!isMissing(input29))
            tally += (input29 * coeff29);
        if (!isMissing(input30))
            tally += (input30 * coeff30);
        debug3("doAWTimeSlice input1=" + input1 + ", coeff1=" + coeff1
        +", input2=" + input2 + ", coeff2=" + coeff2 + ", tally=" + tally);
        setOutput(output, tally);
    }

    /**
     * This method is called once after iterating all time slices.
     */
    @Override
    protected void afterTimeSlices() throws DbCompException
    {
    }
}
