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
import zone.cogni.sem.jena.template.JenaResultSetHandler;
import zone.cogni.semanticz.connectors.general.RdfStoreService;

import java.io.IOException;
import java.io.StringWriter;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

@Deprecated
public class GraphDBRdfStoreService implements RdfStoreService {

    private static final Logger log = LoggerFactory.getLogger(GraphDBRdfStoreService.class);

    private final GraphDBConfig config;
    private volatile HttpClient httpClient;

    public GraphDBRdfStoreService(GraphDBConfig config) {
        this.config = Objects.requireNonNull(config, "config must not be null");
    }

    private static boolean hasBindings(QuerySolution qs) {
        return qs != null && qs.varNames().hasNext();
    }

    private synchronized HttpClient getHttpClient() {
        if (httpClient != null) return httpClient;

        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(Redirect.NORMAL)
                .proxy(ProxySelector.getDefault());

        if (StringUtils.isNoneBlank(config.getUser(), config.getPassword())) {
            builder.authenticator(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(
                            config.getUser(), config.getPassword().toCharArray());
                }
            });
        } else if (!StringUtils.isAllBlank(config.getUser(), config.getPassword())) {
            log.error("GraphDB credentials are incomplete: user='{}', pwd='{}'",
                    config.getUser(), config.getPassword());
        }

        httpClient = builder.build();
        return httpClient;
    }

    @Override
    public void addData(Model model) {
        addData(model, null);   // default graph
    }

    @Override
    public void addData(Model model, String graphUri) {
        Objects.requireNonNull(model, "model must not be null");

        StringWriter writer = new StringWriter();
        model.write(writer, "ttl");
        String turtle = writer.toString();

        String endpoint = config.getSparqlUpdateEndpoint();
        if (StringUtils.isNotBlank(graphUri)) {
            String encCtx = URLEncoder.encode("<" + graphUri + ">", StandardCharsets.UTF_8);
            endpoint = endpoint + "?context=" + encCtx;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", Lang.TURTLE.getHeaderString()) // text/turtle
                .POST(HttpRequest.BodyPublishers.ofString(turtle, StandardCharsets.UTF_8))
                .build();

        executeHttpRequest(request, 200, 202, 204);
        log.debug("Uploaded {} triples to {}", model.size(), graphUri == null ? "default graph" : graphUri);
    }

    @Override
    public void replaceGraph(String graphUri, Model model) {
        Objects.requireNonNull(graphUri, "graphUri must not be null");
        Objects.requireNonNull(model, "model must not be null");

        StringWriter writer = new StringWriter();
        model.write(writer, "ttl");
        String turtle = writer.toString();

        String encodedCtx = URLEncoder.encode("<" + graphUri + ">", StandardCharsets.UTF_8);
        URI endpoint = URI.create(config.getSparqlUpdateEndpoint() + "?context=" + encodedCtx);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(endpoint)
                .header("Content-Type", Lang.TURTLE.getHeaderString())    // text/turtle
                .PUT(HttpRequest.BodyPublishers.ofString(turtle, StandardCharsets.UTF_8))
                .build();

        executeHttpRequest(request, 200, 202, 204);
        log.debug("Graph {} successfully replaced with {} triples.", graphUri, model.size());
    }

    @Override
    public <R> R executeSelectQuery(Query query,
                                    QuerySolutionMap bindings,
                                    JenaResultSetHandler<R> resultHandler,
                                    String context) {

        QueryExecutionHTTPBuilder builder = QueryExecutionHTTP.service(config.getSparqlEndpoint())
                .query(query)
                .httpClient(getHttpClient());

        if (hasBindings(bindings)) {
            builder.substitution(bindings);
        }

        if (StringUtils.isNotBlank(context)) {
            builder.param("context-uri", context);
        }

        try (QueryExecution qexec = builder.build()) {
            ResultSet rs = qexec.execSelect();
            return resultHandler.handle(rs);
        }
    }

    @Override
    public boolean executeAskQuery(Query query, QuerySolutionMap bindings) {
        QueryExecutionHTTPBuilder builder = QueryExecutionHTTP.service(config.getSparqlEndpoint())
                .query(query)
                .httpClient(getHttpClient());

        if (hasBindings(bindings)) {
            builder.substitution(bindings);
        }

        try (QueryExecution qexec = builder.build()) {
            return qexec.execAsk();
        }
    }

    @Override
    public Model executeConstructQuery(Query query, QuerySolutionMap bindings) {
        QueryExecutionHTTPBuilder builder = QueryExecutionHTTP.service(config.getSparqlEndpoint())
                .query(query)
                .httpClient(getHttpClient());

        if (hasBindings(bindings)) {
            builder.substitution(bindings);
        }

        try (QueryExecution qexec = builder.build()) {
            return qexec.execConstruct();
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
        log.debug("SPARQL‑UPDATE executed successfully (204 NO CONTENT)");
    }

    @Override
    public void delete() {
        executeUpdateQuery("CLEAR ALL");
        log.info("Repository {} cleared with 'CLEAR ALL'.", config.getRepository());
    }

    @Override
    public void close() {
        super.close();
    }

    private void executeHttpRequest(HttpRequest request, int... expectedCodes) {
        try {
            HttpResponse<String> resp = getHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            int actual = resp.statusCode();
            for (int expected : expectedCodes) {
                if (actual == expected) return;
            }

            String body = StringUtils.defaultIfBlank(resp.body(), "<no body>");
            throw new RuntimeException(
                    "GraphDB request to " + request.uri() + " failed. HTTP " + actual + ". " + body);

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("I/O error talking to GraphDB", e);
        }
    }
}
