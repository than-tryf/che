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
package org.eclipse.che.ide.navigation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.web.bindery.event.shared.EventBus;
import org.eclipse.che.api.core.jsonrpc.commons.RequestTransmitter;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.machine.DevMachine;
import org.eclipse.che.ide.api.machine.events.WsAgentStateEvent;
import org.eclipse.che.ide.api.resources.Container;
import org.eclipse.che.ide.api.resources.File;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.resource.Path;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.websocket.MessageBus;
import org.eclipse.che.ide.websocket.MessageBusProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Test for {@link NavigateToFilePresenter}.
 *
 * @author Ann Shumilova
 * @author Artem Zatsarynnyi
 * @author Vlad Zhukovskyi
 */
@RunWith(MockitoJUnitRunner.class)
public class NavigateToFilePresenterTest {

  @Mock private NavigateToFileView view;
  @Mock private EventBus eventBus;
  @Mock private MessageBusProvider messageBusProvider;
  @Mock private Container container;
  @Mock private MessageBus messageBus;
  @Mock private DtoUnmarshallerFactory dtoUnmarshallerFactory;
  @Mock private WsAgentStateEvent wsAgentStateEvent;
  @Mock private Promise<Optional<File>> optFilePromise;
  @Mock private AppContext appContext;
  @Mock private EditorAgent editorAgent;
  @Mock private DtoFactory dtoFactory;
  @Mock private RequestTransmitter requestTransmitter;

  private NavigateToFilePresenter presenter;

  @Before
  public void setUp() {
    DevMachine devMachine = mock(DevMachine.class);
    when(devMachine.getId()).thenReturn("id");
    when(appContext.getDevMachine()).thenReturn(devMachine);
    when(appContext.getWorkspaceRoot()).thenReturn(container);
    when(container.getFile(any(Path.class))).thenReturn(optFilePromise);
    when(messageBusProvider.getMachineMessageBus()).thenReturn(messageBus);

    presenter =
        new NavigateToFilePresenter(view, appContext, editorAgent, requestTransmitter, dtoFactory);
  }

  @Test
  public void testShowDialog() throws Exception {
    presenter.showDialog();

    verify(view).showPopup();
  }

  @Test
  public void testOnFileSelected() throws Exception {
    presenter.onFileSelected(Path.ROOT);

    verify(view).hidePopup();
    verify(container).getFile(eq(Path.ROOT));
  }
}
