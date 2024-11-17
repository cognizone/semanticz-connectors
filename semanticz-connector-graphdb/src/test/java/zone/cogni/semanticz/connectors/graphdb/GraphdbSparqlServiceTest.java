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

import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import zone.cogni.semanticz.connectors.utils.AbstractSparqlServiceTest;

import java.util.function.Function;

@Disabled("An integration test dependent on a running GraphDB instance. To run it manually, set the GraphDBConfig below properly and run the tests.")
public class GraphdbSparqlServiceTest extends AbstractSparqlServiceTest<GraphDBSparqlService> {

  public GraphDBSparqlService createSUT() {
    final GraphDBConfig config = new GraphDBConfig();
    config.setUrl("http://localhost:7200");
    config.setRepository("test");
    config.setUser("test");
    config.setPassword("test");
    return new GraphDBSparqlService(config);
  }

  @Override
  protected void disposeSUT(GraphDBSparqlService sparqlService) {
    // do nothing
  }

  @Test
  public void testSelectQueryReturnsResultsFromAllGraphs() {
    final ResultSet result = getSUT().executeSelectQuery("SELECT * { ?s ?p ?o }", Function.identity());
    while (result.hasNext()) {
      result.next();
    }
    Assertions.assertEquals(2, result.getRowNumber());
  }

  @Test
  public void testQueryForModelReturnsUnionOfAllGraphs() {
    final Model model = getSUT().executeConstructQuery("CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }");
    Assertions.assertEquals(2, model.size());
  }
}