package frontend.networkVisualisation;

public class NetworkSelectionEvent {
	private boolean ctrlDownFlag;
	
	public NetworkSelectionEvent( boolean b ) {
		this.ctrlDownFlag = b;
	}

	public boolean isCtrlDownFlag() {
		return ctrlDownFlag;
	}
}
