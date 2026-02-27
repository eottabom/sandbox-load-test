package eottabom.perf.core;

import eottabom.perf.TestFixtures;
import eottabom.perf.api.dto.CreateScenarioRequest;
import eottabom.perf.api.dto.UpdateScenarioRequest;
import eottabom.perf.domain.Engine;
import eottabom.perf.domain.Scenario;
import eottabom.perf.infrastructure.RunRepository;
import eottabom.perf.infrastructure.ScenarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ScenarioServiceTests {

	@Mock
	private ScenarioRepository scenarioRepository;

	@Mock
	private RunRepository runRepository;

	@InjectMocks
	private ScenarioService scenarioService;

	@Test
	void findAllReturnsAllScenarios() {
		// given
		given(scenarioRepository.findAll()).willReturn(List.of(
				TestFixtures.scenario("id-1", "Smoke Test", Engine.K6),
				TestFixtures.scenario("id-2", "Load Test", Engine.GATLING)
		));

		// when
		var result = scenarioService.findAll();

		// then
		assertThat(result).hasSize(2)
				.extracting(Scenario::name)
				.containsExactly("Smoke Test", "Load Test");
	}

	@Test
	void findByIdReturnsScenarioWhenFound() {
		// given
		given(scenarioRepository.findById("id-1")).willReturn(Optional.of(
				TestFixtures.scenario("id-1", "Load Test", Engine.K6)
		));

		// when
		var result = scenarioService.findById("id-1");

		// then
		assertThat(result.id()).isEqualTo("id-1");
		assertThat(result.name()).isEqualTo("Load Test");
	}

	@Test
	void findByIdThrowsWhenNotFound() {
		// given
		given(scenarioRepository.findById("not-exist")).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> scenarioService.findById("not-exist"))
				.isInstanceOf(NoSuchElementException.class)
				.hasMessageContaining("not-exist");
	}

	@Test
	void createSavesScenarioWithGeneratedIdAndTimestamp() {
		// given
		given(scenarioRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

		// when
		var result = scenarioService.create(new CreateScenarioRequest("My Test", Engine.K6, "k6 script", "desc"));

		// then
		assertThat(result.id()).isNotBlank();
		assertThat(result.name()).isEqualTo("My Test");
		assertThat(result.engine()).isEqualTo(Engine.K6);
		assertThat(result.content()).isEqualTo("k6 script");
		assertThat(result.createdAt()).isNotNull();
		assertThat(result.updatedAt()).isNull();
	}

	@Test
	void updateUpdatesOnlyProvidedFields() {
		// given
		given(scenarioRepository.findById("id-1")).willReturn(Optional.of(
				TestFixtures.scenario("id-1", "Old Name", Engine.K6, "old script", "old desc")
		));
		given(scenarioRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

		// when
		var result = scenarioService.update("id-1", new UpdateScenarioRequest("New Name", null, null));

		// then
		assertThat(result.name()).isEqualTo("New Name");
		assertThat(result.content()).isEqualTo("old script");
		assertThat(result.updatedAt()).isNotNull();
	}

	@Test
	void updateThrowsWhenNotFound() {
		// given
		given(scenarioRepository.findById("not-exist")).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> scenarioService.update("not-exist", new UpdateScenarioRequest(null, null, null)))
				.isInstanceOf(NoSuchElementException.class);
	}

	@Test
	void deleteDeletesWhenExists() {
		// given
		given(scenarioRepository.existsById("id-1")).willReturn(true);
		given(runRepository.existsByScenarioId("id-1")).willReturn(false);

		// when
		scenarioService.delete("id-1");

		// then
		verify(scenarioRepository).deleteById("id-1");
	}

	@Test
	void deleteThrowsWhenNotFound() {
		// given
		given(scenarioRepository.existsById("not-exist")).willReturn(false);

		// when & then
		assertThatThrownBy(() -> scenarioService.delete("not-exist"))
				.isInstanceOf(NoSuchElementException.class);
	}

	@Test
	void deleteThrowsWhenRunsExist() {
		// given
		given(scenarioRepository.existsById("id-1")).willReturn(true);
		given(runRepository.existsByScenarioId("id-1")).willReturn(true);

		// when & then
		assertThatThrownBy(() -> scenarioService.delete("id-1"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("cannot be deleted");
	}
}
