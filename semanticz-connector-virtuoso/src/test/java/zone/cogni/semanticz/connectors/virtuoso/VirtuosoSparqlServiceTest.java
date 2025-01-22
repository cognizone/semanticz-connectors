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

import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Objects;

import org.apache.http.entity.ByteArrayEntity;
import org.apache.jena.query.Dataset;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.jupiter.api.Disabled;
import zone.cogni.semanticz.connectors.utils.ApacheHttpClientUtils;
import zone.cogni.semanticz.connectors.utils.AbstractSparqlServiceTest;
import zone.cogni.semanticz.connectors.general.Config;

import static zone.cogni.semanticz.connectors.utils.Constants.TEXT_TURTLE;

@Disabled("An integration test dependent on a running Virtuoso instance. To run it manually, set the Config below properly and run the tests.")
public class VirtuosoSparqlServiceTest extends AbstractSparqlServiceTest<VirtuosoSparqlService> {

  public VirtuosoSparqlService createSUT() {
    final Config config = new Config();
    config.setUrl("http://localhost:8890/sparql-auth");
    config.setUser("dba");
    config.setPassword("dba");
    config.setGraphCrudUseBasicAuth(false);

    final Dataset dataset;
    try {
      dataset = RDFDataMgr.loadDataset(
              Objects.requireNonNull(AbstractSparqlServiceTest.class.getResource("/dataset.trig")).toURI()
                      .toString());
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }

    final Iterator<String> names = dataset.listNames();
    while (names.hasNext()) {
      final String name = names.next();
      final StringWriter w = new StringWriter();
      RDFDataMgr.write(w, dataset.getNamedModel(name), Lang.TURTLE);

      final String url = VirtuosoHelper.getVirtuosoUpdateUrl(config.getUrl(), name);
      ApacheHttpClientUtils.executeAuthenticatedPostOrPut(url, config.getUser(), config.getPassword(),
              config.isGraphCrudUseBasicAuth(), new ByteArrayEntity(w.toString().getBytes()), true,
              TEXT_TURTLE + ";charset=utf-8");
    }

    return new VirtuosoSparqlService(config);
  }

  @Override
  protected void disposeSUT(VirtuosoSparqlService sparqlService) {
    // do nothing;
  }
}