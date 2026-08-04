package io.github.eottabom.gatling.runner;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuntimeCompilerTests {

	private Path tempDir;

	@AfterEach
	void cleanup() throws IOException {
		if (tempDir != null && Files.exists(tempDir)) {
			try (var paths = Files.walk(tempDir)) {
				paths.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
			}
		}
	}

	@Test
	void parsesPackageDeclaration() {
		var source = "package io.github.eottabom.gatling.simulations;\n\npublic class Foo {}\n";

		assertThat(RuntimeCompiler.parsePackage(source)).isEqualTo("io.github.eottabom.gatling.simulations");
	}

	@Test
	void returnsEmptyStringWhenPackageDeclarationIsAbsent() {
		var source = "public class Foo {}\n";

		assertThat(RuntimeCompiler.parsePackage(source)).isEmpty();
	}

	@Test
	void parsesPublicClassName() {
		var source = "public class MyScenario {}\n";

		assertThat(RuntimeCompiler.parseClassName(source)).isEqualTo("MyScenario");
	}

	@Test
	void returnsNullWhenNoPublicClassDeclarationExists() {
		var source = "class MyScenario {}\n";

		assertThat(RuntimeCompiler.parseClassName(source)).isNull();
	}

	@Test
	void compilesSourceFileWithoutPackageAndLoadsClass() throws Exception {
		var source = """
				public class CompilerTestScenario {
				    public static String greet() {
				        return "hello";
				    }
				}
				""";
		var sourceFile = writeSource("CompilerTestScenario", source);

		var clazz = RuntimeCompiler.compile(sourceFile);

		assertThat(clazz.getName()).isEqualTo("CompilerTestScenario");
		assertThat(clazz.getMethod("greet").invoke(null)).isEqualTo("hello");
	}

	@Test
	void compilesSourceFileWithPackageDeclaration() throws Exception {
		var source = """
				package io.github.eottabom.gatling.compiled;

				public class PackagedTestScenario {
				}
				""";
		var sourceFile = writeSource("PackagedTestScenario", source);

		var clazz = RuntimeCompiler.compile(sourceFile);

		assertThat(clazz.getName()).isEqualTo("io.github.eottabom.gatling.compiled.PackagedTestScenario");
	}

	@Test
	void throwsWhenSourceHasNoPublicClassDeclaration() throws IOException {
		var sourceFile = writeSource("NoPublicClass", "class NoPublicClass {}\n");

		assertThatThrownBy(() -> RuntimeCompiler.compile(sourceFile))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Could not find public class declaration");
	}

	@Test
	void throwsWithDiagnosticsWhenSourceFailsToCompile() throws IOException {
		var sourceFile = writeSource("BrokenScenario", "public class BrokenScenario { this is not valid java }\n");

		assertThatThrownBy(() -> RuntimeCompiler.compile(sourceFile))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("Compilation failed");
	}

	private Path writeSource(String fileName, String content) throws IOException {
		tempDir = Files.createTempDirectory("runtime-compiler-test-");
		var sourceFile = tempDir.resolve(fileName + ".java");
		Files.writeString(sourceFile, content);
		return sourceFile;
	}
}
