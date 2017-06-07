package core.bot.slack;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author Spectre
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Controller {
    EventType[] events() default EventType.MESSAGE;
    
    String pattern() default "";
    
    String next() default "";
}
