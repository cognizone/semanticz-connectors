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

package zone.cogni.semanticz.connectors.virtuoso;

import io.vavr.control.Try;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zone.cogni.semanticz.connectors.general.RdfStoreService;
import zone.cogni.semanticz.connectors.utils.ApacheHttpClientUtils;
import zone.cogni.sem.jena.template.JenaResultSetHandler;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static zone.cogni.semanticz.connectors.utils.Constants.CONTENT_TYPE;
import static zone.cogni.semanticz.connectors.utils.Constants.TEXT_TURTLE;

/**
 * Implementation of VirtuosoRdfStoreService backed directly by Apache HttpClient.
 */
@Deprecated
public class VirtuosoApacheHttpClientRdfStoreService implements RdfStoreService {

  private static final Logger log = LoggerFactory.getLogger(
      VirtuosoApacheHttpClientRdfStoreService.class);

  protected final String rdfStoreUrl;
  protected final String rdfStoreUser;
  protected final String rdfStorePassword;
  private final boolean graphCrudUseBasicAuth;

  public VirtuosoApacheHttpClientRdfStoreService(String rdfStoreUrl, String rdfStoreUser,
      String rdfStorePassword) {
    this(rdfStoreUrl, rdfStoreUser, rdfStorePassword, false);
  }

  public VirtuosoApacheHttpClientRdfStoreService(String rdfStoreUrl, String rdfStoreUser,
      String rdfStorePassword, boolean graphCrudUseBasicAuth) {
    this.rdfStoreUrl = rdfStoreUrl;
    this.rdfStoreUser = rdfStoreUser;
    this.rdfStorePassword = rdfStorePassword;
    this.graphCrudUseBasicAuth = graphCrudUseBasicAuth;
  }

  protected CloseableHttpClient buildHttpClient() {
    HttpClientBuilder httpClientBuilder = HttpClients.custom().useSystemProperties();
    httpClientBuilder.setConnectionManager(
        new PoolingHttpClientConnectionManager(60L, TimeUnit.SECONDS));
    if (!StringUtils.isBlank(rdfStoreUser)) {
      CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
      credentialsProvider.setCredentials(AuthScope.ANY,
          new UsernamePasswordCredentials(rdfStoreUser, rdfStorePassword));
      httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
    } else {
      log.warn("Virtuoso executor service {} is configured without credentials.", rdfStoreUrl);
    }
    return httpClientBuilder.build();
  }

  @Override
  public void addData(Model model) {
    addData(model, null);
  }

  @Override
  public void addData(Model model, String graphUri) {
    addData(model, graphUri, false);
  }

  private void addData(Model model, String graphUri, boolean replace) {
    //If this method fails, check what is done in treaties (upload in batches with insert queries)
    StringWriter writer = new StringWriter();
    VirtuosoHelper.patchModel(model).write(writer, "ttl");
    byte[] data = writer.toString().getBytes(StandardCharsets.UTF_8);

    String url = StringUtils.substringBeforeLast(rdfStoreUrl, "/") + "/sparql-graph-crud-auth?" +
        // force Graph Update protocol
        (StringUtils.isBlank(graphUri) ? "default" : ("graph-uri=" + graphUri));
    log.info("Calling {} with basic auth: {}", url, graphCrudUseBasicAuth);
    HttpEntityEnclosingRequestBase request = replace ? new HttpPut(url) : new HttpPost(url);
    request.setEntity(new ByteArrayEntity(data));
    request.setHeader(CONTENT_TYPE, TEXT_TURTLE + ";charset=utf-8");
    if (graphCrudUseBasicAuth) {
      request.setHeader("Authorization", "Basic " + Base64.encodeBase64String(
          (rdfStoreUser + ":" + rdfStorePassword).getBytes(StandardCharsets.UTF_8)));
    }

    try (CloseableHttpClient client = buildHttpClient()) {
      HttpResponse response = client.execute(request);
      int responseCode = response.getStatusLine().getStatusCode();
      String reason = response.getStatusLine().getReasonPhrase();
      String responseBody = Try.of(
              () -> IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8))
          .onFailure(ex -> log.error("Failed to read response body", ex))
          .getOrElse(StringUtils.EMPTY);
      EntityUtils.consume(response.getEntity());
      if (!is2xxSuccessful(responseCode)) {
        log.error("Virtuoso server sent response with status code {}, with message {} and body {}",
            responseCode, reason, responseBody);
        throw new VirtuosoOperationException(
            "Virtuoso server sent response with status code " + responseCode + ", with message "
                + reason, responseBody);
      }
    } catch (IOException ex) {
      log.error("Exception during data exchange with virtuoso server {}", ex.getMessage());
      throw new VirtuosoOperationException(ex);
    }
  }

  @Override
  public <R> R executeSelectQuery(Query query, QuerySolutionMap bindings,
      JenaResultSetHandler<R> resultSetHandler, String context) {
    log.debug("Select [{}] - {} \n{}", context, bindings, query);

    query = buildQuery(query, bindings);

    return ApacheHttpClientUtils.executeSelect(rdfStoreUrl, rdfStoreUser, rdfStorePassword,
        query.toString(), graphCrudUseBasicAuth, resultSetHandler::handle);
  }

  @Override
  public boolean executeAskQuery(Query query, QuerySolutionMap bindings) {
    query = buildQuery(query, bindings);
    return ApacheHttpClientUtils.executeAsk(rdfStoreUrl, rdfStoreUser, rdfStorePassword,
        query.toString(), graphCrudUseBasicAuth);
  }

  @Override
  public Model executeConstructQuery(Query query, QuerySolutionMap bindings) {
    query = buildQuery(query, bindings);

    return ApacheHttpClientUtils.executeConstruct(rdfStoreUrl, rdfStoreUser, rdfStorePassword,
        query.toString(), graphCrudUseBasicAuth);
  }

  @Override
  public void executeUpdateQuery(String updateQuery) {
    CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    credentialsProvider.setCredentials(AuthScope.ANY,
        new UsernamePasswordCredentials(rdfStoreUser, rdfStorePassword));

    try (CloseableHttpClient httpClient = buildHttpClient()) {
      HttpPost httpPost = new HttpPost(rdfStoreUrl);
      httpPost.setEntity(new UrlEncodedFormEntity(
          Collections.singletonList(new BasicNameValuePair("query", updateQuery)),
          StandardCharsets.UTF_8));
      HttpResponse response = httpClient.execute(httpPost);
      StatusLine statusLine = response.getStatusLine();

      if (!is2xxSuccessful(statusLine.getStatusCode())) {
        log.error("Virtuoso update failed with http status code {}", statusLine.getStatusCode());
        String errorResponse = IOUtils.toString(response.getEntity().getContent(),
            StandardCharsets.UTF_8);
        log.error("Virtuoso update failed with result {}", errorResponse);
      }
    } catch (IOException ex) {
      log.error("Virtuoso update failed: {}", ex.getMessage());
      throw new VirtuosoOperationException();
    }
  }

  @Override
  public void delete() {
    throw new UnsupportedOperationException("unsupported operation");
  }

  protected Query buildQuery(Query query, QuerySolutionMap bindings) {
    ParameterizedSparqlString string = new ParameterizedSparqlString(query.toString(), bindings);
    return string.asQuery();
  }

  @Override
  public void replaceGraph(String graphUri, Model model) {
    addData(model, graphUri, true);
  }

  private static boolean is2xxSuccessful(final int responseCode) {
    return responseCode >= 200 && responseCode < 300;
  }
}
