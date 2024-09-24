package zone.cogni.libs.core.utils;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.Charset;

public class FileHelper {

  private FileHelper() {
  }

  /**
   * @deprecated use {@link #writeStringToFile(File, String, Charset)} instead
   */
  @Deprecated
  public static void writeStringToFile(File file, String data) {
    try {
      FileUtils.writeStringToFile(file, data);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
