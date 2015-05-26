package frontend.experimenter;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import controller.experiment.data.ExperimentBean;
import frontend.MainWindow;
import frontend.replication.ReplicationPanel;

@SuppressWarnings("serial")
public class ExperimentInfoPanel extends JPanel {

	private JLabel agentCountLabel;
	private JLabel faceValueLabel;
	private JLabel topologyLabel;
	private JLabel assetLoanMarketLabel;
	private JLabel loanCashMarketLabel;
	private JLabel bondsPledgeabilityLabel;
	private JLabel collateralMarketLabel;
	private JLabel importanceSamplingLabel;
	private JLabel terminationModeLabel;
	private JLabel maxTxLabel;
	private JLabel replicationsLabel;

	private JButton openAsReplicationButton;
	
	public ExperimentInfoPanel( ExperimentBean bean, boolean isResult ) {
		this.setLayout( new GridBagLayout() );
		this.setPreferredSize( new Dimension( 1024, 100 ) );
		
		this.createControls( bean, isResult );
	}

	private void createControls( final ExperimentBean bean, boolean isResult ) {
		JLabel agentCountInfoLabel = new JLabel( "Agents: ");
		JLabel faceValueInfoLabel = new JLabel( "Loan-Type: ");
		JLabel topologyInfoLabel = new JLabel( "Topology: ");
		
		JLabel assetLoanMarketInfoLabel = new JLabel( "Asset/Loan: ");
		JLabel loanCashMarketInfoLabel = new JLabel( "Loan/Cash: ");
		JLabel collateralMarketInfoLabel = new JLabel( "Collateral/Cash: ");
		JLabel bondsPledgeabilityInfoLabel = new JLabel( "BP: ");
		
		JLabel importanceSamplingInfoLabel = new JLabel( "Importance-Sampling: ");
		JLabel terminationModeInfoLabel = new JLabel( "Termination: ");
		
		JLabel maxTxInfoLabel = new JLabel( "Max TX: ");
		JLabel replicationsInfoLabel = new JLabel( "Replications: ");
		
		this.agentCountLabel = new JLabel( "" + bean.getAgentCount() );
		this.faceValueLabel = new JLabel( bean.getLoanType().name() );
		this.topologyLabel = new JLabel( bean.getTopology() );
		this.assetLoanMarketLabel = new JLabel( "" + bean.isAssetLoanMarket() );
		this.loanCashMarketLabel = new JLabel( "" + bean.isLoanCashMarket() );
		this.collateralMarketLabel = new JLabel( "" + bean.isCollateralCashMarket() );
		this.bondsPledgeabilityLabel = new JLabel( "" + bean.isBondsPledgeability() );
		this.importanceSamplingLabel = new JLabel( "" + bean.isImportanceSampling() );
		this.terminationModeLabel = new JLabel( bean.getTerminationMode().name() );
		this.maxTxLabel = new JLabel( "" + bean.getMaxTx() );
		this.replicationsLabel = new JLabel( "" + bean.getReplications() );
		
		if ( false == isResult ) {
			this.openAsReplicationButton = new JButton( "Open As Replication" );
			this.openAsReplicationButton.addActionListener( new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					ReplicationPanel replicationPanel = new ReplicationPanel( bean );
					MainWindow.getInstance().addPanel( replicationPanel, bean.getName() );
				}
			} );
		}
		
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.ipady = 2;
		c.ipadx = 10;
		c.gridwidth = 1;
		c.gridheight = 1;
		
		c.gridx = 0;
		c.gridy = 0;
		this.add( agentCountInfoLabel, c );
		c.gridx = 1;
		this.add( this.agentCountLabel, c );
		c.gridy = 1;
		c.gridx = 0;
		this.add( faceValueInfoLabel, c );
		c.gridx = 1;
		this.add( this.faceValueLabel, c );
		c.gridy = 2;
		c.gridx = 0;
		this.add( topologyInfoLabel, c );
		c.gridx = 1;
		this.add( this.topologyLabel, c );
		
		c.gridy = 0;
		c.gridx = 2;
		this.add( assetLoanMarketInfoLabel, c );
		c.gridx = 3;
		this.add( this.assetLoanMarketLabel, c );
		c.gridy = 1;
		c.gridx = 2;
		this.add( loanCashMarketInfoLabel, c );
		c.gridx = 3;
		this.add( this.loanCashMarketLabel, c );
		c.gridy = 2;
		c.gridx = 2;
		this.add( bondsPledgeabilityInfoLabel, c );
		c.gridx = 3;
		this.add( this.bondsPledgeabilityLabel, c );

		c.gridy = 0;
		c.gridx = 4;
		this.add( collateralMarketInfoLabel, c );
		c.gridx = 5;
		this.add( this.collateralMarketLabel, c );
		c.gridy = 1;
		c.gridx = 4;
		this.add( importanceSamplingInfoLabel, c );
		c.gridx = 5;
		this.add( this.importanceSamplingLabel, c );
		c.gridy = 2;
		c.gridx = 4;
		this.add( terminationModeInfoLabel, c );
		c.gridx = 5;
		this.add( this.terminationModeLabel, c );
		
		c.gridy = 0;
		c.gridx = 6;
		this.add( maxTxInfoLabel, c );
		c.gridx = 7;
		this.add( this.maxTxLabel, c );
		c.gridy = 1;
		c.gridx = 6;
		this.add( replicationsInfoLabel, c );
		c.gridx = 7;
		this.add( this.replicationsLabel, c );
		
		if ( false == isResult ) {
			c.gridy = 0;
			c.gridx = 28;
			c.gridwidth = 2;
			c.gridheight = 3;
			this.add( this.openAsReplicationButton, c );
		}
	}
}
