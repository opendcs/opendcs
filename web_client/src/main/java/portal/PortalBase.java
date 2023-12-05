/**
 * @author Will Jonassen
 *
 */

package portal;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import api.Gateway;

/**
 * Represents a PortalBase.
 * This class is the base class for any of the Web Client pages.  It is the base class for handling the web requests from each of the 
 * web client web pages.
 *
 * @author Will Jonassen
 *
 */

public class PortalBase extends HttpServlet { 

    /**
     * Represents the relative url path for the corresponding web page.
     */
    
    private String destination;
    
    /**
     * Represents the tool that this web page falls under (DECODES, Algorithms, Processes).
     * This is to pass to the front end and highlight the corresponding tool in the side menu of the web client.
     */
    
    private String tool;
    
    /**
     * The name of the page.  This is to highlight the corresponding menu item (page) in the side menu of the web client.
     */
    
    private String page;
    
    /**
     * This is the path for the base url of the API.
     */
    
    private String apiBaseUrl;
    

    /**
     * Creates a new PortalBase
     *
     * @param destination    This will be set int the constructor of the child class.
     * @param tool            This will be set int the constructor of the child class.
     * @param page            This will be set int the constructor of the child class.
     * @throws IOException 
     */
    public PortalBase(String destination, String tool, String page) throws IOException
    {
        this.setDestination(destination);
        this.setTool(tool);
        this.setPage(page);
    }

    /**
     * Handles the GET calls from the front end and returns the requested web page.
     * 
     * @param request            The incoming request.
     * @param response            The incoming response.
     * @throws ServletException
     * @throws IOException  
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("doGet has been called from PortalBase.java...");

        //This needs to be run from the "GET" and not the constructor.  It gets the servlet context this way,
        //Where it cannot from the constructor.
        if (this.apiBaseUrl == null)
        {
            Gateway g = new Gateway();
            g.setApiDetails(this.getServletContext());
            System.out.println(String.format("setting the base url of the api to %s.", g.baseUrl));
            this.setApiBaseUrl(g.baseUrl);
        }

        String destination = this.getDestination();
        request.setAttribute("tool",  this.getTool());
        request.setAttribute("page",  this.getPage());
        request.setAttribute("api_swaggerui_url", this.getApiBaseUrl());

        RequestDispatcher rd = getServletContext().getRequestDispatcher(destination);
        rd.forward(request, response);           
    }

    /**
     * Handles the POST calls from the front end for the corresponding web page (This is not used currently, but it is a placeholder if it ever needs to be used).
     *
     * @param request            The incoming request.
     * @param response            The incoming response.
     * @throws ServletException
     * @throws IOException 
     */
    /*
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("doPost has been called from PortalBase.java...");

        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(200);
        response.getWriter().write("NOT USED");   
    }
     */

    /**
     * Sets the destination parameter
     * @param destination     The relative url for the page.
     */
    protected void setDestination(String destination)
    {
        this.destination = destination;
    }

    /**
     * Gets the destination parameter
     * @return     destination
     */
    public String getDestination()
    {
        return this.destination;
    }

    /**
     * Sets the tool parameter
     * @param tool     The tool that the page resides in.
     */
    protected void setTool(String tool)
    {
        this.tool = tool;
    }

    /**
     * Gets the tool parameter.
     * @return     tool
     */
    public String getTool()
    {
        return this.tool;
    }

    /**
     * Sets the page parameter
     * @param page the page that the user is requesting.
     */
    protected void setPage(String page)
    {
        this.page = page;
    }

    /**
     * Gets the page parameter
     * @return     page
     */
    public String getPage()
    {
        return this.page;
    }

    /**
     * Sets the apiBaseUrl parameter
     * @param apiBaseUrl the apiBaseUrl to set
     */
    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    /**
     * Gets apiBaseUrl parameter
     * @return the apiBaseUrl
     */
    public String getApiBaseUrl() {
        return this.apiBaseUrl;
    }


}
