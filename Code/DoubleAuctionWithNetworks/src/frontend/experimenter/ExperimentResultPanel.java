package frontend.experimenter;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import backend.Auction.EquilibriumStatistics;
import backend.agents.Agent;
import frontend.experimenter.xml.result.AgentBean;
import frontend.experimenter.xml.result.ReplicationBean;
import frontend.experimenter.xml.result.ResultBean;
import frontend.replication.EquilibriumInfoPanel;
import frontend.replication.ReplicationData;
import frontend.replication.ReplicationTable;
import frontend.visualisation.WealthVisualizer;

@SuppressWarnings("serial")
public class ExperimentResultPanel extends JPanel {

	private ExperimentPanel experimentPanel;
	private EquilibriumInfoPanel equilibriumInfoPanel;
	private WealthVisualizer wealthvisualizer;
	
	private JFrame replicationInfoFrame;
	
	private ResultBean bean;
	
	public ExperimentResultPanel( ResultBean bean ) {
		this.bean = bean;
		
		this.setLayout( new BorderLayout() );
		
		this.createControls( bean );
	}
	
	private void createControls( ResultBean bean ) {
		List<Agent> agents = new ArrayList<Agent>();
		EquilibriumStatistics stats = new EquilibriumStatistics( bean.getEquilibrium() );
		
		Iterator<AgentBean> iter = bean.getAgents().iterator();
		while ( iter.hasNext() ) {
			AgentBean agentBean = iter.next();
			Agent a = new Agent( agentBean ) {
				public double getCollateral() {
					double collateral = this.getLoanTaken();
					if ( bean.getExperiment().isBondsPledgeability() ) {
						collateral -= this.getLoanGiven();
					}
					
					return collateral;
				}
			};
			
			agents.add( a );
		}
		
		this.experimentPanel = new ExperimentPanel( bean.getExperiment(), null );
		
		this.wealthvisualizer = new WealthVisualizer();
		this.wealthvisualizer.setAgents( agents );
		
		this.equilibriumInfoPanel = new EquilibriumInfoPanel();
		this.equilibriumInfoPanel.setStats( stats );

		JButton showReplicationInfoButton = new JButton( "Replication-Info" );
		showReplicationInfoButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ExperimentResultPanel.this.showReplicationInfo();
			}
		});
		JPanel northPanel = new JPanel( new BorderLayout() );
		northPanel.add( showReplicationInfoButton, BorderLayout.CENTER );
		northPanel.add( this.experimentPanel, BorderLayout.NORTH );
		
		this.add( northPanel, BorderLayout.NORTH );
		this.add( this.wealthvisualizer, BorderLayout.CENTER );
		this.add( this.equilibriumInfoPanel, BorderLayout.SOUTH );
	}
	
	private void showReplicationInfo() {
		if ( null == this.replicationInfoFrame ) {
			this.replicationInfoFrame = new JFrame( "Replication-Info " + this.bean.getExperiment().getName() );
			this.replicationInfoFrame.setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
			this.replicationInfoFrame.getContentPane().setPreferredSize( new Dimension( 
					1300, 580 ) );
			
			ReplicationTable replicationTable = new ReplicationTable();
			JScrollPane replicationsScrollPane = new JScrollPane( replicationTable );
			replicationsScrollPane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED );
			replicationsScrollPane.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
			
			this.replicationInfoFrame.getContentPane().add( replicationsScrollPane );
			this.replicationInfoFrame.pack();
			
			for ( ReplicationBean bean : this.bean.getReplications() ) {
				replicationTable.addReplication( new ReplicationData( bean ) );
			}
		}
		
		this.replicationInfoFrame.setVisible( true );
	}
}
