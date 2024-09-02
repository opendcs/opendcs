package decodes.decoder;


import ilex.var.Variable;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegexTest
{

	@Test
	void simpleHTMLTest() throws Exception
	{
		String filename = "decodes/decoder/nebraska_download.html";
		Path path = Paths.get("src", "test", "resources").resolve(filename).toAbsolutePath();
		String content = new String(Files.readAllBytes(path));
		String r = "<td data-header=\"Location\">Lake McConaughy</td>\\r*\\s*<td data-header=\"Today \\(Feet above Sea Level\\)\">(?<sensor2>[0-9\\,\\.]+)";
		Pattern pattern = Pattern.compile(r);
		Matcher matcher = pattern.matcher(content);
		Variable d = RegexFunction.getValue(matcher,2);

		assertEquals(3238.6,d.getDoubleValue(),0.01);

	}

}
