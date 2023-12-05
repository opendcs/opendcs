package api;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

//import com.sun.xml.internal.bind.v2.schemagen.xmlschema.List;

import java.net.HttpURLConnection;
import java.net.URL;
//import org.sqlite.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.websocket.Session;


/**
 * Represents a Gateway.
 *
 * @author Will Jonassen
 *
 */
public class Gateway extends HttpServlet {


    /**
     * The base url for the API to get OpenDCS data from.
     */
    public String baseUrl;
    public String context;

    /**
     * Initializes the Gateway instance.
     *
     * @throws ServletException 
     */
    public void init() throws ServletException {
        System.out.println("Initializing the Gateway instance.");
        try {
            this.setApiDetails(null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }    

    /**
     * GET requests come in through here.  They are rerouted through to the API.
     *
     * @param request                The incoming request.
     * @param response                The incoming response.
     * @throws ServletException
     * @throws IOException
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("GET");
        this.doRequest("GET", request, response);
    }

    /**
     * POST requests come in through here.  They are rerouted through to the API.
     *
     * @param request                The incoming request.
     * @param response                The incoming response.
     * @throws ServletException
     * @throws IOException
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("POST");
        this.doRequest("POST", request, response);
    }

    /**
     * DELETE requests come in through here.  They are rerouted through to the API.
     *
     * @param request                The incoming request.
     * @param response                The incoming response.
     * @throws ServletException
     * @throws IOException
     */
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("DELETE");
        this.doRequest("DELETE", request, response);
    }


    /**
     * This is the function that is run upon initialization of a Gateway instance.  It grabs the config file,
     * parses it, and initializes the gateway.  The main function of this function is to identify (and set)
     * how to connect to the OpenDCS api.
     * 
     * @throws IOException 
     */
    public void setApiDetails(ServletContext sc) throws IOException
    {
        //Gets the initialization details from the conf file.
        String apiConfigFilePath;
        if (sc == null)
        {
            apiConfigFilePath = this.getServletContext().getRealPath("/conf/api.conf");
        }
        else
        {
            apiConfigFilePath = sc.getRealPath("/conf/api.conf");
        }
        File cf = new File(apiConfigFilePath);

        // Creating an object of BufferedReader class
        BufferedReader br = new BufferedReader(new FileReader(cf));

        String line = br.readLine();
        String tempUrl = "";
        String tempPort = "";
        String tempContext = "";
        while (line != null)
        {
            System.out.println(line);
            String nameValue[] = line.split("=");
            String name = nameValue[0].trim();
            String value = nameValue[1].trim();
            if (name.equalsIgnoreCase("url"))
            {
                tempUrl = value;
                System.out.println("Setting Base Url to " + tempUrl); 
            }
            else if (name.equalsIgnoreCase("port"))
            {
                tempPort = ":" + value;
                System.out.println("Setting Port to " + tempPort);
            }
            else if (name.equalsIgnoreCase("context"))
            {
                tempContext = value;
                System.out.println("Setting Context to " + tempContext);
            }
            // read next line
            line = br.readLine();
        }
        br.close();
        //Sets the URL for the API
        this.baseUrl = tempUrl + tempPort;
        this.context = tempContext;
        System.out.println(String.format("API base url: %s.", this.baseUrl));
    }

    /**
     * Handles the front end requests and redirects them to the API.
     * 
     * @param method              The type of web request.  (GET, POST, DELETE)
     * @param request            The incoming request.
     * @param response            The incoming response.
     * @throws IOException 
     */
    private void doRequest(String method, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(200);
        String inline = "";
        System.out.println(String.format("Requested Method: %s.", method));
        try {
            Set<String> paramSet = request.getParameterMap().keySet();
            List<String> paramNames = new ArrayList<String>();
            paramNames.addAll(paramSet);
            String apiCall;
            if (paramNames.contains("opendcs_api_call"))
            {
                apiCall = request.getParameter("opendcs_api_call");
                paramNames.remove("opendcs_api_call");
                String paramString = "";

                String authToken = request.getHeader("Authorization");
                System.out.println("*************************");
                System.out.println("Got the authToken: " + authToken);
                System.out.println("*************************");
                
                for (String name : paramNames)
                {
                    String value = request.getParameter(name);
                    paramString += (paramString.length() == 0) ? "?" + name + "=" + value : "&" + name + "=" + value;
                }

                System.out.println(String.format("API Param String: %s", paramString));

                //Passing token in HTTP Header - This is the only way the web client was designed to pass the token.
                URL url = new URL(new URL(this.baseUrl), String.format("%s/", this.context) + apiCall + paramString);
                System.out.println(String.format("URL Path: %s.", url.getPath()));

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestMethod(method);
                if (authToken != null && !authToken.equals(""))
                {
                    System.out.println("Setting Authorization Bearer Token.");
                    conn.setRequestProperty("Authorization","Bearer " + authToken);
                }

                if (method.equalsIgnoreCase("get"))
                {
                    System.out.println("Method = GET");
                    BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = rd.readLine()) != null) {
                        sb.append(line);
                    }
                    rd.close();
                    inline = sb.toString();
                }
                else
                {
                    System.out.println("Method != GET");
                    conn.setDoOutput(true);
                    try(OutputStream os = conn.getOutputStream()) 
                    {
                        String postBody = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));

                        OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");    
                        osw.write(postBody);
                        osw.flush();
                        osw.close();
                        os.close();
                    }
                }
                System.out.println("Connecting to the API.");
                conn.connect();


                //Getting the response code
                int responsecode = conn.getResponseCode();
                System.out.println(String.format("API Response Code: %s.", responsecode));
                if (responsecode != 200) 
                {
                    System.out.println(String.format("Error - response code != 200.  responsecode=%s.", responsecode));
                    inline = "";

                    Scanner scanner = new Scanner(conn.getErrorStream());

                    //Write all the JSON data into a string using a scanner
                    while (scanner.hasNext()) {
                        inline += scanner.nextLine();
                    }

                    //Close the scanner
                    scanner.close();
                    response.setStatus(responsecode);
                } 
                else 
                {
                    if (!method.equalsIgnoreCase("get"))
                    {
                        BufferedInputStream bis = new BufferedInputStream(conn.getInputStream());
                        ByteArrayOutputStream buf = new ByteArrayOutputStream();
                        int result2 = bis.read();
                        while(result2 != -1) {
                            buf.write((byte) result2);
                            result2 = bis.read();
                        }
                        inline = buf.toString();
                    }
                }
            }
            else
            {
                inline = "{\"message\": \"error - you must pass the opendcs_api_call parameter.\"}";
                System.out.println(String.format("Error: %s.", inline));
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(400);
            inline = "error";
        }
        response.getWriter().write(inline);  
    }
}