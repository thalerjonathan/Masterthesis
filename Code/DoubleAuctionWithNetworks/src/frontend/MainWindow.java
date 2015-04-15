package frontend;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import frontend.inspection.InspectionPanel;
import frontend.replication.ReplicationPanel;

@SuppressWarnings("serial")
public class MainWindow extends JFrame {

	private InspectionPanel inspectorPanel;
	private ReplicationPanel replicatorPanel;
	
	private JTabbedPane simulationModesTabPane;
	
	public MainWindow() {
		super("Continuous Double-Auctions");
		
		this.setExtendedState( JFrame.MAXIMIZED_BOTH ); 
		this.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        
        this.createPanels();
        
        this.pack();
        this.setVisible( true );
	}

	private void createPanels() {
		this.inspectorPanel = new InspectionPanel();
		this.replicatorPanel = new ReplicationPanel();
		
		this.simulationModesTabPane = new JTabbedPane();
		this.simulationModesTabPane.addTab( "Inspector", this.inspectorPanel );
		this.simulationModesTabPane.addTab( "Replicator", this.replicatorPanel );

		this.simulationModesTabPane.setSelectedIndex( 0 );
		
		this.getContentPane().add( this.simulationModesTabPane );
		
	}
}
