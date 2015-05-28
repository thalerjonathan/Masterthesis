package frontend.experimenter;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import utils.Utils;
import backend.EquilibriumStatistics;
import backend.agents.Agent;
import backend.agents.network.AgentNetwork;
import backend.markets.Markets;
import controller.replication.data.AgentBean;
import controller.replication.data.ReplicationBean;
import controller.replication.data.ReplicationData;
import controller.replication.data.ResultBean;
import frontend.networkVisualisation.NetworkRenderPanel;
import frontend.networkVisualisation.NetworkVisualisationFrame;
import frontend.replication.EquilibriumInfoPanel;
import frontend.replication.ReplicationTable;
import frontend.visualisation.MarketsAccuOfflineVisualizer;
import frontend.visualisation.MarketsTimeOfflineVisualizer;
import frontend.visualisation.WealthVisualizer;

@SuppressWarnings("serial")
public class ExperimentResultPanel extends JPanel {

	private JFrame replicationInfoFrame;
	private NetworkVisualisationFrame netVisFrame;
	
	private ResultBean bean;

	public ExperimentResultPanel( ResultBean bean ) {
		this.bean = bean;
		
		this.setLayout( new BorderLayout() );
		
		this.createControls( bean );
	}
	
	private void createControls( ResultBean bean ) {
		List<Agent> agents = new ArrayList<Agent>();
		EquilibriumStatistics equilibriumMean = new EquilibriumStatistics( bean.getEquilibriumMean() );
		EquilibriumStatistics equilibriumVariance = new EquilibriumStatistics( bean.getEquilibriumVariance() );
		
		Markets markets = new Markets();
		markets.setABM( bean.getExperiment().isAssetLoanMarket() );
		markets.setBP( bean.getExperiment().isBondsPledgeability() );
		markets.setCollateralMarket( bean.getExperiment().isCollateralCashMarket() );
		markets.setLoanMarket( bean.getExperiment().isLoanCashMarket() );
		
		for ( int i = 0; i < bean.getAgents().size(); ++i ) {
			AgentBean agentBean = bean.getAgents().get( i );
			
			if ( agentBean.getH() < equilibriumMean.i0 ) {
				equilibriumMean.i0Index = i;
			}
			
			if ( agentBean.getH() < equilibriumMean.i1 ) {
				equilibriumMean.i1Index = i;
			}
			
			if ( agentBean.getH() < equilibriumMean.i2 ) {
				equilibriumMean.i2Index = i;
			}
			
			agents.add( new Agent( agentBean, markets ) );
		}
		
		JTabbedPane visualizersTabbedPane = new JTabbedPane();
		ExperimentInfoPanel experimentPanel = new ExperimentInfoPanel( bean.getExperiment(), true );
		EquilibriumInfoPanel equilibriumInfoPanel = new EquilibriumInfoPanel();
		WealthVisualizer wealthvisualizer = new WealthVisualizer();
		MarketsTimeOfflineVisualizer marketsTimeVisualizer = new MarketsTimeOfflineVisualizer( bean.getMedianMarkets() );
		MarketsAccuOfflineVisualizer marketsAccuVisualizer = new MarketsAccuOfflineVisualizer( bean.getMedianMarkets() );

		wealthvisualizer.setAgents( agents );
		equilibriumInfoPanel.setMeanAndVariance( equilibriumMean, equilibriumVariance );

		visualizersTabbedPane.addTab( "Agents", wealthvisualizer );
		visualizersTabbedPane.addTab( "Markets Time", marketsTimeVisualizer );
		visualizersTabbedPane.addTab( "Markets Accum", marketsAccuVisualizer );
		
		JButton showReplicationInfoButton = new JButton( "Replication-Info" );
		showReplicationInfoButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ExperimentResultPanel.this.showReplicationInfo();
			}
		});
		
		JButton showNetworkButton = new JButton( "Show Network" );
		showNetworkButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ExperimentResultPanel.this.showNetwork();
			}
		});
		
		JPanel resultInfoPanel = new JPanel( new GridBagLayout() );
		JLabel startingTimeInfoLabel = new JLabel( "Starting Time:" );
		JLabel startingTimeLabel = new JLabel( Utils.DATE_FORMATTER.format( bean.getStartingTime() ) );
		JLabel endingTimeInfoLabel = new JLabel( "Ending Time:" );
		JLabel endingTimeLabel = new JLabel( Utils.DATE_FORMATTER.format( bean.getEndingTime() ) );
		JLabel durationInfoLabel = new JLabel( "Duration:" );
		JLabel durationLabel = new JLabel( "" + bean.getDuration() + " sec");

		JLabel meanDurationInfoLabel = new JLabel( "Mean Replication Duration:" );
		JLabel meanDurationLabel = new JLabel( "" + Utils.DECIMAL_3_DIGITS_FORMATTER.format( bean.getMeanDuration() ) + " sec");
		
		JLabel meanSuccessfulTxInfoLabel = new JLabel( "Mean Replication Successful TX:" );
		JLabel meanSuccessfulTxLabel = new JLabel( "" + Utils.DECIMAL_3_DIGITS_FORMATTER.format( bean.getMeanSuccessfulTransactions() ) +
				" (" + Utils.DECIMAL_2_DIGITS_FORMATTER.format( bean.getStdSuccessfulTransactions() ) + ")" );
		JLabel meanTotalTxInfoLabel = new JLabel( "Mean Replication Total TX:" );
		JLabel meanTotalTxLabel = new JLabel( "" + Utils.DECIMAL_3_DIGITS_FORMATTER.format( bean.getMeanTotalTransactions() ) + 
				" (" + Utils.DECIMAL_2_DIGITS_FORMATTER.format( bean.getStdTotalTransactions() ) + ")" );
		JLabel meanFailedTxInfoLabel = new JLabel( "Mean Replication Failed TX:" );
		JLabel meanFailedTxLabel = new JLabel( "" + Utils.DECIMAL_3_DIGITS_FORMATTER.format( bean.getMeanFailedTransactions() ) +
				" (" + Utils.DECIMAL_2_DIGITS_FORMATTER.format( bean.getStdFailedTransactions() ) + ")" );
		
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridheight = 1;
		c.gridwidth = 1;
		c.gridy = 0;
		c.ipadx = 10;
		
		c.gridx = 0;
		c.gridy = 0;
		resultInfoPanel.add( startingTimeInfoLabel, c );
		c.gridx = 1;
		c.gridy = 0;
		resultInfoPanel.add( startingTimeLabel, c );
		c.gridx = 0;
		c.gridy = 1;
		resultInfoPanel.add( endingTimeInfoLabel, c );
		c.gridx = 1;
		c.gridy = 1;
		resultInfoPanel.add( endingTimeLabel, c );
		c.gridx = 0;
		c.gridy = 2;
		resultInfoPanel.add( durationInfoLabel, c );
		c.gridx = 1;
		c.gridy = 2;
		resultInfoPanel.add( durationLabel, c );
		c.gridx = 0;
		c.gridy = 3;
		resultInfoPanel.add( meanDurationInfoLabel, c );
		c.gridx = 1;
		c.gridy = 3;
		resultInfoPanel.add( meanDurationLabel, c );
		
		c.gridx = 2;
		c.gridy = 0;
		resultInfoPanel.add( meanSuccessfulTxInfoLabel, c );
		c.gridx = 3;
		c.gridy = 0;
		resultInfoPanel.add( meanSuccessfulTxLabel, c );
		c.gridx = 2;
		c.gridy = 1;
		resultInfoPanel.add( meanTotalTxInfoLabel, c );
		c.gridx = 3;
		c.gridy = 1;
		resultInfoPanel.add( meanTotalTxLabel, c );
		c.gridx = 2;
		c.gridy = 2;
		resultInfoPanel.add( meanFailedTxInfoLabel, c );
		c.gridx = 3;
		c.gridy = 2;
		resultInfoPanel.add( meanFailedTxLabel, c );
		
		JPanel northPanel = new JPanel( new BorderLayout() );
		JPanel buttonPanel = new JPanel( new BorderLayout() );
		buttonPanel.add( showReplicationInfoButton, BorderLayout.NORTH );
		buttonPanel.add( showNetworkButton, BorderLayout.SOUTH );
		
		northPanel.add( buttonPanel, BorderLayout.SOUTH );
		northPanel.add( resultInfoPanel, BorderLayout.CENTER );
		northPanel.add( experimentPanel, BorderLayout.EAST );
		
		this.add( northPanel, BorderLayout.NORTH );
		this.add( visualizersTabbedPane, BorderLayout.CENTER );
		this.add( equilibriumInfoPanel, BorderLayout.SOUTH );
	}
	
	private void showNetwork() {
		if ( null == this.netVisFrame ) {
			this.netVisFrame = new NetworkVisualisationFrame();
		}

		this.netVisFrame.setVisible( true );
		
		// NOTE: for visualization purposes only!
		AgentNetwork agentNetwork = AgentNetwork.createByGraphBean( this.bean.getGraph() );
		
		NetworkRenderPanel networkPanel = agentNetwork.getNetworkRenderingPanel( this.netVisFrame.getSelectedLayout(), null );
		this.netVisFrame.setNetworkRenderPanel( networkPanel, agentNetwork );
		this.netVisFrame.setTitle( "Agent Network (" + this.bean.getExperiment().getName() + ")" );
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
