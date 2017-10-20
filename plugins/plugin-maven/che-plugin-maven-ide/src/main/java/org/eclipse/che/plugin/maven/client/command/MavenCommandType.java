/*
 * Copyright (c) 2012-2017 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.plugin.maven.client.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.LinkedList;
import java.util.List;
import org.eclipse.che.ide.api.command.CommandPage;
import org.eclipse.che.ide.api.command.CommandType;
import org.eclipse.che.ide.api.icon.Icon;
import org.eclipse.che.ide.api.icon.IconRegistry;
import org.eclipse.che.ide.macro.CurrentProjectPathMacro;
import org.eclipse.che.ide.macro.CurrentProjectRelativePathMacro;
import org.eclipse.che.ide.macro.ServerAddressMacroRegistrar;
import org.eclipse.che.plugin.maven.client.MavenResources;

/**
 * Maven command type.
 *
 * @author Artem Zatsarynnyi
 */
@Singleton
public class MavenCommandType implements CommandType {

  private static final String ID = "mvn";
  private static final String COMMAND_TEMPLATE = "mvn clean install";
  private static final String DEF_PORT = "8080";

  private final CurrentProjectPathMacro currentProjectPathMacro;
  private final CurrentProjectRelativePathMacro currentProjectRelativePathMacro;
  private final List<CommandPage> pages;

  @Inject
  public MavenCommandType(
      MavenResources resources,
      MavenCommandPagePresenter page,
      CurrentProjectPathMacro currentProjectPathMacro,
      CurrentProjectRelativePathMacro currentProjectRelativePathMacro,
      IconRegistry iconRegistry) {
    this.currentProjectPathMacro = currentProjectPathMacro;
    this.currentProjectRelativePathMacro = currentProjectRelativePathMacro;

    pages = new LinkedList<>();
    pages.add(page);

    iconRegistry.registerIcon(new Icon("command.type." + ID, resources.maven()));
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getDisplayName() {
    return "Maven";
  }

  @Override
  public String getDescription() {
    return "Command for executing Maven command line";
  }

  @Override
  public List<CommandPage> getPages() {
    return pages;
  }

  @Override
  public String getCommandLineTemplate() {
    return COMMAND_TEMPLATE + " -f " + currentProjectPathMacro.getName();
  }

  @Override
  public String getPreviewUrlTemplate() {
    // TODO: hardcode http after switching WS Master to https
    return "http://"
        + ServerAddressMacroRegistrar.MACRO_NAME_TEMPLATE.replace("%", DEF_PORT)
        + "/"
        + currentProjectRelativePathMacro.getName();
  }
}
