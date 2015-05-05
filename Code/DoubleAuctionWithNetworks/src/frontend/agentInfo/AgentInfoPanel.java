package frontend.agentInfo;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

import backend.agents.Agent;
import frontend.inspection.InspectionPanel;

@SuppressWarnings("serial")
public class AgentInfoPanel extends JPanel {
	private JLabel idLabel;
	private JLabel hLabel;
	private JLabel consumEndowLabel;
	private JLabel assetEndowLabel;
	private JLabel freeAssetEndowLabel;
	private JLabel loansLabel;
	private JLabel loansGivenLabel;
	private JLabel loansTakenLabel;
	
	private JLabel expectedAssetPriceLabel;
	private JLabel expectedLoansPriceLabel;
	private JLabel expectedAssetLoansPriceLabel;
	
	public AgentInfoPanel() {
		this.createControls();
	}
	
	public void setAgent( Agent a ) {
		this.idLabel.setText( "" + a.getId() );
		this.hLabel.setText( InspectionPanel.AGENT_H_FORMAT.format( a.getH() ) );
		
		this.consumEndowLabel.setText( InspectionPanel.TRADING_VALUES_FORMAT.format( a.getCash() ) );
		this.assetEndowLabel.setText( InspectionPanel.TRADING_VALUES_FORMAT.format( a.getAssets() ) );
		this.loansLabel.setText( InspectionPanel.TRADING_VALUES_FORMAT.format( a.getLoans() ) );
		
		this.freeAssetEndowLabel.setText( InspectionPanel.TRADING_VALUES_FORMAT.format( a.getUncollateralizedAssets() ) );
		this.loansGivenLabel.setText( InspectionPanel.TRADING_VALUES_FORMAT.format( a.getLoansGiven() ) );
		this.loansTakenLabel.setText( InspectionPanel.TRADING_VALUES_FORMAT.format( a.getLoansTaken() ) );
		
		this.expectedAssetPriceLabel.setText( InspectionPanel.TRADING_VALUES_FORMAT.format( a.getLimitPriceAsset() ) );
		this.expectedLoansPriceLabel.setText( InspectionPanel.TRADING_VALUES_FORMAT.format( a.getLimitPriceLoans() ) );
		this.expectedAssetLoansPriceLabel.setText( InspectionPanel.TRADING_VALUES_FORMAT.format( a.getLimitPriceAssetLoans() ) );
	}
	
	private void createControls() {
		JLabel idInfoLabel = new JLabel( "Id:" );
		JLabel hInfoLabel = new JLabel( "Optimism (h):" );
		JLabel consumEndowInfoLabel = new JLabel( "Cash:" );
		JLabel assetEndowInfoLabel = new JLabel( "Assets:" );
		JLabel loansInfoLabel = new JLabel( "Loans:" );
		JLabel freeAssetEndowInfoLabel = new JLabel( "Uncoll. Assets:" );
		JLabel loansGivenInfoLabel = new JLabel( "Loans Given:" );
		JLabel loansTakenInfoLabel = new JLabel( "Loans Taken:" );
		
		JLabel expectedLoansPriceInfoLabel = new JLabel( "Loans Limit:" );
		JLabel expectedAssetPriceInfoLabel = new JLabel( "Assets Limit:" );
		JLabel expectedAssetLoansPriceInfoLabel = new JLabel( "Assets/Loans Limit:" );
		
		this.idLabel = new JLabel();
		this.hLabel = new JLabel();
		
		this.consumEndowLabel = new JLabel();
		this.assetEndowLabel = new JLabel();
		this.freeAssetEndowLabel = new JLabel();

		this.loansLabel = new JLabel();
		this.loansGivenLabel = new JLabel();
		this.loansTakenLabel = new JLabel();
		
		this.expectedLoansPriceLabel = new JLabel();
		this.expectedAssetPriceLabel = new JLabel();
		this.expectedAssetLoansPriceLabel = new JLabel();
		
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
		this.add( loansInfoLabel, c );
		
		c.gridx = 0;
		c.gridy = 2;
		this.add( loansGivenInfoLabel, c );
		c.gridx = 2;
		c.gridy = 2;
		this.add( loansTakenInfoLabel, c );
		c.gridx = 4;
		c.gridy = 2;
		this.add( freeAssetEndowInfoLabel, c );
		
		c.gridx = 0;
		c.gridy = 3;
		this.add( expectedAssetPriceInfoLabel, c );
		c.gridx = 2;
		c.gridy = 3;
		this.add( expectedLoansPriceInfoLabel, c );
		c.gridx = 4;
		c.gridy = 3;
		this.add( expectedAssetLoansPriceInfoLabel, c );
		
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
		this.add( this.loansLabel, c );
		
		c.gridx = 1;
		c.gridy = 2;
		this.add( this.loansGivenLabel , c );
		c.gridx = 3;
		c.gridy = 2;
		this.add( this.loansTakenLabel, c );
		c.gridx = 5;
		c.gridy = 2;
		this.add( this.freeAssetEndowLabel, c );
		
		c.gridx = 1;
		c.gridy = 3;
		this.add( this.expectedAssetPriceLabel, c );
		c.gridx = 3;
		c.gridy = 3;
		this.add( this.expectedLoansPriceLabel, c );
		c.gridx = 5;
		c.gridy = 3;
		this.add( this.expectedAssetLoansPriceLabel, c );
	}
}
