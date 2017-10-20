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
package org.eclipse.che.plugin.docker.machine;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.plugin.docker.client.WorkspacesRoutingSuffixProvider;

/**
 * Represents a server evaluation strategy for the configuration where the strategy can be
 * customized through template properties.
 *
 * @author Florent Benoit
 * @see ServerEvaluationStrategy
 */
public class CustomServerEvaluationStrategy extends BaseServerEvaluationStrategy {
  /** Default constructor */
  @Inject
  public CustomServerEvaluationStrategy(
      @Nullable @Named("che.docker.ip") String cheDockerIp,
      @Nullable @Named("che.docker.ip.external") String cheDockerIpExternal,
      @Nullable @Named("che.docker.server_evaluation_strategy.custom.template")
          String cheDockerCustomExternalTemplate,
      @Nullable @Named("che.docker.server_evaluation_strategy.custom.external.protocol")
          String cheDockerCustomExternalProtocol,
      @Named("che.port") String chePort,
      WorkspacesRoutingSuffixProvider cheWorkspacesRoutingSuffixProvider) {
    super(
        cheDockerIp,
        cheDockerIpExternal,
        cheDockerCustomExternalTemplate,
        cheDockerCustomExternalProtocol,
        chePort,
        false,
        cheWorkspacesRoutingSuffixProvider);
  }
}
