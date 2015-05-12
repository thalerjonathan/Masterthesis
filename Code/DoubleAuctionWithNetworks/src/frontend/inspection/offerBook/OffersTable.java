package frontend.inspection.offerBook;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import backend.markets.MarketType;
import backend.offers.Offering;
import frontend.inspection.InspectionPanel;

@SuppressWarnings("serial")
public class OffersTable extends JTable {

	private DefaultTableModel model;
	
	private final static Object[] COLUMN_LABELS_ASSETCASH = new Object[] { "Asset Amount", "Asset Price" };
	private final static Object[] COLUMN_LABELS_LOANS = new Object[] { "Loan Amount", "Loan Price" };
	private final static Object[] COLUMN_LABELS_ASSETLOANS = new Object[] { "Asset Amount", "Loan Amount (Asset Price in Loans)" };
	private final static Object[] COLUMN_LABELS_COLLATERALCASH = new Object[] { "Asset Amount", "Asset Price", "Loan Amount" };
	
	public OffersTable( MarketType marketType ) {
		Object[] columns = COLUMN_LABELS_ASSETCASH;
		
		if ( MarketType.ASSET_CASH == marketType ) {
			columns = COLUMN_LABELS_ASSETCASH;
			
		} else if ( MarketType.ASSET_LOAN == marketType ) {
			columns = COLUMN_LABELS_ASSETLOANS;
			
		} else if ( MarketType.LOAN_CASH == marketType ) {
			columns = COLUMN_LABELS_LOANS;
			
		} else if ( MarketType.COLLATERAL_CASH == marketType ) {
			columns = COLUMN_LABELS_COLLATERALCASH;
		}
		
		this.model = new DefaultTableModel( columns, 0 ) {
		    @Override
		    public boolean isCellEditable(int row, int column) {
		       return false;
		    }
		};
		
		this.setModel( this.model );
		this.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		this.setAutoCreateRowSorter( true );
	}
	
	public void clearAll() {
		this.clearSelection();
		this.model.setRowCount( 0 );
		this.revalidate();
	}
	
	public void addOffering( Offering ask ) {
		if ( null == ask ) {
			return;
		}
		
		if ( MarketType.COLLATERAL_CASH == ask.getMarketType() ) {
			this.model.addRow( new Object[] {
					InspectionPanel.TRADING_VALUES_FORMAT.format( ask.getAmount() ),
					InspectionPanel.TRADING_VALUES_FORMAT.format( ask.getPrice() ),
					InspectionPanel.TRADING_VALUES_FORMAT.format( 
							ask.getAgent().calculateLoanValueOfAsset( ask.getAmount() ) ),
			});
			
		} else {
			this.model.addRow( new Object[] {
					InspectionPanel.TRADING_VALUES_FORMAT.format( ask.getAmount() ),
					InspectionPanel.TRADING_VALUES_FORMAT.format( ask.getPrice() )
			});
		}
	}
}
