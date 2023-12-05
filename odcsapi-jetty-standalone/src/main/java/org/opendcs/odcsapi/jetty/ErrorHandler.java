/*
 * Copyright (c) 2023
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */

package org.opendcs.odcsapi.jetty;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


public class ErrorHandler extends ErrorPageErrorHandler {

    /*
        Messages to error made based on:
        Hypertext Transfer Protocol (HTTP/1.1): Semantics and Content
        https://tools.ietf.org/html/rfc7231
    */

    private static final String ERROR_404_MESSAGE = "Target resource not found";

    private static final String ERROR_501_MESSAGE = "Server functionality to process request is not implemented";

    private static final String ERROR_502_MESSAGE = "Server cannot proxy request";

    private static final String ERROR_503_MESSAGE = "Server is currently unable to handle the request";

    private static final String ERROR_504_MESSAGE = "Server did not receive a timely response from an upstream server";

    private static final String ERROR_UNEXPECTED_MESSAGE = "Unexpected error occurs";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    protected void generateAcceptableResponse(Request baseRequest, HttpServletRequest request, HttpServletResponse response, int code, String message, String mimeType)
            throws IOException
    {
        if (isRestRequest(request)) {
            baseRequest.setHandled(true);
            Writer writer = getAcceptableWriter(baseRequest, request, response);
            if (null != writer) {
                response.setContentType(MimeTypes.Type.APPLICATION_JSON.asString());
                response.setStatus(code);
                handleErrorPage(request, writer, code, message);
            }
        }
        else {
            super.generateAcceptableResponse(baseRequest, request, response, code, message, mimeType);
        }
    }

    @Override
    protected Writer getAcceptableWriter(Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException
    {
        if (isRestRequest(request)) {
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            return response.getWriter();
        }
        else {
            return super.getAcceptableWriter(baseRequest, request, response);
        }
    }

    @Override
    protected void writeErrorPage(HttpServletRequest request, Writer writer, int code, String message, boolean showStacks)
            throws IOException
    {
        if (isRestRequest(request)) {
            try {
                writer.write(MAPPER.writeValueAsString(new Errors(getMessage(code))));
            }
            catch (Exception e) {
                // Log if needed
            }
        }
        else {
            super.writeErrorPage(request, writer, code, message, showStacks);
        }
    }

    private boolean isRestRequest(HttpServletRequest request) {
        return request.getServletPath().startsWith("/api/");
    }

    private String getMessage(int code) {
        switch (code) {
            case 404 : return ERROR_404_MESSAGE;
            case 501 : return ERROR_501_MESSAGE;
            case 502 : return ERROR_502_MESSAGE;
            case 503 : return ERROR_503_MESSAGE;
            case 504 : return ERROR_504_MESSAGE;
            default  : return ERROR_UNEXPECTED_MESSAGE;
        }
    }
    
    public class Errors {

        private String message;

        private List errors = new ArrayList();

        public Errors(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public List getErrors() {
            return errors;
        }

        public void setErrors(List errors) {
            this.errors = errors;
        }
    }
}