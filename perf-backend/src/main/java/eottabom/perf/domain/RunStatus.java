package eottabom.perf.domain;

public enum RunStatus {
	PENDING, RUNNING, COMPLETED, FAILED, STOPPED;

	public boolean isTerminal() {
		return this == COMPLETED || this == FAILED || this == STOPPED;
	}

	@Override
	public String toString() {
		return name().toLowerCase();
	}
}
