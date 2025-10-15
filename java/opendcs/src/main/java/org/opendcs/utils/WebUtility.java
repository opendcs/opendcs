package org.opendcs.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.cert.X509Certificate;
import java.util.Scanner;
import java.util.function.Predicate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.util.EnvExpander;
import nl.altindag.ssl.SSLFactory;
import nl.altindag.ssl.model.TrustManagerParameters;

public class WebUtility
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();

    /**
     * Default Certificate Trust that only logs the host information for the user to review.
     */
    public static final Predicate<TrustManagerParameters> TRUST_EXISTING_CERTIFICATES = cert ->
    {
        X509Certificate[] chain = cert.getChain();
        if (log.isWarnEnabled() && chain != null && chain.length > 0)
        {
            log.warn("Certificate for host {} is not recognized. Signed by {}. If you trust this " +
                 "Certificate you will need to manually add this to {}",
                         cert.getHostname().orElse("No hostname"),
                         chain[0].getIssuerX500Principal().getName(),
                         EnvExpander.expand("$DCSTOOL_USERDIR/local_trust.p12"));
        }
        return false;
    };

    /**
     * Read the full contents of a URL using the default TLS Trust (System, Java, and $DCSTOOL_USERDIR/local_trust.p12)
     * @param requestURL
     * @return The full contents provided by the URL.
     * @throws IOException Any error
     */
    public static String readStringFromURL(String requestURL) throws IOException
    {
        return readStringFromURL(requestURL, WebUtility.TRUST_EXISTING_CERTIFICATES);
    }

    /**
     * Read the full contents of a URL using the default TLS Trust (System, Java, and $DCSTOOL_USERDIR/local_trust.p12)
     * @param requestURL
     * @param certTest a user provided callback to decided if a certificate chain that isn't already justed should be.
     * @return
     * @throws IOException Any error
     */
    // https://stackoverflow.com/questions/4328711/read-url-to-string-in-few-lines-of-java-code
    public static String readStringFromURL(String requestURL, Predicate<TrustManagerParameters> certTest) throws IOException
    {
        try (Scanner scanner = new Scanner(openUrl(requestURL, 0, certTest),StandardCharsets.UTF_8.toString()))
        {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    /**
     * Open and return the input stream of a given URL using the default TLS Trust (System, Java, and $DCSTOOL_USERDIR/local_trust.p12)
     * and default of no timeout.
     * @param requestUrl
     * @return the input stream ready for reading.
     * @throws IOException
     */
    public static InputStream openUrl(String requestUrl) throws IOException
    {
        return openUrl(requestUrl, 0);
    }


    /**
     * Open and return the input stream of a given URL using the default TLS Trust (System, Java, and $DCSTOOL_USERDIR/local_trust.p12)
     * and user specified timeout.
     * @param requestUrl
     * @param timeoutMilliseconds
     * @return the input stream ready for reading.
     * @throws IOException
     */
    public static InputStream openUrl(String requestURL, int timeoutMilliseconds) throws IOException
    {
        return openUrl(requestURL, timeoutMilliseconds, WebUtility.TRUST_EXISTING_CERTIFICATES);
    }

    /**
     * Open and return the input stream of a given URL using the default TLS Trust (System, Java, and $DCSTOOL_USERDIR/local_trust.p12)
     * and user specified timeout.
     * @param requestUrl
     * @param timeoutMilliseconds
     * @return the input stream ready for reading.
     * @throws IOException
     */
    public static InputStream openUrl(String requestURL, int timeoutMilliseconds, Predicate<TrustManagerParameters> certTest) throws IOException
    {
        URL url = new URL(requestURL);
        URLConnection con = url.openConnection();
        if (timeoutMilliseconds > 0)
        {
            con.setConnectTimeout(timeoutMilliseconds);
        }
        if (con instanceof HttpsURLConnection)
        {
            HttpsURLConnection httpsCon = (HttpsURLConnection)con;
            httpsCon.setSSLSocketFactory(WebUtility.socketFactory(certTest));
        }
        return con.getInputStream();
    }

    /**
     * Get a SSLSocketFactory using the Java, System, and $DCSTOOL_USERDIR/local_trust.p12 sources.
     *
     * @param certTest a user supplied callback used to determine if a certificate chain that isn't already trusted
     *                 should be.
     * @return
     */
    public static SSLSocketFactory socketFactory(Predicate<TrustManagerParameters> certTest)
    {
        SSLFactory sslFactory = SSLFactory.builder()
                                          .withDefaultTrustMaterial()
                                          .withSystemTrustMaterial()
                                          .withInflatableTrustMaterial(
                                            Paths.get(EnvExpander.expand("$DCSTOOL_USERDIR/local_trust.p12")),
                                            "local_trust".toCharArray(),
                                            "PKCS12",
                                            certTest)
                                          .build();
        return sslFactory.getSslContext().getSocketFactory();
    }
}
