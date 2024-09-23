package zone.cogni.libs.core.utils;

import org.apache.commons.io.IOUtils;

import java.io.*;

public class IOHelper {
  public static <X extends Flushable & Closeable> void flushAndClose(X closeFlusher) {
    if (closeFlusher == null) return;

    try {
      closeFlusher.flush();
    }
    catch (IOException ignore) {
    }

    try {
      closeFlusher.close();
    }
    catch (IOException ignore) {
    }
  }

  public static <X extends Flushable> void flush(X closeFlusher) {
    if (closeFlusher == null) return;

    try {
      closeFlusher.flush();
    }
    catch (IOException ignore) {
    }
  }

  public static long copyLarge(InputStream inputStream, OutputStream outputStream) {
    try {
      return IOUtils.copyLarge(inputStream, outputStream);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
