package com.daasuu.mp4compose.logger;

/**
 * The logger interface used to log information to the console.
 */
public interface Logger {

    /**
     * Logs a debug message.
     *
     * @param tag     The tag of the message.
     * @param message The message body.
     */
    void debug(String tag, String message);

    /**
     * Logs an error message.
     *
     * @param tag     The tag of the message.
     * @param message The message body.
     * @param error   The cause of the error.
     */
    void error(String tag, String message, Throwable error);

    /**
     * Logs a warning message.
     *
     * @param tag     The tag of the message.
     * @param message The message body.
     */
    void warning(String tag, String message);

}
