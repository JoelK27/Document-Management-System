package at.technikum_wien.ocrworker.exceptions;

public class GenAiException extends Exception {
    public GenAiException(String message) { super(message); }
    public GenAiException(String message, Throwable t) { super(message, t); }
}