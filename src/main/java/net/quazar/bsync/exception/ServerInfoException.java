package net.quazar.bsync.exception;

public class ServerInfoException extends RuntimeException {

    public ServerInfoException(String message) {
        super(message);
    }

    public ServerInfoException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServerInfoException(Throwable cause) {
        super(cause);
    }
}
