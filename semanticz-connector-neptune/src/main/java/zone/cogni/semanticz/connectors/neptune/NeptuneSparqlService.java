package zone.cogni.semanticz.connectors.neptune;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionRemote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zone.cogni.semanticz.connectors.general.SparqlService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

public class NeptuneSparqlService implements SparqlService {

    private static final Logger log = LoggerFactory.getLogger(NeptuneSparqlService.class);

    private final String sparqlEndpoint;
    private final String gspEndpoint;

    public NeptuneSparqlService(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        this.sparqlEndpoint = baseUrl + "/sparql";
        this.gspEndpoint = baseUrl + "/sparql/gsp";
    }

    @Override
    public void uploadTtlFile(File file) {
        try (InputStream in = new FileInputStream(file)) {
            Model model = ModelFactory.createDefaultModel();
            model.read(in, null, "TURTLE");
            upload(model, null);
        }
        catch (IOException e) {
            log.error("Error reading TTL file", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Model executeConstructQuery(String query) {
        Query parsedQuery = QueryFactory.create(query, Syntax.syntaxARQ);
        try (RDFConnection conn = getConnection();
             QueryExecution qExec = conn.query(parsedQuery)) {
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
    public boolean executeAskQuery(String query) {
        Query parsedQuery = QueryFactory.create(query, Syntax.syntaxARQ);
        try (RDFConnection conn = getConnection();
             QueryExecution qExec = conn.query(parsedQuery)) {
            return qExec.execAsk();
        }
        catch (Exception e) {
            log.error("Error executing ASK query on Neptune", e);
            throw new RuntimeException(e);
        }
    }

    public void upload(Model model, String graphUri) {
        try (RDFConnection conn = getConnection()) {
            if (graphUri == null || graphUri.isEmpty()) {
                conn.load(model);
            } else {
                conn.load(graphUri, model);
            }
        }
        catch (Exception e) {
            log.error("Error uploading model to graph {} in Neptune", graphUri, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public <R> R executeSelectQuery(String query, Function<ResultSet, R> resultHandler) {
        Query parsedQuery = QueryFactory.create(query, Syntax.syntaxARQ);
        try (RDFConnection conn = getConnection();
             QueryExecution qExec = conn.query(parsedQuery)) {
            ResultSet resultSet = qExec.execSelect();
            return resultHandler.apply(resultSet);
        }
        catch (Exception e) {
            log.error("Error executing SELECT query on Neptune", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void dropGraph(String graphUri) {
        String deleteQuery = String.format("DROP GRAPH <%s>", graphUri);
        executeUpdateQuery(deleteQuery);
    }

    @Override
    public void updateGraph(String graphUri, Model model) {
        upload(model, graphUri);
    }

    private RDFConnection getConnection() {
        return RDFConnectionRemote.newBuilder()
                                  .destination(sparqlEndpoint)
                                  .queryEndpoint(sparqlEndpoint)
                                  .updateEndpoint(sparqlEndpoint)
                                  .gspEndpoint(gspEndpoint)
                                  .build();
    }
}
