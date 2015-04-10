package gui.offerBook;

import gui.MainWindow;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

import agents.Agent;

@SuppressWarnings("serial")
public class AgentInfoPanel extends JPanel {
	private JLabel idLabel;
	private JLabel hLabel;
	private JLabel consumEndowLabel;
	private JLabel assetEndowLabel;
	private JLabel freeAssetEndowLabel;
	private JLabel loansGivenLabel;
	private JLabel loansTakenLabel;
	
	private JLabel expectedAssetPriceLabel;
	private JLabel expectedLoansBuyLabel;
	private JLabel expectedLoansSellLabel;

	public AgentInfoPanel() {
		this.createControls();
	}
	
	public void setAgent( Agent a ) {
		this.idLabel.setText( "" + a.getId() );
		this.hLabel.setText( MainWindow.AGENT_H_FORMAT.format( a.getH() ) );
		this.consumEndowLabel.setText( MainWindow.TRADING_VALUES_FORMAT.format( a.getConumEndow() ) );
		this.assetEndowLabel.setText( MainWindow.TRADING_VALUES_FORMAT.format( a.getAssetEndow() ) );
		this.expectedAssetPriceLabel.setText( MainWindow.TRADING_VALUES_FORMAT.format( a.getLimitPriceAsset() ) );

		this.freeAssetEndowLabel.setText( MainWindow.TRADING_VALUES_FORMAT.format( a.getFreeAssetEndow() ) );
		
		this.expectedLoansBuyLabel.setText( MainWindow.TRADING_VALUES_FORMAT.format( a.getLimitPriceLoansBuy() ) );
		this.expectedLoansSellLabel.setText( MainWindow.TRADING_VALUES_FORMAT.format( a.getLimitPriceLoansSell() ) );
		this.loansGivenLabel.setText( MainWindow.TRADING_VALUES_FORMAT.format( a.getLoanGiven() ) );
		this.loansTakenLabel.setText( MainWindow.TRADING_VALUES_FORMAT.format( a.getLoanTaken() ) );
	}
	
	private void createControls() {
		JLabel idInfoLabel = new JLabel( "Id: " );
		JLabel hInfoLabel = new JLabel( "Optimism (h): " );
		JLabel consumEndowInfoLabel = new JLabel( "Cash: " );
		JLabel assetEndowInfoLabel = new JLabel( "Assets: " );
		JLabel freeAssetEndowInfoLabel = new JLabel( "Free Assets: " );
		JLabel loansGivenInfoLabel = new JLabel( "Loans Given: " );
		JLabel loansTakenInfoLabel = new JLabel( "Loans Taken: " );
		
		JLabel expectedLoansBuyInfoLabel = new JLabel( "Loans Limit-Price Buy: " );
		JLabel expectedLoansSellInfoLabel = new JLabel( "Loans Limit-Price Sell: " );
		JLabel expectedAssetPriceInfoLabel = new JLabel( "Assets Limit-Price: " );
		
		this.idLabel = new JLabel();
		this.hLabel = new JLabel();
		
		this.consumEndowLabel = new JLabel();
		this.assetEndowLabel = new JLabel();
		this.freeAssetEndowLabel = new JLabel();

		this.loansGivenLabel = new JLabel();
		this.loansTakenLabel = new JLabel();
		
		this.expectedLoansBuyLabel = new JLabel();
		this.expectedLoansSellLabel = new JLabel();
		this.expectedAssetPriceLabel = new JLabel();

		this.setLayout( new GridBagLayout() );
		
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.ipadx = 15;
		
		c.gridx = 0;
		c.gridy = 0;
		this.add( idInfoLabel, c );
		c.gridy = 0;
		c.gridx = 2;
		this.add( hInfoLabel, c );
		
		c.gridx = 0;
		c.gridy = 1;
		this.add( consumEndowInfoLabel, c );
		c.gridx = 2;
		c.gridy = 1;
		this.add( assetEndowInfoLabel, c );
		c.gridx = 4;
		c.gridy = 1;
		this.add( freeAssetEndowInfoLabel, c );
		
		c.gridx = 0;
		c.gridy = 2;
		this.add( loansGivenInfoLabel, c );
		c.gridx = 2;
		c.gridy = 2;
		this.add( loansTakenInfoLabel, c );
		
		c.gridx = 0;
		c.gridy = 3;
		this.add( expectedLoansBuyInfoLabel, c );
		c.gridx = 2;
		c.gridy = 3;
		this.add( expectedLoansSellInfoLabel, c );
		c.gridx = 4;
		c.gridy = 3;
		this.add( expectedAssetPriceInfoLabel, c );
		
		
		
		c.gridx = 1;
		c.gridy = 0;
		this.add( this.idLabel, c );
		c.gridy = 0;
		c.gridx = 3;
		this.add( this.hLabel, c );
		
		c.gridx = 1;
		c.gridy = 1;
		this.add( this.consumEndowLabel, c );
		c.gridx = 3;
		c.gridy = 1;
		this.add( this.assetEndowLabel, c );
		c.gridx = 5;
		c.gridy = 1;
		this.add( this.freeAssetEndowLabel, c );
		
		c.gridx = 1;
		c.gridy = 2;
		this.add( this.loansGivenLabel, c );
		c.gridx = 3;
		c.gridy = 2;
		this.add( this.loansTakenLabel, c );
		
		c.gridx = 1;
		c.gridy = 3;
		this.add( this.expectedLoansBuyLabel, c );
		c.gridx = 3;
		c.gridy = 3;
		this.add( this.expectedLoansSellLabel, c );
		c.gridx = 5;
		c.gridy = 3;
		this.add( this.expectedAssetPriceLabel, c );
	}
}
