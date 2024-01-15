package org.opendcs.odcsapi.res;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
public class SwaggerResources
{
    @Context ServletContext servletContext;

    @GET
    @Path("swaggerui")
    @Produces(MediaType.TEXT_HTML)
    public Response getSwagger() throws IOException
    {
        InputStream inputStream = servletContext.getResourceAsStream("/WEB-INF/classes/swaggerui/index.html");
        int bufferSize = 1024;
        char[] buffer = new char[bufferSize];
        StringBuilder out = new StringBuilder();
        Reader in = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        for (int numRead; (numRead = in.read(buffer, 0, buffer.length)) > 0; )
        {
            out.append(buffer, 0, numRead);
        }
        String result = "";
        result = out.toString();
        return Response.status(Response.Status.OK).entity(result).build();
    }

    @GET
    @Path("index.css")
    public Response getIndexCss() throws IOException
    {
        String result = this.getSwaggerTextResource("index.css");
        return Response.status(Response.Status.OK).entity(result).build();
    }

    @GET
    @Path("open_api.json")
    public Response getOpenApiJson() throws IOException
    {
        String result = this.getSwaggerTextResource("open_api.json");
        return Response.status(Response.Status.OK).entity(result).build();
    }

    @GET
    @Path("swagger-initializer.js")
    public Response getSwaggerInitializer() throws IOException
    {
        String result = this.getSwaggerTextResource("swagger-initializer.js");
        return Response.status(Response.Status.OK).entity(result).build();
    }

    @GET
    @Path("swagger-ui.css")
    public Response getSwaggerUiCss() throws IOException
    {
        String result = this.getSwaggerTextResource("swagger-ui.css");
        return Response.status(Response.Status.OK).entity(result).build();
    }

    @GET
    @Path("swagger-ui.js")
    public Response getSwaggerUiJs() throws IOException
    {
        String result = this.getSwaggerTextResource("swagger-ui.js");
        return Response.status(Response.Status.OK).entity(result).build();
    }

    @GET
    @Path("swagger-ui-bundle.js")
    public Response getSwaggerUiBundleJs() throws IOException
    {
        String result = this.getSwaggerTextResource("swagger-ui-bundle.js");
        return Response.status(Response.Status.OK).entity(result).build();
    }

    @GET
    @Path("swagger-ui-es-bundle.js")
    public Response getSwaggerUiEsBundleJs() throws IOException
    {
        String result = this.getSwaggerTextResource("swagger-ui-es-bundle.js");
        return Response.status(Response.Status.OK).entity(result).build();
    }

    @GET
    @Path("swagger-ui-es-bundle-core.js")
    public Response getSwaggerUiEsBundleCoreJs() throws IOException
    {
        String result = this.getSwaggerTextResource("swagger-ui-es-bundle-core.js");
        return Response.status(Response.Status.OK).entity(result).build();
    }

    @GET
    @Path("swagger-ui-standalone-preset.js")
    public Response getSwaggerUiStandalonePresetJs() throws IOException
    {
        String result = this.getSwaggerTextResource("swagger-ui-standalone-preset.js");
        return Response.status(Response.Status.OK).entity(result).build();
    }

    @GET
    @Produces("image/png")
    @Path("favicon-16x16.png")
    public Response getFavIcon16() throws IOException
    {
        byte[] imageData = getSwaggerImageResource("favicon-16x16.png");
        return Response.ok(imageData).build();
    }

    @GET
    @Produces("image/png")
    @Path("favicon-32x32.png")
    public Response getFavIcon32() throws IOException
    {
        byte[] imageData = getSwaggerImageResource("favicon-32x32.png");
        return Response.ok(imageData).build();
    }

    private byte[] getSwaggerImageResource(String fileName) throws IOException
    {
        String filePath = servletContext.getRealPath(String.format("/WEB-INF/classes/swaggerui/%s", fileName));
        File f = new File(filePath);
        BufferedImage image = ImageIO.read(f);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    private String getSwaggerTextResource(String fileName) throws IOException
    {
        InputStream inputStream = servletContext.getResourceAsStream(String.format("/WEB-INF/classes/swaggerui/%s", fileName));
        int bufferSize = 1024;
        char[] buffer = new char[bufferSize];
        StringBuilder out = new StringBuilder();
        Reader in = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        for (int numRead; (numRead = in.read(buffer, 0, buffer.length)) > 0; )
        {
            out.append(buffer, 0, numRead);
        }
        String result = "";
        result = out.toString();
        return result;
    }
}
