/*
 * Copyright (C) 2000 - 2015 Silverpeas
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
package org.silverpeas.tools.file.lastmodifieddate;

import org.apache.commons.lang3.time.DurationFormatUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * User: Yohann Chastagnier
 * Date: 26/02/14
 */
public class Executor {

  private final LastModifiedDate.Config config;
  private final List<String> paths;

  /**
   * @param paths the paths to perform
   * @return the instance
   */
  public static Executor execute(LastModifiedDate.Config config, List<String> paths)
      throws Exception {
    return new Executor(config, paths).execute();
  }

  /**
   * Default constructor
   * @param paths the paths to perform
   */
  private Executor(LastModifiedDate.Config config, List<String> paths) {
    this.config = config;
    this.paths = paths;
  }

  /**
   * Executing treatments
   */
  public Executor execute() throws Exception {
    LastModifiedDate.execute(config, paths);
    return this;
  }

  /**
   * @param args
   * @see
   */
  public static void main(String[] args) throws Exception {
    LastModifiedDate.Config config = new LastModifiedDate.Config();
    List<String> files = new ArrayList<>();
    Iterator<String> argsIt = Arrays.asList(args).iterator();
    while (argsIt.hasNext()) {
      String currentArg = argsIt.next();
      if (currentArg.startsWith("-")) {
        config.set(currentArg.substring(1), Long.parseLong(argsIt.next()));
      } else {
        files.add(currentArg);
      }
    }

    long start = System.currentTimeMillis();
    Executor.execute(config, files);
    long end = System.currentTimeMillis();
    System.out.println("Treatment duration: " + DurationFormatUtils.formatDurationHMS(end - start));
  }
}
