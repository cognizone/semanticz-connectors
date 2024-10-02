package zone.cogni.semanticz.connectors.fuseki;

import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.query.DatasetFactory;
import zone.cogni.semanticz.connectors.utils.AbstractSparqlServiceTest;

public class FusekiSparqlServiceTest extends AbstractSparqlServiceTest<FusekiSparqlService> {

  private FusekiServer server;

  protected FusekiSparqlService createSUT() {
    server = FusekiServer.create()
            .loopback(true)
            .port(12345)
            .add("/rdf", DatasetFactory.create())
            .build();

    server.start();
    final FusekiConfig config = new FusekiConfig();
    config.setUrl("http://localhost:12345/rdf");
    return new FusekiSparqlService(config);
  }

  protected void disposeSUT(FusekiSparqlService service) {
    server.stop();
  }

}