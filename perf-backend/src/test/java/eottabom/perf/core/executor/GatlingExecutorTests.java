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
class GatlingExecutorTests {

	@Mock
	private RunRepository runRepository;

	@InjectMocks
	private GatlingExecutor gatlingExecutor;

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
}
