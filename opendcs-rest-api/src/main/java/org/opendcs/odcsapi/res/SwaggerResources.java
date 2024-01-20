package org.opendcs.odcsapi.res;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
public class SwaggerResources
{
    @Context ServletContext servletContext;

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("swaggerui")
    public Response getSwagger() throws IOException
    {
        String result = this.getSwaggerTextResource("index.html");
        return Response.status(Response.Status.OK).entity(result).build();
    }
    @GET
    @Path("{fileName: .*\\.(css|js)$ }")
    public Response getTextFile(@PathParam("fileName") String fileName) throws IOException
    {
        String result = this.getSwaggerTextResource(fileName);
        return Response.status(Response.Status.OK).entity(result).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{fileName: .*\\.(json)$ }")
    public Response getJsonFile(@PathParam("fileName") String fileName) throws IOException
    {
        String result = this.getSwaggerTextResource(fileName);
        return Response.status(Response.Status.OK).entity(result).build();
    }

    @GET
    @Produces("image/png")
    @Path("{fileName: .*\\.(png|jpeg)$ }")
    public Response getImageFile(@PathParam("fileName") String fileName) throws IOException
    {
        byte[] imageData = getSwaggerImageResource(fileName);
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
