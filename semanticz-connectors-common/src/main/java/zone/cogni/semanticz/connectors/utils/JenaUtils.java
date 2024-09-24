package zone.cogni.semanticz.connectors.utils;

import org.apache.jena.rdf.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zone.cogni.semanticz.connectors.CognizoneException;

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

  public static byte[] toByteArray(Model model, TripleSerializationFormat tripleSerializationFormat) {
    return toByteArray(model, tripleSerializationFormat.getJenaLanguage());
  }

  public static byte[] toByteArray(Model model, String language) {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      model.write(outputStream, language);
      outputStream.flush();
      return outputStream.toByteArray();
    } catch (IOException e) {
      throw new CognizoneException(e);
    }
  }

  public static void closeQuietly(Iterable<Model> models) {
    for (Model model : models) {
      if (model == null) {
        continue;
      }

      if (model.isClosed()) {
        log.warn("Closing an already closed model.");
        continue;
      }

      try {
        model.close();
      } catch (Exception e) {
        log.warn("Closing model failed.", e);
      }
    }
  }
}

