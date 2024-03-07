/*
 *  Copyright 2023 OpenDCS Consortium
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Represents a Gateway.
 *
 * @author Will Jonassen
 *
 */
public class Gateway extends HttpServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(Gateway.class);

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
    public void init() throws ServletException
    {
        try
        {
            this.setApiDetails(null);
        }
        catch (IOException ex)
        {
            LOGGER.error("Error initializing gateway instance", ex);
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
        LOGGER.debug("Gateway GET");
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
        LOGGER.debug("Gateway POST");
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
        LOGGER.debug("Gateway DELETE");
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
        if (sc == null)
        {
            sc = this.getServletContext();
        }
        Object configAttr = sc.getAttribute("api_details_file_path");
        String apiConfigFilePath = (configAttr == null) ? null : configAttr.toString();

        if (apiConfigFilePath == null)
        {
            //Use the config file within WAR file.
            //default api file location.
            apiConfigFilePath = sc.getRealPath("/conf/api.conf");
        }

        //Gets the initialization details from the conf file.
        LOGGER.debug("API Config File Path: {}", apiConfigFilePath);
        File cf = new File(apiConfigFilePath);

        String tempUrl = "";
        String tempPort = "";
        String tempContext = "";
        // Creating an object of BufferedReader class
        try(BufferedReader br = new BufferedReader(new FileReader(cf)))
        {
            String line = br.readLine();
            while(line != null)
            {
                String nameValue[] = line.split("=");
                if(nameValue.length > 1)
                {
                    String name = nameValue[0].trim();
                    String value = nameValue[1].trim();
                    if("url".equalsIgnoreCase(name))
                    {
                        tempUrl = value;
                    }
                    else if("port".equalsIgnoreCase(name))
                    {
                        tempPort = ":" + value;
                    }
                    else if("context".equalsIgnoreCase(name))
                    {
                        tempContext = value;
                    }
                }
                // read next line
                line = br.readLine();
            }
        }
        //Sets the URL for the API
        this.baseUrl = tempUrl + tempPort;
        this.context = tempContext;
       LOGGER.debug("API base url: {}.", this.baseUrl);

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
        LOGGER.debug("Requested Method: {}.", method);
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
                LOGGER.debug("Auth Token received.");
                
                for (String name : paramNames)
                {
                    String value = request.getParameter(name);
                    paramString += (paramString.length() == 0) ? "?" + name + "=" + value : "&" + name + "=" + value;
                }

                LOGGER.debug("API Param String: {}", paramString);

                //Passing token in HTTP Header - This is the only way the web client was designed to pass the token.
                URL url = new URL(new URL(this.baseUrl), String.format("%s/", this.context) + apiCall + paramString);
                LOGGER.debug("URL Path: {}.", url.getPath());

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestMethod(method);
                if (authToken != null && !authToken.equals(""))
                {
                    conn.setRequestProperty("Authorization","Bearer " + authToken);
                }

                if (method.equalsIgnoreCase("get"))
                {
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
                    conn.setDoOutput(true);
                    try(OutputStream os = conn.getOutputStream()) 
                    {
                        String postBody = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));

                        OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");    
                        osw.write(postBody);
                        osw.flush();
                        osw.close();
                    }
                }
                conn.connect();

                //Getting the response code
                int responsecode = conn.getResponseCode();
                LOGGER.debug("Connecting to the API.  Response Code: {}.", responsecode);
                if (responsecode != 200) 
                {
                    LOGGER.error("Error - responsecode = {}.", responsecode);
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
                LOGGER.error(inline);
            }
        }
        catch (IOException ex)
        {
            LOGGER.error("Error connecting to API from gateway: ", ex);
            response.setStatus(400);
            inline = "error";
        }
        response.getWriter().write(inline);  
    }
}