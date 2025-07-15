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

package zone.cogni.semanticz.connectors.graphdb;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.http.auth.AuthEnv;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionRemote;
import org.apache.jena.sparql.exec.http.UpdateSendMode;
import zone.cogni.semanticz.connectors.general.RDFConnectionSparqlService;
import zone.cogni.semanticz.connectors.general.SparqlService;
import zone.cogni.semanticz.connectors.utils.Constants;

import java.net.URI;

public class GraphDBSparqlService extends RDFConnectionSparqlService implements SparqlService {

    private final GraphDBConfig config;

    public GraphDBSparqlService(GraphDBConfig config) {
        this.config = config;
        if (StringUtils.isNotBlank(config.getUser()) && StringUtils.isNotBlank(config.getPassword())) {
            // Register auth for base URL
            AuthEnv.get()
                    .registerUsernamePassword(URI.create(config.getUrl()),
                            config.getUser(), config.getPassword());
            // Also register auth for the specific repository endpoint
            AuthEnv.get()
                    .registerUsernamePassword(URI.create(config.getSparqlEndpoint()),
                            config.getUser(), config.getPassword());
        }
    }

    @Override
    protected RDFConnection getConnection() {
        return RDFConnectionRemote.newBuilder()
                .destination(config.getUrl())
                .queryEndpoint(config.getRepositoryPath())
                .updateEndpoint(config.getSparqlUpdateEndpoint())
                .gspEndpoint(config.getGspEndpoint())
                .updateSendMode(UpdateSendMode.asPostForm)
                .build();
    }

    @Override
    protected RDFConnection getConstructConnection() {
        return RDFConnectionRemote
                .newBuilder()
                .queryEndpoint(config.getSparqlEndpoint())
                .updateEndpoint(config.getSparqlEndpoint())
                .updateSendMode(UpdateSendMode.asPostForm)
                .destination(config.getSparqlEndpoint())
                .acceptHeaderQuery(Constants.TEXT_TURTLE)
                .updateEndpoint(config.getSparqlUpdateEndpoint())
                .build();
    }

}