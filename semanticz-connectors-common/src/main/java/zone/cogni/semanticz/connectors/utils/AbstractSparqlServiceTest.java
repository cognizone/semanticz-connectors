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
import zone.cogni.semanticz.connectors.general.SparqlService;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Function;

import static org.apache.jena.rdf.model.ResourceFactory.createResource;

/**
 * Abstract test class for testing implementations of {@link SparqlService}.
 *
 * @param <T> the SparqlService implementation to test
 */
public abstract class AbstractSparqlServiceTest<T extends SparqlService> {

  /**
   * SparqlService under test.
   */
  private T sut;

  protected abstract T createSUT();

  protected abstract void disposeSUT(T sparqlService);

  protected T getSUT() {
    return sut;
  }

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
    sut.executeUpdateQuery("DELETE { ?s ?p ?o } WHERE { ?s ?p ?o }");
    sut.executeUpdateQuery("INSERT DATA { " + data + " }");
    final Iterator<String> graphs = dataset.listNames();
    while (graphs.hasNext()) {
      final String name = graphs.next();
      sut.updateGraph(name, dataset.getNamedModel(name));
    }
  }

  @AfterEach
  public void destroy() {
    disposeSUT(sut);
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
  public void testUploadTtlFileWorksCorrectly() throws IOException {
    final Path dir = Files.createTempDirectory("testUploadTtlFileWorksCorrectly-");
    final String fileName = "testUploadTtlFileWorksCorrectly.ttl";
    final File file = Files.createTempFile(dir, fileName, "").toFile();
    try {
      final Model model = ModelFactory.createDefaultModel();
      model.add(createResource(r("c1")), RDFS.comment, "comment");
      model.write(new FileWriter(file), "TURTLE");

      final String checkTripleExists = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> ASK { GRAPH ?g { <https://example.org/c1> rdfs:comment 'comment' } }";

      Assertions.assertFalse(sut.executeAskQuery(checkTripleExists));
      sut.uploadTtlFile(file);
      Assertions.assertTrue(sut.executeAskQuery(checkTripleExists));
    } finally {
      sut.dropGraph(file.toURI().toString());
      file.delete();
    }
  }

  @Test
  public void testUploadTtlFileThrowsRuntimeExceptionIfTheFileWasNotFound() {
    Assertions.assertThrows(RuntimeException.class,
            () -> {
              final File file = File.createTempFile("fusekisparqlservicetest-", ".ttl");
              file.delete();
              sut.uploadTtlFile(file);
            });
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
    sut.updateGraph(r("m2"), model);
    Assertions.assertTrue(sut.executeAskQuery(check));
  }

  @Test
  public void testSelectQueryReturnsResultsFromRespectiveGraphs() {
    final ResultSet result = sut.executeSelectQuery(
            "SELECT * { GRAPH ?g { ?s ?p ?o } FILTER (?g in (<https://example.org/m1>, <https://example.org/m2>))}", Function.identity());

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
  public void testIsEmptyOfNamedGraphReturnsTrueWheneverNoTripleExistsThere() {
    final boolean isEmpty = sut.isEmpty("https://example.org/m3");
    Assertions.assertTrue(isEmpty);
  }

  @Test
  public void testIsEmptyOfNamedGraphReturnsTrueWheneverATripleExists() {
    final boolean isEmpty = sut.isEmpty("https://example.org/m2");
    Assertions.assertFalse(isEmpty);
  }

  @Test
  public void testIsEmptyOfDefaultGraphReturnsTrueWheneverNoTripleExistsThere() {
    final boolean isEmpty = sut.isEmpty(null );
    Assertions.assertTrue(isEmpty);
  }
}