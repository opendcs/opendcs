/*
 *  $Id$
 *
 *  This is open-source software written by ILEX Engineering, Inc., under
 *  contract to the federal government. You are free to copy and use this
 *  source code for your own purposes, except that no part of the information
 *  contained in this file may be claimed to be proprietary.
 *
 *  Except for specific contractual terms between ILEX and the federal 
 *  government, this source code is provided completely without warranty.
 *  For more information contact: info@ilexeng.com
 */
package decodes.tsdb.algoedit;

import java.util.ArrayList;

import decodes.tsdb.algo.AWAlgoType;
import decodes.tsdb.BadTimeSeriesException;

public interface AlgoData
{
	public String getAlgorithmName();
	public void setAlgorithmName(String name);

	public AWAlgoType getAlgorithmType();
	public void setAlgorithmType(AWAlgoType type);

	public String getJavaClassName();
	public void setJavaClassName(String name);

	public String getExtends();
	public void setExtends(String ext);

	public String getImplements();
	public void setImplements(String imp);

	public String getJavaPackage();
	public void setJavaPackage(String name);

	public String getComment();
	public void setComment(String s);

	public ArrayList<InputTimeSeries> getAllInputTimeSeries();
	public void clearInputTimeSeries();
	public void addInputTimeSeries(InputTimeSeries its);

	public ArrayList<String> getAllOutputTimeSeries();
	public void clearOutputTimeSeries();
	public void addOutputTimeSeries(String ots);

	public ArrayList<AlgoProp> getAllAlgoProps();
	public void clearAlgoProps();
	public void addAlgoProp(AlgoProp ap);

	public String getImportsCode();
	public void setImportsCode(String s);

	public String getLocalVarsCode();
	public void setLocalVarsCode(String s);

	public String getOneTimeInitCode();
	public void setOneTimeInitCode(String s);

	public String getBeforeIterCode();
	public void setBeforeIterCode(String s);

	public String getTimeSliceCode();
	public void setTimeSliceCode(String s);

	public String getAfterIterCode();
	public void setAfterIterCode(String s);

	public String getAggPeriodOutput();
	public void setAggPeriodOutput(String s);

	public String validateName(String nm)
		throws BadTimeSeriesException;
}
