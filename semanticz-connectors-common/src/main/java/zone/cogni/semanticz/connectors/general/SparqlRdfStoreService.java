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

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.Model;
import zone.cogni.sem.jena.template.JenaResultSetHandler;

@Deprecated
public class SparqlRdfStoreService implements RdfStoreService {

  private final SparqlService sparqlService;

  public SparqlRdfStoreService(SparqlService sparqlService) {
    this.sparqlService = sparqlService;
  }

  @Override
  public void addData(Model model) {
    sparqlService.updateGraph("",model);
  }

  @Override
  public void addData(Model model, String graphUri) {
    sparqlService.updateGraph(graphUri, model);
  }

  protected Query buildQuery(Query query, QuerySolutionMap bindings) {
    ParameterizedSparqlString string = new ParameterizedSparqlString(query.toString(), bindings);
    return string.asQuery();
  }

  @Override
  public <R> R executeSelectQuery(Query query,
                                  QuerySolutionMap bindings,
                                  JenaResultSetHandler<R> resultSetHandler,
                                  String context) {
    query = buildQuery(query, bindings);
    return sparqlService.executeSelectQuery(query.toString(), resultSetHandler::handle);
  }

  @Override
  public boolean executeAskQuery(Query query, QuerySolutionMap bindings) {
    query = buildQuery(query, bindings);
    return sparqlService.executeAskQuery(query.toString());
  }

  @Override
  public Model executeConstructQuery(Query query, QuerySolutionMap bindings) {
    query = buildQuery(query, bindings);
    return sparqlService.executeConstructQuery(query.toString());
  }

  @Override
  public void executeUpdateQuery(String updateQuery) {
    sparqlService.executeUpdateQuery(updateQuery);
  }

  @Override
  public void delete() {
    throw new RuntimeException("not implemented");
  }
}
