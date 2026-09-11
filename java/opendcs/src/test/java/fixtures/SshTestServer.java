package fixtures;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.List;

import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.kex.BuiltinDHFactories;
import org.apache.sshd.common.keyprovider.MappedKeyPairProvider;
import org.apache.sshd.scp.server.ScpCommandFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.kex.DHGEXServer;
import org.apache.sshd.server.kex.DHGServer;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;

/**
 * An in-process SSH server (Apache MINA sshd) that serves a temporary directory over both SCP
 * and SFTP.
 * 
 * <p>Intended for tests that exercise the OpenDCS SSH clients against a real protocol
 *
 * <p>Note on key exchange: the server is deliberately configured with the legacy
 * diffie-hellman-group1/group14/group-exchange factories because the ganymed-ssh2 client cannot
 * negotiate anything newer. Once the OpenDCS clients are migrated to MINA sshd, remove the
 * {@code setKeyExchangeFactories} call so the tests run over the modern defaults.
 */
public final class SshTestServer implements AutoCloseable
{
    /** User name the server accepts. */
    public static final String USERNAME = "testuser";
    /** Password the server accepts for {@link #USERNAME}. */
    public static final String PASSWORD = "testpass";

    private final SshServer server;
    private final Path remoteDirectory;

    /**
     * Starts a server rooted at the given directory.
     *
     * @param remoteDirectory directory exposed to clients as the root of the remote file system
     */
    public SshTestServer(Path remoteDirectory) throws IOException, GeneralSecurityException
    {
        this.remoteDirectory = remoteDirectory;
        Files.createDirectories(remoteDirectory);

        server = SshServer.setUpDefaultServer();
        server.setHost("localhost");
        server.setPort(0);
        server.setKeyExchangeFactories(List.of(
            DHGEXServer.newFactory(BuiltinDHFactories.dhgex),
            DHGServer.newFactory(BuiltinDHFactories.dhg14),
            DHGServer.newFactory(BuiltinDHFactories.dhg1)));
        server.setKeyPairProvider(new MappedKeyPairProvider(generateHostKey()));
        server.setPasswordAuthenticator((username, password, session) ->
            USERNAME.equals(username) && PASSWORD.equals(password));
        server.setFileSystemFactory(new VirtualFileSystemFactory(remoteDirectory));
        server.setCommandFactory(new ScpCommandFactory.Builder().build());
        server.setSubsystemFactories(List.of(new SftpSubsystemFactory.Builder().build()));
        server.start();
    }

    private static KeyPair generateHostKey() throws GeneralSecurityException
    {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }

    /** @return the ephemeral port the server is listening on. */
    public int getPort()
    {
        return server.getPort();
    }

    /** @return the directory clients see as the root of the remote file system. */
    public Path getRemoteDirectory()
    {
        return remoteDirectory;
    }

    /**
     * Creates a file on the server, creating parent directories as needed.
     *
     * @param relativePath path relative to the remote root, using '/' as the separator
     * @param content bytes to write
     * @return the path of the created file on the local disk
     */
    public Path writeRemoteFile(String relativePath, byte[] content) throws IOException
    {
        Path file = remoteDirectory.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.write(file, content);
        return file;
    }

    @Override
    public void close() throws IOException
    {
        server.stop(true);
    }
}
