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
package org.eclipse.che.selenium.debugger;

import static org.eclipse.che.selenium.core.constant.TestTimeoutsConstants.REDRAW_UI_ELEMENTS_TIMEOUT_SEC;

import com.google.inject.Inject;
import java.net.URL;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import org.eclipse.che.commons.json.JsonParseException;
import org.eclipse.che.commons.lang.NameGenerator;
import org.eclipse.che.selenium.core.client.TestProjectServiceClient;
import org.eclipse.che.selenium.core.constant.TestMenuCommandsConstants;
import org.eclipse.che.selenium.core.project.ProjectTemplates;
import org.eclipse.che.selenium.core.workspace.InjectTestWorkspace;
import org.eclipse.che.selenium.core.workspace.TestWorkspace;
import org.eclipse.che.selenium.core.workspace.WorkspaceTemplate;
import org.eclipse.che.selenium.pageobject.CodenvyEditor;
import org.eclipse.che.selenium.pageobject.Ide;
import org.eclipse.che.selenium.pageobject.Menu;
import org.eclipse.che.selenium.pageobject.NotificationsPopupPanel;
import org.eclipse.che.selenium.pageobject.debug.DebugPanel;
import org.eclipse.che.selenium.pageobject.debug.NodeJsDebugConfig;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** Created by mmusienko on 12.02.17. */
public class NodeJsDebugTest {

  private static final String PROJECT_NAME =
      NameGenerator.generate(NodeJsDebugTest.class.getSimpleName(), 4);
  private static final String APP_FILE = "app.js";

  @InjectTestWorkspace(template = WorkspaceTemplate.ECLIPSE_NODEJS)
  private TestWorkspace ws;

  @Inject private Ide ide;

  @Inject private DebugPanel debugPanel;
  @Inject private NodeJsDebugConfig debugConfig;
  @Inject private Menu menuPageObj;
  @Inject private CodenvyEditor editorPageObj;
  @Inject private NotificationsPopupPanel notifications;
  @Inject private Menu menu;
  @Inject private TestProjectServiceClient testProjectServiceClient;

  @BeforeClass
  public void prepare() throws Exception {
    URL resource = getClass().getResource("/projects/node-js-simple");
    testProjectServiceClient.importProject(
        ws.getId(), Paths.get(resource.toURI()), PROJECT_NAME, ProjectTemplates.NODE_JS);
    ide.open(ws);
  }

  @Test(priority = 0)
  public void debugNodeJsTest()
      throws ExecutionException, JsonParseException, InterruptedException {
    String nameOfDebugCommand = "check_node_js_debug";
    menu.runCommand(
        TestMenuCommandsConstants.Run.RUN_MENU,
        TestMenuCommandsConstants.Run.EDIT_DEBUG_CONFIGURATION);
    debugConfig.createConfig(nameOfDebugCommand);
    menuPageObj.runCommand(
        TestMenuCommandsConstants.Run.RUN_MENU,
        TestMenuCommandsConstants.Run.DEBUG,
        TestMenuCommandsConstants.Run.DEBUG + "/" + nameOfDebugCommand);
    notifications.waitExpectedMessageOnProgressPanelAndClosed("Remote debugger connected");
    editorPageObj.waitTabFileWithSavedStatus(APP_FILE);
    editorPageObj.waitActiveEditor();
    debugPanel.waitDebugHighlightedText("var greetings = require(\"./greetings.js\");");
    checkDebugStepsFeatures();
    checkEvaluationFeatures();

    // disconnect session, check highlighter is disappear
    debugPanel.clickOnButton(DebugPanel.DebuggerActionButtons.RESUME_BTN_ID);
    new WebDriverWait(ide.driver(), REDRAW_UI_ELEMENTS_TIMEOUT_SEC)
        .until(
            ExpectedConditions.invisibilityOfElementLocated(
                By.xpath("//div[text()='{/app.js:13} ']")));
    debugPanel.waitBreakPointsPanelIsEmpty();
  }

  @Test(priority = 1)
  public void checkCleanStoragedAfterEndDebugSession() {
    String checkedData =
        "\"connectionProperties\":{\"SCRIPT\":\"/projects/" + PROJECT_NAME + "/app/app.js\"}";
    Assert.assertFalse(getDataAboutDebugSessionFromStorage().contains(checkedData));
  }

  /** @return 'Che-debug-configurations' values from browser storage */
  private String getDataAboutDebugSessionFromStorage() {
    JavascriptExecutor js = (JavascriptExecutor) ide.driver();
    String injectedJsScript = "return window.localStorage.getItem('che-debug-configurations');";
    return js.executeScript(injectedJsScript).toString();
  }

  /** Check step into, step over and step out feature */
  private void checkDebugStepsFeatures() {
    debugPanel.clickOnButton(DebugPanel.DebuggerActionButtons.STEP_OVER);
    debugPanel.waitDebugHighlightedText("var b = greetings.sayHelloInEnglish();");
    debugPanel.clickOnButton(DebugPanel.DebuggerActionButtons.STEP_INTO);
    editorPageObj.waitActiveEditor();
    editorPageObj.waitTabIsPresent("greetings.js");
    debugPanel.waitDebugHighlightedText("return \"HELLO\";");
    debugPanel.clickOnButton(DebugPanel.DebuggerActionButtons.STEP_OUT);
    debugPanel.waitDebugHighlightedText("var c=\"some add value\" + b;");
    new WebDriverWait(ide.driver(), REDRAW_UI_ELEMENTS_TIMEOUT_SEC)
        .until(
            ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//div[text()='{app.js:13} ']")));
    debugPanel.clickOnButton(DebugPanel.DebuggerActionButtons.STEP_OVER);
  }

  private void checkEvaluationFeatures() {
    debugPanel.clickOnButton(DebugPanel.DebuggerActionButtons.EVALUATE_EXPRESSIONS);
    debugPanel.typeEvaluateExpression("c.length");
    debugPanel.clickEvaluateBtn();
    debugPanel.waitExpectedResultInEvaluateExpression("19");
    debugPanel.clickCloseEvaluateBtn();
  }
}
