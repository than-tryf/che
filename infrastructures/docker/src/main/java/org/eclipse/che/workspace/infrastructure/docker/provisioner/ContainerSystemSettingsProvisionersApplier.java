/*******************************************************************************
 * Copyright (c) 2012-2017 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.workspace.infrastructure.docker.provisioner;

import org.eclipse.che.api.core.model.workspace.runtime.RuntimeIdentity;
import org.eclipse.che.api.workspace.server.model.impl.EnvironmentImpl;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.workspace.infrastructure.docker.model.DockerEnvironment;

import javax.inject.Inject;
import java.util.Set;

/**
 * Applies {@link ContainerSystemSettingsProvisioner}s to docker environment.
 *
 * </p>Provisioners order is not respected, so no provisioner is allowed to be dependent on any other provisioner.
 *
 * @author Alexander Garagatyi
 */
public class ContainerSystemSettingsProvisionersApplier implements ConfigurationProvisioner {
    private Set<ContainerSystemSettingsProvisioner> dockerSettingsProvisioners;

    @Inject
    public ContainerSystemSettingsProvisionersApplier(Set<ContainerSystemSettingsProvisioner> dockerSettingsProvisioners) {
        this.dockerSettingsProvisioners = dockerSettingsProvisioners;
    }

    @Override
    public void provision(EnvironmentImpl envConfig, DockerEnvironment internalEnv, RuntimeIdentity identity)
            throws InfrastructureException {
        for (ContainerSystemSettingsProvisioner dockerSettingsProvisioner : dockerSettingsProvisioners) {
            dockerSettingsProvisioner.provision(internalEnv);
        }
    }
}