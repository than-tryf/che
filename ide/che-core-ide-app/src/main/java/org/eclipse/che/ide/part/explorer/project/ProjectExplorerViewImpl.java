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
package org.eclipse.che.ide.part.explorer.project;

import static java.util.Collections.singletonList;
import static org.eclipse.che.ide.project.node.SyntheticNode.CUSTOM_BACKGROUND_FILL;
import static org.eclipse.che.ide.ui.menu.PositionController.HorizontalAlign.MIDDLE;
import static org.eclipse.che.ide.ui.menu.PositionController.VerticalAlign.BOTTOM;
import static org.eclipse.che.ide.ui.smartTree.event.GoIntoStateEvent.State.ACTIVATED;
import static org.eclipse.che.ide.ui.smartTree.event.GoIntoStateEvent.State.DEACTIVATED;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.FontAwesome;
import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.actions.RefreshPathAction;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.action.Presentation;
import org.eclipse.che.ide.api.data.tree.HasAction;
import org.eclipse.che.ide.api.data.tree.HasAttributes;
import org.eclipse.che.ide.api.data.tree.Node;
import org.eclipse.che.ide.api.data.tree.NodeInterceptor;
import org.eclipse.che.ide.api.parts.PerspectiveManager;
import org.eclipse.che.ide.api.parts.base.BaseView;
import org.eclipse.che.ide.api.parts.base.ToolButton;
import org.eclipse.che.ide.api.resources.Container;
import org.eclipse.che.ide.api.resources.Project;
import org.eclipse.che.ide.api.resources.Resource;
import org.eclipse.che.ide.menu.ContextMenu;
import org.eclipse.che.ide.project.node.SyntheticNode;
import org.eclipse.che.ide.resources.tree.ContainerNode;
import org.eclipse.che.ide.resources.tree.ResourceNode;
import org.eclipse.che.ide.resources.tree.SkipHiddenNodesInterceptor;
import org.eclipse.che.ide.ui.Tooltip;
import org.eclipse.che.ide.ui.smartTree.NodeDescriptor;
import org.eclipse.che.ide.ui.smartTree.NodeLoader;
import org.eclipse.che.ide.ui.smartTree.NodeStorage;
import org.eclipse.che.ide.ui.smartTree.NodeStorage.StoreSortInfo;
import org.eclipse.che.ide.ui.smartTree.SortDir;
import org.eclipse.che.ide.ui.smartTree.Tree;
import org.eclipse.che.ide.ui.smartTree.TreeStyles;
import org.eclipse.che.ide.ui.smartTree.event.GoIntoStateEvent;
import org.eclipse.che.ide.ui.smartTree.event.GoIntoStateEvent.GoIntoStateHandler;
import org.eclipse.che.ide.ui.smartTree.presentation.DefaultPresentationRenderer;
import org.eclipse.che.ide.ui.status.StatusWidget;
import org.eclipse.che.ide.ui.toolbar.PresentationFactory;

/**
 * Implementation of the {@link ProjectExplorerView}.
 *
 * @author Vlad Zhukovskiy
 */
@Singleton
public class ProjectExplorerViewImpl extends BaseView<ProjectExplorerView.ActionDelegate>
    implements ProjectExplorerView, GoIntoStateHandler {
  private final Tree tree;
  private final SkipHiddenNodesInterceptor skipHiddenNodesInterceptor;

  private ToolButton goBackButton;
  private ToolButton linkWithEditorButton;

  private static final String GO_BACK_BUTTON_ID = "goBackButton";
  private static final String COLLAPSE_ALL_BUTTON_ID = "collapseAllButton";
  private static final String REFRESH_BUTTON_ID = "refreshSelectedPath";
  private static final String LINK_WITH_EDITOR_ID = "linkWithEditor";
  private static final String PROJECT_TREE_WIDGET_ID = "projectTree";

  @Inject
  public ProjectExplorerViewImpl(
      final Set<NodeInterceptor> nodeInterceptorSet,
      Resources resources,
      ContextMenu contextMenu,
      CoreLocalizationConstant coreLocalizationConstant,
      SkipHiddenNodesInterceptor skipHiddenNodesInterceptor,
      RefreshPathAction refreshPathAction,
      PresentationFactory presentationFactory,
      Provider<PerspectiveManager> managerProvider,
      ActionManager actionManager,
      EmptyTreePanel emptyTreePanel) {
    super(resources);
    this.skipHiddenNodesInterceptor = skipHiddenNodesInterceptor;

    setTitle(coreLocalizationConstant.projectExplorerTitleBarText());

    NodeStorage nodeStorage = new NodeStorage();

    NodeLoader nodeLoader = new NodeLoader(nodeInterceptorSet);
    nodeLoader.getNodeInterceptors().add(skipHiddenNodesInterceptor);

    tree = new Tree(nodeStorage, nodeLoader, new StatusWidget<Tree>(emptyTreePanel));
    tree.setContextMenuInvocationHandler(
        new Tree.ContextMenuInvocationHandler() {
          @Override
          public void onInvokeContextMenu(int x, int y) {
            contextMenu.show(x, y);
          }
        });

    tree.getNodeStorage().addSortInfo(new StoreSortInfo(new NodeTypeComparator(), SortDir.ASC));
    tree.getNodeStorage()
        .addSortInfo(
            new StoreSortInfo(
                new Comparator<Node>() {
                  @Override
                  public int compare(Node o1, Node o2) {
                    if (o1 instanceof ResourceNode && o2 instanceof ResourceNode) {
                      return ((ResourceNode) o1).compareTo((ResourceNode) o2);
                    }

                    return 0;
                  }
                },
                SortDir.ASC));

    if (tree.getGoInto() != null) {
      tree.getGoInto().addGoIntoHandler(this);
    }

    tree.setPresentationRenderer(new ProjectExplorerRenderer(tree.getTreeStyles()));
    tree.ensureDebugId(PROJECT_TREE_WIDGET_ID);
    tree.setAutoSelect(true);
    tree.getNodeLoader().setUseCaching(false);

    setContentWidget(tree);

    ToolButton collapseAllButton = new ToolButton(FontAwesome.COMPRESS);
    collapseAllButton.addClickHandler(
        new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            if (tree.getGoInto().isActive()) {
              Node lastNode = tree.getGoInto().getLastUsed();
              tree.setExpanded(lastNode, false, true);
              return;
            }

            tree.collapseAll();
          }
        });
    Tooltip.create(
        (elemental.dom.Element) collapseAllButton.getElement(), BOTTOM, MIDDLE, "Collapse All");
    collapseAllButton.ensureDebugId(COLLAPSE_ALL_BUTTON_ID);
    collapseAllButton.setVisible(true);
    addToolButton(collapseAllButton);

    linkWithEditorButton = new ToolButton(FontAwesome.EXCHANGE);
    linkWithEditorButton.getElement().setAttribute("name", LINK_WITH_EDITOR_ID);
    linkWithEditorButton.addClickHandler(event -> delegate.onLinkWithEditorButtonClicked());
    Tooltip.create(
        (elemental.dom.Element) linkWithEditorButton.getElement(),
        BOTTOM,
        MIDDLE,
        coreLocalizationConstant.projectExplorerLinkWithEditorTooltip());
    linkWithEditorButton.ensureDebugId(LINK_WITH_EDITOR_ID);
    linkWithEditorButton.setVisible(true);
    addToolButton(linkWithEditorButton);

    ToolButton refreshPathButton = new ToolButton(FontAwesome.REFRESH);
    refreshPathButton.addClickHandler(
        new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            final Presentation presentation =
                presentationFactory.getPresentation(refreshPathAction);
            final ActionEvent actionEvent =
                new ActionEvent(presentation, actionManager, managerProvider.get(), null);

            refreshPathAction.update(actionEvent);

            if (presentation.isEnabled() && presentation.isVisible()) {
              refreshPathAction.actionPerformed(actionEvent);
            }
          }
        });

    Tooltip.create(
        (elemental.dom.Element) refreshPathButton.getElement(),
        BOTTOM,
        MIDDLE,
        "Refresh selected path");
    refreshPathButton.ensureDebugId(REFRESH_BUTTON_ID);
    refreshPathButton.setVisible(true);
    addToolButton(refreshPathButton);
  }

  @Override
  protected void focusView() {
    tree.setFocus(true);
  }

  @Override
  protected void blurView() {
    tree.setFocus(false);
  }

  @Override
  public Tree getTree() {
    return tree;
  }

  @Override
  public void activateLinkWithEditorButton(boolean activated) {
    linkWithEditorButton.getElement().setAttribute("activated", String.valueOf(activated));
  }

  /** {@inheritDoc} */
  @Override
  public void reloadChildren(Node parent) {
    reloadChildren(parent, false);
  }

  /** {@inheritDoc} */
  @Override
  public void reloadChildren(Node parent, boolean deep) {
    // iterate on root nodes and call tree widget to reload their children
    for (Node node : parent == null ? tree.getRootNodes() : singletonList(parent)) {
      if (node.isLeaf()) { // just preventive actions
        continue;
      }

      if (tree.isExpanded(node)) {
        tree.getNodeLoader().loadChildren(node, deep);
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public void reloadChildrenByType(Class<?> type) {
    List<Node> rootNodes = tree.getRootNodes();
    for (Node rootNode : rootNodes) {
      List<Node> allChildren = tree.getNodeStorage().getAllChildren(rootNode);
      for (Node child : allChildren) {
        if (child.getClass().equals(type)) {
          NodeDescriptor nodeDescriptor = tree.getNodeDescriptor(child);
          if (nodeDescriptor.isLoaded()) {
            tree.getNodeLoader().loadChildren(child);
          }
        }
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public void select(Node node, boolean keepExisting) {
    tree.getSelectionModel().select(node, keepExisting);
  }

  /** {@inheritDoc} */
  @Override
  public void select(List<Node> nodes, boolean keepExisting) {
    tree.getSelectionModel().select(nodes, keepExisting);
  }

  /** {@inheritDoc} */
  @Override
  public void showHiddenFilesForAllExpandedNodes(boolean show) {
    if (show) {
      tree.getNodeLoader().getNodeInterceptors().remove(skipHiddenNodesInterceptor);
    } else {
      tree.getNodeLoader().getNodeInterceptors().add(skipHiddenNodesInterceptor);
    }

    for (Node node : tree.getRootNodes()) {
      reloadChildren(node, true);
    }
  }

  /** {@inheritDoc} */
  @Override
  public boolean setGoIntoModeOn(Node node) {
    return tree.getGoInto().activate(node);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isGoIntoActivated() {
    return tree.getGoInto().isActive();
  }

  /** {@inheritDoc} */
  @Override
  public void onGoIntoStateChanged(GoIntoStateEvent event) {
    if (event.getState() == ACTIVATED) {
      // lazy button initializing
      if (goBackButton == null) {
        initGoIntoBackButton();
        return;
      }

      goBackButton.setVisible(true);

    } else if (event.getState() == DEACTIVATED) {
      goBackButton.setVisible(false);
    }
  }

  private void initGoIntoBackButton() {
    goBackButton = new ToolButton(FontAwesome.ARROW_CIRCLE_O_LEFT);
    goBackButton.addClickHandler(
        new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            tree.getGoInto().reset();
          }
        });
    goBackButton.ensureDebugId(GO_BACK_BUTTON_ID);
    Tooltip.create((elemental.dom.Element) goBackButton.getElement(), BOTTOM, MIDDLE, "Go Back");
    addToolButton(goBackButton);
  }

  /** {@inheritDoc} */
  @Override
  public void collapseAll() {
    tree.collapseAll();
  }

  private class ProjectExplorerRenderer extends DefaultPresentationRenderer<Node> {

    ProjectExplorerRenderer(TreeStyles treeStyles) {
      super(treeStyles);
    }

    @Override
    public Element render(Node node, String domID, Tree.Joint joint, int depth) {
      Element element = super.render(node, domID, joint, depth);

      element.setAttribute("name", node.getName());

      if (node instanceof ResourceNode) {
        final Resource resource = ((ResourceNode) node).getData();
        element.setAttribute("path", resource.getLocation().toString());

        Project project = resource.getProject();
        if (project != null) {
          element.setAttribute("project", project.getLocation().toString());
        }
      }

      if (node instanceof HasAction) {
        element.setAttribute("actionable", "true");
      }

      if (node instanceof SyntheticNode<?>) {
        element.setAttribute("synthetic", "true");
        element.setAttribute("project", ((SyntheticNode) node).getProject().toString());
      }

      if (node instanceof HasAttributes
          && ((HasAttributes) node).getAttributes().containsKey(CUSTOM_BACKGROUND_FILL)) {
        element
            .getFirstChildElement()
            .getStyle()
            .setBackgroundColor(
                ((HasAttributes) node).getAttributes().get(CUSTOM_BACKGROUND_FILL).get(0));
      }

      if (node instanceof ContainerNode) {
        Container container = ((ContainerNode) node).getData();
        if (container instanceof Project) {
          String head = container.getProject().getAttribute("git.current.head.name");
          if (head != null) {
            Element nodeContainer = element.getFirstChildElement();
            DivElement divElement = Document.get().createDivElement();
            divElement.setInnerText("(" + head + ")");
            divElement.setClassName(treeStyles.styles().vcsHeadContainer());
            nodeContainer.insertBefore(divElement, nodeContainer.getLastChild());
          }
        }
      }
      return element;
    }
  }
}
