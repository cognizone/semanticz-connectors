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

package zone.cogni.semanticz.connectors.rdf4j;

import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import zone.cogni.semanticz.connectors.utils.AbstractSparqlServiceTest;

import java.util.function.Function;

public class Rdf4jInMemorySparqlServiceTest extends AbstractSparqlServiceTest<Rdf4jInMemorySparqlService> {

  public Rdf4jInMemorySparqlService createSUT() {
    return new Rdf4jInMemorySparqlService();
  }

  @Override
  protected void disposeSUT(Rdf4jInMemorySparqlService sparqlService) {
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