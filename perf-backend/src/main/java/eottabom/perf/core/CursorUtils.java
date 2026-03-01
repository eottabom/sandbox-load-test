package eottabom.perf.core;

public final class CursorUtils {

	private CursorUtils() {
	}

	public static String normalizeCursorId(String afterId) {
		return (afterId == null || afterId.isBlank()) ? "~" : afterId.trim();
	}
}
