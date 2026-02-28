package eottabom.perf.core.executor;

import eottabom.perf.TestFixtures;
import eottabom.perf.domain.Engine;
import eottabom.perf.domain.Run;
import eottabom.perf.domain.RunStatus;
import eottabom.perf.infrastructure.RunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GatlingExecutorTests {

	@Mock
	private RunRepository runRepository;

	private GatlingExecutor gatlingExecutor;

	@BeforeEach
	void setUp() {
		gatlingExecutor = Mockito.spy(new GatlingExecutor(runRepository));
	}

	@Test
	void engineReturnsGatling() {
		// when
		var engine = gatlingExecutor.engine();

		// then
		assertThat(engine).isEqualTo(Engine.GATLING);
	}

	@Test
	void stopDoesNothingWhenNoActiveRun() {
		// when & then
		assertThatNoException().isThrownBy(() -> gatlingExecutor.stop("not-exist"));
	}

	@Test
	void stopBeforeExecutePreventsRunning() {
		// given
		var scenario = TestFixtures.scenario("s-1", "Load Test", Engine.GATLING,
				"public class StubSimulation { }", "desc");
		var run = TestFixtures.pendingRun();
		when(runRepository.findById("r-1")).thenReturn(Optional.of(run));

		// when — stop() 먼저 호출 후 execute()
		gatlingExecutor.stop(run.id());
		gatlingExecutor.execute(scenario, run);

		// then — 실행 스레드가 cancelledRuns 체크 후 즉시 리턴하므로 RUNNING 전이가 발생하지 않아야 함
		verify(runRepository, after(500).never())
				.save(argThat(r -> r.status() == RunStatus.RUNNING));
	}

	@Test
	void stopDuringExecutionSetsStopped() throws Exception {
		// given
		var startedLatch = new CountDownLatch(1);
		var releaseLatch = new CountDownLatch(1);
		var scenario = TestFixtures.scenario("s-1", "Load Test", Engine.GATLING,
				"public class StubSimulation { }", "desc");
		var run = TestFixtures.pendingRun();

		when(runRepository.findById("r-1")).thenReturn(Optional.of(
				new Run("r-1", "s-1", RunStatus.RUNNING, TestFixtures.FIXED_TIME, null)));

		doAnswer(_ -> {
			startedLatch.countDown();
			releaseLatch.await(10, TimeUnit.SECONDS);
			return 1; // non-zero: stop에 의한 중단이므로 실패 exitCode
		}).when(gatlingExecutor).runGatling(any());

		// when
		gatlingExecutor.execute(scenario, run);
		assertThat(startedLatch.await(5, TimeUnit.SECONDS)).isTrue();

		gatlingExecutor.stop(run.id());
		releaseLatch.countDown(); // runGatling 해제

		// then — 최종 저장 상태는 STOPPED
		var captor = ArgumentCaptor.forClass(Run.class);
		verify(runRepository, after(2000).atLeast(2)).save(captor.capture());
		var lastStatus = captor.getAllValues().getLast().status();
		assertThat(lastStatus).isEqualTo(RunStatus.STOPPED);
	}

	@Test
	void executeWithInvalidSourceSetsFailed() {
		// given
		var scenario = TestFixtures.scenario("s-1", "Load Test", Engine.GATLING,
				"no class declaration here", "desc");
		var run = TestFixtures.pendingRun();

		// when
		gatlingExecutor.execute(scenario, run);

		// then
		verify(runRepository, timeout(1000))
				.save(argThat(r -> r.status() == RunStatus.FAILED));
	}
}