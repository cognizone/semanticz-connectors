package zone.cogni.semanticz.connectors.neptune;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionRemote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zone.cogni.sem.jena.template.JenaResultSetHandler;
import zone.cogni.semanticz.connectors.general.RdfStoreService;

public class NeptuneRdfStoreService implements RdfStoreService {

    private static final Logger log = LoggerFactory.getLogger(NeptuneRdfStoreService.class);

    private final String sparqlEndpoint;
    private final String gspEndpoint;

    public NeptuneRdfStoreService(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        this.sparqlEndpoint = baseUrl + "/sparql";
        this.gspEndpoint = baseUrl + "/sparql/gsp";
    }

    @Override
    public void addData(Model model) {
        try (RDFConnection conn = getConnection()) {
            conn.load(model);
        }
        catch (Exception e) {
            log.error("Error adding data to Neptune", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addData(Model model, String graphUri) {
        try (RDFConnection conn = getConnection()) {
            conn.load(graphUri, model);
        }
        catch (Exception e) {
            log.error("Error adding data to Neptune in graph {}", graphUri, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public <R> R executeSelectQuery(Query query,
                                    QuerySolutionMap bindings,
                                    JenaResultSetHandler<R> resultSetHandler,
                                    String context) {
        Query boundQuery = buildQuery(query, bindings);
        try (RDFConnection conn = getConnection();
             QueryExecution qExec = conn.query(boundQuery)) {

            ResultSet resultSet = qExec.execSelect();
            return resultSetHandler.handle(resultSet);
        }
        catch (Exception e) {
            log.error("Error executing SELECT query on Neptune", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean executeAskQuery(Query query, QuerySolutionMap bindings) {
        Query boundQuery = buildQuery(query, bindings);
        try (RDFConnection conn = getConnection();
             QueryExecution qExec = conn.query(boundQuery)) {

            return qExec.execAsk();
        }
        catch (Exception e) {
            log.error("Error executing ASK query on Neptune", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Model executeConstructQuery(Query query, QuerySolutionMap bindings) {
        Query boundQuery = buildQuery(query, bindings);
        try (RDFConnection conn = getConnection();
             QueryExecution qExec = conn.query(boundQuery)) {

            return qExec.execConstruct();
        }
        catch (Exception e) {
            log.error("Error executing CONSTRUCT query on Neptune", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void executeUpdateQuery(String updateQuery) {
        try (RDFConnection conn = getConnection()) {
            log.debug("Executing update query:\n{}", updateQuery);
            conn.update(updateQuery);
        }
        catch (Exception e) {
            log.error("Error executing UPDATE query on Neptune. Query:\n{}", updateQuery, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteGraph(String graphUri) {
        String deleteQuery = String.format("DROP GRAPH <%s>", graphUri);
        executeUpdateQuery(deleteQuery);
    }

    @Override
    public void delete() {
        String deleteAllQuery = "DELETE WHERE { ?s ?p ?o }";
        executeUpdateQuery(deleteAllQuery);
    }

    private RDFConnection getConnection() {
        return RDFConnectionRemote.newBuilder()
                                  .destination(sparqlEndpoint)
                                  .queryEndpoint(sparqlEndpoint)
                                  .updateEndpoint(sparqlEndpoint)
                                  .gspEndpoint(gspEndpoint)
                                  .build();
    }

    private Query buildQuery(Query query, QuerySolutionMap bindings) {
        ParameterizedSparqlString pss = new ParameterizedSparqlString(query.toString());
        if (bindings != null) {
            pss.setParams(bindings);
        }
        return pss.asQuery();
    }
}
