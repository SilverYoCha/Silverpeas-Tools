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

import org.apache.commons.lang3.time.DurationFormatUtils;

import java.io.File;

/**
 * User: Yohann Chastagnier
 * Date: 26/02/14
 */
public class Executor {

  private final File dbBuilderLogs;

  /**
   * @param dbBuilderLogs the path (with filename) of dbBuilder.log
   * @return the instance
   */
  public static Executor execute(File dbBuilderLogs) throws Exception {
    return new Executor(dbBuilderLogs).execute();
  }

  /**
   * Default constructor
   * @param dbBuilderLogs the path (with filename) of dbBuilder.log
   */
  private Executor(File dbBuilderLogs) {
    this.dbBuilderLogs = dbBuilderLogs;
  }

  /**
   * Executing treatments
   */
  public Executor execute() throws Exception {

    // 1 - Rewrite the dbBuilder.log file (sort)
    LogRewriter logRewriter = LogRewriter.execute(dbBuilderLogs);

    return this;
  }

  /**
   * @param args
   * @see
   */
  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      throw new IllegalArgumentException(
          "Expected 1 parameter (dbBuilder log file) but actual " + args.length + " parameter(s)");
    }

    long start = System.currentTimeMillis();
    Executor.execute(new File(args[0]));
    long end = System.currentTimeMillis();
    System.out.println("Treatment duration: " + DurationFormatUtils.formatDurationHMS(end - start));
  }
}
