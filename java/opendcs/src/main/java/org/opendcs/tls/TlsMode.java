package org.opendcs.tls;

public enum TlsMode {
    /**
     * No Encryption
     */
    NONE,
    /**
     * On server: allow clients to initiate TLS after connection if they support it
     * On client: enable TLS on the current connection. Error if enabling TLS fails
     */
    START_TLS,
    /**
     * Require direct TLS
     */
    TLS
}
