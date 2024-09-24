package zone.cogni.core.util;

import org.joda.time.format.ISODateTimeFormat;

import java.text.SimpleDateFormat;
import java.util.*;

public class DateFormats {

  public static final String XML_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

  private static final DateFormatsThreadLocal FORMATS = new DateFormatsThreadLocal();

  private static SimpleDateFormat getDateFormat(Format format) {
    Map<String, SimpleDateFormat> instanceMap = FORMATS.get();
    String template = format.getDateFormatTemplate();
    if (instanceMap.containsKey(template)) {
      return instanceMap.get(template);
    }
    SimpleDateFormat dateFormat = new SimpleDateFormat(template);
    instanceMap.put(template, dateFormat);
    return dateFormat;
  }

  /**
   * Formats a Date to given date(time) representation.
   * If the passed date is null, an empty String will be returned.
   *
   * @param format The format to use.
   * @param date   The date to convert.
   * @return The formatted date.
   */
  public static String format(Date date, Format format) {
    if (date == null) return "";
    if (format == Format.FULLXMLDATETIME) {
      return ISODateTimeFormat.dateTime().print(date.getTime());
    }
    return getDateFormat(format).format(date);
  }

  private DateFormats() {
  }

  /**
   * XMLDATETIME: yyyy-MM-dd'T'HH:mm:ss
   * DATE: yyyy-MM-dd
   * DATETIME: yyyyMMdd-HHmmss
   * READABLEDATETIME: yyyy-MM-dd' 'HH:mm:ss
   * DAYFIRTFDATE: dd-MM-yyyy
   */
  public enum Format {
    /**
     * Format yyyy-MM-dd'T'HH:mm:ss
     */
    XMLDATETIME("yyyy-MM-dd'T'HH:mm:ss", true, true),
    /**
     * Format yyyy-MM-dd'T'HH:mm:ss.SSSZ (with jodaTime)
     */
    FULLXMLDATETIME(XML_DATE_TIME_FORMAT, true, true),
    /**
     * Format yyyy-MM-dd'T'HH:mm:ss.SSSZ (with java SimpleDateFormat)
     */
    JAVAFULLXMLDATETIME(XML_DATE_TIME_FORMAT, true, true),

    /**
     * Format yyyyMMdd-HHmmss.SSS
     */
    DATETIMEMS("yyyyMMdd-HHmmss.SSS", true, true),
    /**
     * Format: yyyy-MM-dd
     */
    DATE("yyyy-MM-dd", true, false),
    /**
     * Format: yyyyMMdd
     */
    SHORTDATE("yyyyMMdd", true, false),
    /**
     * Format: yyyyMMdd-HHmmss
     */
    DATETIME("yyyyMMdd-HHmmss", true, true),
    /**
     * Format: yyyyMMddHHmmss
     */
    UNREADABLEDATETIME("yyyyMMddHHmmss", true, true),
    /**
     * Format: yyyyMMdd-HHmmss
     */
    DATETIME_AS_FOLDERNAME("yyyyMMdd_HHmmss", true, true),
    /**
     * Format: yyyy-MM-dd' 'HH:mm:ss
     */
    READABLEDATETIME("yyyy-MM-dd' 'HH:mm:ss", true, true),
    /**
     * Format: dd-MM-yyyy
     */
    DAYFIRSTDATE("dd-MM-yyyy", true, false),
    /**
     * Format: dd-MM-yyyy HH:mm
     */
    DMY_HM("dd-MM-yyyy HH:mm", true, true),
    /**
     * Format: HH:mm
     */
    SHORT_TIME("HH:mm", false, true),
    /**
     * Format: HH:mm:ss
     */
    TIME("HH:mm:ss", false, true);

    private final String dateFormatTemplate;


    Format(String dateFormatTemplate, boolean showsDate, boolean showsTime) {
      this.dateFormatTemplate = dateFormatTemplate;

    }

    public String getDateFormatTemplate() {
      return dateFormatTemplate;
    }

  }

  private static class DateFormatsThreadLocal extends ThreadLocal<Map<String, SimpleDateFormat>> {
    @Override
    protected Map<String, SimpleDateFormat> initialValue() {
      return new HashMap<>();
    }
  }
}
