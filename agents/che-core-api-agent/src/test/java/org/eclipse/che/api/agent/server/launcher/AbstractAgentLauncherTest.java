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
package org.eclipse.che.api.agent.server.launcher;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.ArrayList;
import org.eclipse.che.api.agent.server.exception.AgentStartException;
import org.eclipse.che.api.agent.shared.model.Agent;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.util.LineConsumer;
import org.eclipse.che.api.machine.server.exception.MachineException;
import org.eclipse.che.api.machine.server.model.impl.CommandImpl;
import org.eclipse.che.api.machine.server.spi.Instance;
import org.eclipse.che.api.machine.server.spi.InstanceNode;
import org.eclipse.che.api.machine.server.spi.InstanceProcess;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/** @author Alexander Garagatyi */
@Listeners(MockitoTestNGListener.class)
public class AbstractAgentLauncherTest {
  @Mock private Instance machine;
  @Mock private Agent agent;
  @Mock private InstanceProcess process;
  @Mock private AgentLaunchingChecker agentChecker;

  private AbstractAgentLauncher launcher;

  @BeforeMethod
  public void setUp() throws Exception {
    launcher = spy(new TestAgentLauncher(500, 100, agentChecker));

    when(agent.getScript()).thenReturn("script content");
    doReturn(process)
        .when(launcher)
        .start(nullable(Instance.class), nullable(Agent.class), nullable(LineConsumer.class));
    when(agentChecker.isLaunched(
            nullable(Agent.class), nullable(InstanceProcess.class), nullable(Instance.class)))
        .thenReturn(true);
    when(machine.getNode()).thenReturn(mock(InstanceNode.class));
  }

  @Test
  public void shouldBeAbleToCheckAgentState() throws Exception {
    // when
    launcher.launch(machine, agent);

    // then
    verify(agentChecker)
        .isLaunched(any(Agent.class), any(InstanceProcess.class), any(Instance.class));
  }

  @Test
  public void doNothingIfAgentScriptIsNull() throws Exception {
    // given
    when(agent.getScript()).thenReturn(null);

    // when
    launcher.launch(machine, agent);

    // then
    verify(launcher, never()).start(any(Instance.class), any(Agent.class), any(LineConsumer.class));
    verify(agent).getScript();
    verifyNoMoreInteractions(agent);
    verifyZeroInteractions(machine);
  }

  @Test
  public void doNothingIfAgentScriptIsEmpty() throws Exception {
    // given
    when(agent.getScript()).thenReturn("");

    // when
    launcher.launch(machine, agent);

    // then
    verify(launcher, never()).start(any(Instance.class), any(Agent.class), any(LineConsumer.class));
    verify(agent).getScript();
    verifyNoMoreInteractions(agent);
    verifyZeroInteractions(machine);
  }

  @Test
  public void shouldCheckIfAgentIsLaunchedUntilItIsLaunched() throws Exception {
    // given
    when(agentChecker.isLaunched(any(Agent.class), any(InstanceProcess.class), any(Instance.class)))
        .thenReturn(false)
        .thenReturn(false)
        .thenReturn(false)
        .thenReturn(false)
        .thenReturn(true);

    // when
    launcher.launch(machine, agent);

    // then
    verify(agentChecker, times(5))
        .isLaunched(any(Agent.class), any(InstanceProcess.class), any(Instance.class));
  }

  @Test(
    expectedExceptions = AgentStartException.class,
    expectedExceptionsMessageRegExp = "Fail launching agent .*. Workspace ID:.*"
  )
  public void shouldNotCheckIfAgentIsLaunchedMoreThanAgentMaxStartTime() throws Exception {
    // given
    launcher = spy(new TestAgentLauncher(200, 100, agentChecker));
    doReturn(process)
        .when(launcher)
        .start(nullable(Instance.class), nullable(Agent.class), nullable(LineConsumer.class));
    when(agentChecker.isLaunched(any(Agent.class), any(InstanceProcess.class), any(Instance.class)))
        .thenReturn(false)
        .thenReturn(false)
        .thenReturn(false)
        .thenReturn(false)
        .thenReturn(true);

    // when
    launcher.launch(machine, agent);

    // then
    // ensure that isLaunched was called several times and then max pinging time was exceeded
    verify(agentChecker, atLeast(2))
        .isLaunched(any(Agent.class), any(InstanceProcess.class), any(Instance.class));
  }

  @Test
  public void shouldNotCheckMoreFrequentThanAgentCheckDelay() throws Exception {
    // given
    launcher = spy(new TestAgentLauncher(200, 10, agentChecker));
    doReturn(process)
        .when(launcher)
        .start(nullable(Instance.class), nullable(Agent.class), nullable(LineConsumer.class));
    // record time of each check of agent state
    ArrayList<Long> checkTimestamps = new ArrayList<>(5);
    Answer<Boolean> recordTimestampAndReturnFalse =
        invocationOnMock -> {
          checkTimestamps.add(System.currentTimeMillis());
          return false;
        };
    Answer<Boolean> recordTimestampAndReturnTrue =
        invocationOnMock -> {
          checkTimestamps.add(System.currentTimeMillis());
          return true;
        };
    when(agentChecker.isLaunched(any(Agent.class), any(InstanceProcess.class), any(Instance.class)))
        .thenAnswer(recordTimestampAndReturnFalse)
        .thenAnswer(recordTimestampAndReturnFalse)
        .thenAnswer(recordTimestampAndReturnFalse)
        .thenAnswer(recordTimestampAndReturnFalse)
        .thenAnswer(recordTimestampAndReturnTrue);

    // when
    launcher.launch(machine, agent);

    // then
    // ensure that each check was done after required timeout
    for (int i = 1; i < checkTimestamps.size(); i++) {
      assertTrue(checkTimestamps.get(i) - checkTimestamps.get(i - 1) >= 10);
    }
  }

  @Test(
    expectedExceptions = ServerException.class,
    expectedExceptionsMessageRegExp = "agent launcher test exception"
  )
  public void shouldThrowServerExceptionIfMachineExceptionIsThrownByAgentCheck() throws Exception {
    // given
    when(agentChecker.isLaunched(any(Agent.class), any(InstanceProcess.class), any(Instance.class)))
        .thenThrow(new MachineException("agent launcher test exception"));

    // when
    launcher.launch(machine, agent);
  }

  @Test
  public void shouldSetBackInterruptedFlagIfThreadWasInterrupted() throws Exception {
    try {
      // imitate interruption of launching thread
      when(agentChecker.isLaunched(
              any(Agent.class), any(InstanceProcess.class), any(Instance.class)))
          .thenAnswer(
              invocationOnMock -> {
                Thread.currentThread().interrupt();
                return false;
              });

      // when
      launcher.launch(machine, agent);
    } catch (ServerException e) {
      // Ensure that after exiting launcher thread is still in interrupted state
      assertTrue(Thread.currentThread().isInterrupted());
    } finally {
      // cleanup interrupted state
      Thread.interrupted();
    }
  }

  @Test(
    expectedExceptions = ServerException.class,
    expectedExceptionsMessageRegExp = "Launching agent .* is interrupted"
  )
  public void shouldThrowServerExceptionIfAgentCheckWasInterrupted() throws Exception {
    try {
      when(agentChecker.isLaunched(
              any(Agent.class), any(InstanceProcess.class), any(Instance.class)))
          .thenAnswer(
              invocationOnMock -> {
                Thread.currentThread().interrupt();
                return false;
              });

      // when
      launcher.launch(machine, agent);
    } finally {
      // cleanup interrupted state
      Thread.interrupted();
    }
  }

  @Test
  public void shouldStartMachineProcessWithAgentScriptExecution() throws Exception {
    // given
    String agentId = "testAgentId";
    String agentScript = "testAgentScript";
    when(agent.getId()).thenReturn(agentId);
    when(agent.getScript()).thenReturn(agentScript);
    doCallRealMethod()
        .when(launcher)
        .start(nullable(Instance.class), nullable(Agent.class), nullable(LineConsumer.class));

    // when
    launcher.launch(machine, agent);

    // then
    verify(machine).createProcess(eq(new CommandImpl(agentId, agentScript, "agent")), eq(null));
  }

  @Test(
    expectedExceptions = AgentStartException.class,
    expectedExceptionsMessageRegExp = "Fail launching agent .*\\. Workspace ID:.*"
  )
  public void shouldLogAgentStartLogsIfTimeoutReached() throws Exception {
    // given
    launcher = spy(new TestAgentLauncher(-1, 100, agentChecker));
    when(agentChecker.isLaunched(any(Agent.class), any(InstanceProcess.class), any(Instance.class)))
        .thenReturn(false);

    doReturn(process)
        .when(launcher)
        .start(nullable(Instance.class), nullable(Agent.class), nullable(LineConsumer.class));

    // when
    try {
      launcher.launch(machine, agent);
      fail("Should throw AgentStartException");
    } catch (AgentStartException e) {
      // then
      verify(launcher)
          .logAsErrorAgentStartLogs(
              nullable(Instance.class), nullable(String.class), nullable(String.class));
      // rethrow exception to verify message
      throw e;
    }
  }

  @Test(
    expectedExceptions = ServerException.class,
    expectedExceptionsMessageRegExp = "An error on agent start"
  )
  public void shouldLogAgentStartLogsIfMachineExceptionOccurs() throws Exception {
    // given
    doThrow(new MachineException("An error on agent start"))
        .when(launcher)
        .start(nullable(Instance.class), nullable(Agent.class), nullable(LineConsumer.class));

    // when
    try {
      launcher.launch(machine, agent);
      fail("Should throw ServerException");
      verify(launcher)
          .logAsErrorAgentStartLogs(
              nullable(Instance.class), nullable(String.class), nullable(String.class));
    } catch (ServerException e) {
      // then
      // rethrow exception to verify message
      throw e;
    }
  }

  @Test(
    expectedExceptions = ServerException.class,
    expectedExceptionsMessageRegExp = "An error on process kill"
  )
  public void shouldLogAgentStartLogsIfMachineExceptionOccursAfterAgentStartTimeoutHadReached()
      throws Exception {
    // given
    launcher = spy(new TestAgentLauncher(-1, 100, agentChecker));
    when(agentChecker.isLaunched(any(Agent.class), any(InstanceProcess.class), any(Instance.class)))
        .thenReturn(false);

    doReturn(process)
        .when(launcher)
        .start(nullable(Instance.class), nullable(Agent.class), nullable(LineConsumer.class));
    doThrow(new MachineException("An error on process kill")).when(process).kill();

    // when
    try {
      launcher.launch(machine, agent);
      fail("Should throw ServerException");
    } catch (ServerException e) {
      // then
      verify(launcher)
          .logAsErrorAgentStartLogs(
              nullable(Instance.class), nullable(String.class), nullable(String.class));
      // rethrow exception to verify message
      throw e;
    }
  }

  private static class TestAgentLauncher extends AbstractAgentLauncher {
    public TestAgentLauncher(
        long agentMaxStartTimeMs,
        long agentPingDelayMs,
        AgentLaunchingChecker agentLaunchingChecker) {
      super(agentMaxStartTimeMs, agentPingDelayMs, agentLaunchingChecker);
    }

    @Override
    protected InstanceProcess start(Instance machine, Agent agent, LineConsumer lineConsumer)
        throws ServerException {
      return super.start(machine, agent, lineConsumer);
    }

    @Override
    public String getAgentId() {
      return "testAgentId";
    }

    @Override
    public String getMachineType() {
      return "testMachineType";
    }
  }
}
