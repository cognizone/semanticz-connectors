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
