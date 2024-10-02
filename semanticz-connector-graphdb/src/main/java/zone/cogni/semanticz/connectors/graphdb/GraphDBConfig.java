package zone.cogni.semanticz.connectors.graphdb;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import zone.cogni.semanticz.connectors.general.Config;

@Setter
@Getter
@Accessors(chain = true)
public class GraphDBConfig extends Config {

  private String repository;

  public GraphDBConfig() {
  }

  public GraphDBConfig(Config config) {
    setUrl(config.getUrl());
    setUser(config.getUser());
    setPassword(config.getPassword());
  }

  public String getSparqlEndpoint() {
    return getUrl() + "/repositories/" + getRepository();
  }

  public String getSparqlUpdateEndpoint() {
    return getSparqlEndpoint() + "/statements";
  }

  public String getImportTextEndpoint() {
    return getUrl() + "/rest/data/import/upload/" + getRepository() + "/text";
  }
}
