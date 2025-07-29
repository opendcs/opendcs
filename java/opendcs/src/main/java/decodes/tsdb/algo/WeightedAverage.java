/**
 * Copyright 2024 The OpenDCS Consortium and contributors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package decodes.tsdb.algo;

import decodes.tsdb.DbCompException;
import decodes.tsdb.MissingAction;
import decodes.tsdb.ParmRef;
import ilex.var.NamedVariable;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;
import org.slf4j.LoggerFactory;

@Algorithm(description = "Calculate Weighted Average values (eg Water Temperature) using: sum(inputN*weightN) / weightTotal\n" +
                         "weightTotal is required input timeseries, other input timeseries obey FAIL/IGNORE specifications.")
public class WeightedAverage extends AW_AlgorithmBase
{
    public static final org.slf4j.Logger log = LoggerFactory.getLogger(WeightedAverage.class);

    @Input
    public double weightTotal;
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
    @Input
    public double input6;
    @Input
    public double input7;
    @Input
    public double input8;
    @Input
    public double weight1;
    @Input
    public double weight2;
    @Input
    public double weight3;
    @Input
    public double weight4;
    @Input
    public double weight5;
    @Input
    public double weight6;
    @Input
    public double weight7;
    @Input
    public double weight8;

    @Output(type = Double.class)
    public NamedVariable output = new NamedVariable("output", 0);

    // Allow javac to generate a no-args constructor.

    /**
     * Algorithm-specific initialization provided by the subclass.
     */
    @Override
    protected void initAWAlgorithm()
            throws DbCompException
    {
        _awAlgoType = AWAlgoType.TIME_SLICE;
    }

    /**
     * This method is called once before iterating all time slices.
     */
    @Override
    protected void beforeTimeSlices()
            throws DbCompException
    {
        if (!isAssigned("weightTotal"))
        {
            throw new DbCompException("WeightedAverage called with unassigned weightTotal input series, cannot run.");
        }

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
    @SuppressWarnings("checkstyle:LeftCurly")
    @Override
    protected void doAWTimeSlice()
            throws DbCompException
    {
        if (isMissing(weightTotal))
        {
            log.debug("Skipping time slice with base time {} because of missing value for weightTotal", debugSdf.format(_timeSliceBaseTime));
            return;
        }

        double tot = 0.0;
        ParmRef pr = null;
        String t = null;
        if (((pr = getParmRef(t = "input1")) != null && isAssigned(t) && isMissing(input1) &&
              pr.missingAction != MissingAction.IGNORE)
         || ((pr = getParmRef(t = "input2")) != null && isAssigned(t) && isMissing(input2) &&
              pr.missingAction != MissingAction.IGNORE)
         || ((pr = getParmRef(t = "input3")) != null && isAssigned(t) && isMissing(input3) &&
              pr.missingAction != MissingAction.IGNORE)
         || ((pr = getParmRef(t = "input4")) != null && isAssigned(t) && isMissing(input4) &&
              pr.missingAction != MissingAction.IGNORE)
         || ((pr = getParmRef(t = "input5")) != null && isAssigned(t) && isMissing(input5) &&
              pr.missingAction != MissingAction.IGNORE)
         || ((pr = getParmRef(t = "input6")) != null && isAssigned(t) && isMissing(input6) &&
              pr.missingAction != MissingAction.IGNORE)
         || ((pr = getParmRef(t = "input7")) != null && isAssigned(t) && isMissing(input7) &&
              pr.missingAction != MissingAction.IGNORE)
         || ((pr = getParmRef(t = "input8")) != null && isAssigned(t) && isMissing(input8) &&
              pr.missingAction != MissingAction.IGNORE))
        {
            log.debug("Skipping time slice with base time {} because of missing value for param {}",
                    debugSdf.format(_timeSliceBaseTime), t);
            return;
        }
        if (((pr = getParmRef(t = "weight1")) != null && isAssigned(t) && isMissing(weight1) &&
              pr.missingAction != MissingAction.IGNORE)
         || ((pr = getParmRef(t = "weight2")) != null && isAssigned(t) && isMissing(weight2) &&
              pr.missingAction != MissingAction.IGNORE)
         || ((pr = getParmRef(t = "weight3")) != null && isAssigned(t) && isMissing(weight3) &&
              pr.missingAction != MissingAction.IGNORE)
         || ((pr = getParmRef(t = "weight4")) != null && isAssigned(t) && isMissing(weight4) &&
              pr.missingAction != MissingAction.IGNORE)
         || ((pr = getParmRef(t = "weight5")) != null && isAssigned(t) && isMissing(weight5) &&
              pr.missingAction != MissingAction.IGNORE)
         || ((pr = getParmRef(t = "weight6")) != null && isAssigned(t) && isMissing(weight6) &&
              pr.missingAction != MissingAction.IGNORE)
         || ((pr = getParmRef(t = "weight7")) != null && isAssigned(t) && isMissing(weight7) &&
              pr.missingAction != MissingAction.IGNORE)
         || ((pr = getParmRef(t = "weight8")) != null && isAssigned(t) && isMissing(weight8) &&
              pr.missingAction != MissingAction.IGNORE))
        {
            log.debug("Skipping time slice with base time {} because of missing value for param {}",
                    debugSdf.format(_timeSliceBaseTime), t);
            return;
        }
        if (!isMissing(input1) && !isMissing(weight1))
            tot += (input1 * weight1);
        if (!isMissing(input2) && !isMissing(weight2))
            tot += (input2 * weight2);
        if (!isMissing(input3) && !isMissing(weight3))
            tot += (input3 * weight3);
        if (!isMissing(input4) && !isMissing(weight4))
            tot += (input4 * weight4);
        if (!isMissing(input5) && !isMissing(weight5))
            tot += (input5 * weight5);
        if (!isMissing(input6) && !isMissing(weight6))
            tot += (input6 * weight6);
        if (!isMissing(input7) && !isMissing(weight7))
            tot += (input7 * weight7);
        if (!isMissing(input8) && !isMissing(weight8))
            tot += (input8 * weight8);

        // Output only if nonzero total weight
        if (weightTotal != 0)
        {
            tot /= weightTotal;
            log.trace("doAWTimeSlice baseTime={}, input1={}, weight1={}, input2={}, weight2={}, tot={}",
                    debugSdf.format(_timeSliceBaseTime), input1, weight1, input2, weight2, tot);

            setOutput(output, tot);
        }
    }

    /**
     * This method is called once after iterating all time slices.
     */
    @Override
    protected void afterTimeSlices()
            throws DbCompException
    {}
}
