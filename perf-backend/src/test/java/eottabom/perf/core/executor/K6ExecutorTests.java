package eottabom.perf.core.executor;

import eottabom.perf.domain.Engine;
import eottabom.perf.infrastructure.RunRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@ExtendWith(MockitoExtension.class)
class K6ExecutorTests {

	@Mock
	private RunRepository runRepository;

	@InjectMocks
	private K6Executor k6Executor;

	@Test
	void engineReturnsK6() {
		// when
		var engine = k6Executor.engine();

		// then
		assertThat(engine).isEqualTo(Engine.K6);
	}

	@Test
	void stopDoesNothingWhenNoActiveRun() {
		// when & then
		assertThatNoException().isThrownBy(() -> k6Executor.stop("not-exist"));
	}
}
