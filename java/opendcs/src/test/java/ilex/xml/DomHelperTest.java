package ilex.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;

import ilex.util.ErrorException;

class DomHelperTest
{
    private static final String HEADER = "<?xml version=\"1.0\"?>";

    @TempDir
    Path tmp;

    @ParameterizedTest
    @ValueSource(strings = {
        "<root>hello</root>",
        "<!DOCTYPE root SYSTEM \"{uri}\"><root>hello</root>"
    })
    void test_xml_is_parsed(String body) throws Exception
    {
        Document doc = DomHelper.readStream("test", stream(body));
        assertEquals("hello", doc.getDocumentElement().getTextContent());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "<!DOCTYPE root [<!ENTITY xxe SYSTEM \"{uri}\">]><root>&xxe;</root>",
        "<!DOCTYPE root [<!ENTITY % xxe SYSTEM \"{uri}\"> %xxe;]><root>hello</root>"
    })
    void test_external_entity_is_rejected(String body) throws Exception
    {
        assertThrows(ErrorException.class, () -> DomHelper.readStream("test", stream(body)));
    }

    private InputStream stream(String body) throws Exception
    {
        Path secret = Files.write(tmp.resolve("secret.txt"), "secret".getBytes(StandardCharsets.UTF_8));
        String xml = HEADER + body.replace("{uri}", secret.toUri().toString());
        return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    }
}
