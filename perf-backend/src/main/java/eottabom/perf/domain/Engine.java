package eottabom.perf.domain;

public enum Engine {
	K6, GATLING;

	@Override
	public String toString() {
		return name().toLowerCase();
	}
}
