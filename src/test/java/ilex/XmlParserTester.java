package ilex;

import ilex.xml.LoggerErrorHandler;
import java.io.File;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

/**
 * This class defines a program that tests the Ilex XML parsing
 * and writing classes, as well as providing an example of how
 * they work.
 */

public class XmlParserTester extends Tester
{
  /** The top-level parser.  */

    XmlFileParser _topParser;


  /**
   * This array defines the permissable XML elements in our test
   * file, and associates factory objects with each.
   */

    XmlElementDescriptor[] _elemDescs = {
        new XmlElementDescriptor("EnumList", new TestEnumListParser()),
    };

  /** The main program entry point */

    public static void main(String[] arg)
    {
		XmlParserTester self = new XmlParserTester(arg);

		self.run();

		System.exit(0);
    }

  /** Construct from a set of command-line arguments  */

    public XmlParserTester(String[] arg)
    {
        super();
	}

  /**
   * Run the test.  This returns 0 if successful.  If this test
   * is not successful, this exits with a non-zero exit status.
   */

    public int run()
    {
        try
        {
			_topParser = new XmlFileParser(_elemDescs);

			Object obj =
			    _topParser.parse(new File("XmlParserTester.xml"));
        }

        catch (Exception e)
        {
            failedWithException(e);
        }

        _log.println("ok");
        return 0;
    }


}