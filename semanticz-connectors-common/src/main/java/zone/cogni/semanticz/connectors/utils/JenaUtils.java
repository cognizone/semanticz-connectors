package zone.cogni.semanticz.connectors.utils;

import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFLanguages;
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

  public static Model readInto(File file, Model model) {
    try (InputStream inputStream = new FileInputStream(file)) {
      final String filePath = file.getAbsolutePath();
      // TODO simplify
      RDFReaderI reader = model.getReader(RDFLanguages.filenameToLang(filePath).toString());
      InternalRdfErrorHandler errorHandler = new InternalRdfErrorHandler(filePath);
      reader.setErrorHandler(errorHandler);
      reader.read(model, inputStream, null);

      if (errorHandler.isFailure()) {
        throw new RuntimeException(errorHandler.getInfo());
      }
      return model;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void write(Model model, File file) {
    try (final FileOutputStream fos = new FileOutputStream(file)) {
      model.write(fos);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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

