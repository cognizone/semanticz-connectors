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

import zone.cogni.semanticz.connectors.general.Config;

public class Utils {

  static final String PREFIX = "semanticz.connector.virtuoso.tests.";

  private static String getProperty(String property) {
    return System.getProperty(PREFIX + property);
  }

  public static Config createTestConfig() {
    return new Config()
            .setUrl(getProperty("url"))
            .setUser(getProperty("username"))
            .setPassword(getProperty("password"))
            .setGraphCrudUseBasicAuth(false);
  }
}