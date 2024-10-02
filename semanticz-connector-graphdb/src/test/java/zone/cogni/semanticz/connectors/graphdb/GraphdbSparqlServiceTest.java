package zone.cogni.semanticz.connectors.graphdb;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import zone.cogni.semanticz.connectors.utils.HttpClientUtils;
import zone.cogni.semanticz.connectors.general.SparqlService;
import zone.cogni.semanticz.connectors.utils.AbstractSparqlServiceTest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.function.Function;

import static zone.cogni.semanticz.connectors.utils.HttpClientUtils.execute;

@Disabled("An integration test dependent on a running GraphDB instance. To run it manually, set the GraphDBConfig below properly and run the tests.")
public class GraphdbSparqlServiceTest extends AbstractSparqlServiceTest {

  private static SparqlService sut;

  @BeforeEach
  public void init() throws URISyntaxException, IOException {
    final GraphDBConfig config = new GraphDBConfig();
     config.setUrl("http://localhost:7200/");
     config.setRepository("test");
     config.setUser("test");
     config.setPassword("test");

    final String trig = IOUtils.toString(
        Objects.requireNonNull(GraphdbSparqlServiceTest.class.getResource("/dataset.trig")).toURI(),Charset.defaultCharset());
    final HttpRequest request = HttpRequest
            .newBuilder()
            .PUT(HttpRequest.BodyPublishers.ofString(trig))
            .header(HttpHeaders.CONTENT_TYPE, Lang.TRIG.getHeaderString())
            .uri(URI.create(config.getSparqlUpdateEndpoint()))
            .build();
    final HttpClient httpClient = HttpClientUtils.createHttpClientBuilder(config.getUser(), config.getPassword()).build();
    execute(request, httpClient);

    sut = new GraphDBSparqlService(config);
  }

  @Override
  protected SparqlService getSUT() {
    return sut;
  }

  @Test
  public void testSelectQueryReturnsResultsFromAllGraphs() {
    final ResultSet result = getSUT().executeSelectQuery("SELECT * { ?s ?p ?o }", Function.identity());
    while (result.hasNext()) {
      result.next();
    }
    Assertions.assertEquals(2, result.getRowNumber());
  }

  @Test
  public void testQueryForModelReturnsUnionOfAllGraphs() {
    final Model model = getSUT().queryForModel("CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }");
    Assertions.assertEquals(2, model.size());
  }
}