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

import java.util.ArrayList;
import java.util.List;

/**
 * User: Yohann Chastagnier
 * Date: 26/02/14
 */
public class ComponentLogs {

  private final String componentId;
  private final List<String> lines = new ArrayList<String>();

  /**
   * Default constructor.
   * @param componentId the identifier of the component.
   */
  public ComponentLogs(String componentId) {
    this.componentId = componentId;
  }

  /**
   * Gets the component identifier.
   * @return
   */
  public String getComponentId() {
    return componentId;
  }

  /**
   * Add a line linked with the component handled by this class instance.
   * @param line
   */
  public void addLine(String line) {
    lines.add(line);
  }

  /**
   * Gets the log lines associated to the component.
   * @return
   */
  public List<String> getLines() {
    return lines;
  }
}
