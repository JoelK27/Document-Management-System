package at.technikum_wien.DocumentDAL.validation;

import at.technikum_wien.DocumentDAL.validation.implementation.AllowedMimeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = AllowedMimeValidator.class)
@Target({ ElementType.PARAMETER, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowedMime {
    String message() default "Unsupported file type";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    String[] types();
}