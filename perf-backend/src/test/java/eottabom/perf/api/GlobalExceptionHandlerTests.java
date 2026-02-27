package eottabom.perf.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTests {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	void handleNotFoundReturns404WithDetail() {
		// given
		var exception = new NoSuchElementException("Scenario not found: id-1");

		// when
		ProblemDetail result = handler.handleNotFound(exception);

		// then
		assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
		assertThat(result.getDetail()).isEqualTo("Scenario not found: id-1");
	}

	@Test
	void handleBadRequestReturns400WithDetail() {
		// given
		var exception = new IllegalArgumentException("invalid engine");

		// when
		ProblemDetail result = handler.handleBadRequest(exception);

		// then
		assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(result.getDetail()).isEqualTo("invalid engine");
	}

	@Test
	void handleConflictReturns409WithDetail() {
		// given
		var exception = new IllegalStateException("Run is not stoppable: COMPLETED");

		// when
		ProblemDetail result = handler.handleConflict(exception);

		// then
		assertThat(result.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
		assertThat(result.getDetail()).isEqualTo("Run is not stoppable: COMPLETED");
	}
}
