package zone.cogni.semanticz.connectors.fuseki;

import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import zone.cogni.semanticz.connectors.general.SparqlService;
import zone.cogni.semanticz.connectors.utils.AbstractSparqlServiceTest;

import java.net.URISyntaxException;
import java.util.Objects;

public class FusekiSparqlServiceTest extends AbstractSparqlServiceTest {

  private static FusekiServer server;

  private static SparqlService sut;

  @BeforeEach
  public void init() throws URISyntaxException {
    server = FusekiServer.create().port(12345)
        .add("/rdf", RDFDataMgr.loadDataset(
            Objects.requireNonNull(AbstractSparqlServiceTest.class.getResource("/dataset.trig"))
                .toURI()
                .toString())).build();
    server.start();
    final FusekiConfig config = new FusekiConfig();
    config.setUrl("http://localhost:12345/rdf");
    sut = new FusekiSparqlService(config);
  }

  @Override
  protected SparqlService getSUT() {
    return sut;
  }

  @AfterEach
  public void destroy() {
    server.stop();
  }

}