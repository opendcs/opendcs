package decodes.datasource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.kex.BuiltinDHFactories;
import org.apache.sshd.common.keyprovider.MappedKeyPairProvider;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.kex.DHGEXServer;
import org.apache.sshd.server.kex.DHGServer;
import org.apache.sshd.scp.server.ScpCommandFactory;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.SFTPv3Client;
import ch.ethz.ssh2.SFTPv3FileHandle;

class GanymedSshInteropTest
{
    private static final String USERNAME = "testuser";
    private static final String PASSWORD = "testpass";
    private static final byte[] CONTENT = "OpenDCS SSH transfer test\n".getBytes();

    @TempDir
    Path tempDirectory;

    private SshServer server;
    private Path remoteDirectory;

    @BeforeEach
    void startServer() throws Exception
    {
        remoteDirectory = Files.createDirectory(tempDirectory.resolve("remote"));
        server = SshServer.setUpDefaultServer();
        server.setPort(0);
        server.setKeyExchangeFactories(java.util.List.of(
            DHGEXServer.newFactory(BuiltinDHFactories.dhgex),
            DHGServer.newFactory(BuiltinDHFactories.dhg14),
            DHGServer.newFactory(BuiltinDHFactories.dhg1)));
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair hostKey = keyPairGenerator.generateKeyPair();
        server.setKeyPairProvider(new MappedKeyPairProvider(hostKey));
        server.setPasswordAuthenticator((username, password, session) ->
            USERNAME.equals(username) && PASSWORD.equals(password));
        server.setFileSystemFactory(new VirtualFileSystemFactory(remoteDirectory));
        server.setCommandFactory(new ScpCommandFactory.Builder().build());
        server.setSubsystemFactories(java.util.List.of(new SftpSubsystemFactory.Builder().build()));
        server.start();
    }

    @AfterEach
    void stopServer() throws Exception
    {
        if (server != null)
            server.stop(true);
    }

    @Test
    void ganymedScpClientDownloadsFromSshdServer() throws Exception
    {
        Path remoteFile = remoteDirectory.resolve("incoming/message.txt");
        Files.createDirectories(remoteFile.getParent());
        Files.write(remoteFile, CONTENT);

        Connection connection = connect();
        try
        {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            new SCPClient(connection).get("incoming/message.txt", output);
            assertArrayEquals(CONTENT, output.toByteArray());
        }
        finally
        {
            connection.close();
        }
    }

    @Test
    void ganymedSftpClientDownloadsFromSshdServer() throws Exception
    {
        Files.write(remoteDirectory.resolve("message.txt"), CONTENT);

        Connection connection = connect();
        SFTPv3Client sftp = new SFTPv3Client(connection);
        try
        {
            SFTPv3FileHandle handle = sftp.openFileRO("message.txt");
            try
            {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                long offset = 0;
                int length;
                while ((length = sftp.read(handle, offset, buffer, 0, buffer.length)) != -1)
                {
                    output.write(buffer, 0, length);
                    offset += length;
                }
                assertArrayEquals(CONTENT, output.toByteArray());
            }
            finally
            {
                sftp.closeFile(handle);
            }
        }
        finally
        {
            sftp.close();
            connection.close();
        }
    }

    private Connection connect() throws IOException
    {
        Connection connection = new Connection("localhost", server.getPort());
        connection.connect();
        if (!connection.authenticateWithPassword(USERNAME, PASSWORD))
        {
            connection.close();
            throw new IOException("SSH authentication failed");
        }
        return connection;
    }
}