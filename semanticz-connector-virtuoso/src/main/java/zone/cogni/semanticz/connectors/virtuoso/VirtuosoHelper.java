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
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;

import java.util.ArrayList;
import java.util.List;

public class VirtuosoHelper {

  public static Model patchModel(Model model) {
    //avoid some weird virtuoso behaviour
    // it converts false to '0'^^xsd:integer
    List<Statement> booleanStatements = new ArrayList<>();
    model.listStatements().forEachRemaining(statement -> {
      RDFNode object = statement.getObject();
      if (!object.isLiteral() || !XSDDatatype.XSDboolean.getURI().equals(object.asLiteral().getDatatypeURI())) return;
      booleanStatements.add(statement);
    });

    model.remove(booleanStatements);
    booleanStatements.forEach(statement -> {
      Literal newObject = model.createTypedLiteral(statement.getLiteral().getBoolean() ? "1" : "0", XSDDatatype.XSDboolean);
      model.add(statement.getSubject(), statement.getPredicate(), newObject);
    });
    return model;
  }

  public static String getVirtuosoUpdateUrl(final String sparqlEndpointUrl,
      final String graphIri) {
    return getVirtuosoGspFromSparql(sparqlEndpointUrl) + "?" + (StringUtils.isBlank(graphIri)
        ? "default" : ("graph=" + graphIri));
  }

  public static String getVirtuosoGspFromSparql(final String sparqlEndpointUrl) {
    return StringUtils.substringBeforeLast(sparqlEndpointUrl, "/") + "/sparql-graph-crud-auth";
  }
}
