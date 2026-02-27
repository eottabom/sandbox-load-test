package eottabom.perf.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum Engine {
	K6, GATLING;

	@JsonValue
	public String toJson() {
		return name().toLowerCase(Locale.ROOT);
	}

	@JsonCreator
	public static Engine fromJson(String value) {
		return valueOf(value.toUpperCase(Locale.ROOT));
	}
}
