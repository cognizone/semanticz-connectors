package zone.cogni.libs.jena.utils;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zone.cogni.libs.core.CognizoneException;
import zone.cogni.libs.spring.utils.ResourceHelper;

import java.io.*;
import java.util.*;

public class JenaUtils {
  private static final Logger log = LoggerFactory.getLogger(JenaUtils.class);
  private static final Map<String, String> extensionToLanguageMap = Collections.synchronizedMap(new HashMap<>());

  static {
    extensionToLanguageMap.put("nt", "N-TRIPLE");
    extensionToLanguageMap.put("n3", "N3");
    extensionToLanguageMap.put("ttl", "TURTLE");
    extensionToLanguageMap.put("jsonld", "JSONLD");
  }

  private JenaUtils() {
  }

  public static Model create(Model model, boolean copyPrefixes) {
    Model newModel = ModelFactory.createDefaultModel();
    if (copyPrefixes) newModel.setNsPrefixes(model.getNsPrefixMap());
    newModel.add(model);
    return newModel;
  }

  public static Model create(Model... models) {
    Model result = ModelFactory.createDefaultModel();
    for (Model model : models) {
      result.add(model);
    }
    return result;
  }

  public static Model create(Map<String, String> namespaces, Model... models) {
    Model result = ModelFactory.createDefaultModel();
    result.setNsPrefixes(namespaces);
    for (Model model : models) {
      result.add(model);
    }
    return result;
  }

  public static Model read(org.springframework.core.io.Resource... resources) {
    return read(Arrays.asList(resources));
  }

  public static Model read(Iterable<org.springframework.core.io.Resource> resources) {
    return read(resources, null);
  }

  public static Model read(Iterable<org.springframework.core.io.Resource> resources, Map<String, Object> readerProperties) {
    Model model = ModelFactory.createDefaultModel();

    for (org.springframework.core.io.Resource resource : resources) {
      try (InputStream inputstream = ResourceHelper.getInputStream(resource)) {
        InternalRdfErrorHandler errorHandler = new InternalRdfErrorHandler(resource.getDescription());

        RDFReaderI rdfReader = getReader(model, resource, errorHandler, readerProperties);
        rdfReader.read(model, inputstream, null);

        Preconditions.checkState(!errorHandler.isFailure(), errorHandler.getInfo());
      } catch (Exception e) {
        closeQuietly(model);
        throw CognizoneException.rethrow(e);
      }
    }

    return model;
  }

  private static RDFReaderI getReader(Model model, org.springframework.core.io.Resource resource, RDFErrorHandler rdfErrorHandler, Map<String, Object> readerProperties) {
    return getReader(model, rdfErrorHandler, readerProperties, getRdfSyntax(resource));
  }

  private static RDFReaderI getReader(Model model, RDFErrorHandler rdfErrorHandler, Map<String, Object> readerProperties, String language) {
    RDFReaderI rdfReader = getReaderByRdfSyntax(model, language);
    rdfReader.setErrorHandler(rdfErrorHandler);
    if (readerProperties == null) return rdfReader;

    for (String propertyName : readerProperties.keySet()) {
      rdfReader.setProperty(propertyName, readerProperties.get(propertyName));
    }
    return rdfReader;
  }

  private static RDFReaderI getReaderByRdfSyntax(Model model, String language) {
    try {
      return model.getReader(language);
    } catch (IllegalStateException ignored) {
      return model.getReader();
    }
  }

  private static String getRdfSyntax(org.springframework.core.io.Resource resource) {
    String extension = StringUtils.lowerCase(StringUtils.substringAfterLast(resource.getFilename(), "."));

    // when return value is null, fall back to RDF/XML
    return extensionToLanguageMap.getOrDefault(extension, null);
  }

  public static String toString(Model model) {
    return toString(model, "RDF/XML");
  }

  public static String toString(Model model, String language) {
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      model.write(out, language);

      return out.toString("UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  public static byte[] toByteArray(Model model, TripleSerializationFormat tripleSerializationFormat) {
    return toByteArray(model, tripleSerializationFormat.getJenaLanguage());
  }

  public static byte[] toByteArray(Model model, String language) {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      model.write(outputStream, language);
      outputStream.flush();
      return outputStream.toByteArray();
    } catch (IOException e) {
      throw CognizoneException.rethrow(e);
    }
  }


  public static void closeQuietly(Model... models) {
    Arrays.stream(models).filter(Objects::nonNull).filter(model -> !model.isClosed()).forEach(model -> {
      try {
        model.close();
      } catch (Exception e) {
        log.warn("Closing model failed.", e);
      }
    });
  }

  private static class InternalRdfErrorHandler implements RDFErrorHandler {

    private final String info;
    private boolean failure;

    private InternalRdfErrorHandler(String loadedFile) {
      info = "Load rdf file (" + loadedFile + ") problem.";
    }

    public boolean isFailure() {
      return failure;
    }

    public String getInfo() {
      return info;
    }

    @Override
    public void warning(Exception e) {
      String message = e.getMessage();
      if (null != message && message.contains("ISO-639 does not define language:")) {
        log.warn("{}: {}", info, message);
        return;
      }
      log.warn(info, e);
    }

    @Override
    public void error(Exception e) {
      failure = true;
      log.error(info, e);
    }

    @Override
    public void fatalError(Exception e) {
      failure = true;
      log.error(info, e);
    }
  }
}

