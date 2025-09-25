package at.technikum_wien.DocumentDAL.validation;

import at.technikum_wien.DocumentDAL.validation.implementation.MaxFileSizeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = MaxFileSizeValidator.class)
@Target({ ElementType.PARAMETER, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxFileSize {
    String message() default "File exceeds max size";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    long bytes();
}
