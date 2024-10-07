package zone.cogni.example.semanticz.connectors.spring;

import org.apache.jena.query.ResultSetFormatter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import zone.cogni.semanticz.connectors.general.SparqlService;
import zone.cogni.semanticz.connectors.spring.SparqlServiceProvider;

import java.io.ByteArrayOutputStream;

@RestController
public class Controller {

  public SparqlServiceProvider sparqlServiceProvider;

  public SparqlService sparqlService;

  public Controller(SparqlServiceProvider sparqlServiceProvider) {
    this.sparqlServiceProvider = sparqlServiceProvider;
    sparqlService = sparqlServiceProvider.createSparqlService(TripleStoreEnum.fuseki);
  }

  @PatchMapping("/data")
  public void storeAsTriple(@RequestParam("s") String subject, @RequestParam("p") String predicate, @RequestParam("o") String object) {
    sparqlService.executeUpdateQuery("BASE <https://data.cogni.zone/example/> INSERT DATA { <" + subject + "> <" + predicate + "> <" + object + "> } ");
  }

  @GetMapping(value = "/data", produces = "application/json")
  public String getTriples() {
    return sparqlService.executeSelectQuery("SELECT ?s ?p ?o { ?s ?p ?o }", r -> {
      final ByteArrayOutputStream stream = new ByteArrayOutputStream();
      ResultSetFormatter.outputAsJSON(stream, r);
      return stream.toString();
    });
  }

  enum TripleStoreEnum {
    fuseki
  }
}