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


import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zone.cogni.semanticz.connectors.utils.AutoCloseModels;
import zone.cogni.sem.jena.template.JenaResultSetHandler;

import java.io.Closeable;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

@Deprecated
public interface RdfStoreService extends Closeable {

  Logger log = LoggerFactory.getLogger(RdfStoreService.class);

  @Override
  default void close() {
    log.info("Closing RdfStoreService ({}) : {}", getClass().getName(), this);
  }

  void addData(Model model);

  void addData(Model model, String graphUri);

  <R> R executeSelectQuery(Query query, QuerySolutionMap bindings, JenaResultSetHandler<R> resultSetHandler, String context);

  default <R> R executeSelectQuery(String query, JenaResultSetHandler<R> resultSetHandler, String context) {
    Query parsedQuery = QueryFactory.create(query, Syntax.syntaxARQ);
    return executeSelectQuery(parsedQuery, new QuerySolutionMap(), resultSetHandler, context);
  }

  default <R> R executeSelectQuery(Query query, QuerySolutionMap bindings, JenaResultSetHandler<R> resultSetHandler) {
    return executeSelectQuery(query, bindings, resultSetHandler, null);
  }

  default <R> R executeSelectQuery(String query, JenaResultSetHandler<R> resultSetHandler) {
    return executeSelectQuery(query, resultSetHandler, null);
  }

  boolean executeAskQuery(Query query, QuerySolutionMap bindings);

  default boolean executeAskQuery(String query) {
    Query parsedQuery = QueryFactory.create(query, Syntax.syntaxARQ);
    return executeAskQuery(parsedQuery, new QuerySolutionMap());
  }

  Model executeConstructQuery(Query query, QuerySolutionMap bindings);

  default Model executeConstructQuery(String query) {
    Query parsedQuery = QueryFactory.create(query, Syntax.syntaxARQ);
    return executeConstructQuery(parsedQuery, new QuerySolutionMap());
  }

  void executeUpdateQuery(String updateQuery);

  void delete();

  default boolean graphExists(String graphUri) {  //we can overwrite this in the implementation if we have a better way of checking this
    return executeAskQuery("ask where {graph <" + graphUri + "> {?s a ?o}}" );
  }

  default void deleteGraph(String graphUri) {  //we can overwrite this in the implementation if we have a better way of checking this
    executeUpdateQuery(String.format("CLEAR GRAPH <%s>;", graphUri));
  }

  default void replaceGraph(String graphUri, Model model) {  //we can overwrite this in the implementation if we have a better way of checking this
    deleteGraph(graphUri);
    addData(model, graphUri);
  }

  default Map<String, Boolean> checkExisting(Set<String> uris) {
    //We make a construct query, where the construct itself is "<uri> <http://example.com/prop> ?b{n}" for each URI
    //  and the where is "<uri> a ?anyType . bind(<http://example.com/somthing> as ?b{n}" for each URI
    //This will make a triple with "<uri> <http://example.com/prop> <http://example.com/somthing>" if the URI has a type, or nothing if no type
    //Then we just check if each URI exists as subject in the resulting model.
    String construct = "";
    String where = "";
    int counter = 0;
    for (String uri : uris) {
      counter++;
      if (counter > 1) where += " UNION ";
      construct += "<" + uri + "> <http://example.com/prop> ?b" + counter + " . ";
      where += "{ <" + uri + "> a ?anyType . bind(<http://example.com/somthing> as ?b" + counter + ")} ";
    }
    String query = "PREFIX rdf: <" + RDF.uri + "> " +
                   "CONSTRUCT { " + construct + " } " +
                   "WHERE { " + where + " }";
    try (AutoCloseModels autoCloseModels = new AutoCloseModels()) {
      Model model = autoCloseModels.add(executeConstructQuery(query));
      return uris.stream().collect(Collectors.toMap(identity(), uri -> model.contains(model.createResource(uri), null)));
    }
  }
}