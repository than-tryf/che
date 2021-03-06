/*
 * Copyright (c) 2015-2017 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
'use strict';

/**
 * @ngdoc directive
 * @name workspace.status.directive:cheWorkspaceStatus
 * @restrict E
 * @element
 *
 * @description
 * <che-workspace-status che-workspace-item="workspace"></che-workspace-status>
 *
 * @usage
 *   <che-workspace-status che-workspace-item="workspace"></che-workspace-status>
 *
 * @author Oleksii Orel
 */
export class CheWorkspaceStatus {
  restrict: string;
  replace: boolean;

  scope: {
    [propName: string]: string
  };

  templateUrl: string;
  controller: string;
  controllerAs: string;
  bindToController: boolean;

  /**
   * Default constructor.
   */
  constructor() {
    this.restrict = 'E';
    this.replace = false;

    // scope values
    this.scope = {
      workspace: '=cheWorkspaceItem'
    };

    this.templateUrl = 'app/workspaces/list-workspaces/workspace-status-action/workspace-status.html';

    this.controller = 'WorkspaceStatusController';
    this.controllerAs = 'workspaceStatusController';
    this.bindToController = true;
  }

}
