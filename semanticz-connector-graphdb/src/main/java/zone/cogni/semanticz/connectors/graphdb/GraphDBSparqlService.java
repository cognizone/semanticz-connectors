/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package zone.cogni.semanticz.connectors.graphdb;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTP;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTPBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zone.cogni.semanticz.connectors.general.SparqlService;
import zone.cogni.semanticz.connectors.utils.HttpClientUtils;

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
    private static final String HTTP_METHOD_PUT = "PUT";
    private static final String HTTP_METHOD_POST = "POST";
    
    private final GraphDBConfig config;
    private HttpClient httpClient;

    public GraphDBSparqlService(GraphDBConfig config) {
        this.config = config;
    }

    private synchronized HttpClient getHttpClient() {
        if (httpClient != null) return httpClient;

        httpClient = HttpClientUtils.createHttpClientBuilder(
                config.getUser(),
                config.getPassword(),
                Duration.ofSeconds(config.getConnectTimeoutSeconds()),
                true,  // followRedirects
                true   // useSystemProxy
        ).build();
        
        return httpClient;
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

    private QueryExecution createQueryExecution(String query) {
        return QueryExecutionHTTP.service(config.getSparqlEndpoint())
                .queryString(query)
                .httpClient(getHttpClient())
                .build();
    }

    @Override
    public <R> R executeSelectQuery(String query, Function<ResultSet, R> resultHandler) {
        try (QueryExecution queryExecution = createQueryExecution(query)) {
            return resultHandler.apply(queryExecution.execSelect());
        }
    }

    @Override
    public boolean executeAskQuery(String askQuery) {
        try (QueryExecution queryExecution = createQueryExecution(askQuery)) {
            return queryExecution.execAsk();
        }
    }


    @Override
    public void dropGraph(String graphUri) {
        executeUpdateQuery("clear graph <" + graphUri + ">");
    }

    private String modelToTurtle(Model model) {
        StringWriter writer = new StringWriter();
        model.write(writer, "ttl");
        return writer.toString();
    }

    private URI buildGraphEndpoint(String graphUri, boolean isReplace) {
        String base = config.getSparqlUpdateEndpoint();
        if (StringUtils.isBlank(graphUri)) {
            return URI.create(base);  // default graph
        } else {
            String encCtx = URLEncoder.encode("<" + graphUri + ">", StandardCharsets.UTF_8);
            return URI.create(base + "?context=" + encCtx);
        }
    }

    private void sendGraphData(URI endpoint, String turtleContent, String httpMethod, String graphUri, long modelSize) {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(endpoint)
                .header("Content-Type", Lang.TURTLE.getHeaderString()); // text/turtle

        HttpRequest request;
        if (HTTP_METHOD_PUT.equals(httpMethod)) {
            request = requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(turtleContent, StandardCharsets.UTF_8)).build();
        } else {
            request = requestBuilder.POST(HttpRequest.BodyPublishers.ofString(turtleContent, StandardCharsets.UTF_8)).build();
        }

        try {
            HttpResponse<Void> resp = getHttpClient()
                    .send(request, HttpResponse.BodyHandlers.discarding());

            int code = resp.statusCode();
            if (code != 200 && code != 202 && code != 204) {
                String action = HTTP_METHOD_PUT.equals(httpMethod) ? "replace" : "update";
                log.error("Failed to {} graph. HTTP {} {}", action, code, resp);
                throw new RuntimeException("Failed to " + action + " graph. HTTP " + code);
            }
            
            String graphName = StringUtils.defaultIfBlank(graphUri, "default graph");
            if (HTTP_METHOD_PUT.equals(httpMethod)) {
                log.debug("Graph {} successfully replaced with {} triples (HTTP {})", graphName, modelSize, code);
            } else {
                log.debug("Uploaded {} triples to {}", modelSize, graphName);
            }

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            String action = HTTP_METHOD_PUT.equals(httpMethod) ? "replacing" : "uploading to";
            log.error("I/O error while {} graph", action, e);
            throw new RuntimeException("I/O error while " + action + " graph", e);
        }
    }

    @Override
    public void updateGraph(String graphUri, Model model) {
        Objects.requireNonNull(graphUri, "graphUri must not be null");
        Objects.requireNonNull(model, "model must not be null");

        if (model.isEmpty()) {
            log.debug("updateGraph called with an empty model – nothing to do.");
            return;
        }

        String turtle = modelToTurtle(model);
        URI endpoint = buildGraphEndpoint(graphUri, false);
        sendGraphData(endpoint, turtle, HTTP_METHOD_POST, graphUri, model.size());
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
        Objects.requireNonNull(graphUri, "graphUri must not be null");
        Objects.requireNonNull(model, "model must not be null");

        log.info("Replacing graph {} with single PUT request", graphUri);

        String turtleContent = modelToTurtle(model);
        URI endpoint = buildGraphEndpoint(graphUri, true);
        sendGraphData(endpoint, turtleContent, HTTP_METHOD_PUT, graphUri, model.size());
    }

}