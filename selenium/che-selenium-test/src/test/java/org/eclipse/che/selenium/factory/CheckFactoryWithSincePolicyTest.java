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
package org.eclipse.che.selenium.factory;

import static org.eclipse.che.dto.server.DtoFactory.newDto;
import static org.eclipse.che.selenium.core.constant.TestTimeoutsConstants.PREPARING_WS_TIMEOUT_SEC;
import static org.testng.Assert.assertTrue;

import com.google.inject.Inject;
import org.eclipse.che.api.factory.shared.dto.PoliciesDto;
import org.eclipse.che.commons.lang.NameGenerator;
import org.eclipse.che.selenium.core.SeleniumWebDriver;
import org.eclipse.che.selenium.core.factory.FactoryTemplate;
import org.eclipse.che.selenium.core.factory.TestFactory;
import org.eclipse.che.selenium.core.factory.TestFactoryInitializer;
import org.eclipse.che.selenium.core.utils.WaitUtils;
import org.eclipse.che.selenium.pageobject.Ide;
import org.eclipse.che.selenium.pageobject.PopupDialogsBrowser;
import org.eclipse.che.selenium.pageobject.ProjectExplorer;
import org.eclipse.che.selenium.pageobject.dashboard.Dashboard;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** @author Mihail Kuznyetsov */
public class CheckFactoryWithSincePolicyTest {
  private static final String FACTORY_NAME = NameGenerator.generate("sincePolicy", 3);
  private static final String ALERT_EXPIRE_MESSAGE =
      "Error: This Factory is not yet valid due to time restrictions applied"
          + " by its owner. Please, contact owner for more information.";
  private static final long INIT_TIME = System.currentTimeMillis();
  private static final int ADDITIONAL_TIME = 60000;

  @Inject private ProjectExplorer projectExplorer;
  @Inject private Ide ide;
  @Inject private TestFactoryInitializer testFactoryInitializer;
  @Inject private PopupDialogsBrowser popupDialogsBrowser;
  @Inject private Dashboard dashboard;
  @Inject private SeleniumWebDriver seleniumWebDriver;

  private TestFactory testFactory;

  @BeforeClass
  public void setUp() throws Exception {
    TestFactoryInitializer.TestFactoryBuilder factoryBuilder =
        testFactoryInitializer.fromTemplate(FactoryTemplate.MINIMAL);
    long initTime = System.currentTimeMillis();
    factoryBuilder.setPolicies(newDto(PoliciesDto.class).withSince(initTime + ADDITIONAL_TIME));
    factoryBuilder.setName(FACTORY_NAME);
    testFactory = factoryBuilder.build();
  }

  @AfterClass
  public void tearDown() throws Exception {
    testFactory.delete();
  }

  @Test
  public void checkFactoryAcceptingWithSincePolicy() throws Exception {
    // check factory now, make sure its restricted
    dashboard.open();
    testFactory.open(ide.driver());

    // driver.get(factoryUrl);
    new WebDriverWait(ide.driver(), PREPARING_WS_TIMEOUT_SEC)
        .until(ExpectedConditions.alertIsPresent());
    assertTrue(
        ide.driver().switchTo().alert().getText().contains(ALERT_EXPIRE_MESSAGE),
        "actual message: " + ide.driver().switchTo().alert().getText());
    popupDialogsBrowser.acceptAlert();

    // wait until factory becomes avaialble
    while (System.currentTimeMillis() <= INIT_TIME + ADDITIONAL_TIME) {
      WaitUtils.sleepQuietly(1);
    }

    // check again
    testFactory.open(ide.driver());
    seleniumWebDriver.switchFromDashboardIframeToIde();
    projectExplorer.waitProjectExplorer();
  }
}
