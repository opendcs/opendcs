/**
 * $Id$
 */
package lrgs.archive;

import java.util.Date;
import lrgs.lrgsmain.LrgsInputInterface;
import lrgs.common.DcpMsg;
import decodes.util.PdtEntry;


/**
 * Interface for a class that uses MsgValidator.
 * This interface defines the callback method used by the validator to
 * inform the 'validatee' of the results of validation.
 */
public interface MsgValidatee
{
	/**
	 * The validatee should use the validation results passed in the args.
	 * @param failureCode Each validation result is expressed in a failure code
	 * @param msg Provides additional details
	 */
	public void useValidationResults(char failureCode, String explanation,
		DcpMsg msg, LrgsInputInterface src, Date t, PdtEntry pdtEntry);
	

}
