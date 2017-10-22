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
package org.eclipse.che.api.workspace.server;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.intThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceImpl;
import org.eclipse.che.api.workspace.server.spi.WorkspaceDao;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/** @author Max Shaposhnik (mshaposhnik@codenvy.com) */
@Listeners(MockitoTestNGListener.class)
public class TemporaryWorkspaceRemoverTest {

  static final int COUNT_OF_WORKSPACES = 250;

  @Mock private WorkspaceDao workspaceDao;

  @InjectMocks private TemporaryWorkspaceRemover remover;

  @Test
  public void shouldRemoveTemporaryWorkspaces() throws Exception {
    doNothing().when(workspaceDao).remove(anyString());
    // As we want to check pagination, we return items 100 when skip count is 0 or 100,
    // return 50 items when skip count is 200, and return empty list when skip count is 300.
    doReturn(createEntities(100))
        .when(workspaceDao)
        .getWorkspaces(eq(true), intThat(integer -> integer.intValue() < 200), anyInt());
    doReturn(createEntities(50))
        .when(workspaceDao)
        .getWorkspaces(eq(true), intThat(argument -> ((int) argument) == 200), anyInt());
    doReturn(Collections.emptyList())
        .when(workspaceDao)
        .getWorkspaces(
            eq(true), intThat(argument -> ((int) argument) > COUNT_OF_WORKSPACES), anyInt());

    remover.removeTemporaryWs();

    verify(workspaceDao, times(COUNT_OF_WORKSPACES)).remove(anyString());
  }

  private List<WorkspaceImpl> createEntities(int number) {
    List<WorkspaceImpl> wsList = new ArrayList<>();
    for (int i = 0; i < number; i++) {
      wsList.add(new WorkspaceImpl("id" + i, null, null));
    }
    return wsList;
  }
}
