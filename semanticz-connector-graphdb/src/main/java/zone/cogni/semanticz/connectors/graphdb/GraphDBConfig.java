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

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import zone.cogni.semanticz.connectors.general.Config;

@Setter
@Getter
@Accessors(chain = true)
public class GraphDBConfig extends Config {

  private String repository;

  public GraphDBConfig() {
  }

  public GraphDBConfig(Config config) {
    setUrl(config.getUrl());
    setUser(config.getUser());
    setPassword(config.getPassword());
  }

  public String getSparqlEndpoint() {
    return getUrl() + "/repositories/" + getRepository();
  }

  public String getSparqlUpdateEndpoint() {
    return getSparqlEndpoint() + "/statements";
  }

  public String getImportTextEndpoint() {
    return getUrl() + "/rest/data/import/upload/" + getRepository() + "/text";
  }
}
