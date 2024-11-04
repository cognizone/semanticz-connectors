package zone.cogni.semanticz.connectors.spring;

import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(SparqlConfiguration.class)

public @interface EnableSparqlServer {

  /**
   * The prefix of the configuration.
   */
  String value();

}