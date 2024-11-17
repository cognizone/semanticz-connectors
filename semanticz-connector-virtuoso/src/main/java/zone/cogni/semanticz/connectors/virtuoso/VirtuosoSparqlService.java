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

package zone.cogni.semanticz.connectors.virtuoso;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.http.auth.AuthEnv;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionRemote;
import zone.cogni.semanticz.connectors.general.SparqlService;
import zone.cogni.semanticz.connectors.general.Config;
import zone.cogni.semanticz.connectors.general.RDFConnectionSparqlService;
import zone.cogni.semanticz.connectors.utils.Constants;

import java.net.URI;

public class VirtuosoSparqlService extends RDFConnectionSparqlService implements
    SparqlService {

  private final Config config;

  public VirtuosoSparqlService(Config config) {
    this.config = config;
    AuthEnv.get()
        .registerUsernamePassword(URI.create(StringUtils.substringBeforeLast(config.getUrl(), "/")),
            this.config.getUser(), this.config.getPassword());
  }

  protected RDFConnection getConnection() {
    return RDFConnectionRemote
        .newBuilder()
        .queryEndpoint(config.getUrl())
        .updateEndpoint(config.getUrl())
        .destination(config.getUrl())
        .gspEndpoint(VirtuosoHelper.getVirtuosoGspFromSparql(config.getUrl()))
        .build();
  }

  protected RDFConnection getConstructConnection() {
    return RDFConnectionRemote
        .newBuilder()
        .queryEndpoint(config.getUrl())
        .updateEndpoint(config.getUrl())
        .destination(config.getUrl())
        .acceptHeaderQuery(Constants.TEXT_TURTLE)
        .gspEndpoint(VirtuosoHelper.getVirtuosoGspFromSparql(config.getUrl()))
        .build();
  }
}
