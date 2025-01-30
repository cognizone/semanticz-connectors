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

package zone.cogni.semanticz.connectors.jenamemory;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import zone.cogni.semanticz.connectors.general.SparqlService;

import java.io.File;
import java.util.function.Function;

public class JenaModelSparqlService implements SparqlService {

    protected final Dataset dataset = DatasetFactory.create();

    protected final Boolean simulateRelaxedVirtuosoSparqlSelect;

    public JenaModelSparqlService() {
        this(false);
    }

    public JenaModelSparqlService(Boolean simulateRelaxedVirtuosoSparqlSelect) {
        this.simulateRelaxedVirtuosoSparqlSelect = simulateRelaxedVirtuosoSparqlSelect;
    }

    @Override
    public void uploadTtlFile(File file) {
        final String uri = file.toURI().toString();
        final Model model = RDFDataMgr.loadModel(uri, Lang.TTL);
        dataset.addNamedModel(uri, model);
    }

    @Override
    public void executeUpdateQuery(String updateQuery) {
        UpdateRequest request = UpdateFactory.create(updateQuery);
        UpdateAction.execute(request, dataset);
    }

    @Override
    public void updateGraph(String graphUri, Model model) {
        dataset.addNamedModel(graphUri, model);
    }

    private Dataset getDatasetForSelect() {
        if (simulateRelaxedVirtuosoSparqlSelect) {
            Dataset relaxedDataset = DatasetFactory.wrap(DatasetGraphFactory.cloneStructure(dataset.asDatasetGraph()));
            relaxedDataset.asDatasetGraph().setDefaultGraph(relaxedDataset.asDatasetGraph().getUnionGraph());
            return relaxedDataset;
        }
        return dataset;
    }

    @Override
    public <R> R executeSelectQuery(String query, Function<ResultSet, R> resultHandler) {
        try (QueryExecution queryExecution = QueryExecutionFactory.create(QueryFactory.create(query), getDatasetForSelect())) {
            return resultHandler.apply(queryExecution.execSelect().materialise());
        }
    }

    @Override
    public boolean executeAskQuery(String query) {
        try (QueryExecution queryExecution = QueryExecutionFactory.create(QueryFactory.create(query), getDatasetForSelect())) {
            return queryExecution.execAsk();
        }
    }

    public Model executeConstructQuery(String query) {
        try (QueryExecution queryExecution = QueryExecutionFactory.create(QueryFactory.create(query), getDatasetForSelect())) {
            return queryExecution.execConstruct();
        }
    }

    @Override
    public void dropGraph(String graphUri) {
        dataset.removeNamedModel(graphUri);
    }
}
