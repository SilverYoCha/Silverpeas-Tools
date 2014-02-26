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
package org.silverpeas.tools.dbBuilder.wysiwyg.adjustment;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.silverpeas.tools.util.Config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * User: Yohann Chastagnier
 * Date: 26/02/14
 */
public class LogRewriter {

  private final File dbBuilderLogs;
  private File rewrittenDbBuilderLogs;
  private DataWiring dataWiring;

  private Map<String, ComponentLogs> currents = new LinkedHashMap<String, ComponentLogs>();

  /**
   * @param dbBuilderLogs the path (with filename) of dbBuilder.log
   * @return the instance containing the result file
   */
  public static LogRewriter execute(File dbBuilderLogs) throws Exception {
    return new LogRewriter(dbBuilderLogs).execute();
  }

  /**
   * Default constructor
   * @param dbBuilderLogs the path (with filename) of dbBuilder.log
   */
  private LogRewriter(File dbBuilderLogs) {
    if (!dbBuilderLogs.isFile()) {
      throw new IllegalArgumentException("given dbBuilder log file is not a physical file ...");
    }
    this.dbBuilderLogs = dbBuilderLogs;
    String parentPath = dbBuilderLogs.getParentFile().getAbsolutePath();
    String fileBasename = FilenameUtils.getBaseName(dbBuilderLogs.getName());
    String fileExtension = FilenameUtils.getExtension(dbBuilderLogs.getName());
    this.rewrittenDbBuilderLogs = FileUtils.getFile(parentPath,
        fileBasename + "_" + Config.getFormattedExecutionStart() + "." + fileExtension);
  }

  /**
   * Executing treatments
   */
  private LogRewriter execute() throws Exception {
    dataWiring = DataWiring.execute(dbBuilderLogs);

    BufferedReader dbBuilderLogsReader = IOUtils.toBufferedReader(new FileReader(dbBuilderLogs));
    try {
      FileOutputStream dbBuilderLogsOS = FileUtils.openOutputStream(rewrittenDbBuilderLogs);
      try {

        dataWiring.writeStatistics(dbBuilderLogsOS);

        if (Boolean.valueOf(System.getProperty("statsOnly"))) {
          return this;
        }

        String line;
        do {

          // A line
          line = dbBuilderLogsReader.readLine();

          if (line == null) {
            break;
          }

          String componentIdEnding = null;

          // New component ?
          Matcher matcher = DataWiring.REGEXP_NEW_COMPONENT_DETECTOR.matcher(line);
          if (matcher.find()) {
            String componentId = matcher.group(1);
            if (currents.containsKey(componentId)) {
              throw new IllegalStateException(
                  "The componentId " + componentId + " has already been started !!!");
            }
            currents.put(componentId, new ComponentLogs(componentId));
          }

          // End component ?
          matcher = DataWiring.REGEXP_END_COMPONENT_DETECTOR.matcher(line);
          if (matcher.find()) {
            String componentId = matcher.group(1);
            if (!currents.containsKey(componentId)) {
              throw new IllegalStateException(
                  "The componentId " + componentId + " has already been ending !!!");
            }

            componentIdEnding = componentId;
          }

          if (!currents.isEmpty()) {

            String componentId = dataWiring.getComponentIdFromLine(line);
            if (componentId == null) {
              //  System.out.println("No component found in the line : " + line);
              continue;
            }

            ComponentLogs componentLogs = currents.get(componentId);
            if (componentLogs == null) {
              System.out.println("No component logs found the line : " + line);
              continue;
            }

            componentLogs.addLine(line);
          }

          if (componentIdEnding != null) {
            ComponentLogs componentLogs = currents.remove(componentIdEnding);
            dataWiring.clearComponentId(componentIdEnding);

            IOUtils.writeLines(componentLogs.getLines(), "\n", dbBuilderLogsOS);
          }

        } while (true);
      } finally {
        IOUtils.closeQuietly(dbBuilderLogsOS);
      }
    } finally {
      IOUtils.closeQuietly(dbBuilderLogsReader);
    }
    return this;
  }
}
