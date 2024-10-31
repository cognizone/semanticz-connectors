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
  public void upload(Model model, String graphUri) {
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
