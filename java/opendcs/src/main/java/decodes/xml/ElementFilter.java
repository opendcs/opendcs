package decodes.xml;

/**
 * Used to allow an application to choose whether or not to parse an element
 * when it is detected in the XML stream. If true is returned, an appropriate
 * parser will be used to parse the element. If false is returned, ElementIgnorer
 * is used to ignore the element and any sub-elements.
 */
public interface ElementFilter
{
	public boolean acceptElement(String elementName);
}
