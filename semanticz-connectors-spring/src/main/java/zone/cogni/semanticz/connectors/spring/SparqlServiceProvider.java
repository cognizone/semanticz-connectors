package zone.cogni.semanticz.connectors.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import zone.cogni.semanticz.connectors.fuseki.FusekiConfig;
import zone.cogni.semanticz.connectors.fuseki.FusekiSparqlService;
import zone.cogni.semanticz.connectors.general.Config;
import zone.cogni.semanticz.connectors.jenamemory.JenaModelSparqlService;
import zone.cogni.semanticz.connectors.general.SparqlService;
import zone.cogni.semanticz.connectors.graphdb.GraphDBConfig;
import zone.cogni.semanticz.connectors.graphdb.GraphDBSparqlService;
import zone.cogni.semanticz.connectors.stardog.StardogSparqlService;
import zone.cogni.semanticz.connectors.virtuoso.VirtuosoSparqlService;

public class SparqlServiceProvider {
  private final String configPrefix;

  @Autowired
  private Environment environment;

  public SparqlServiceProvider(String configPrefix) {
    this.configPrefix = configPrefix.trim() + (configPrefix.endsWith(".") ? "" : ".");
  }

  public SparqlService createSparqlService(Enum enumValue) {
    String base = configPrefix + enumValue.name() + ".";

    String value = environment.getProperty(base + "type");
    if (value == null || value.isBlank()) {
      throw new RuntimeException("Type property not found: " + base + "type");
    }
    switch (value) {
      case "virtuoso":
        return new VirtuosoSparqlService(createDefaultConfig(base));
      case "fuseki":
        return new FusekiSparqlService(createFusekiConfig(base));
      case "inMemory":
        return new JenaModelSparqlService();
      case "graphdb":
        return new GraphDBSparqlService(createGraphDBConfig(base));
      case "stardog":
        return new StardogSparqlService(createDefaultConfig(base));
      default:
        throw new RuntimeException(String.format("SparqlService type %s unknown.", value));
    }
  }

  private Config createDefaultConfig(String base) {
    return fillDefaultConfig(new Config(), base);
  }

  private FusekiConfig createFusekiConfig(String base) {
    final FusekiConfig config = new FusekiConfig();
    fillDefaultConfig(config, base);
    return config.setQueryUrl(p(base, "queryUrl"))
            .setUpdateUrl(p(base, "updateUrl"))
            .setGraphStoreUrl(p(base, "graphStoreUrl"))
            .setOverwriteTurtleMimeType(p(base, "overwriteTurtleMimeType"));
  }

  private GraphDBConfig createGraphDBConfig(String base) {
    GraphDBConfig config = new GraphDBConfig();
    fillDefaultConfig(config, base);
    return config.setRepository(p(base, "repository"));
  }

  private Config fillDefaultConfig(Config config, String base) {
    return config
            .setUrl(p(base, "url"))
            .setUser(p(base, "user"))
            .setPassword(p(base, "password"))
            .setGraphCrudUseBasicAuth(Boolean.parseBoolean(p(base, "sparqlGraphCrudUseBasicAuth")));
  }

  private String p(final String base, final String property) {
    return environment.getProperty(base + property);
  }
}
