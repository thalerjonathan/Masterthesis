package frontend.inspection.offerBook;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import backend.agents.Agent;

public class OfferBook {

	private List<Agent> agents;
	private List<OfferBookFrame> offerBookInstances;
	
	public OfferBook() {
		this.offerBookInstances = new ArrayList<>();
	}
	
	public void agentsChanged( List<Agent> agents ) {
		this.agents = agents;
		
		for ( OfferBookFrame obf : this.offerBookInstances ) {
			obf.setVisible( false );
			obf.dispose();
		}
		
		this.offerBookInstances.clear();
	}
	
	// NOTE: won't kill all offer-book instances but just updates them
	public void agentsUpdated( List<Agent> agents ) {
		this.agents = agents;
		this.offerBookChanged();
	}
	
	public void showOfferBook() {
		this.createAndShowInstance( 0, 0 );
	}

	public void offerBookChanged() {
		for ( OfferBookFrame obf : this.offerBookInstances ) {
			obf.refresh();
		}
	}
	
	public void createAndShowInstance( int agentIndex, int tabIndex ) {
		OfferBookFrame instance = new OfferBookFrame( this, agentIndex, tabIndex, new WindowAdapter() {
			@Override
			public void windowClosed( WindowEvent e ) {
				OfferBook.this.offerBookInstances.remove( e.getComponent() );
			}
		} );
		instance.refresh();
		instance.setVisible( true );
		this.offerBookInstances.add( instance );
	}
	
	public List<Agent> getAgents() {
		return this.agents;
	}
	
}
