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

package zone.cogni.semanticz.connectors.fuseki;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionBuilder;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTPBuilder;
import zone.cogni.semanticz.connectors.utils.Constants;
import zone.cogni.semanticz.connectors.utils.HttpClientUtils;
import zone.cogni.semanticz.connectors.general.SparqlService;
import zone.cogni.semanticz.connectors.general.Config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import static zone.cogni.semanticz.connectors.utils.Constants.CONTENT_TYPE;
import static zone.cogni.semanticz.connectors.utils.HttpClientUtils.execute;

/**
 * SparqlService implementation for Fuseki, backed by JDK11 HttpClient
 */
public class FusekiSparqlService implements SparqlService {

  private final FusekiConfig config;

  private final HttpClient httpClient;

  @Deprecated
  public FusekiSparqlService(Config config) {
    this(FusekiConfig.from(config));
  }

  public FusekiSparqlService(FusekiConfig config) {
    this.config = config;
    httpClient = HttpClientUtils.createHttpClientBuilder(config.getUser(), config.getPassword()).build();
  }

  private QueryExecutionBuilder getQueryExecutionBuilder() {
    return QueryExecutionHTTPBuilder.service(config.getQueryUrl()).httpClient(httpClient);
  }

  @Override
  public void uploadTtlFile(File file) {
    final String sparqlUrl = config.getGraphStoreUrl() + "?graph=" + URLEncoder.encode(file.toURI().toString(), StandardCharsets.UTF_8);
    final HttpRequest request;
    try {
      request = HttpRequest
          .newBuilder(URI.create(sparqlUrl))
          .POST(BodyPublishers.ofFile(file.toPath()))
          .header(CONTENT_TYPE, config.getTurtleMimeType() + ";charset=utf-8")
          .build();
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
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
        .newBuilder(URI.create(config.getUpdateUrl()))
        .POST(BodyPublishers.ofString("update=" + URLEncoder.encode(updateQuery,
                StandardCharsets.UTF_8)))
        .header(CONTENT_TYPE, Constants.APPLICATION_FORM_URLENCODED_VALUE)
        .build();
    System.out.println(request);
    execute(request, httpClient);
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
    String insertUrl = config.getGraphStoreUrl() + "?graph=" + URLEncoder.encode(graphUri,
        StandardCharsets.UTF_8);
    StringWriter writer = new StringWriter();
    model.write(writer, "ttl");
    final BodyPublisher p = BodyPublishers.ofByteArray(writer.toString().getBytes());
    final HttpRequest.Builder builder = HttpRequest
        .newBuilder(URI.create(insertUrl))
        .header(CONTENT_TYPE, config.getTurtleMimeType() + ";charset=utf-8");

    final HttpRequest request = (replace ? builder.PUT(p) : builder.POST(p)).build();
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
    executeUpdateQuery("drop silent graph <" + graphUri + ">");
  }
}
