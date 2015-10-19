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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.lang3.time.DateUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Yohann Chastagnier
 */
public class LastModifiedDate {

  private File current = new File(".");
  private List<File> files = new ArrayList<>();
  private Config config;

  public static LastModifiedDate execute(final Config config, final List<String> paths)
      throws Exception {
    return new LastModifiedDate(config,
        (paths == null || paths.isEmpty()) ? Collections.singletonList("") : paths).execute();
  }

  private LastModifiedDate(final Config config, List<String> paths) {
    this.config = config;
    files.addAll(paths.stream().map(path -> {
      File currentPath = new File(path);
      return currentPath.getPath().equals(currentPath.getAbsolutePath()) ? currentPath :
          new File(current, path);
    }).collect(Collectors.toList()));
  }

  private LastModifiedDate execute() throws Exception {
    long newLastModifiedDate = config.getTranslatedDate().getTime();
    for (File currentFile : files) {
      if (currentFile.isFile()) {
        currentFile.setLastModified(newLastModifiedDate);
      } else if (currentFile.isDirectory()) {
        for (File file : FileUtils.listFilesAndDirs(currentFile, FileFilterUtils.trueFileFilter(),
            FileFilterUtils.trueFileFilter())) {
          file.setLastModified(newLastModifiedDate);
        }
      }
    }
    return this;
  }

  public static class Config {
    private Date dateToTranslate = new Date();
    private int nbMilliseconds = 0;
    private int nbSeconds = 0;
    private int nbMinutes = 0;
    private int nbHours = 0;
    private int nbDays = 0;
    private int nbWeeks = 0;
    private int nbMonths = 0;
    private int nbYears = 0;

    public Date getTranslatedDate() {
      Date translatedDate = DateUtils.addMilliseconds(dateToTranslate, nbMilliseconds);
      translatedDate = DateUtils.addSeconds(translatedDate, nbSeconds);
      translatedDate = DateUtils.addMinutes(translatedDate, nbMinutes);
      translatedDate = DateUtils.addHours(translatedDate, nbHours);
      translatedDate = DateUtils.addDays(translatedDate, nbDays);
      translatedDate = DateUtils.addWeeks(translatedDate, nbWeeks);
      translatedDate = DateUtils.addMonths(translatedDate, nbMonths);
      translatedDate = DateUtils.addYears(translatedDate, nbYears);
      return translatedDate;
    }

    public Config set(String directive, long value) {
      switch (directive) {
        case "ttt":
          dateToTranslate = new Date(value);
          break;
        // Offset
        case "oms":
          nbMilliseconds = (int) value;
          break;
        case "os":
          nbSeconds = (int) value;
          break;
        case "om":
          nbMinutes = (int) value;
          break;
        case "oh":
          nbHours = (int) value;
          break;
        case "oD":
          nbDays = (int) value;
          break;
        case "oW":
          nbWeeks = (int) value;
          break;
        case "oM":
          nbMonths = (int) value;
          break;
        case "oY":
          nbYears = (int) value;
          break;
        // Set
        case "ms":
          DateUtils.setMilliseconds(dateToTranslate, (int) value);
          break;
        case "s":
          DateUtils.setSeconds(dateToTranslate, (int) value);
          break;
        case "m":
          DateUtils.setMinutes(dateToTranslate, (int) value);
          break;
        case "h":
          DateUtils.setHours(dateToTranslate, (int) value);
          break;
        case "D":
          DateUtils.setDays(dateToTranslate, (int) value);
          break;
        case "M":
          DateUtils.setMonths(dateToTranslate, (int) value);
          break;
        case "Y":
          DateUtils.setYears(dateToTranslate, (int) value);
          break;
        // Errors
        default:
          throw new IllegalArgumentException(
              "{-" + directive + "} with [" + value + "] can not be set...");
      }
      return this;
    }
  }
}
