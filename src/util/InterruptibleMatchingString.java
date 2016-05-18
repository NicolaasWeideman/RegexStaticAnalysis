package util;

public final class InterruptibleMatchingString implements CharSequence {
		private final CharSequence string;

		public InterruptibleMatchingString(CharSequence string) {
			this.string = string;
		}

		public char charAt(int index) {
			if (Thread.interrupted()) {
				throw new RuntimeException(new InterruptedException());
			}
			return string.charAt(index);
		}

		public int length() {
			return string.length();
		}

		public CharSequence subSequence(int start, int end) {
			if (Thread.interrupted()) {
				throw new RuntimeException(new InterruptedException());
			}
			return new InterruptibleMatchingString(string.subSequence(start, end));
		}
}
