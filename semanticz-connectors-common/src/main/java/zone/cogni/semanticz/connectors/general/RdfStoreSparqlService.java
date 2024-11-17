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
import org.apache.jena.riot.RDFDataMgr;

import java.io.File;
import java.util.function.Function;

@SuppressWarnings("deprecation")
public class RdfStoreSparqlService implements SparqlService {

  private final RdfStoreService rdfStoreService;

  public RdfStoreSparqlService(RdfStoreService rdfStoreService) {
    this.rdfStoreService = rdfStoreService;
  }

  @Override
  public void uploadTtlFile(File file) {
    Model model = RDFDataMgr.loadModel(file.getAbsolutePath());
    rdfStoreService.addData(model);
  }

  @Override
  public Model executeConstructQuery(String query) {
    return rdfStoreService.executeConstructQuery(query);
  }

  @Override
  public void executeUpdateQuery(String updateQuery) {
    rdfStoreService.executeUpdateQuery(updateQuery);
  }

  @Override
  public boolean executeAskQuery(String updateQuery) {
    return rdfStoreService.executeAskQuery(updateQuery);
  }

  @Override
  public void updateGraph(String graphUri, Model model) {
    rdfStoreService.addData(model);
  }

  @Override
  public <R> R executeSelectQuery(String query, Function<ResultSet, R> resultHandler) {
    return rdfStoreService.executeSelectQuery(query, resultHandler::apply);
  }

  @Override
  public void dropGraph(String graphUri) {
    rdfStoreService.deleteGraph(graphUri);
  }
}
