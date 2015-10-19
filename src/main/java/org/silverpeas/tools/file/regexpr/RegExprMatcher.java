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
package org.silverpeas.tools.file.regexpr;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Yohann Chastagnier
 */
public class RegExprMatcher {

  private File current = new File(".");
  private List<File> files = new ArrayList<>();
  private Config config;
  private long nbAnalysedFiles = 0;
  private long nbMatchedFiles = 0;

  public static RegExprMatcher execute(final Config config, final List<String> paths)
      throws Exception {
    return new RegExprMatcher(config,
        (paths == null || paths.isEmpty()) ? Collections.singletonList("") : paths).execute();
  }

  private RegExprMatcher(final Config config, List<String> paths) {
    this.config = config;
    files.addAll(paths.stream().map(path -> {
      File currentPath = new File(path);
      return currentPath.getPath().equals(currentPath.getAbsolutePath()) ? currentPath :
          new File(current, path);
    }).collect(Collectors.toList()));
  }

  private RegExprMatcher execute() throws Exception {
    for (File file : files) {
      analyse(file);
    }
    System.out.println("Just analyzing " + nbAnalysedFiles + " files.");
    System.out.println(nbMatchedFiles + " of them matched.");
    return this;
  }

  private void analyse(File startFile) throws Exception {
    Queue<File> fileQueue = new ArrayDeque<>(100000);
    fileQueue.add(startFile);
    while (!fileQueue.isEmpty()) {
      File file = fileQueue.poll();
      if (file.isFile()) {
        if (config.getFileFilter().accept(file)) {
          boolean fileMatched = false;
          for (PatternConfig patternConfig : config.getPatternConfigs()) {
            boolean found = patternConfig.pattern.matcher(FileUtils.readFileToString(file)).find();
            fileMatched =
                (found && patternConfig.mustMatch) || (!found && !patternConfig.mustMatch);
            if (!fileMatched) {
              break;
            }
          }
          if (fileMatched) {
            System.out.println(file.getPath());
            nbMatchedFiles++;
          }
          nbAnalysedFiles++;
        }
      } else if (file.isDirectory() && config.getDirFilter().accept(file)) {
        for (File subFile : file.listFiles()) {
          fileQueue.add(subFile);
        }
      } else {
        int i = 0;
      }
    }
  }

  public static class Config {
    private List<PatternConfig> patterns = new ArrayList<>();
    private StringBuilder currentPattern = new StringBuilder();

    private FileFilter fileFilter = FileFileFilter.FILE;
    private FileFilter dirFilter = DirectoryFileFilter.DIRECTORY;

    public List<PatternConfig> getPatternConfigs() {
      registerPattern();
      return patterns;
    }

    public FileFilter getFileFilter() {
      return fileFilter;
    }

    public FileFilter getDirFilter() {
      return dirFilter;
    }

    public Config set(String directive, String value) {
      switch (directive) {
        case "-fileFilter":
          fileFilter = new RegexFileFilter(value);
          break;
        case "-dirFilter":
          dirFilter = new RegexFileFilter(value);
          break;
        case "-!fileFilter":
          fileFilter = new NotFileFilter(new RegexFileFilter(value));
          break;
        case "-!dirFilter":
          dirFilter = new NotFileFilter(new RegexFileFilter(value));
          break;
        // Errors
        default:
          throw new IllegalArgumentException(
              "{" + directive + "} with [" + value + "] can not be set...");
      }
      return this;
    }

    public Config set(String pattern) {
      switch (pattern) {
        case "|":
          registerPattern();
          break;
        default:
          currentPattern.append(pattern);
      }
      return this;
    }

    private void registerPattern() {
      if (currentPattern.length() > 0) {
        String pattern = currentPattern.toString();
        boolean mustMatch = true;
        if (pattern.startsWith("!")) {
          pattern = pattern.substring(1);
          mustMatch = false;
        }
        if (!pattern.startsWith("#")) {
          patterns.add(new PatternConfig(pattern, mustMatch));
        }
        currentPattern = new StringBuilder();
      }
    }
  }

  private static class PatternConfig {
    private final Pattern pattern;
    private final boolean mustMatch;

    private PatternConfig(final String pattern, final boolean mustMatch) {
      this.pattern = Pattern.compile(pattern);
      this.mustMatch = mustMatch;
    }
  }
}
