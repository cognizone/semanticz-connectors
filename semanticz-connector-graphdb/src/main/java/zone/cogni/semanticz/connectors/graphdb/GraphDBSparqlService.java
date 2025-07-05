package zone.cogni.semanticz.connectors.graphdb;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTP;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTPBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zone.cogni.semanticz.connectors.general.SparqlService;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;


public class GraphDBSparqlService implements SparqlService {

    private static final Logger log = LoggerFactory.getLogger(GraphDBSparqlService.class);
    private final GraphDBConfig config;
    private HttpClient httpClient;

    private synchronized HttpClient getHttpClient() {
        if (httpClient != null) return httpClient;

        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .proxy(ProxySelector.getDefault());

        if (StringUtils.isNoneBlank(config.getUser(), config.getPassword())) {
            builder.authenticator(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(
                            config.getUser(),
                            config.getPassword().toCharArray());
                }
            });
        } else if (!StringUtils.isAllBlank(config.getUser(), config.getPassword())) {
            log.error("Endpoint credentials not properly configured");
        }

        httpClient = builder.build();
        return httpClient;
    }

    public GraphDBSparqlService(GraphDBConfig config) {
        this.config = config;
    }

    @Override
    public Model executeConstructQuery(String constructQuery) {
        Objects.requireNonNull(constructQuery, "constructQuery must not be null");

        Query query = QueryFactory.create(constructQuery, Syntax.syntaxARQ);

        QueryExecutionHTTPBuilder builder = QueryExecutionHTTP
                .service(config.getSparqlEndpoint())
                .query(query)
                .httpClient(getHttpClient());

        try (QueryExecution qExec = builder.build()) {
            return qExec.execConstruct();
        }
    }

    @Override
    public void executeUpdateQuery(String updateQuery) {
        String formBody = "update=" + URLEncoder.encode(updateQuery, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getSparqlUpdateEndpoint()))
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build();

        executeHttpRequest(request, 204);
    }

    @Override
    public <R> R executeSelectQuery(String query, Function<ResultSet, R> resultHandler) {
        try (QueryExecution queryExecution = QueryExecutionHTTP.service(config.getSparqlEndpoint())
                .queryString(query)
                .httpClient(getHttpClient())
                .build()) {
            return resultHandler.apply(queryExecution.execSelect());
        }
    }

    @Override
    public boolean executeAskQuery(String askQuery) {
        try (QueryExecution queryExecution = QueryExecutionHTTP.service(config.getSparqlEndpoint())
                .queryString(askQuery)
                .httpClient(getHttpClient())
                .build()) {
            return queryExecution.execAsk();
        }
    }


    @Override
    public void dropGraph(String graphUri) {
        executeUpdateQuery("clear graph <" + graphUri + ">");
    }

    @Override
    public void updateGraph(String graphUri, Model model) {
        Objects.requireNonNull(graphUri, "graphUri must not be null");
        Objects.requireNonNull(model,    "model must not be null");

        if (model.isEmpty()) {
            log.debug("addData called with an empty model – nothing to do.");
            return;
        }

        StringWriter writer = new StringWriter();
        model.write(writer, "ttl");
        String turtle = writer.toString();

        String base = config.getSparqlUpdateEndpoint();      // …/statements
        URI endpoint;
        if (StringUtils.isBlank(graphUri)) {
            endpoint = URI.create(base);                       // default graph
        } else {
            String encCtx = URLEncoder.encode("<" + graphUri + ">", StandardCharsets.UTF_8);
            endpoint = URI.create(base + "?context=" + encCtx); // named graph
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(endpoint)
                .header("Content-Type", Lang.TURTLE.getHeaderString()) // text/turtle
                .POST(HttpRequest.BodyPublishers.ofString(turtle, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<Void> resp = getHttpClient()
                    .send(request, HttpResponse.BodyHandlers.discarding());

            int code = resp.statusCode();
            if (code != 200 && code != 202 && code != 204) {
                throw new RuntimeException("Failed to add data. HTTP " + code);
            }
            log.debug("Uploaded {} triples to {}", model.size(),
                    StringUtils.defaultIfBlank(graphUri, "default graph"));

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("I/O error while uploading triples", e);
        }
        log.debug("Added {} triples to existing graph {}", model.size(), graphUri);
    }

    private void executeHttpRequest(HttpRequest request, int expectedCode) {
        try {
            HttpResponse<String> response =
                    getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            checkResponse(response, expectedCode);

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private void checkResponse(HttpResponse<String> response, int expectedCode) {
        int actual = response.statusCode();
        if (actual == expectedCode) return;

        String body = response.body();
        if (body == null || body.isBlank()) {
            body = "No response body from server.";
        }

        String msg = "Update didn't answer " + expectedCode +
                " code: HTTP " + actual + ". " + body;

        throw new RuntimeException(msg);
    }

    @Override
    public void replaceGraph(String graphUri, Model model) {
        StringWriter writer = new StringWriter();
        model.write(writer, "ttl");
        String turtleContent = writer.toString();

        log.info("Replacing graph {} with single PUT request", graphUri);

        String encoded = URLEncoder.encode("<" + graphUri + ">", StandardCharsets.UTF_8);
        URI endpoint = URI.create(config.getSparqlUpdateEndpoint() + "?context=" + encoded);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(endpoint)
                .header("Content-Type", Lang.TURTLE.getHeaderString())   // "text/turtle"
                .PUT(HttpRequest.BodyPublishers.ofString(turtleContent, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<Void> resp = getHttpClient()
                    .send(request, HttpResponse.BodyHandlers.discarding());

            int code = resp.statusCode();
            if (code != 200 && code != 204 && code != 202) {
                log.error("Failed to upload Turtle data. HTTP {} {}", code, resp);
                throw new RuntimeException("Failed to upload TTL data. HTTP " + code);
            }
            log.debug("Graph {} successfully replaced (HTTP {})", graphUri, code);

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();               // preserve interrupt flag
            log.error("Error replacing Turtle data to GraphDB", e);
            throw new RuntimeException("I/O error during graph replacement", e);
        }
    }

}