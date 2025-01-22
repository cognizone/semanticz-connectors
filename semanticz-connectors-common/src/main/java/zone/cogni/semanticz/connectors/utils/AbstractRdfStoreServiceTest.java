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

package zone.cogni.semanticz.connectors.utils;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import zone.cogni.semanticz.connectors.general.RdfStoreService;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Objects;

import static org.apache.jena.rdf.model.ResourceFactory.createResource;

/**
 * Abstract test class for testing implementations of {@link RdfStoreService}.
 *
 * @param <T> the RdfStoreService implementation to test
 */
public abstract class AbstractRdfStoreServiceTest<T extends RdfStoreService> {

  /**
   * RdfStoreService under test.
   */
  private T sut;

  protected abstract T createSUT();

  private static String r(final String localName) {
    return "https://example.org/" + localName;
  }

  @BeforeEach
  public void init() throws URISyntaxException, IOException {
    sut = createSUT();
    final Dataset dataset = DatasetFactory.create();
    RDFParser.create().source(Objects.requireNonNull(
            getClass().getResource("/dataset.trig")).toURI().toURL().openStream()).lang(Lang.TRIG).parse(dataset);

    String data = RDFWriter.source(dataset.getDefaultModel()).format(RDFFormat.NTRIPLES).asString();
    sut.executeUpdateQuery("DELETE { GRAPH ?g { ?s ?p ?o } } WHERE { GRAPH ?g { ?s ?p ?o } }");
    sut.executeUpdateQuery("INSERT DATA { " + data + " }");
    final Iterator<String> graphs = dataset.listNames();
    while (graphs.hasNext()) {
      final String name = graphs.next();
      sut.addData(dataset.getNamedModel(name), name);
    }
  }

  @AfterEach
  public void destroy() {
    sut.close();
  }

  @Test
  public void testAskQueryIsCorrectlyEvaluated() {
    final boolean result = sut.executeAskQuery(
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> ASK { GRAPH ?g { <https://example.org/c1> rdfs:subClassOf <https://example.org/c2> } }");
    Assertions.assertTrue(result);
  }

  @Test
  public void testUpdateInsertsDataCorrectly() {
    Assertions.assertFalse(sut.executeAskQuery(
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> ASK { GRAPH <https://example.org/m1> { <https://example.org/c1> rdfs:subClassOf <https://example.org/c3> } }"));
    sut.executeUpdateQuery(
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> INSERT DATA { GRAPH <https://example.org/m1> { <https://example.org/c1> rdfs:subClassOf <https://example.org/c3> } }");
    Assertions.assertTrue(sut.executeAskQuery(
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> ASK { GRAPH <https://example.org/m1> { <https://example.org/c1> rdfs:subClassOf <https://example.org/c3> } }"));
    sut.executeUpdateQuery(
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> DELETE DATA { GRAPH <https://example.org/m1> { <https://example.org/c1> rdfs:subClassOf <https://example.org/c3> } }");
    Assertions.assertFalse(sut.executeAskQuery(
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> ASK { GRAPH <https://example.org/m1> { <https://example.org/c1> rdfs:subClassOf <https://example.org/c3> } }"));
  }

  @Test
  public void testReplaceGraphReplaceGraphCorrectly() {
    final Model model = ModelFactory.createDefaultModel();
    model.add(createResource(r("c1")), RDFS.label, "Class 1 - label 2");
    model.add(createResource(r("c1")), RDFS.label, "Class 1 - label 3");

    final String check =
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> ASK { GRAPH <" + r("m2")
                    + "> { <https://example.org/c1> rdfs:label 'Class 1 - label 2' . <https://example.org/c1> rdfs:label 'Class 1 - label 3' FILTER NOT EXISTS { <https://example.org/c1> rdfs:label 'Class 1' } } }";

    Assertions.assertFalse(sut.executeAskQuery(check));
    sut.replaceGraph(r("m2"), model);
    Assertions.assertTrue(sut.executeAskQuery(check));
  }

  @Test
  public void testUpdateGraphUpdatesGraphCorrectly() {
    final Model model = ModelFactory.createDefaultModel();
    model.add(createResource(r("c1")), RDFS.label, "Class 1 - label 2");
    model.add(createResource(r("c1")), RDFS.label, "Class 1 - label 3");

    final String check =
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> ASK { GRAPH <" + r("m2")
                    + "> { <https://example.org/c1> rdfs:label 'Class 1 - label 2' . <https://example.org/c1> rdfs:label 'Class 1 - label 3' . <https://example.org/c1> rdfs:label 'Class 1' } }";

    Assertions.assertFalse(sut.executeAskQuery(check));
    sut.addData(model, r("m2"));
    Assertions.assertTrue(sut.executeAskQuery(check));
  }

  @Test
  public void testSelectQueryReturnsResultsFromRespectiveGraphs() {
    final ResultSet result = sut.executeSelectQuery(
            "SELECT * { GRAPH ?g { ?s ?p ?o } FILTER (?g in (<https://example.org/m1>, <https://example.org/m2>))}", resultSet -> resultSet);

    while (result.hasNext()) {
      result.next();
    }
    Assertions.assertEquals(2, result.getRowNumber());
  }

  @Test
  public void testQueryForModelReturnsResultsFromRespectiveGraphs() {
    final Model model = sut.executeConstructQuery("CONSTRUCT { ?s ?p ?o } WHERE { GRAPH ?g { ?s ?p ?o } FILTER (?g in (<https://example.org/m1>, <https://example.org/m2>)) }");
    Assertions.assertEquals(2, model.size());
  }

  @Test
  public void testGraphExistsOfNamedGraphReturnsFalseWheneverNoTripleExistsThere() {
    final boolean exists = sut.graphExists("https://example.org/m3");
    Assertions.assertFalse(exists);
  }

  @Test
  public void testGraphExistsOfNamedGraphReturnsTrueWheneverATripleExistsThere() {
    final boolean exists = sut.graphExists("https://example.org/m2");
    Assertions.assertFalse(exists);
  }
}