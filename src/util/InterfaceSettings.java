package util;

public class InterfaceSettings {

	public enum InputType {
		USER_INPUT,
		FILE_INPUT,
		COMMAND_LINE_INPUT
	}
	
	private final InputType inputType;
	public InputType getInputType() {
		return inputType;
	}

	private final boolean isVerbose;
	public boolean getIsVerbose() {
		return isVerbose;
	}

	public InterfaceSettings(InputType inputType, boolean isVerbose) {
		this.inputType = inputType;
		this.isVerbose = isVerbose;
	}
}
