package io.github.eottabom.gatling.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 부하 테스트 시나리오 클래스를 표시한다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoadTest {

	/**
	 * 시뮬레이션 이름 (기본값: 클래스명)
	 */
	String name() default "";
}
