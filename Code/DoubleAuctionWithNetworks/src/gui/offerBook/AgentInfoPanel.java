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
	private JLabel uncollAssetEndowLabel;
	private JLabel loansLabel;
	private JLabel loansGivenLabel;
	private JLabel loansTakenLabel;
	
	private JLabel expectedAssetPriceLabel;
	private JLabel expectedLoansPriceLabel;

	public AgentInfoPanel() {
		this.createControls();
	}
	
	public void setAgent( Agent a ) {
		this.idLabel.setText( "" + a.getId() );
		this.hLabel.setText( MainWindow.AGENT_H_FORMAT.format( a.getH() ) );
		
		this.consumEndowLabel.setText( MainWindow.TRADING_VALUES_FORMAT.format( a.getConumEndow() ) );
		this.assetEndowLabel.setText( MainWindow.TRADING_VALUES_FORMAT.format( a.getAssetEndow() ) );
		this.uncollAssetEndowLabel.setText( MainWindow.TRADING_VALUES_FORMAT.format( a.getAssetEndow() - a.getCollateral() ) );
		
		this.expectedAssetPriceLabel.setText( MainWindow.TRADING_VALUES_FORMAT.format( a.getLimitPriceAsset() ) );
		this.expectedLoansPriceLabel.setText( MainWindow.TRADING_VALUES_FORMAT.format( a.getLimitPriceLoans() ) );
		
		this.loansLabel.setText( MainWindow.TRADING_VALUES_FORMAT.format( a.getLoan() ) );
		this.loansGivenLabel.setText( MainWindow.TRADING_VALUES_FORMAT.format( a.getLoanGiven() ) );
		this.loansTakenLabel.setText( MainWindow.TRADING_VALUES_FORMAT.format( a.getLoanTaken() ) );
	}
	
	private void createControls() {
		JLabel idInfoLabel = new JLabel( "Id:" );
		JLabel hInfoLabel = new JLabel( "Optimism (h):" );
		JLabel consumEndowInfoLabel = new JLabel( "Cash:" );
		JLabel assetEndowInfoLabel = new JLabel( "Assets:" );
		JLabel freeAssetEndowInfoLabel = new JLabel( "Uncoll. Assets:" );
		JLabel loansInfoLabel = new JLabel( "Loans:" );
		JLabel loansGivenInfoLabel = new JLabel( "Loans Given:" );
		JLabel loansTakenInfoLabel = new JLabel( "Loans Taken:" );
		
		JLabel expectedLoansPriceInfoLabel = new JLabel( "Loans Limit-Price:" );
		JLabel expectedAssetPriceInfoLabel = new JLabel( "Assets Limit-Price:" );
		
		this.idLabel = new JLabel();
		this.hLabel = new JLabel();
		
		this.consumEndowLabel = new JLabel();
		this.assetEndowLabel = new JLabel();
		this.uncollAssetEndowLabel = new JLabel();

		this.loansLabel = new JLabel();
		this.loansGivenLabel = new JLabel();
		this.loansTakenLabel = new JLabel();
		
		this.expectedLoansPriceLabel = new JLabel();
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
		this.add( loansInfoLabel, c );
		c.gridx = 2;
		c.gridy = 2;
		this.add( loansGivenInfoLabel, c );
		c.gridx = 4;
		c.gridy = 2;
		this.add( loansTakenInfoLabel, c );
		
		c.gridx = 0;
		c.gridy = 3;
		this.add( expectedAssetPriceInfoLabel, c );
		c.gridx = 2;
		c.gridy = 3;
		this.add( expectedLoansPriceInfoLabel, c );
		
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
		this.add( this.uncollAssetEndowLabel, c );
		
		c.gridx = 1;
		c.gridy = 2;
		this.add( this.loansLabel, c );
		c.gridx = 3;
		c.gridy = 2;
		this.add( this.loansGivenLabel, c );
		c.gridx = 5;
		c.gridy = 2;
		this.add( this.loansTakenLabel, c );
		
		c.gridx = 1;
		c.gridy = 3;
		this.add( this.expectedAssetPriceLabel, c );
		c.gridx = 3;
		c.gridy = 3;
		this.add( this.expectedLoansPriceLabel, c );
	}
}
