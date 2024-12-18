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

package zone.cogni.semanticz.connectors.general;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnection;

import java.io.File;
import java.util.function.Function;

/**
 * Implementation of SparqlService based on Jena RDFConnection API.
 */
public abstract class RDFConnectionSparqlService implements SparqlService {

  /**
   * Provides an RDFConnection instance for the given endpoint.
   *
   * @return RDFConnection instance.
   */
  protected abstract RDFConnection getConnection();

  /**
   * Provides an RDFConnection instance for CONSTRUCT queries for the given endpoint.
   * This was introduced due to incorrect accept headers when testing against Virtuoso.
   * Ideally should be removed.
   *
   * @return RDFConnection instance.
   */
  protected abstract RDFConnection getConstructConnection();

  @Override
  public void uploadTtlFile(File file) {
    try (RDFConnection connection = getConnection()) {
      connection.load(file.toURI().toString(), file.getPath());
    }
  }

  @Override
  public Model executeConstructQuery(String query) {
    try (RDFConnection connection = getConstructConnection();
        QueryExecution queryExecution = connection.query(query)) {
      return queryExecution.execConstruct();
    }
  }

  @Override
  public void executeUpdateQuery(String query) {
    try (RDFConnection connection = getConnection()) {
      connection.update(query);
    }
  }

  @Override
  public void updateGraph(String graphUri, Model model) {
    try (RDFConnection connection = getConnection()) {
      connection.load(graphUri, model);
    }
  }

  @Override
  public <R> R executeSelectQuery(String query, Function<ResultSet, R> resultHandler) {
    try (RDFConnection connection = getConnection();
        QueryExecution queryExecution = connection.query(
        query)) {
      return resultHandler.apply(queryExecution.execSelect());
    }
  }

  @Override
  public boolean executeAskQuery(String askQuery) {
    try (RDFConnection connection = getConnection();
        QueryExecution queryExecution = connection.query(
        askQuery)) {
      return queryExecution.execAsk();
    }
  }

  @Override
  public void dropGraph(String graphUri) {
    try (RDFConnection connection = getConnection()) {
      connection.delete(graphUri);
    }
  }

  @Override
  public void replaceGraph(String graphUri, Model model) {
    try (RDFConnection connection = getConnection()) {
      connection.put(graphUri, model);
    }
  }
}
