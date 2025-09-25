package at.technikum_wien.DocumentDAL.validation.implementation;

import at.technikum_wien.DocumentDAL.validation.AllowedMime;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AllowedMimeValidator implements ConstraintValidator<AllowedMime, MultipartFile> {
    private Set<String> allowed;

    @Override
    public void initialize(AllowedMime annotation) {
        this.allowed = new HashSet<>(Arrays.asList(annotation.types()));
    }

    @Override
    public boolean isValid(MultipartFile value, ConstraintValidatorContext context) {
        if (value == null) return true;
        String ct = value.getContentType();
        // Bei manchen Browsern kann ct null sein; dann ablehnen
        return ct != null && allowed.contains(ct.toLowerCase());
    }
}
