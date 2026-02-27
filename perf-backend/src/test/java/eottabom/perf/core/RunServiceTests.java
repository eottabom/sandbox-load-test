package eottabom.perf.core;

import eottabom.perf.TestFixtures;
import eottabom.perf.api.dto.StartRunRequest;
import eottabom.perf.core.executor.TestExecutor;
import eottabom.perf.domain.Engine;
import eottabom.perf.domain.RunStatus;
import eottabom.perf.infrastructure.RunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RunServiceTests {

	@Mock
	private RunRepository runRepository;

	@Mock
	private ScenarioService scenarioService;

	@Mock
	private TestExecutor k6Executor;

	private RunService runService;

	@BeforeEach
	void setUp() {
		given(k6Executor.engine()).willReturn(Engine.K6);
		runService = new RunService(runRepository, scenarioService, List.of(k6Executor));
	}

	@Test
	void startCreatesRunAndCallsExecutor() {
		// given
		var scenario = TestFixtures.k6Scenario();
		var savedRun = TestFixtures.pendingRun();
		given(scenarioService.findById("s-1")).willReturn(scenario);
		given(runRepository.save(any())).willReturn(savedRun);

		// when
		var result = runService.start(new StartRunRequest("s-1"));

		// then
		assertThat(result.scenarioId()).isEqualTo("s-1");
		assertThat(result.status()).isEqualTo(RunStatus.PENDING);
		verify(k6Executor).execute(scenario, savedRun);
	}

	@Test
	void startThrowsWhenExecutorNotFound() {
		// given
		var scenario = TestFixtures.gatlingScenario();
		given(scenarioService.findById("s-1")).willReturn(scenario);

		// when & then
		assertThatThrownBy(() -> runService.start(new StartRunRequest("s-1")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Unsupported engine");
		verify(runRepository, never()).save(any());
	}

	@Test
	void findAllReturnsLatestWhenAfterIsNull() {
		// given
		var scenario = TestFixtures.k6Scenario();
		given(runRepository.findFirst(anyInt())).willReturn(List.of(TestFixtures.completedRun()));
		given(scenarioService.findAllById(any())).willReturn(List.of(scenario));

		// when
		var result = runService.findAll(null, null, 50);

		// then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).id()).isEqualTo("r-1");
		assertThat(result.get(0).status()).isEqualTo(RunStatus.COMPLETED);
	}

	@Test
	void findAllReturnsRecordsOlderThanAfter() {
		// given
		var scenario = TestFixtures.k6Scenario();
		given(runRepository.findAfter(any(), any(), anyInt())).willReturn(List.of(TestFixtures.completedRun()));
		given(scenarioService.findAllById(any())).willReturn(List.of(scenario));

		// when
		var result = runService.findAll(TestFixtures.FIXED_TIME, "r-1", 50);

		// then
		assertThat(result).hasSize(1);
	}

	@Test
	void findByIdReturnsRunResponse() {
		// given
		var scenario = TestFixtures.k6Scenario();
		given(runRepository.findById("r-1")).willReturn(Optional.of(TestFixtures.runningRun()));
		given(scenarioService.findById("s-1")).willReturn(scenario);

		// when
		var result = runService.findById("r-1");

		// then
		assertThat(result.id()).isEqualTo("r-1");
		assertThat(result.status()).isEqualTo(RunStatus.RUNNING);
	}

	@Test
	void findByIdThrowsWhenNotFound() {
		// given
		given(runRepository.findById("not-exist")).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> runService.findById("not-exist"))
				.isInstanceOf(NoSuchElementException.class)
				.hasMessageContaining("not-exist");
	}

	@Test
	void stopStopsRunningRun() {
		// given
		var scenario = TestFixtures.k6Scenario();
		given(runRepository.findById("r-1")).willReturn(Optional.of(TestFixtures.runningRun()));
		given(scenarioService.findById("s-1")).willReturn(scenario);

		// when
		runService.stop("r-1");

		// then
		verify(k6Executor).stop("r-1");
	}

	@Test
	void stopStopsPendingRun() {
		// given
		var scenario = TestFixtures.k6Scenario();
		given(runRepository.findById("r-1")).willReturn(Optional.of(TestFixtures.pendingRun()));
		given(scenarioService.findById("s-1")).willReturn(scenario);

		// when
		runService.stop("r-1");

		// then
		verify(k6Executor).stop("r-1");
	}

	@Test
	void stopThrowsWhenExecutorNotFound() {
		// given
		var scenario = TestFixtures.gatlingScenario();
		given(runRepository.findById("r-1")).willReturn(Optional.of(TestFixtures.runningRun()));
		given(scenarioService.findById("s-1")).willReturn(scenario);

		// when & then
		assertThatThrownBy(() -> runService.stop("r-1"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Unsupported engine");
		verify(k6Executor, never()).stop(any());
	}

	@Test
	void stopThrowsWhenAlreadyCompleted() {
		// given
		given(runRepository.findById("r-1")).willReturn(Optional.of(TestFixtures.completedRun()));

		// when & then
		assertThatThrownBy(() -> runService.stop("r-1"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining(RunStatus.COMPLETED.toJson());
		verify(k6Executor, never()).stop(any());
	}

	@Test
	void stopThrowsWhenAlreadyFailed() {
		// given
		given(runRepository.findById("r-1")).willReturn(Optional.of(TestFixtures.failedRun()));

		// when & then
		assertThatThrownBy(() -> runService.stop("r-1"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining(RunStatus.FAILED.toJson());
		verify(k6Executor, never()).stop(any());
	}

	@Test
	void stopThrowsWhenNotFound() {
		// given
		given(runRepository.findById("not-exist")).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> runService.stop("not-exist"))
				.isInstanceOf(NoSuchElementException.class);
	}
}
