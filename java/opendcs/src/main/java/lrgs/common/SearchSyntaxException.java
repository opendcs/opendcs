package lrgs.common;


/**
Thrown when a search could not be performed because of some kind of syntax
error in the request, the search criteria, or the network lists.
*/
public class SearchSyntaxException extends ArchiveException
{
    /**
     * Constructor.
     * @param msg the message
     */
    public SearchSyntaxException(String msg, int errorCode)
    {
        super(msg, errorCode, false);
    }

    public SearchSyntaxException(String msg, int errorCode, Throwable cause)
    {
        super(msg, errorCode, false, cause);
    }
}
