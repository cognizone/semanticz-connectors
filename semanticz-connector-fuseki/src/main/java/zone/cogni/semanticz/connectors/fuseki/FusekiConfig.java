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

package zone.cogni.semanticz.connectors.fuseki;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;
import zone.cogni.semanticz.connectors.general.Config;
import zone.cogni.semanticz.connectors.utils.Constants;

@Getter
@Setter
@Accessors(chain = true)
public class FusekiConfig extends Config {

  private String updateUrl;
  private String queryUrl;
  private String graphStoreUrl;
  private String overwriteTurtleMimeType;

  public static FusekiConfig from(Config config) {
    FusekiConfig fusekiConfig = new FusekiConfig();
    fusekiConfig.setUrl(config.getUrl());
    fusekiConfig.setUser(config.getUser());
    fusekiConfig.setPassword(config.getPassword());
    return fusekiConfig;
  }

  public String getUpdateUrl() {
    return getServiceUrl(updateUrl, "/update");
  }

  public String getQueryUrl() {
    return getServiceUrl(queryUrl, "/query");
  }

  public String getGraphStoreUrl() {
    return getServiceUrl(graphStoreUrl, "/data");
  }

  public String getTurtleMimeType() {
    return StringUtils.defaultIfBlank(overwriteTurtleMimeType, Constants.TEXT_TURTLE);
  }

  private String getServiceUrl(String fixedUrl, String defaultSuffix) {
    if (StringUtils.isNotBlank(fixedUrl)) return fixedUrl;
    else if (StringUtils.isNotBlank(getUrl())) return getUrl() + defaultSuffix;
    else return null;
  }
}
