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
package org.eclipse.che.plugin.debugger.ide.debug.tree.node;

import com.google.inject.assistedinject.Assisted;
import org.eclipse.che.api.debug.shared.model.Expression;
import org.eclipse.che.api.debug.shared.model.Variable;

public interface DebuggerNodeFactory {

  VariableNode createVariableNode(@Assisted Variable variable);

  WatchExpressionNode createExpressionNode(@Assisted Expression expression);
}
