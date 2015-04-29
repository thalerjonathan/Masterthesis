package frontend.replication;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

import backend.Auction.EquilibriumStatistics;

@SuppressWarnings("serial")
public class EquilibriumInfoPanel extends JPanel {

	private JLabel assetPriceMeanLabel;
	private JLabel loanPriceMeanLabel;
	private JLabel assetLoanPriceMeanLabel;

	private JLabel i0Label;
	private JLabel i1Label;
	private JLabel i2Label;

	private JLabel pessimistLabel;
	private JLabel mediumLabel;
	private JLabel optimistLabel;
	
	public EquilibriumInfoPanel() {
		this.setLayout( new GridBagLayout() );
		
		this.createControls();
	}
	
	private void createControls() {
		JLabel assetPriceMeanInfoLabel = new JLabel( "Asset-Price: ");
		JLabel loanPriceMeanInfoLabel = new JLabel( "Loan-Price: ");
		JLabel assetLoanPriceMeanInfoLabel = new JLabel( "Asset/Loan-Price: ");

		JLabel i0InfoLabel = new JLabel( "i0: ");
		JLabel i1InfoLabel = new JLabel( "i1: ");
		JLabel i2InfoLabel = new JLabel( "i2: ");

		JLabel pessimistInfoLabel = new JLabel( "P: ");
		JLabel mediumInfoLabel = new JLabel( "M: ");
		JLabel optimistInfoLabel = new JLabel( "O: ");
		
		this.assetPriceMeanLabel = new JLabel();
		this.loanPriceMeanLabel = new JLabel();
		this.assetLoanPriceMeanLabel = new JLabel();

		this.i0Label = new JLabel();
		this.i1Label = new JLabel();
		this.i2Label = new JLabel();

		this.pessimistLabel = new JLabel();
		this.mediumLabel = new JLabel();
		this.optimistLabel = new JLabel();
		
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = 1.0;
		c.weighty = 1.0;
		
		c.gridy = 0;
		c.gridx = 0;
		this.add( assetPriceMeanInfoLabel, c );
		c.gridx = 1;
		this.add( assetPriceMeanLabel, c );
		c.gridy = 1;
		c.gridx = 0;
		this.add( loanPriceMeanInfoLabel, c );
		c.gridx = 1;
		this.add( loanPriceMeanLabel, c );
		c.gridy = 2;
		c.gridx = 0;
		this.add( assetLoanPriceMeanInfoLabel, c );
		c.gridx = 1;
		this.add( assetLoanPriceMeanLabel, c );
		
		c.gridy = 0;
		c.gridx = 2;
		this.add( i0InfoLabel, c );
		c.gridx = 3;
		this.add( i0Label, c );
		c.gridy = 1;
		c.gridx = 2;
		this.add( i1InfoLabel, c );
		c.gridx = 3;
		this.add( i1Label, c );
		c.gridy = 2;
		c.gridx = 2;
		this.add( i2InfoLabel, c );
		c.gridx = 3;
		this.add( i2Label, c );
		
		c.gridy = 0;
		c.gridx = 4;
		this.add( pessimistInfoLabel, c );
		c.gridx = 5;
		this.add( pessimistLabel, c );
		c.gridy = 1;
		c.gridx = 4;
		this.add( mediumInfoLabel, c );
		c.gridx = 5;
		this.add( mediumLabel, c );
		c.gridy = 2;
		c.gridx = 4;
		this.add( optimistInfoLabel, c );
		c.gridx = 5;
		this.add( optimistLabel, c );
	}
	
	public void clearStats() {
		this.assetPriceMeanLabel.setText("");
		this.loanPriceMeanLabel.setText("");
		this.assetLoanPriceMeanLabel.setText("");
		this.i0Label.setText("");
		this.i1Label.setText("");
		this.i2Label.setText("");
		this.pessimistLabel.setText("");
		this.mediumLabel.setText("");
		this.optimistLabel.setText("");
	}
	
	public void setStats( EquilibriumStatistics stats ) {
		this.setAssetPrice( stats.p );
		this.setLoanPrice( stats.q );
		this.setAssetLoanPrice( stats.pq );
		
		this.setI0( stats.i0 );
		this.setI1( stats.i1 );
		this.setI2( stats.i2 );
		
		this.setPessimist( stats.P );
		this.setMedium( stats.M );
		this.setOptimist( stats.O );
	}
	
	public void setAssetPrice( double v ) {
		this.assetPriceMeanLabel.setText( "" + ReplicationPanel.VALUES_FORMAT.format( v ) );
	}
	
	public void setLoanPrice( double v ) {
		this.loanPriceMeanLabel.setText( "" + ReplicationPanel.VALUES_FORMAT.format( v ) );
	}
	
	public void setAssetLoanPrice( double v ) {
		this.assetLoanPriceMeanLabel.setText( "" + ReplicationPanel.VALUES_FORMAT.format( v ) );
	}
	
	public void setI0( double v ) {
		this.i0Label.setText( "" + ReplicationPanel.VALUES_FORMAT.format( v ) );
	}
	
	public void setI1( double v ) {
		this.i1Label.setText( "" + ReplicationPanel.VALUES_FORMAT.format( v ) );
	}
	
	public void setI2( double v ) {
		this.i2Label.setText( "" + ReplicationPanel.VALUES_FORMAT.format( v ) );
	}
	
	public void setPessimist( double v ) {
		this.pessimistLabel.setText( "" + ReplicationPanel.VALUES_FORMAT.format( v ) );
	}
	
	public void setMedium( double v ) {
		this.mediumLabel.setText( "" + ReplicationPanel.VALUES_FORMAT.format( v )  );
	}
	
	public void setOptimist( double v ) {
		this.optimistLabel.setText( "" + ReplicationPanel.VALUES_FORMAT.format( v ) );
	}
}
