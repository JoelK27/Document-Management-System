package at.technikum_wien.DocumentDAL.exceptions;

public class DocumentNotFoundException extends RuntimeException {
    public DocumentNotFoundException(int id) {
        super("Document with id=" + id + " not found");
    }
}