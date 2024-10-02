package zone.cogni.semanticz.connectors.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Map;
import java.util.Objects;

@Configuration
public class SparqlConfiguration implements ImportAware {

  private String configPrefix;

  @Override
  public void setImportMetadata(AnnotationMetadata importMetadata) {
    Map<String, Object> annotationAttributes = Objects.requireNonNull(importMetadata.getAnnotationAttributes(EnableSparqlServer.class.getName()), "No EnableSparqlServer annotations found");
    configPrefix = (String) annotationAttributes.get("value");
  }

  @Bean
  public SparqlServiceProvider sparqlServiceProvider() {
    return new SparqlServiceProvider(configPrefix);
  }

}