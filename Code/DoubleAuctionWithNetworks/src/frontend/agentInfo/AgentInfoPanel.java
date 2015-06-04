package frontend.agentInfo;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

import utils.Utils;
import backend.agents.Agent;

@SuppressWarnings("serial")
public class AgentInfoPanel extends JPanel {
	private JLabel idLabel;
	private JLabel hLabel;
	
	private JLabel consumEndowLabel;
	private JLabel assetEndowLabel;
	private JLabel uncollateralizedAssets;
	
	private JLabel loansGivenLabel;
	private JLabel loansTakenLabel;
	private JLabel loansLabel;
	
	private JLabel minAssetPriceLabel;
	private JLabel limitPriceAssetLabel;
	private JLabel maxAssetPriceLabel;
	
	private JLabel minLoansPriceLabel;
	private JLabel limitPriceLoansLabel;
	private JLabel maxLoansPriceLabel;
	
	private JLabel minAssetLoansPriceLabel;
	private JLabel limitPriceAssetLoansLabel;
	private JLabel maxAssetLoansPriceLabel;
	
	private JLabel minCollateralPriceLabel;
	private JLabel limitPriceCollateralLabel;
	private JLabel maxCollateralPriceLabel;
	
	public AgentInfoPanel() {
		this.createControls();
	}
	
	public void setAgent( Agent a ) {
		this.idLabel.setText( "" + a.getId() );
		this.hLabel.setText( Utils.DECIMAL_3_DIGITS_FORMATTER.format( a.getH() ) );
		
		this.consumEndowLabel.setText( Utils.DECIMAL_4_DIGITS_FORMATTER.format( a.getCash() ) );
		this.assetEndowLabel.setText( Utils.DECIMAL_4_DIGITS_FORMATTER.format( a.getAssets() ) );
		this.uncollateralizedAssets.setText( Utils.DECIMAL_4_DIGITS_FORMATTER.format( a.getUncollateralizedAssets() ) );
		
		this.loansGivenLabel.setText( Utils.DECIMAL_4_DIGITS_FORMATTER.format( a.getLoansGiven() ) );
		this.loansTakenLabel.setText( Utils.DECIMAL_4_DIGITS_FORMATTER.format( a.getLoansTaken() ) );
		this.loansLabel.setText( Utils.DECIMAL_4_DIGITS_FORMATTER.format( a.getLoans() ) );
		
		this.minAssetPriceLabel.setText( Utils.DECIMAL_4_DIGITS_FORMATTER.format( a.getMinAssetPriceInCash() ) );
		this.limitPriceAssetLabel.setText( Utils.DECIMAL_4_DIGITS_FORMATTER.format( a.getLimitPriceAsset() ) );
		this.maxAssetPriceLabel.setText( Utils.DECIMAL_4_DIGITS_FORMATTER.format( a.getMaxAssetPriceInCash() ) );
		
		this.minLoansPriceLabel.setText( Utils.DECIMAL_4_DIGITS_FORMATTER.format( a.getMinLoanPriceInCash() ) );
		this.limitPriceLoansLabel.setText( Utils.DECIMAL_4_DIGITS_FORMATTER.format( a.getLimitPriceLoans() ) );
		this.maxLoansPriceLabel.setText( Utils.DECIMAL_4_DIGITS_FORMATTER.format( a.getMaxLoanPriceInCash() ) );
		
		this.minAssetLoansPriceLabel.setText( Utils.DECIMAL_4_DIGITS_FORMATTER.format( a.getMinAssetPriceInLoans() ) );
		this.limitPriceAssetLoansLabel.setText( Utils.DECIMAL_4_DIGITS_FORMATTER.format( a.getLimitPriceAssetLoans() ) );
		this.maxAssetLoansPriceLabel.setText( Utils.DECIMAL_4_DIGITS_FORMATTER.format( a.getMaxAssetPriceInLoans() ) );
		
		this.minCollateralPriceLabel.setText( Utils.DECIMAL_4_DIGITS_FORMATTER.format( a.getMinCollateralPriceInCash() ) );
		this.limitPriceCollateralLabel.setText( Utils.DECIMAL_4_DIGITS_FORMATTER.format( a.getLimitPriceCollateral() ) );
		this.maxCollateralPriceLabel.setText( Utils.DECIMAL_4_DIGITS_FORMATTER.format( a.getMaxCollateralPriceInCash() ) );
	}
	
	private void createControls() {
		JLabel idInfoLabel = new JLabel( "Id:" );
		JLabel hInfoLabel = new JLabel( "Optimism:" );
		
		JLabel consumEndowInfoLabel = new JLabel( "Cash:" );
		JLabel assetEndowInfoLabel = new JLabel( "Assets:" );
		JLabel freeAssetEndowInfoLabel = new JLabel( "Uncoll. Assets:" );
		
		JLabel loansGivenInfoLabel = new JLabel( "Loans Given:" );
		JLabel loansTakenInfoLabel = new JLabel( "Loans Taken:" );
		JLabel loansInfoLabel = new JLabel( "Loans:" );

		JLabel minAssetPriceInfoLabel = new JLabel( "Min Assset Price:" );
		JLabel limitPriceAssetInfoLabel = new JLabel( "Exp Assets Price:" );
		JLabel maxAssetPriceInfoLabel = new JLabel( "Max Asset Price:" );
		
		JLabel minLoansPriceInfoLabel = new JLabel( "Min Loans Price:" );
		JLabel limitPriceLoansInfoLabel = new JLabel( "Exp Loans Price:" );
		JLabel maxLoansPriceInfoLabel = new JLabel( "Max Loans Price:" );
		
		JLabel minAssetLoansPriceInfoLabel = new JLabel( "Min Assets/Loans Price:" );
		JLabel limitPriceAssetLoansInfoLabel = new JLabel( "Exp Assets/Loans Price:" );
		JLabel maxAssetLoansPriceInfoLabel = new JLabel( "Max Assets/Loans Price:" );
		
		JLabel minCollateralPriceInfoLabel = new JLabel( "Min Collateral Price:" );
		JLabel limitPriceCollateralInfoLabel = new JLabel( "Exp Collateral Price:" );
		JLabel maxCollateralPriceInfoLabel = new JLabel( "Max Collateral Price:" );
		
		this.idLabel = new JLabel();
		this.hLabel = new JLabel();
		
		this.consumEndowLabel = new JLabel();
		this.assetEndowLabel = new JLabel();
		this.uncollateralizedAssets = new JLabel();

		this.loansLabel = new JLabel();
		this.loansGivenLabel = new JLabel();
		this.loansTakenLabel = new JLabel();
		
		this.minAssetPriceLabel = new JLabel();
		this.limitPriceAssetLabel = new JLabel();
		this.maxAssetPriceLabel = new JLabel();

		this.minLoansPriceLabel = new JLabel();
		this.limitPriceLoansLabel = new JLabel();
		this.maxLoansPriceLabel = new JLabel();
		
		this.minAssetLoansPriceLabel = new JLabel();
		this.limitPriceAssetLoansLabel = new JLabel();
		this.maxAssetLoansPriceLabel = new JLabel();
		
		this.minCollateralPriceLabel = new JLabel();
		this.limitPriceCollateralLabel = new JLabel();
		this.maxCollateralPriceLabel = new JLabel();

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
		c.gridx = 4;
		c.gridy = 2;
		this.add( loansInfoLabel, c );
		
		c.gridx = 0;
		c.gridy = 3;
		this.add( minAssetPriceInfoLabel, c );
		c.gridx = 2;
		c.gridy = 3;
		this.add( limitPriceAssetInfoLabel, c );
		c.gridx = 4;
		c.gridy = 3;
		this.add( maxAssetPriceInfoLabel, c );
		
		c.gridx = 0;
		c.gridy = 4;
		this.add( minLoansPriceInfoLabel, c );
		c.gridx = 2;
		c.gridy = 4;
		this.add( limitPriceLoansInfoLabel, c );
		c.gridx = 4;
		c.gridy = 4;
		this.add( maxLoansPriceInfoLabel, c );
		
		c.gridx = 0;
		c.gridy = 5;
		this.add( minAssetLoansPriceInfoLabel, c );
		c.gridx = 2;
		c.gridy = 5;
		this.add( limitPriceAssetLoansInfoLabel, c );
		c.gridx = 4;
		c.gridy = 5;
		this.add( maxAssetLoansPriceInfoLabel, c );
		
		c.gridx = 0;
		c.gridy = 6;
		this.add( minCollateralPriceInfoLabel, c );
		c.gridx = 2;
		c.gridy = 6;
		this.add( limitPriceCollateralInfoLabel, c );
		c.gridx = 4;
		c.gridy = 6;
		this.add( maxCollateralPriceInfoLabel, c );
		
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
		this.add( this.uncollateralizedAssets, c );
		
		c.gridx = 1;
		c.gridy = 2;
		this.add( this.loansGivenLabel , c );
		c.gridx = 3;
		c.gridy = 2;
		this.add( this.loansTakenLabel, c );
		c.gridx = 5;
		c.gridy = 2;
		this.add( this.loansLabel, c );
		
		c.gridx = 1;
		c.gridy = 3;
		this.add( this.minAssetPriceLabel, c );
		c.gridx = 3;
		c.gridy = 3;
		this.add( this.limitPriceAssetLabel, c );
		c.gridx = 5;
		c.gridy = 3;
		this.add( this.maxAssetPriceLabel, c );
		
		c.gridx = 1;
		c.gridy = 4;
		this.add( this.minLoansPriceLabel, c );
		c.gridx = 3;
		c.gridy = 4;
		this.add( this.limitPriceLoansLabel, c );
		c.gridx = 5;
		c.gridy = 4;
		this.add( this.maxLoansPriceLabel, c );
		
		c.gridx = 1;
		c.gridy = 5;
		this.add( this.minAssetLoansPriceLabel, c );
		c.gridx = 3;
		c.gridy = 5;
		this.add( this.limitPriceAssetLoansLabel, c );
		c.gridx = 5;
		c.gridy = 5;
		this.add( this.maxAssetLoansPriceLabel, c );
		
		c.gridx = 1;
		c.gridy = 6;
		this.add( this.minCollateralPriceLabel, c );
		c.gridx = 3;
		c.gridy = 6;
		this.add( this.limitPriceCollateralLabel, c );
		c.gridx = 5;
		c.gridy = 6;
		this.add( this.maxCollateralPriceLabel, c );
	}
}
