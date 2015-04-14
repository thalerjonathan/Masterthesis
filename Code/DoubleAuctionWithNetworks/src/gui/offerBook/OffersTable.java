package gui.offerBook;

import gui.MainWindow;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import agents.markets.MarketType;
import doubleAuction.offer.AskOffering;
import doubleAuction.offer.BidOffering;

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
		if ( null == bid ) {
			return;
		}
		
		this.model.addRow( new Object[] {
				MainWindow.TRADING_VALUES_FORMAT.format( bid.getAmount() ),
				MainWindow.TRADING_VALUES_FORMAT.format( bid.getPrice() ),
				"-", "-"
		});
	}
	
	public void addAskOffering( AskOffering ask ) {
		if ( null == ask ) {
			return;
		}
		
		this.model.addRow( new Object[] {
				MainWindow.TRADING_VALUES_FORMAT.format( ask.getAmount() ),
				MainWindow.TRADING_VALUES_FORMAT.format( ask.getPrice() ),
				"-", "-"
		});
	}
}
