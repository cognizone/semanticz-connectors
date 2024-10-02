package zone.cogni.semanticz.connectors.virtuoso;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.http.auth.AuthEnv;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionRemote;
import zone.cogni.semanticz.connectors.general.SparqlService;
import zone.cogni.semanticz.connectors.general.Config;
import zone.cogni.semanticz.connectors.general.RDFConnectionSparqlService;
import zone.cogni.semanticz.connectors.utils.Constants;

import java.net.URI;

public class VirtuosoSparqlService extends RDFConnectionSparqlService implements
    SparqlService {

  private final Config config;

  public VirtuosoSparqlService(Config config) {
    this.config = config;
    AuthEnv.get()
        .registerUsernamePassword(URI.create(StringUtils.substringBeforeLast(config.getUrl(), "/")),
            this.config.getUser(), this.config.getPassword());
  }

  protected RDFConnection getConnection() {
    return RDFConnectionRemote
        .newBuilder()
        .queryEndpoint(config.getUrl())
        .updateEndpoint(config.getUrl())
        .destination(config.getUrl())
        .gspEndpoint(VirtuosoHelper.getVirtuosoGspFromSparql(config.getUrl()))
        .build();
  }

  protected RDFConnection getConstructConnection() {
    return RDFConnectionRemote
        .newBuilder()
        .queryEndpoint(config.getUrl())
        .updateEndpoint(config.getUrl())
        .destination(config.getUrl())
        .acceptHeaderQuery(Constants.TEXT_TURTLE)
        .gspEndpoint(VirtuosoHelper.getVirtuosoGspFromSparql(config.getUrl()))
        .build();
  }
}