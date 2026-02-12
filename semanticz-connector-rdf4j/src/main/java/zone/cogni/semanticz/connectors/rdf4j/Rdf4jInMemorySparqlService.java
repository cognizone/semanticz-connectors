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
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.resultio.QueryResultIO;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import zone.cogni.semanticz.connectors.general.SparqlService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.function.Function;

/**
 * SparqlService backed by RDF4J in-memory repository.
 */
public class Rdf4jInMemorySparqlService implements SparqlService {

    private final Repository repository;
    private final ValueFactory vf = SimpleValueFactory.getInstance();

    public Rdf4jInMemorySparqlService() {
        repository = new SailRepository(new MemoryStore());
        repository.init();
    }

    @Override
    public void uploadTtlFile(File file) {
        try (RepositoryConnection conn = repository.getConnection()) {
            IRI context = vf.createIRI(file.toURI().toString());
            conn.add(file, "", RDFFormat.TURTLE, context);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Model executeConstructQuery(String query) {
        try (RepositoryConnection conn = repository.getConnection()) {
            GraphQuery graphQuery = conn.prepareGraphQuery(query);
            var rdf4jModel = QueryResults.asModel(graphQuery.evaluate());
            return rdf4jModelToJena(rdf4jModel);
        }
    }

    @Override
    public void executeUpdateQuery(String updateQuery) {
        try (RepositoryConnection conn = repository.getConnection()) {
            conn.prepareUpdate(updateQuery).execute();
        }
    }

    @Override
    public boolean executeAskQuery(String query) {
        try (RepositoryConnection conn = repository.getConnection()) {
            BooleanQuery booleanQuery = conn.prepareBooleanQuery(query);
            return booleanQuery.evaluate();
        }
    }

    @Override
    public <R> R executeSelectQuery(String query, Function<ResultSet, R> resultHandler) {
        try (RepositoryConnection conn = repository.getConnection()) {
            TupleQuery tupleQuery = conn.prepareTupleQuery(query);
            try (TupleQueryResult result = tupleQuery.evaluate()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                QueryResultIO.writeTuple(result, TupleQueryResultFormat.JSON, out);
                var rs = ResultSetFactory.fromJSON(new ByteArrayInputStream(out.toByteArray()));
                return resultHandler.apply(ResultSetFactory.copyResults(rs));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void dropGraph(String graphUri) {
        IRI context = vf.createIRI(graphUri);
        try (RepositoryConnection conn = repository.getConnection()) {
            conn.clear(context);
        }
    }

    @Override
    public void updateGraph(String graphUri, Model model) {
        var rdf4jModel = jenaToRdf4j(model);
        IRI context = vf.createIRI(graphUri);
        try (RepositoryConnection conn = repository.getConnection()) {
            conn.add(rdf4jModel, context);
        }
    }

    private org.eclipse.rdf4j.model.Model jenaToRdf4j(Model model) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        RDFDataMgr.write(out, model, RDFLanguages.TURTLE);
        try (ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray())) {
            return Rio.parse(in, "", RDFFormat.TURTLE);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Jena model to RDF4J", e);
        }
    }

    private Model rdf4jModelToJena(org.eclipse.rdf4j.model.Model rdf4jModel) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Rio.write(rdf4jModel, out, RDFFormat.TURTLE);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize RDF4J model", e);
        }
        var jenaModel = ModelFactory.createDefaultModel();
        try (ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray())) {
            RDFDataMgr.read(jenaModel, in, RDFLanguages.TURTLE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return jenaModel;
    }
}
