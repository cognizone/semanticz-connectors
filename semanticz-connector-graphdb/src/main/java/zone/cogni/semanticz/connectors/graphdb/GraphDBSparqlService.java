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

import org.apache.commons.io.FileUtils;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionBuilder;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTPBuilder;
import zone.cogni.semanticz.connectors.utils.Constants;
import zone.cogni.semanticz.connectors.utils.HttpClientUtils;
import zone.cogni.semanticz.connectors.general.SparqlService;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import static zone.cogni.semanticz.connectors.utils.Constants.CONTENT_TYPE;
import static zone.cogni.semanticz.connectors.utils.HttpClientUtils.execute;

public class GraphDBSparqlService implements SparqlService {

  private final GraphDBConfig config;
  private final HttpClient httpClient;

  public GraphDBSparqlService(GraphDBConfig config) {
    this.config = config;
    //  TODO check loading from systemproperties - e.g. proxy settings?
    //  HttpClientBuilder httpClientBuilder = HttpClients.custom().useSystemProperties();
    httpClient = HttpClientUtils.createHttpClientBuilder(config.getUser(), config.getPassword()).build();
  }

  private QueryExecutionBuilder getQueryExecutionBuilder() {
    return QueryExecutionHTTPBuilder.service(config.getSparqlEndpoint()).httpClient(httpClient);
  }

  @Override
  public void uploadTtlFile(File file) {
    try {
      uploadTtl(file.toURI().toString(), FileUtils.readFileToString(file, "UTF-8"));
    }
    catch (IOException e) {
      throw new RuntimeException("Couldn't read file " + file.getName(), e);
    }
  }

  @Override
  public void updateGraph(String graphUri, Model model) {
    StringWriter writer = new StringWriter();
    model.write(writer, "ttl");
    uploadTtl(graphUri, writer.toString());
  }

  private void uploadTtl(String graphUri, String ttl) {
    final HttpRequest request = HttpRequest
        .newBuilder()
        .POST(BodyPublishers.ofString(ttl, StandardCharsets.UTF_8))
        .header(CONTENT_TYPE, Lang.TURTLE.getHeaderString())
        .uri(URI.create(config.getSparqlUpdateEndpoint()+"?context=" + URLEncoder.encode("<"+graphUri+">", StandardCharsets.UTF_8)))
        .build();
    execute(request, httpClient);
  }

  @Override
  public Model executeConstructQuery(String query) {
    try (QueryExecution queryExecution = getQueryExecutionBuilder().query(query).build()) {
      return queryExecution.execConstruct();
    }
  }

  @Override
  public void executeUpdateQuery(String updateQuery) {
    final HttpRequest request = HttpRequest
        .newBuilder(URI.create(config.getSparqlUpdateEndpoint()))
        .POST(BodyPublishers.ofString("update=" + URLEncoder.encode(updateQuery, StandardCharsets.UTF_8), StandardCharsets.UTF_8))
        .header(CONTENT_TYPE, Constants.APPLICATION_FORM_URLENCODED_VALUE)
        .build();
    execute(request, httpClient);
  }

  @Override
  public <R> R executeSelectQuery(String query, Function<ResultSet, R> resultHandler) {
    try (QueryExecution queryExecution = getQueryExecutionBuilder().query(query).build()) {
      return resultHandler.apply(queryExecution.execSelect());
    }
  }

  @Override
  public boolean executeAskQuery(String askQuery) {
    try (QueryExecution queryExecution = getQueryExecutionBuilder().query(askQuery).build()) {
      return queryExecution.execAsk();
    }
  }

  @Override
  public void dropGraph(String graphUri) {
    executeUpdateQuery("CLEAR SILENT GRAPH <" + graphUri + ">");
  }
}
