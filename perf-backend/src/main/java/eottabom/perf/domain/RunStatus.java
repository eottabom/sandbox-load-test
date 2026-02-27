package eottabom.perf.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum RunStatus {
	PENDING, RUNNING, COMPLETED, FAILED, STOPPED;

	public boolean isTerminal() {
		return this == COMPLETED || this == FAILED || this == STOPPED;
	}

	@JsonValue
	public String toJson() {
		return name().toLowerCase(Locale.ROOT);
	}

	@JsonCreator
	public static RunStatus fromJson(String value) {
		return valueOf(value.toUpperCase(Locale.ROOT));
	}
}
