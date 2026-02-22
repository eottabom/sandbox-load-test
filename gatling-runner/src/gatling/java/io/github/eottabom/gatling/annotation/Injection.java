package io.github.eottabom.gatling.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 부하 주입 패턴 필드를 표시한다.
 * 지원 타입
 * - OpenInjectionStep[]
 * - OpenInjectionStep
 * - ClosedInjectionStep[]
 * - ClosedInjectionStep,
 * - List&lt;OpenInjectionStep&gt;
 * - List&lt;ClosedInjectionStep&gt;
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Injection {
}
