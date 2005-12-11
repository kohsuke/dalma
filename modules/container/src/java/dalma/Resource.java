package dalma;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 * Identifies a field/method that needs to be injected by the dalma container.
 *
 * <p>
 * A resource must be one of the following types:
 *
 * <ol>
 *  <li>{@link EndPoint}-derived type.
 *      The container will require the user to configure an endpoint and injects the
 *      configured endpoint.
 *  <li>{@link String}
 *      The container will inject a string that the user configured.
 *  <li>{@code boolean}
 *      The container will inject a true/false option that the user configured.
 *  <li>{@code int}
 *      The container will inject a number that the user configured.
 * </ol>
 *
 * <p>
 * ... and it must be either a public field of the form {@code public T xyz}
 * or a public setter method of the form {@code public void setXyz(T)}.
 *
 * <p>
 * The container uses these annotations to determine what configuration is needed
 * by a program.
 *
 * @author Kohsuke Kawaguchi
 */
@Retention(RUNTIME)
@Target({FIELD, METHOD})
public @interface Resource {
    /**
     * Human-readable message that explains what this resource is.
     *
     * This message is presented to the user in the configuration screen.
     * For example, this could be something like
     * "e-mail address that is connected to the daemon" (for e-mail endpoint)
     * or maybe "the greeting message" (for a string resource.)
     */
    String description() default "";

    /**
     * Flag that indicates if this resource can be absent.
     *
     * <p>
     * If true, the user may choose not to set the value (in which case
     * the VM-default value or null will be injected.) If false, which
     * is the default, the resource must be configured by the user to
     * a non-null value.
     */
    boolean optional() default false;
}
