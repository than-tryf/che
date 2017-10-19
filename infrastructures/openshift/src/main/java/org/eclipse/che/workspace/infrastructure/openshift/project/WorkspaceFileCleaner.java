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

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.openshift.client.OpenShiftClient;
import java.util.List;
import javax.inject.Named;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.notification.EventSubscriber;
import org.eclipse.che.api.workspace.shared.event.WorkspaceRemovedEvent;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.workspace.infrastructure.openshift.OpenShiftClientFactory;

/**
 * Listener for removing OpenShift project on {@code WorkspaceRemovedEvent}.
 *
 * @author Sergii Leshchenko
 */
@Singleton
public class WorkspaceFileCleaner {

  private final OpenShiftClientFactory clientFactory;
  private final boolean pvcStrategy;
  private final String projectName;
  private final String pvcName;

  @Inject
  public WorkspaceFileCleaner(
      @Named("che.infra.openshift.pvc.one_per_workspace") boolean pvcStrategy,
      @Nullable @Named("che.infra.openshift.project") String projectName,
      @Named("che.infra.openshift.pvc.name") String pvcName,
      OpenShiftClientFactory clientFactory) {
    this.clientFactory = clientFactory;
    this.pvcStrategy = pvcStrategy;
    this.projectName = projectName;
    this.pvcName = pvcName;
  }

  @Inject
  public void subscribe(EventService eventService) {
    if (projectName == null) {
      eventService.subscribe(new RemoveProjectOnWorkspaceRemove());
    } else {
      if (pvcStrategy) {
        // TODO implement https://github.com/eclipse/che/issues/6767
      } else {
        eventService.subscribe(new RemoveProjectPvcOnWokrspaceRemove());
      }
    }
  }

  class RemoveProjectOnWorkspaceRemove implements EventSubscriber<WorkspaceRemovedEvent> {

    @Override
    public void onEvent(WorkspaceRemovedEvent event) {
      doRemoveProject(event.getWorkspace().getId());
    }

    @VisibleForTesting
    void doRemoveProject(String projectName) {
      try (OpenShiftClient client = clientFactory.create()) {
        client.projects().withName(projectName).delete();
      }
    }
  }

  class RemoveProjectPvcOnWokrspaceRemove implements EventSubscriber<WorkspaceRemovedEvent> {

    @Override
    public void onEvent(WorkspaceRemovedEvent event) {
      doRemoveProject(event.getWorkspace().getId());
    }

    @VisibleForTesting
    void doRemoveProject(String projectName) {
      try (OpenShiftClient client = clientFactory.create()) {
        final String pvcUniqueName = pvcName + '-' + projectName;
        final List<PersistentVolumeClaim> pvcs =
            client.persistentVolumeClaims().inNamespace(projectName).list().getItems();
        for (PersistentVolumeClaim pvc : pvcs) {
          if (pvc.getMetadata().getName().equals(pvcUniqueName)) {
            client.persistentVolumeClaims().inNamespace(projectName).delete(pvc);
            return;
          }
        }
      }
    }
  }
}
