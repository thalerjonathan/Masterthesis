package frontend;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import frontend.inspection.InspectionPanel;
import frontend.replication.ReplicationPanel;

@SuppressWarnings("serial")
public class MainWindow extends JFrame {

	private ReplicationPanel[] replicationPanels;
	private InspectionPanel[] inspectionPanels;
	private JTabbedPane simulationModesTabPane;
	
	public MainWindow( int inspectionPanelCount, int replicationPanelCount ) {
		super("Continuous Double-Auctions");
		
		this.setExtendedState( JFrame.MAXIMIZED_BOTH ); 
		this.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        
        this.createPanels( inspectionPanelCount, replicationPanelCount );
        
        this.pack();
        this.setVisible( true );
	}

	private void createPanels( int inspectionPanelCount, int replicationPanelCount ) {
		this.simulationModesTabPane = new JTabbedPane();

		this.inspectionPanels = new InspectionPanel[ inspectionPanelCount ];
		this.replicationPanels = new ReplicationPanel[ replicationPanelCount ];	
		
		for ( int i = 0; i < inspectionPanelCount; ++i ) {
			InspectionPanel p = new InspectionPanel();
			this.inspectionPanels[ i ] = p;
			this.simulationModesTabPane.addTab( "Inspection " + ( i + 1 ), p );			
		}

		for ( int i = 0; i < replicationPanelCount; ++i ) {
			ReplicationPanel p = new ReplicationPanel();
			this.replicationPanels[ i ] = p;
			this.simulationModesTabPane.addTab( "Replications " + ( i + 1 ), p );
		}
		
		this.simulationModesTabPane.setSelectedIndex( 0 );
		
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
