
package lrgs.drgsrecv;

import java.io.File;
import java.util.Iterator;

import lrgs.common.DcpAddress;

/**
 *
 * @author mjmaloney
 * @deprecated We are using decodes.util.Pdt instead.
 */
@Deprecated
public class PdtSchedTest
{
	public static void main(String args[])
		throws Exception
	{
		PdtSched ps = new PdtSched();
		ps.load(new File(args[0]));
		for(Iterator it = ps.iterator(); it.hasNext();)
		{
			PdtSchedEntry pse = (PdtSchedEntry)it.next();
			DcpAddress da = new DcpAddress(pse.getDcpAddress());
			System.out.println(da.toString()
				+ " ST=" + pse.getStChan() + ", t=" +pse.getFirstXmit()
				+ ", i=" + pse.getXmitInterval()
				+ ", w=" + pse.getXmitWindow()
				+ ", RD=" + pse.getRdChan());
		}
	}
}
