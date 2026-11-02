package com.cardinalstar.cubicchunks.server.chunkio;

public class LoadFailureException extends RuntimeException {

    public LoadFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
