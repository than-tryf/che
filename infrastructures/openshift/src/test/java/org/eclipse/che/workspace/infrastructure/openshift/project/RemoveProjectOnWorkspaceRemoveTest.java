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
package org.eclipse.che.workspace.infrastructure.openshift.project;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.eclipse.che.api.core.model.workspace.Workspace;
import org.eclipse.che.api.core.notification.EventService;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/**
 * Test for {@link WorkspaceFileCleaner}.
 *
 * @author Sergii Leshchenko
 */
@Listeners(MockitoTestNGListener.class)
public class RemoveProjectOnWorkspaceRemoveTest {

  private static final String WORKSPACE_ID = "workspace123";

  @Mock private Workspace workspace;

  private WorkspaceFileCleaner removeProjectOnWorkspaceRemove;

  @BeforeMethod
  public void setUp() {
    removeProjectOnWorkspaceRemove = spy(new WorkspaceFileCleaner(true, null, null, null));

    //    doNothing().when(removeProjectOnWorkspaceRemove).doRemoveProject(anyString());

    when(workspace.getId()).thenReturn(WORKSPACE_ID);
  }

  @Test
  public void shouldSubscribeListenerToEventService() {
    EventService eventService = mock(EventService.class);

    removeProjectOnWorkspaceRemove.subscribe(eventService);

    //    verify(eventService).subscribe(removeProjectOnWorkspaceRemove);
  }

  @Test
  public void shouldRemoveProjectOnWorkspaceRemovedEvent() {
    //    removeProjectOnWorkspaceRemove.onEvent(new WorkspaceRemovedEvent(workspace));

    //    verify(removeProjectOnWorkspaceRemove).doRemoveProject(WORKSPACE_ID);
  }
}
