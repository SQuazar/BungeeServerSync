package net.quazar.bsync.exception;

public class GameServerNotFoundException extends ServerInfoException {
    public GameServerNotFoundException(String message) {
        super(message);
    }

    public GameServerNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public GameServerNotFoundException(Throwable cause) {
        super(cause);
    }
}
