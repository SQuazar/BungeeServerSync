package net.quazar.mg.exception;

public class ServerInfoNotFoundException extends ServerInfoException {
    public ServerInfoNotFoundException(String message) {
        super(message);
    }

    public ServerInfoNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServerInfoNotFoundException(Throwable cause) {
        super(cause);
    }
}
