package gui.offerBook;

import gui.MainWindow;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import doubleAuction.offer.AskOffering;
import doubleAuction.offer.AskOfferingWithLoans;
import doubleAuction.offer.BidOffering;
import doubleAuction.offer.BidOfferingWithLoans;
import doubleAuction.offer.MarketType;

@SuppressWarnings("serial")
public class OffersTable extends JTable {

	private DefaultTableModel model;
	
	private final static Object[] COLUMN_LABELS_ASSETCASH = new Object[] { "Asset Amount", "Asset Price" };
	private final static Object[] COLUMN_LABELS_ASSETLOANS = new Object[] { "Asset Amount", "Asset Price",  "Loan Amount", "Loan Price" };
	private final static Object[] COLUMN_LABELS_LOANS = new Object[] { "Loan Amount", "Loan Price" };
	
	public OffersTable( MarketType marketType ) {
		Object[] columns = COLUMN_LABELS_ASSETCASH;
		
		if ( MarketType.ASSET_CASH == marketType ) {
			columns = COLUMN_LABELS_ASSETCASH;
			
		} else if ( MarketType.ASSET_LOAN == marketType ) {
			columns = COLUMN_LABELS_ASSETLOANS;
			
		} else if ( MarketType.LOAN_CASH == marketType ) {
			columns = COLUMN_LABELS_LOANS;
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
	
	public void addBidOffering( BidOffering bid ) {
		if ( bid instanceof BidOfferingWithLoans ) {
			this.model.addRow( new Object[] {
					MainWindow.TRADING_VALUES_FORMAT.format( bid.getAssetAmount() ),
					MainWindow.TRADING_VALUES_FORMAT.format( bid.getAssetPrice() ),
					MainWindow.TRADING_VALUES_FORMAT.format( ((BidOfferingWithLoans) bid).getLoanAmount() ),
					MainWindow.TRADING_VALUES_FORMAT.format( ((BidOfferingWithLoans) bid).getLoanPrice() )
			});
			
		} else {
			this.model.addRow( new Object[] {
					MainWindow.TRADING_VALUES_FORMAT.format( bid.getAssetAmount() ),
					MainWindow.TRADING_VALUES_FORMAT.format( bid.getAssetPrice() ),
					"-", "-"
			});
		}
	}
	
	public void addAskOffering( AskOffering ask ) {
		if ( ask instanceof AskOfferingWithLoans ) {
			this.model.addRow( new Object[] {
					MainWindow.TRADING_VALUES_FORMAT.format( ask.getAssetAmount() ),
					MainWindow.TRADING_VALUES_FORMAT.format( ask.getAssetPrice() ),
					MainWindow.TRADING_VALUES_FORMAT.format( ((AskOfferingWithLoans) ask).getLoanAmount() ),
					MainWindow.TRADING_VALUES_FORMAT.format( ((AskOfferingWithLoans) ask).getLoanPrice() ),
			});
			
		} else {
			this.model.addRow( new Object[] {
					MainWindow.TRADING_VALUES_FORMAT.format( ask.getAssetAmount() ),
					MainWindow.TRADING_VALUES_FORMAT.format( ask.getAssetPrice() ),
					"-", "-"
			});
		}
	}
}
