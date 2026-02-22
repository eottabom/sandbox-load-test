package io.github.eottabom.gatling.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 외부 .java 파일을 런타임에 컴파일하고 클래스를 로드하는 유틸리티.
 * <p>
 * javax.tools.JavaCompiler API를 사용하여 임의 경로의 .java 소스 파일을
 * 임시 디렉토리에 컴파일한 뒤, URLClassLoader로 클래스를 로드한다.
 * <p>
 * 소스 파일의 package 선언 유무와 관계없이 동작하며,
 * Fully Qualified Class Name 은 소스에서 자동 파싱한다.
 * <p>
 * 사용 예:
 * <pre>
 *   Class&lt;?&gt; clazz = RuntimeCompiler.compile(Path.of("/tmp/SampleScenario.java"));
 * </pre>
 */
public class RuntimeCompiler {

	private static final Logger log = LoggerFactory.getLogger(RuntimeCompiler.class);

	private static final Pattern PACKAGE_PATTERN = Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;", Pattern.MULTILINE);
	private static final Pattern CLASS_PATTERN = Pattern.compile("public\\s+class\\s+(\\w+)");

	public static Class<?> compile(Path sourceFile) throws Exception {
		String fullyQualifiedClassName = parseFQCN(sourceFile);
		log.info("Compiling {} ({})", sourceFile, fullyQualifiedClassName);

		Path outputDir = compileSource(sourceFile);

		log.info("Compiled successfully: {}", fullyQualifiedClassName);
		return loadClass(fullyQualifiedClassName, outputDir);
	}

	private static String parseFQCN(Path sourceFile) throws Exception {
		String source = Files.readString(sourceFile);

		String packageName = parsePackage(source);
		String className = parseClassName(source);
		if (className == null) {
			throw new IllegalArgumentException("Could not find public class declaration in " + sourceFile);
		}

		return packageName.isEmpty() ? className : packageName + "." + className;
	}

	private static Path compileSource(Path sourceFile) throws Exception {
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		if (compiler == null) {
			throw new IllegalStateException("JavaCompiler not available. Run with a JDK (not JRE).");
		}

		Path outputDir = Files.createTempDirectory("gatling-compiled-");
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

		try (StandardJavaFileManager fm = compiler.getStandardFileManager(diagnostics, null, null)) {
			fm.setLocation(StandardLocation.CLASS_OUTPUT, List.of(outputDir.toFile()));
			setClasspath(fm);

			JavaCompiler.CompilationTask task = compiler.getTask(
					null, fm, diagnostics, null, null,
					fm.getJavaFileObjects(sourceFile.toFile())
			);

			if (!task.call()) {
				StringBuilder msg = new StringBuilder("Compilation failed:\n");
				diagnostics.getDiagnostics().forEach(d ->
						msg.append("  ").append(d).append("\n")
				);
				throw new RuntimeException(msg.toString());
			}
		}

		return outputDir;
	}

	private static void setClasspath(StandardJavaFileManager fm) throws Exception {
		String classpath = System.getProperty("java.class.path");
		if (classpath != null && !classpath.isEmpty()) {
			List<File> cpFiles = Stream.of(classpath.split(File.pathSeparator))
					.map(File::new)
					.toList();
			fm.setLocation(StandardLocation.CLASS_PATH, cpFiles);
		}
	}

	private static Class<?> loadClass(String fullyQualifiedClassName, Path outputDir) throws Exception {
		URLClassLoader classLoader = new URLClassLoader(
				new URL[]{outputDir.toUri().toURL()},
				Thread.currentThread().getContextClassLoader()
		);
		return Class.forName(fullyQualifiedClassName, true, classLoader);
	}

	static String parsePackage(String source) {
		Matcher matcher = PACKAGE_PATTERN.matcher(source);
		return matcher.find() ? matcher.group(1) : "";
	}

	static String parseClassName(String source) {
		Matcher matcher = CLASS_PATTERN.matcher(source);
		return matcher.find() ? matcher.group(1) : null;
	}
}
