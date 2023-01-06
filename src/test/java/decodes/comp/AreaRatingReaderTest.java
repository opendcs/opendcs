package decodes.comp;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/** Unit test for AreaRatingReader */
class AreaRatingReaderTest
{

	@ParameterizedTest
	@EmptySource //TODO: get example files
	void testAreaRatingReader(String filename)
	{
		AreaRatingReader rrr = new AreaRatingReader(filename);
		RatingComputation rc = new RatingComputation(rrr);
		assertDoesNotThrow(rc::read);
		rc.dump();
	}
}
