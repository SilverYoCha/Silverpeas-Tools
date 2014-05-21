/*
 * Copyright (C) 2000 - 2014 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of
 * the GPL, you may redistribute this Program in connection with Free/Libre
 * Open Source Software ("FLOSS") applications as described in Silverpeas's
 * FLOSS exception. You should have recieved a copy of the text describing
 * the FLOSS exception, and it is also available here:
 * "http://www.silverpeas.org/docs/core/legal/floss_exception.html"
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.silverpeas.tools.dbBuilder.wysiwyg.purge;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: Yohann Chastagnier
 * Date: 26/02/14
 */
public class DataWiring {
  private final File dbBuilderLogs;

  public final static Pattern REGEXP_NEW_COMPONENT_DETECTOR = Pattern
      .compile("(?i)Starting wysiwyg contents purge for component instance id ([a-z]+[0-9]+)");
  public final static Pattern REGEXP_END_COMPONENT_DETECTOR = Pattern
      .compile("(?i)Finishing wysiwyg contents purge for component instance id ([a-z]+[0-9]+)");

  private final static Pattern REGEXP_COMPONENT_ID = Pattern.compile("(?i)([a-z]+[0-9]+)");

  private Set<String> components = new HashSet<String>();

  /**
   * @param dbBuilderLogs the path (with filename) of dbBuilder.log
   * @return the instance containing the result file
   */
  public static DataWiring execute(File dbBuilderLogs) throws Exception {
    return new DataWiring(dbBuilderLogs).execute();
  }

  /**
   * Default constructor
   * @param dbBuilderLogs the path (with filename) of dbBuilder.log
   */
  private DataWiring(File dbBuilderLogs) {
    this.dbBuilderLogs = dbBuilderLogs;
  }

  /**
   * Executing treatments
   */
  private DataWiring execute() throws Exception {
    BufferedReader dbBuilderLogsReader = IOUtils.toBufferedReader(new FileReader(dbBuilderLogs));
    try {
      String line = "initialisation to start ...";
      do {

        // A line
        line = dbBuilderLogsReader.readLine();

        if (line == null) {
          break;
        }

        performStartingOrEnding(line);

      } while (true);
    } finally {
      IOUtils.closeQuietly(dbBuilderLogsReader);
    }
    return this;
  }

  private boolean performStartingOrEnding(String line) {
    boolean result = false;
    Matcher matcher = REGEXP_NEW_COMPONENT_DETECTOR.matcher(line);
    while (matcher.find()) {
      components.add(verifyComponentId(matcher.group(1), line));
      result = true;
    }
    matcher = REGEXP_END_COMPONENT_DETECTOR.matcher(line);
    while (matcher.find()) {
      components.add(verifyComponentId(matcher.group(1), line));
      result = true;
    }
    return result;
  }

  private String verifyComponentId(String componentId, String line) {
    if (!componentId.matches("(?i)^[a-z]+[0-9]+$")) {
      throw new IllegalArgumentException(componentId + " is not a component id (" + line + ")");
    }
    return componentId;
  }

  /**
   * Retrieve from a line the component id.
   * @param line
   * @return
   */
  public String getComponentIdFromLine(String line) {
    Matcher componentMatcher = REGEXP_COMPONENT_ID.matcher(line);
    for (String componentId : components) {
      while (componentMatcher.find()) {
        String extractedComponentId = componentMatcher.group(1);
        if (extractedComponentId.equals(componentId)) {
          return componentId;
        }
      }

      componentMatcher.reset();
    }
    return null;
  }

  public void clearComponentId(String componentId) {
    components.remove(componentId);
  }
}
