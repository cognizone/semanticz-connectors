package zone.cogni.semanticz.connectors.stardog;

import org.apache.http.HttpHeaders;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionBuilder;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTPBuilder;
import zone.cogni.semanticz.connectors.utils.Constants;
import zone.cogni.semanticz.connectors.utils.HttpClientUtils;
import zone.cogni.semanticz.connectors.utils.JenaUtils;
import zone.cogni.semanticz.connectors.utils.TripleSerializationFormat;
import zone.cogni.semanticz.connectors.general.SparqlService;
import zone.cogni.semanticz.connectors.general.Config;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

public class StardogSparqlService implements SparqlService {
  private final String endpointUrl;
  private final HttpClient httpClient;

  public StardogSparqlService(Config config) {
    endpointUrl = config.getUrl();
    httpClient = HttpClientUtils.createHttpClientBuilder(config.getUser(), config.getPassword()).build();
  }

  private QueryExecutionBuilder getQueryExecutionBuilder() {
    return QueryExecutionHTTPBuilder.service(endpointUrl + "/query").httpClient(httpClient);
  }

  @Override
  public void uploadTtlFile(File file) {
    final HttpRequest request;
    try {
      request = HttpRequest
          .newBuilder(URI.create(endpointUrl))
          .POST(HttpRequest.BodyPublishers.ofFile(file.toPath()))
          .header(HttpHeaders.CONTENT_TYPE, Lang.TURTLE.getHeaderString()+";charset=utf-8")
          .build();
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
    HttpClientUtils.execute(request,httpClient);
  }

  @Override
  public Model queryForModel(String query) {
    try (QueryExecution queryExecution = getQueryExecutionBuilder().query(query).build()) {
      // jena adds empty defaultGraph param to URL because defaultGraph is null but is a "value", stardog doesn't like that
      // TODO check with empty default graph ((QueryEngineHTTP) queryExecution).setDefaultGraphURIs(Collections.emptyList());
      return queryExecution.execConstruct();
    }
  }

  @Override
  public void executeUpdateQuery(String updateQuery) {
    final HttpRequest request = HttpRequest
        .newBuilder(URI.create(endpointUrl + "/update"))
        .POST(HttpRequest.BodyPublishers.ofString("update=" + URLEncoder.encode(updateQuery, StandardCharsets.UTF_8), StandardCharsets.UTF_8))
        .header(HttpHeaders.CONTENT_TYPE, Constants.APPLICATION_FORM_URLENCODED_VALUE)
        .build();
    HttpClientUtils.execute(request, httpClient);
  }

  @Override
  public boolean executeAskQuery(String askQuery) {
    try (QueryExecution queryExecution = getQueryExecutionBuilder().query(askQuery).build()) {
      // jena adds empty defaultGraph param to URL because defaultGraph is null but is a "value", stardog doesn't like that
      // TODO check with empty default graph ((QueryEngineHTTP) queryExecution).setDefaultGraphURIs(Collections.emptyList());
      return queryExecution.execAsk();
    }
  }

  @Override
  public void upload(Model model, String graphUri) {
    updateGraph(graphUri, model);
  }

  @Override
  public void updateGraph(String graphUri, Model model) {
    upload(model, graphUri, false);
  }

  @Override
  public void replaceGraph(String graphUri, Model model) {
    upload(model, graphUri, true);
  }

  private void upload(Model model, String graphUri, boolean replace) {
    String graphStoreUrl = endpointUrl + "?graph=" + URLEncoder.encode(graphUri, StandardCharsets.UTF_8);
    byte[] body = JenaUtils.toByteArray(model, TripleSerializationFormat.turtle);
    final HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(graphStoreUrl))
        .header(HttpHeaders.CONTENT_TYPE, Lang.TURTLE.getHeaderString() + ";charset=utf-8");
    final BodyPublisher p = HttpRequest.BodyPublishers.ofByteArray(body);
    final HttpRequest request = (replace ? builder.PUT(p) : builder.POST(p)).build();
    HttpClientUtils.execute(request, httpClient);
  }

  @Override
  public <R> R executeSelectQuery(String query, Function<ResultSet, R> resultHandler) {
    try (QueryExecution queryExecution = getQueryExecutionBuilder().query(query).build()) {
      // jena adds empty defaultGraph param to URL because defaultGraph is null but is a "value", stardog doesn't like that
      // TODO check with empty default graph ((QueryEngineHTTP) queryExecution).setDefaultGraphURIs(Collections.emptyList());
      return resultHandler.apply(queryExecution.execSelect());
    }
  }

  @Override
  public void dropGraph(String graphUri) {
    executeUpdateQuery("drop silent graph <" + graphUri + ">");
  }
}