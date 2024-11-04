package zone.cogni.semanticz.connectors.general;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class Config {
  /**
   * TODO SPARQL 1.1 endpoint URL
   */
  private String url;

  /**
   * TODO Username to authenticate to the SPARQL endpoint.
   */
  private String user;

  /**
   * TODO Password to authenticate to the SPARQL endpoint.
   */
  private String password;

  /**
   * TODO ???
   */
  private boolean graphCrudUseBasicAuth;
}
