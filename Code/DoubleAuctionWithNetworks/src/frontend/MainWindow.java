package frontend;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;

import frontend.experimenter.ExperimenterPanel;
import frontend.inspection.InspectionPanel;
import frontend.replication.ReplicationPanel;

@SuppressWarnings("serial")
public class MainWindow extends JFrame {

	private JTabbedPane simulationModesTabPane;
	
	private ExperimenterPanel experimenterPanel;
	
	// TODO: add context-menu to add/remove tabs
	
	private class TabPopupMenu extends JPopupMenu {
	    private JMenuItem addPanel;
	    private JMenuItem closePanel;
	    
	    public TabPopupMenu(){
	    	this.addPanel = new JMenuItem( "Add" );
	    	this.closePanel = new JMenuItem( "Close" );
		       
	    	add( this.addPanel );
	    	add( this.closePanel );
	    }
	}
	
	private class PopupListener extends MouseAdapter {
	    public void mousePressed(MouseEvent e) {
	        maybeShowPopup(e);
	    }

	    public void mouseReleased(MouseEvent e) {
	        maybeShowPopup(e);
	    }

	    private void maybeShowPopup(MouseEvent e) {
	        if (e.isPopupTrigger()) {
	        	TabPopupMenu popup = new TabPopupMenu();
	            popup.show(e.getComponent(),
	                       e.getX(), e.getY());
	        }
	    }
	}
	
	public MainWindow() {
		super("Continuous Double-Auctions");
		
		this.setExtendedState( JFrame.MAXIMIZED_BOTH ); 
		this.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        
        this.createPanels();
        
        this.pack();
        this.setVisible( true );
	}

	public void addPanel( JPanel panel, String title ) {		
		this.simulationModesTabPane.addTab( title, panel );
		this.simulationModesTabPane.setSelectedIndex( this.simulationModesTabPane.getTabCount() - 1);
	}
	
	private void createPanels() {
		this.simulationModesTabPane = new JTabbedPane();
		
		this.experimenterPanel = new ExperimenterPanel( this );
		this.simulationModesTabPane.addTab( "Experimenter", this.experimenterPanel );

		InspectionPanel ip = new InspectionPanel();
		this.simulationModesTabPane.addTab( "Inspection", ip );			
		
		ReplicationPanel rp = new ReplicationPanel();
		this.simulationModesTabPane.addTab( "Replications", rp );
		
		this.simulationModesTabPane.setSelectedIndex( 0 );
		this.simulationModesTabPane.addMouseListener( new PopupListener() );
		
		this.getContentPane().add( this.simulationModesTabPane );
	}
	
	/*
	public void changePanelTitle( JPanel panel, String title ) {
		for ( int i = 0; i < this.inspectionPanels.length; ++i ) {
			if ( panel == this.inspectionPanels[ i ] ) {
				this.simulationModesTabPane.setTitleAt( i, "Inspection " + ( i + 1 ) + ": " + title );
			}
		}
		
		for ( int i = 0; i < this.replicationPanels.length; ++i ) {
			if ( panel == this.replicationPanels[ i ] ) {
				this.simulationModesTabPane.setTitleAt( this.inspectionPanels.length + i, "Replications " + ( i + 1 ) + ": " + title );
			}
		}
	}
	*/
}
