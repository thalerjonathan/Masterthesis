package frontend.replication;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

import utils.Utils;
import backend.EquilibriumStatistics;

@SuppressWarnings("serial")
public class EquilibriumInfoPanel extends JPanel {

	private JLabel assetPriceLabel;
	private JLabel loanPriceLabel;
	private JLabel assetLoanPriceLabel;
	private JLabel collateralPriceLabel;

	private JLabel i0Label;
	private JLabel i1Label;
	private JLabel i2Label;

	private JLabel i0IndexLabel;
	private JLabel i1IndexLabel;
	private JLabel i2IndexLabel;

	private JLabel pessimistWealthLabel;
	private JLabel mediumWealthLabel;
	private JLabel optimistWealthLabel;
	
	public EquilibriumInfoPanel() {
		this.setLayout( new GridBagLayout() );
		
		this.createControls();
		this.clearStats();
	}
	
	private void createControls() {
		JLabel assetPriceInfoLabel = new JLabel( "Asset-Price: ");
		JLabel loanPriceInfoLabel = new JLabel( "Loan-Price: ");
		JLabel assetLoanPriceInfoLabel = new JLabel( "Asset/Loan-Price: ");
		JLabel collateralPriceInfoLabel = new JLabel( "Collateral/Cash-Price: ");

		// NOTE: renamed i0 to i1 and i1 to i2 and i2 to last traders
		// problem: would have need to change all result-xmls
		JLabel i0InfoLabel = new JLabel( "i1: ");
		JLabel i1InfoLabel = new JLabel( "i2: ");
		JLabel i2InfoLabel = new JLabel( "last Trader: ");

		JLabel i0IndexInfoLabel = new JLabel( "i1 Index: ");
		JLabel i1IndexInfoLabel = new JLabel( "i2 Index: ");
		JLabel i2IndexInfoLabel = new JLabel( "last Trader Index: ");

		JLabel pessimistInfoLabel = new JLabel( "Pessimist Wealth: ");
		JLabel mediumInfoLabel = new JLabel( "Medianist Wealth: ");
		JLabel optimistInfoLabel = new JLabel( "Optimist Wealth: ");
		
		this.assetPriceLabel = new JLabel();
		this.loanPriceLabel = new JLabel();
		this.assetLoanPriceLabel = new JLabel();
		this.collateralPriceLabel = new JLabel();
		
		this.i0Label = new JLabel();
		this.i1Label = new JLabel();
		this.i2Label = new JLabel();

		this.i0IndexLabel = new JLabel();
		this.i1IndexLabel = new JLabel();
		this.i2IndexLabel = new JLabel();

		this.pessimistWealthLabel = new JLabel();
		this.mediumWealthLabel = new JLabel();
		this.optimistWealthLabel = new JLabel();
		
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.VERTICAL;
		c.ipadx = 10;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		//c.weightx = 1.0;
		//c.weighty = 1.0;
		
		c.gridy = 0;
		c.gridx = 0;
		this.add( assetPriceInfoLabel, c );
		c.gridx = 1;
		this.add( assetPriceLabel, c );
		c.gridy = 1;
		c.gridx = 0;
		this.add( loanPriceInfoLabel, c );
		c.gridx = 1;
		this.add( loanPriceLabel, c );
		c.gridy = 2;
		c.gridx = 0;
		this.add( assetLoanPriceInfoLabel, c );
		c.gridx = 1;
		this.add( assetLoanPriceLabel, c );
		c.gridy = 3;
		c.gridx = 0;
		this.add( collateralPriceInfoLabel, c );
		c.gridx = 1;
		this.add( collateralPriceLabel, c );
		
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
		this.add( i0IndexInfoLabel, c );
		c.gridx = 5;
		this.add( i0IndexLabel, c );
		c.gridy = 1;
		c.gridx = 4;
		this.add( i1IndexInfoLabel, c );
		c.gridx = 5;
		this.add( i1IndexLabel, c );
		c.gridy = 2;
		c.gridx = 4;
		this.add( i2IndexInfoLabel, c );
		c.gridx = 5;
		this.add( i2IndexLabel, c );
		
		c.gridy = 0;
		c.gridx = 6;
		this.add( pessimistInfoLabel, c );
		c.gridx = 7;
		this.add( pessimistWealthLabel, c );
		c.gridy = 1;
		c.gridx = 6;
		this.add( mediumInfoLabel, c );
		c.gridx = 7;
		this.add( mediumWealthLabel, c );
		c.gridy = 2;
		c.gridx = 6;
		this.add( optimistInfoLabel, c );
		c.gridx = 7;
		this.add( optimistWealthLabel, c );
	}
	
	public void clearStats() {
		this.assetPriceLabel.setText("0.00");
		this.loanPriceLabel.setText("0.00");
		this.assetLoanPriceLabel.setText("0.00");
		this.collateralPriceLabel.setText( "0.00" );
		
		this.i0Label.setText("0.00");
		this.i1Label.setText("0.00");
		this.i2Label.setText("0.00");
		
		this.i0IndexLabel.setText("N/A");
		this.i1IndexLabel.setText("N/A");
		this.i2IndexLabel.setText("N/A");
		
		this.pessimistWealthLabel.setText("0.00");
		this.mediumWealthLabel.setText("0.00");
		this.optimistWealthLabel.setText("0.00");
	}
	
	public void setStats( EquilibriumStatistics stats ) {
		this.setAssetPrice( stats.assetPrice );
		this.setLoanPrice( stats.loanPrice );
		this.setAssetLoanPrice( stats.assetLoanPrice );
		this.setCollateralPrice( stats.collateralPrice );
		
		this.setI0( stats.i0 );
		this.setI1( stats.i1 );
		this.setI2( stats.i2 );
		
		this.setI0Index( stats.i0Index );
		this.setI1Index( stats.i1Index );
		this.setI2Index( stats.i2Index );
		
		this.setPessimist( stats.pessimistWealth );
		this.setMedium( stats.medianistWealth );
		this.setOptimist( stats.optimistWealth );
	}
	
	public void setMeanAndVariance( EquilibriumStatistics mean, EquilibriumStatistics variance ) {
		this.setAssetPrice( mean.assetPrice, variance.assetPrice );
		this.setLoanPrice( mean.loanPrice, variance.loanPrice );
		this.setAssetLoanPrice( mean.assetLoanPrice, variance.assetLoanPrice );
		this.setCollateralPrice( mean.collateralPrice, variance.collateralPrice );
		
		this.setI0( mean.i0, variance.i0 );
		this.setI1( mean.i1, variance.i1 );
		this.setI2( mean.i2, variance.i2 );
		
		this.setI0Index( mean.i0Index );
		this.setI1Index( mean.i1Index );
		this.setI2Index( mean.i2Index );
		
		this.setPessimist( mean.pessimistWealth, variance.pessimistWealth );
		this.setMedium( mean.medianistWealth, variance.medianistWealth );
		this.setOptimist( mean.optimistWealth, variance.optimistWealth );
	}
	
	public void setAssetPrice( double v ) {
		this.assetPriceLabel.setText( Utils.DECIMAL_3_DIGITS_FORMATTER.format( v ) );
	}
	
	public void setAssetPrice( double mean, double variance ) {
		this.assetPriceLabel.setText( Utils.DECIMAL_3_DIGITS_FORMATTER.format( mean ) + " (" + Utils.DECIMAL_3_DIGITS_FORMATTER.format( variance ) + ")" );
	}
	
	
	public void setLoanPrice( double v ) {
		this.loanPriceLabel.setText( Utils.DECIMAL_3_DIGITS_FORMATTER.format( v ) );
	}
	
	public void setLoanPrice( double mean, double variance ) {
		this.loanPriceLabel.setText( Utils.DECIMAL_3_DIGITS_FORMATTER.format( mean ) + " (" + Utils.DECIMAL_3_DIGITS_FORMATTER.format( variance ) + ")" );
	}
	
	
	public void setAssetLoanPrice( double v ) {
		this.assetLoanPriceLabel.setText( Utils.DECIMAL_3_DIGITS_FORMATTER.format( v ) );
	}
	
	public void setAssetLoanPrice( double mean, double variance ) {
		this.assetLoanPriceLabel.setText( Utils.DECIMAL_3_DIGITS_FORMATTER.format( mean ) + " (" + Utils.DECIMAL_3_DIGITS_FORMATTER.format( variance ) + ")" );
	}
	
	
	public void setCollateralPrice( double v ) {
		this.collateralPriceLabel.setText( Utils.DECIMAL_3_DIGITS_FORMATTER.format( v ) );
	}
	
	public void setCollateralPrice( double mean, double variance ) {
		this.collateralPriceLabel.setText( Utils.DECIMAL_3_DIGITS_FORMATTER.format( mean ) + " (" + Utils.DECIMAL_3_DIGITS_FORMATTER.format( variance ) + ")" );
	}
	
	
	public void setI0( double v ) {
		this.i0Label.setText( Utils.DECIMAL_3_DIGITS_FORMATTER.format( v ) );
	}
	
	public void setI0( double mean, double variance ) {
		this.i0Label.setText( Utils.DECIMAL_3_DIGITS_FORMATTER.format( mean ) + " (" + Utils.DECIMAL_3_DIGITS_FORMATTER.format( variance ) + ")" );
	}
	

	public void setI0Index( int i ) {
		this.i0IndexLabel.setText( i < 0 ? "N/A" : "" + i );
	}
	
	public void setI1Index( int i ) {
		this.i1IndexLabel.setText( i < 0 ? "N/A" : "" + i );
	}
	
	public void setI2Index( int i ) {
		this.i2IndexLabel.setText( i < 0 ? "N/A" : "" + i );
	}
	
	
	public void setI1( double v ) {
		this.i1Label.setText( Utils.DECIMAL_3_DIGITS_FORMATTER.format( v )  );
	}
	
	public void setI1( double mean, double variance ) {
		this.i1Label.setText( Utils.DECIMAL_3_DIGITS_FORMATTER.format( mean ) + " (" + Utils.DECIMAL_3_DIGITS_FORMATTER.format( variance ) + ")" );
	}
	
	
	public void setI2( double v ) {
		this.i2Label.setText( Utils.DECIMAL_3_DIGITS_FORMATTER.format( v ) );
	}
	
	public void setI2( double mean, double variance ) {
		this.i2Label.setText( Utils.DECIMAL_3_DIGITS_FORMATTER.format( mean ) + " (" + Utils.DECIMAL_3_DIGITS_FORMATTER.format( variance ) + ")" );
	}
	
	
	public void setPessimist( double v ) {
		this.pessimistWealthLabel.setText( Utils.DECIMAL_3_DIGITS_FORMATTER.format( v ) );
	}
	
	public void setPessimist( double mean, double variance ) {
		this.pessimistWealthLabel.setText( Utils.DECIMAL_3_DIGITS_FORMATTER.format( mean ) + " (" + Utils.DECIMAL_3_DIGITS_FORMATTER.format( variance ) + ")" );
	}
	
	
	public void setMedium( double v ) {
		this.mediumWealthLabel.setText( Utils.DECIMAL_3_DIGITS_FORMATTER.format( v )  );
	}
	
	public void setMedium( double mean, double variance ) {
		this.mediumWealthLabel.setText( Utils.DECIMAL_3_DIGITS_FORMATTER.format( mean ) + " (" + Utils.DECIMAL_3_DIGITS_FORMATTER.format( variance ) + ")" );
	}
	
	
	public void setOptimist( double v ) {
		this.optimistWealthLabel.setText( Utils.DECIMAL_3_DIGITS_FORMATTER.format( v ) );
	}
	
	public void setOptimist( double mean, double variance ) {
		this.optimistWealthLabel.setText( Utils.DECIMAL_3_DIGITS_FORMATTER.format( mean ) + " (" + Utils.DECIMAL_3_DIGITS_FORMATTER.format( variance ) + ")" );
	}
}
