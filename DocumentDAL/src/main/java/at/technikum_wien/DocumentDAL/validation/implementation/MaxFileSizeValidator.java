package at.technikum_wien.DocumentDAL.validation.implementation;

import at.technikum_wien.DocumentDAL.validation.MaxFileSize;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

public class MaxFileSizeValidator implements ConstraintValidator<MaxFileSize, MultipartFile> {
    private long max;

    @Override
    public void initialize(MaxFileSize annotation) {
        this.max = annotation.bytes();
    }

    @Override
    public boolean isValid(MultipartFile value, ConstraintValidatorContext context) {
        if (value == null) return true; // null separat prüfen
        return value.getSize() <= max;
    }
}
