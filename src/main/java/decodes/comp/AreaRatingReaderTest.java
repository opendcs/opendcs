package decodes.comp;

/** Unit test for AreaRatingReader */
public class AreaRatingReaderTest
{
	/** Do the test
	* @param args one arg containing the file name
	*/
	public static void main(String args[])
		throws Exception
	{
		AreaRatingReader rrr = new AreaRatingReader(args[0]);
		RatingComputation rc = new RatingComputation(rrr);
		rc.read();
		rc.dump();
	}
}
