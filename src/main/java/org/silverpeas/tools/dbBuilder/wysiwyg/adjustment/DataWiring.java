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

import org.apache.commons.io.IOUtils;
import org.silverpeas.tools.util.Config;
import org.silverpeas.tools.util.MapUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.silverpeas.tools.util.StringUtil.concatValues;

/**
 * User: Yohann Chastagnier
 * Date: 26/02/14
 */
public class DataWiring {
  private final File dbBuilderLogs;

  private final static Pattern REGEXP_COMPONENT_ID = Pattern.compile("(?i)([a-z]+[0-9]+)");
  private final static Pattern REGEXP_SIMPLEDOC_ID = Pattern.compile("(?i)(simpledoc_[0-9]+)");
  private final static Pattern REGEXP_WYSIWYG_BASENAME =
      Pattern.compile("(?i)([a-z_]*[0-9]+wysiwyg)");
  private final static Pattern REGEXP_WYSIWYG_NAME =
      Pattern.compile("(?i)([a-z_]*[0-9]+wysiwyg[^ \\\\/]+)");

  private final static Pattern REGEXP_PATH_LANGUAGE =
      Pattern.compile("(?i)[\\\\/]([a-z]{2})[\\\\/]");
  private final static Pattern REGEXP_WYSIWYG_LANGUAGE =
      Pattern.compile("(?i)wysiwyg_([a-z]{2}|)\\.txt");

  public final static Pattern REGEXP_NEW_COMPONENT_DETECTOR =
      Pattern.compile("(?i)Starting wysiwyg adjustment for component instance id ([a-z]+[0-9]+)");
  public final static Pattern REGEXP_END_COMPONENT_DETECTOR =
      Pattern.compile("(?i)Finishing wysiwyg adjustment for component instance id ([a-z]+[0-9]+)");
  private final static Pattern REGEXP_OPERATION = Pattern
      .compile("(?i)([a-z]+[0-9]+)[\\\\/](simpledoc_[0-9]+)[^ ]+[\\\\/]([a-z_]*[0-9]+wysiwyg)");
  private final static Pattern REGEXP_OPERATION_WYSIWYG_ALL =
      Pattern.compile("(?i)[\\\\/]([a-z_]*[0-9]+wysiwyg[^ ]+)");
  private final static Pattern REGEXP_BACKUP =
      Pattern.compile("(?i)([a-z]+[0-9]+)[\\\\/](simpledoc_[0-9]+)");

  private Set<String> components = new HashSet<String>();
  private Map<String, Set<String>> componentSimpledocs = new LinkedHashMap<String, Set<String>>();
  private Map<String, Set<String>> componentWysiwygBasenames =
      new LinkedHashMap<String, Set<String>>();
  private Map<String, Set<String>> wysiwygBasenamesComponents =
      new LinkedHashMap<String, Set<String>>();
  private Map<String, Map<String, Set<String>>> componentBadWysiwygNamesSimpleDoc =
      new LinkedHashMap<String, Map<String, Set<String>>>();
  private Set<String> deletedSimpleDocs = new HashSet<String>();
  private Map<String, Map<String, Set<String>>> componentMergedWysiwygSimpledocs =
      new LinkedHashMap<String, Map<String, Set<String>>>();
  private Map<String, Map<String, Set<String>>> componentRenamedWysiwygSimpledocs =
      new LinkedHashMap<String, Map<String, Set<String>>>();
  private Map<String, Map<String, Set<String>>> componentCopiedWysiwygSimpledocs =
      new LinkedHashMap<String, Map<String, Set<String>>>();
  private Map<String, Map<String, Set<String>>> componentTranslatedWysiwygSimpledocs =
      new LinkedHashMap<String, Map<String, Set<String>>>();
  private Set<String> translatedSimpleDocs = new HashSet<String>();

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

        performCommon(line);
        performStartingOrEnding(line);
        performOperation(line);
        performBackup(line);

      } while (true);
    } finally {
      IOUtils.closeQuietly(dbBuilderLogsReader);
    }
    return this;
  }

  private boolean performCommon(String line) {
    boolean result = false;
    if (line.contains("has been deleted")) {
      Matcher matcher = REGEXP_SIMPLEDOC_ID.matcher(line);
      matcher.find();
      deletedSimpleDocs.add(matcher.group(1));
      result = true;
    } else if (line.contains("with the right language suffix")) {
      Matcher componentMatcher = REGEXP_COMPONENT_ID.matcher(line);
      Matcher wysiwygMatcher = REGEXP_WYSIWYG_NAME.matcher(line);
      Matcher simpleDocMatcher = REGEXP_SIMPLEDOC_ID.matcher(line);
      componentMatcher.find();
      wysiwygMatcher.find();
      simpleDocMatcher.find();
      String componentId = componentMatcher.group(1);
      Map<String, Set<String>> renamedWysiwygSimpledoc =
          componentRenamedWysiwygSimpledocs.get(componentId);
      if (renamedWysiwygSimpledoc == null) {
        renamedWysiwygSimpledoc = new LinkedHashMap<String, Set<String>>();
        componentRenamedWysiwygSimpledocs.put(componentId, renamedWysiwygSimpledoc);
      }
      Matcher pathLanguageMatcher = REGEXP_PATH_LANGUAGE.matcher(line);
      pathLanguageMatcher.find();
      String renameLog =
          "original path " + pathLanguageMatcher.group(1) + ", file " + wysiwygMatcher.group(1) +
              " to ";
      wysiwygMatcher.find();
      MapUtil.putAddSet(renamedWysiwygSimpledoc, renameLog + wysiwygMatcher.group(1),
          simpleDocMatcher.group(1));
      result = true;
    }
    return result;
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

  private boolean performOperation(String line) {
    boolean result = false;
    Matcher matcher = REGEXP_OPERATION.matcher(line);
    Matcher pathLanguageMatcher = REGEXP_PATH_LANGUAGE.matcher(line);
    Matcher wysiwygLanguageMatcher = REGEXP_WYSIWYG_LANGUAGE.matcher(line);
    String componentId = "dummy";
    String wysiwygBasename = "dummy";
    Set<String> simpleDocIds = new LinkedHashSet<String>();
    List<String> pathLanguages = new ArrayList<String>();
    List<String> wysiwygLanguages = new ArrayList<String>();
    while (matcher.find()) {
      pathLanguageMatcher.find();
      pathLanguages.add(pathLanguageMatcher.group(1));
      if (wysiwygLanguageMatcher.find()) {
        wysiwygLanguages
            .add(defaultString(wysiwygLanguageMatcher.group(1), Config.defaultLanguage));
      } else {
        System.out.println("No language extension: " + line);
        wysiwygLanguages.add(Config.defaultLanguage);
      }

      componentId = verifyComponentId(matcher.group(1), line);
      components.add(componentId);

      String simpleDocId = verifySimpleDocId(matcher.group(2), line);
      simpleDocIds.add(simpleDocId);
      MapUtil.putAddSet(componentSimpledocs, componentId, simpleDocId);

      wysiwygBasename = verifyWysiwygBasename(matcher.group(3), line);
      MapUtil.putAddSet(componentWysiwygBasenames, componentId, wysiwygBasename);
      MapUtil.putAddSet(wysiwygBasenamesComponents, wysiwygBasename, componentId);

      result = true;
    }

    if (result) {
      matcher = REGEXP_OPERATION_WYSIWYG_ALL.matcher(line);
      if (matcher.find()) {
        String wysiwygName = matcher.group(1);
        if (matcher.find()) {
          boolean isMerged = line.contains("has been merged into");
          boolean isTranslated = line.contains("into right location language");

          if (wysiwygName.equals(matcher.group(1))) {
            boolean isCopied = line.contains("has been copied into");
            boolean isNonFrTagAsFrToMerge = !pathLanguages.get(0).equals(Config.defaultLanguage);
            boolean isKmeliaPotentialBadDelete =
                componentId.startsWith("kmelia") && simpleDocIds.size() > 1 &&
                    pathLanguages.get(0).equals(pathLanguages.get(1)) &&
                    line.contains("has not been merged into the target");
            if (isNonFrTagAsFrToMerge || isKmeliaPotentialBadDelete) {
              Map<String, Set<String>> potentialBadSimpleDocWysiwygNames =
                  componentBadWysiwygNamesSimpleDoc.get(componentId);
              if (potentialBadSimpleDocWysiwygNames == null) {
                potentialBadSimpleDocWysiwygNames = new LinkedHashMap<String, Set<String>>();
                componentBadWysiwygNamesSimpleDoc
                    .put(componentId, potentialBadSimpleDocWysiwygNames);
              }
              for (String simpleDocId : simpleDocIds) {
                MapUtil.putAddSet(potentialBadSimpleDocWysiwygNames, wysiwygBasename, simpleDocId);
              }
            }
            if (isCopied) {
              Matcher wysiwygMatcher = REGEXP_WYSIWYG_NAME.matcher(line);
              Map<String, Set<String>> copiedWysiwygSimpledoc =
                  componentCopiedWysiwygSimpledocs.get(componentId);
              if (copiedWysiwygSimpledoc == null) {
                copiedWysiwygSimpledoc = new LinkedHashMap<String, Set<String>>();
                componentCopiedWysiwygSimpledocs.put(componentId, copiedWysiwygSimpledoc);
              }
              wysiwygMatcher.find();
              String copiedLog =
                  "path " + pathLanguages.get(0) + ", file " + wysiwygMatcher.group(1) +
                      " to path " + pathLanguages.get(1) + ", file ";
              wysiwygMatcher.find();
              Iterator<String> simpleDocIt = simpleDocIds.iterator();
              MapUtil.putAddSet(copiedWysiwygSimpledoc, copiedLog + wysiwygMatcher.group(1),
                  (simpleDocIt.next() + " -> " + simpleDocIt.next()));
            }
          }
          if (isTranslated) {
            Matcher wysiwygMatcher = REGEXP_WYSIWYG_NAME.matcher(line);
            Map<String, Set<String>> translatedWysiwygSimpledoc =
                componentTranslatedWysiwygSimpledocs.get(componentId);
            if (translatedWysiwygSimpledoc == null) {
              translatedWysiwygSimpledoc = new LinkedHashMap<String, Set<String>>();
              componentTranslatedWysiwygSimpledocs.put(componentId, translatedWysiwygSimpledoc);
            }
            wysiwygMatcher.find();
            String translatedLog =
                "path " + pathLanguages.get(0) + ", file " + wysiwygMatcher.group(1) +
                    " to path " + pathLanguages.get(1) + ", file ";
            wysiwygMatcher.find();
            Iterator<String> simpleDocIt = simpleDocIds.iterator();
            String simpleDocFrom = simpleDocIt.next();
            String simpleDocTo = simpleDocIt.hasNext() ? simpleDocIt.next() : simpleDocFrom;
            translatedSimpleDocs.add(simpleDocFrom);
            translatedSimpleDocs.add(simpleDocTo);
            MapUtil.putAddSet(translatedWysiwygSimpledoc, translatedLog + wysiwygMatcher.group(1),
                (simpleDocFrom + " -> " + simpleDocTo));
          }

          if (isMerged) {
            Matcher wysiwygMatcher = REGEXP_WYSIWYG_NAME.matcher(line);
            Map<String, Set<String>> mergedWysiwygSimpledoc =
                componentMergedWysiwygSimpledocs.get(componentId);
            if (mergedWysiwygSimpledoc == null) {
              mergedWysiwygSimpledoc = new LinkedHashMap<String, Set<String>>();
              componentMergedWysiwygSimpledocs.put(componentId, mergedWysiwygSimpledoc);
            }
            wysiwygMatcher.find();
            String mergedLog =
                "path " + pathLanguages.get(0) + ", file " + wysiwygMatcher.group(1) +
                    " to path " + pathLanguages.get(1) + ", file ";
            wysiwygMatcher.find();
            Iterator<String> simpleDocIt = simpleDocIds.iterator();
            MapUtil.putAddSet(mergedWysiwygSimpledoc, mergedLog + wysiwygMatcher.group(1),
                (simpleDocIt.next() + " -> " + simpleDocIt.next()));
          }
        } else if (!line.contains("will be deleted from the JCR") &&
            !line.contains("with the right language suffix") &&
            !line.contains("will be renamed with right language suffix")) {
          System.out.println(wysiwygName + " - " + line);
        }
      } else {
        throw new IllegalArgumentException("Wysiwyg not found on line : " + line);
      }
    }

    return result;
  }

  private boolean performBackup(String line) {
    boolean result = false;
    if (line.contains("Physical backup has been performed for directory")) {
      Matcher matcher = REGEXP_BACKUP.matcher(line);
      while (matcher.find()) {
        String componentId = verifyComponentId(matcher.group(1), line);
        components.add(componentId);

        String simpleDocId = verifySimpleDocId(matcher.group(2), line);
        MapUtil.putAddSet(componentSimpledocs, componentId, simpleDocId);

        result = true;
      }
    }
    return result;
  }

  private String verifyComponentId(String componentId, String line) {
    if (!componentId.matches("(?i)^[a-z]+[0-9]+$")) {
      throw new IllegalArgumentException(componentId + " is not a component id (" + line + ")");
    }
    return componentId;
  }


  private String verifySimpleDocId(String simpleDocId, String line) {
    if (!simpleDocId.matches("(?i)^simpledoc_[0-9]+$")) {
      throw new IllegalArgumentException(simpleDocId + " is not a simpledoc id (" + line + ")");
    }
    return simpleDocId;
  }

  private String verifyWysiwygBasename(String wysiwygBaseName, String line) {
    if (!wysiwygBaseName.matches("(?i)^.+wysiwyg$")) {
      throw new IllegalArgumentException(
          wysiwygBaseName + " is not a wysiwyg basename (" + line + ")");
    }
    return wysiwygBaseName;
  }

  public void writeStatistics(FileOutputStream fileOutputStream) throws IOException {
    IOUtils.write("###################################", fileOutputStream);
    IOUtils.write("\nStatistics", fileOutputStream);
    IOUtils.write("\n-----------------------------------", fileOutputStream);
    IOUtils.write("\nNb components : " + components.size(), fileOutputStream);
    IOUtils.write("\nPer component :", fileOutputStream);
    for (String componentId : components) {
      IOUtils.write("\n\t" + componentId, fileOutputStream);

      Set<String> simpleDocIds = componentSimpledocs.get(componentId);
      if (simpleDocIds != null) {
        IOUtils.write("\n\t\tnb simpledocs -> " + simpleDocIds.size(), fileOutputStream);
      }

      Set<String> wysiwygBasenames = componentWysiwygBasenames.get(componentId);
      if (wysiwygBasenames != null) {
        IOUtils.write("\n\t\tnb wysiwyg basenames -> " + wysiwygBasenames.size(), fileOutputStream);
      }
    }

    IOUtils.write("\n-----------------------------------", fileOutputStream);

    IOUtils.write("\nNb simpledoc deletions : " + deletedSimpleDocs.size(), fileOutputStream);

    IOUtils.write("\n-----------------------------------", fileOutputStream);

    Set<String> uniqueWysiwygBasenames = new LinkedHashSet<String>();
    Set<String> uniqueComponentIds = new LinkedHashSet<String>();
    int nbWysiwygBasenamePotentialLosses = 0;
    for (Map.Entry<String, Set<String>> entry : wysiwygBasenamesComponents.entrySet()) {
      if (entry.getValue().size() > 1) {
        nbWysiwygBasenamePotentialLosses++;
        IOUtils.write("\n" + entry.getKey() + " basename occures in several component ids (" +
            entry.getValue().size() + "): ", fileOutputStream);
        uniqueWysiwygBasenames.add(entry.getKey());
        for (String componentId : entry.getValue()) {
          uniqueComponentIds.add(componentId);
        }
        IOUtils.write(concatValues(entry.getValue()), fileOutputStream);
      }
    }
    IOUtils.write("\nNb wysiwyg basenames ids that occures in several component ids : " +
        nbWysiwygBasenamePotentialLosses, fileOutputStream);
    IOUtils.write("\n--> " + concatValues(uniqueWysiwygBasenames), fileOutputStream);
    IOUtils.write("\nNb component ids containing same wysiwyg basenames with others : " +
        uniqueComponentIds.size(), fileOutputStream);
    for (String componentId : uniqueComponentIds) {
      uniqueComponentIds.add(componentId);
    }
    IOUtils.write("\n--> " + concatValues(uniqueComponentIds), fileOutputStream);

    IOUtils.write("\n-----------------------------------", fileOutputStream);

    nbWysiwygBasenamePotentialLosses = 0;
    for (Map.Entry<String, Map<String, Set<String>>> entry : componentBadWysiwygNamesSimpleDoc
        .entrySet()) {
      IOUtils.write("\n" + entry.getKey(), fileOutputStream);
      for (Map.Entry<String, Set<String>> potentialBadEntry : entry.getValue().entrySet()) {
        nbWysiwygBasenamePotentialLosses++;
        IOUtils.write("\n\t" + potentialBadEntry.getKey(), fileOutputStream);
        Set<String> currentDeletedSimpledocs = new LinkedHashSet<String>();
        Set<String> currentSimpledocs = new LinkedHashSet<String>();
        for (String simpleDoc : potentialBadEntry.getValue()) {
          if (deletedSimpleDocs.contains(simpleDoc)) {
            currentDeletedSimpledocs.add(simpleDoc);
            currentSimpledocs.add(simpleDoc + "(d)");
          } else {
            currentSimpledocs.add(simpleDoc);
          }
        }
        if (currentDeletedSimpledocs.size() == potentialBadEntry.getValue().size()) {
          IOUtils.write("\n\t~d\t" + concatValues(currentSimpledocs), fileOutputStream);
        } else {
          IOUtils.write("\n\t\t" + concatValues(currentSimpledocs), fileOutputStream);
        }
      }
    }
    IOUtils.write(
        "\nNb component ids with potential losses: " + componentBadWysiwygNamesSimpleDoc.size(),
        fileOutputStream);
    IOUtils
        .write("\nNb wysiwyg basenames with potential losses: " + nbWysiwygBasenamePotentialLosses,
            fileOutputStream);

    IOUtils.write("\n-----------------------------------", fileOutputStream);

    writeStatisticsActions(fileOutputStream, componentMergedWysiwygSimpledocs, "merged");

    IOUtils.write("\n-----------------------------------", fileOutputStream);

    writeStatisticsActions(fileOutputStream, componentCopiedWysiwygSimpledocs, "copied");

    IOUtils.write("\n-----------------------------------", fileOutputStream);

    writeStatisticsActions(fileOutputStream, componentTranslatedWysiwygSimpledocs, "translated");

    IOUtils.write("\n-----------------------------------", fileOutputStream);

    writeStatisticsActions(fileOutputStream, componentRenamedWysiwygSimpledocs, "renamed");

    IOUtils.write("\n###################################\n\n", fileOutputStream);
  }

  private void writeStatisticsActions(FileOutputStream fileOutputStream,
      Map<String, Map<String, Set<String>>> componentWysiwygSimpledocs, String action)
      throws IOException {
    int nbWysiwyg = 0;
    for (Map.Entry<String, Map<String, Set<String>>> entry : componentWysiwygSimpledocs
        .entrySet()) {
      IOUtils.write("\n" + entry.getKey(), fileOutputStream);
      nbWysiwyg += entry.getValue().size();
      for (Map.Entry<String, Set<String>> wysiwygSimpleDocsEntry : entry.getValue().entrySet()) {
        IOUtils.write("\n\t" + wysiwygSimpleDocsEntry.getKey(), fileOutputStream);
        Set<String> currentSimpledocs = new LinkedHashSet<String>();
        for (String simpleDocs : wysiwygSimpleDocsEntry.getValue()) {
          Matcher simpleDocMatcher = REGEXP_SIMPLEDOC_ID.matcher(simpleDocs);
          while (simpleDocMatcher.find()) {
            String simpleDoc = simpleDocMatcher.group(1);
            if (deletedSimpleDocs.contains(simpleDoc)) {
              currentSimpledocs.add(simpleDoc + "(d)");
            } else if (translatedSimpleDocs.contains(simpleDoc)) {
              currentSimpledocs.add(simpleDoc + "(t)");
            } else {
              currentSimpledocs.add(simpleDoc);
            }
          }
        }
        IOUtils.write("\n\t\t" + concatValues(currentSimpledocs, " -> "), fileOutputStream);
      }
    }
    IOUtils.write(
        "\nNb component ids with " + action + " wysiwyg: " + componentWysiwygSimpledocs.size(),
        fileOutputStream);
    IOUtils.write("\nNb wysiwyg names " + action + ": " + nbWysiwyg, fileOutputStream);
  }

  /**
   * Retrieve from a line the component id.
   * @param line
   * @return
   */
  public String getComponentIdFromLine(String line) {
    Matcher componentMatcher = REGEXP_COMPONENT_ID.matcher(line);
    Matcher simpleDocIdMatcher = REGEXP_SIMPLEDOC_ID.matcher(line);
    Matcher wysiwygBasenameMatcher = REGEXP_WYSIWYG_BASENAME.matcher(line);
    for (String componentId : components) {
      while (componentMatcher.find()) {
        String extractedComponentId = componentMatcher.group(1);
        if (extractedComponentId.equals(componentId)) {
          return componentId;
        }
      }

      Set<String> simpleDocIds = componentSimpledocs.get(componentId);
      if (simpleDocIds != null) {
        while (simpleDocIdMatcher.find()) {
          String extractedSimpleDocId = simpleDocIdMatcher.group(1);
          for (String simpleDocId : simpleDocIds) {
            if (extractedSimpleDocId.equals(simpleDocId)) {
              return componentId;
            }
          }
        }
      }

//      Set<String> wysiwygBasenames = componentWysiwygBasenames.get(componentId);
//      if (wysiwygBasenames != null) {
//        while (wysiwygBasenameMatcher.find()) {
//          String extractedWysiwygBasename = wysiwygBasenameMatcher.group(1);
//          for (String wysiwygBasename : wysiwygBasenames) {
//            if (extractedWysiwygBasename.equals(wysiwygBasename)) {
//              return componentId;
//            }
//          }
//        }
//      }

      componentMatcher.reset();
      simpleDocIdMatcher.reset();
      wysiwygBasenameMatcher.reset();
    }
    return null;
  }

  public void clearComponentId(String componentId) {
    components.remove(componentId);
    componentSimpledocs.remove(componentId);
    componentWysiwygBasenames.remove(componentId);
  }
}
