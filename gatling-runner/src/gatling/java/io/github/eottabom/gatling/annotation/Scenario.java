package io.github.eottabom.gatling.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 시나리오 정의 필드를 표시한다.
 * 필드 타입 : ScenarioBuilder
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Scenario {
}
