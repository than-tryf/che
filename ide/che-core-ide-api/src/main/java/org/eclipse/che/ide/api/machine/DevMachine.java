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
package org.eclipse.che.ide.api.machine;

import javax.validation.constraints.NotNull;
import org.eclipse.che.api.core.model.machine.Machine;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.machine.shared.Constants;
import org.eclipse.che.ide.util.loging.Log;

/**
 * Describe development machine instance. Must contains all information that need to communicate
 * with dev machine such as links, type, environment variable and etc.
 *
 * @author Vitalii Parfonov
 */
public class DevMachine extends MachineEntityImpl {

  public DevMachine(@NotNull Machine devMachineDescriptor) {
    super(devMachineDescriptor);
  }

  public String getWsAgentWebSocketUrl() {
    for (Link link : machineLinks) {
      if (Constants.WSAGENT_WEBSOCKET_REFERENCE.equals(link.getRel())) {
        return link.getHref();
      }
    }
    // should not be
    final String message =
        "Reference "
            + Constants.WSAGENT_WEBSOCKET_REFERENCE
            + " not found in DevMachine description";
    Log.error(getClass(), message);
    throw new RuntimeException(message);
  }

  /**
   * @return return base URL to the ws agent REST services. URL will be always without trailing
   *     slash
   */
  public String getWsAgentBaseUrl() {
    MachineServer server = getServer(Constants.WSAGENT_REFERENCE);
    if (server != null) {
      String url = server.getUrl();
      if (url.endsWith("/")) {
        url = url.substring(0, url.length() - 1);
      }
      return url;
    } else {
      // should not be
      String message =
          "Reference " + Constants.WSAGENT_REFERENCE + " not found in DevMachine description";
      Log.error(getClass(), message);
      throw new RuntimeException(message);
    }
  }

  /** Returns address (protocol://host:port) of the Workspace Agent. */
  public String getAddress() {
    final MachineServer server = getServer(Constants.WSAGENT_REFERENCE);
    return server.getProtocol() + "://" + server.getAddress();
  }
}
