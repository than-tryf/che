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
package org.eclipse.che.ide.actions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;
import org.eclipse.che.api.core.model.machine.Machine;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.command.CommandExecutor;
import org.eclipse.che.ide.api.command.CommandImpl;
import org.eclipse.che.ide.api.command.CommandManager;
import org.eclipse.che.ide.api.machine.DevMachine;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/** @author Max Shaposhnik */
@RunWith(MockitoJUnitRunner.class)
public class RunCommandActionTest {

  private static final String NAME_PROPERTY = "name";

  // constructors mocks
  @Mock CommandManager commandManager;
  @Mock private CommandExecutor commandExecutor;
  @Mock private CoreLocalizationConstant locale;
  @Mock private ActionEvent event;
  @Mock private CommandImpl command;
  @Mock private AppContext appContext;

  @InjectMocks private RunCommandAction action;

  @Before
  public void setUp() throws Exception {
    when(commandManager.getCommand(anyString())).thenReturn(Optional.of(command));
  }

  @Test
  public void commandNameShouldBePresent() {
    when(event.getParameters()).thenReturn(Collections.singletonMap("otherParam", "MCI"));
    action.actionPerformed(event);

    verify(commandExecutor, never()).executeCommand(any(CommandImpl.class), any(Machine.class));
  }

  @Test
  public void actionShouldBePerformed() {
    when(event.getParameters()).thenReturn(Collections.singletonMap(NAME_PROPERTY, "MCI"));
    final DevMachine devMachine = mock(DevMachine.class);
    final Machine machine = mock(Machine.class);
    when(devMachine.getDescriptor()).thenReturn(machine);
    when(appContext.getDevMachine()).thenReturn(devMachine);

    action.actionPerformed(event);

    verify(commandExecutor).executeCommand(eq(command), any(Machine.class));
  }
}
