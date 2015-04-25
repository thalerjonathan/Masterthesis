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
	
	public MainWindow( int panelCount ) {
		super("Continuous Double-Auctions");
		
		this.setExtendedState( JFrame.MAXIMIZED_BOTH ); 
		this.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        
        this.createPanels( panelCount );
        
        this.pack();
        this.setVisible( true );
	}

	private void createPanels( int panelCount ) {
		this.simulationModesTabPane = new JTabbedPane();

		this.inspectionPanels = new InspectionPanel[ panelCount ];	
		for ( int i = 0; i < panelCount; ++i ) {
			this.inspectionPanels[ i ] = new InspectionPanel();
			this.simulationModesTabPane.addTab( "Inspection " + ( i + 1 ), this.inspectionPanels[ i ] );			
		}
		
		panelCount = 1;
		
		this.replicationPanels = new ReplicationPanel[ panelCount ];	
		for ( int i = 0; i < panelCount; ++i ) {
			this.replicationPanels[ i ] = new ReplicationPanel();
			this.simulationModesTabPane.addTab( "Replications " + ( i + 1 ), this.replicationPanels[ i ] );
		}
		
		this.simulationModesTabPane.setSelectedIndex( 0 );

		this.getContentPane().add( this.simulationModesTabPane );
	}
}
