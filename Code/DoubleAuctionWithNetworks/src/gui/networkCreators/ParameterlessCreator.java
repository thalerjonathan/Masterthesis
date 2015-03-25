package gui.networkCreators;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public abstract class ParameterlessCreator implements INetworkCreator {

	public boolean createInstant() {
		return true;
	}
	
	public void deferCreation( Runnable okCallback, Runnable cancelCallback ) {
		throw new NotImplementedException();
	}
}
