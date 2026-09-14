package org.opendcs.dadds;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

import org.opendcs.lrgs.webhook.dadds.DaddsDataMessage;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class SnsMessageCreator
{

    private static final JsonMapper jsonMapper = JsonMapper.builder()
                                                             .enable(MapperFeature.REQUIRE_HANDLERS_FOR_JAVA8_TIMES)
                                                             .addModule(new JavaTimeModule())
                                                             .build();

    private static final SecureRandom random = new SecureRandom();

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT).withZone(ZoneOffset.UTC);


    private SnsMessageCreator()
    {
        /* utility class */
    }


    public static String createDaddsNotification(DaddsDataMessage message, PrivateKey key, String arn, int port)
        throws IOException, InvalidKeyException, NoSuchAlgorithmException, SignatureException
    {
        String messageBody = jsonMapper.writeValueAsString(message);
        String messageId = UUID.randomUUID().toString();
        String timestamp = TIMESTAMP_FORMATTER.format(Instant.now());

        var root = jsonMapper.createObjectNode();
        root.put("Type", "Notification");
        root.put("MessageId", messageId);
        root.put("TopicArn", arn);
        root.put("Message", messageBody);
        root.put("Timestamp", timestamp);
        root.put("SignatureVersion", "1");
        root.put("SigningCertURL", "https://sns.us-east-1.amazonaws.com:" + port + "/cert.pem");

        StringBuilder sb = new StringBuilder();
        sb.append("Message\n").append(messageBody).append("\n")
          .append("MessageId\n").append(messageId).append("\n")
          .append("Timestamp\n").append(timestamp).append("\n")
          .append("TopicArn\n").append(arn).append("\n")
          .append("Type\n").append("Notification").append("\n")
        ;

        var signatureText = signTextV1(sb.toString(), key);
        root.put("Signature", signatureText);
        return jsonMapper.writeValueAsString(root);
    }

    public static String createDaddsConfirmationMessage(PrivateKey key, String arn, int port, String subscribeUrl)
        throws IOException, InvalidKeyException, NoSuchAlgorithmException, SignatureException
    {        
        byte[] tokenBytes = new byte[256];
        random.nextBytes(tokenBytes);
        String token = HexFormat.of().formatHex(tokenBytes);
        String message = "Click the link to confirm";
        String timestamp = TIMESTAMP_FORMATTER.format(Instant.now());
        String messageId = UUID.randomUUID().toString();

        var root = jsonMapper.createObjectNode();
        root.put("Type", "SubscriptionConfirmation");
        root.put("MessageId", messageId);
        root.put("Token", token);
        root.put("TopicArn", arn);
        root.put("Message", message);
        root.put("Timestamp", timestamp);
        root.put("SignatureVersion", "1");
        root.put("SubscribeURL", subscribeUrl);
        root.put("SigningCertURL", "https://sns.us-east-1.amazonaws.com:" + port + "/cert.pem");

        var sb = new StringBuilder();
        sb.append("Message\n").append(message).append("\n")
          .append("MessageId\n").append(messageId).append("\n")
          .append("SubscribeURL\n").append(subscribeUrl).append("\n")
          .append("Timestamp\n").append(timestamp).append("\n")
          .append("Token\n").append(token).append("\n")
          .append("TopicArn\n").append(arn).append("\n")
          .append("Type\n").append("SubscriptionConfirmation").append("\n")
        ;

        var signature = signTextV1(sb.toString(), key);
        root.put("Signature", signature);
        return jsonMapper.writeValueAsString(root);
    }
    
    public static String signTextV1(String text, PrivateKey key)
        throws InvalidKeyException, NoSuchAlgorithmException, SignatureException
    {
        Signature rsaSig = Signature.getInstance("SHA1withRSA");
        rsaSig.initSign(key);
        rsaSig.update(text.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(rsaSig.sign());

    }


}
