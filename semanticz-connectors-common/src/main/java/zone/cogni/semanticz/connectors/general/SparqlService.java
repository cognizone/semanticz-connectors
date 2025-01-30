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

import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;

import java.io.File;
import java.util.function.Function;

/**
 * This is a generic interface for accessing a SPARQL-capable service to:
 * - query (ASK/SELECT/CONSTRUCT)
 * - update (uploading a Jena Model, update/replace/drop a graph)
 */
public interface SparqlService {

  void uploadTtlFile(File file);

  /**
   * Executes SPARQL CONSTRUCT query and returns the result as a Model.
   *
   * @param constructQuery SPARQL CONSTRUCT query to execute
   * @return the Model with the output of the query
   */
  Model executeConstructQuery(String constructQuery);

  void executeUpdateQuery(String updateQuery);

  boolean executeAskQuery(String updateQuery);

  <R> R executeSelectQuery(String query, Function<ResultSet, R> resultHandler);

  /**
   * Deletes all triples from the named graph with the given URI.
   * It might also delete the graph itself in case the store supports it.
   * This method does not fail even if the named graph does not exist.
   *
   * @param graphUri named graph URI
   */
  void dropGraph(String graphUri);

  /**
   * Updates an existing graph by adding triples of passed in model.
   * <p>
   * Note: please use with care since in most case you probably want to use #replaceGraph
   * </p>
   *
   * @param graphUri uri of graph being updated
   * @param model    model which is being added to the current graph
   */
  void updateGraph(String graphUri, Model model);

  /**
   * <p>
   * Replaces current model in a graph with the model passed in as a parameter.
   * </p>
   * <p>
   * Please note:
   * <ul>
   *   <li>default implementation is not considered to be robust and should be overridden</li>
   *   <li>
   *     new method was introduced because a lot of projects are actually trying to emulate a replace
   *     by doing a manual {@link #dropGraph(String)} and {@link #updateGraph(String, Model)}.
   *   </li>
   * </ul>
   * </p>
   *
   * @param graphUri uri of graph being updated
   * @param model    new model which will be in the designated graph.
   */
  default void replaceGraph(String graphUri, Model model) {
    dropGraph(graphUri);
    updateGraph(graphUri, model);
  }

  /**
   * Checks if the given graphIri is empty.
   *
   * @param graphIri IRI of the named graph to check for emptiness, or null if the default graph should be checked.
   * @return true if the graph does not contain any triple. Note that this methods might return false also in case the graph even does not exist.
   */
  default boolean isEmptyGraph(String graphIri) {
    if (graphIri == null) {
      return !executeAskQuery("ASK WHERE { ?s ?p ?o FILTER NOT EXISTS { GRAPH ?g { ?s ?p ?o } } }");
    } else {
      return !executeAskQuery(String.format("ASK { GRAPH <%s> {?s ?p ?o}}", graphIri));
    }
  }
}
