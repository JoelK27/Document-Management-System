package at.technikum_wien.DocumentDAL.exceptions;

public class FileValidationException extends RuntimeException {
    public FileValidationException(String message) {
        super(message);
    }
}