package frontend.inspection.txHistory;


import java.util.List;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import utils.Utils;
import backend.markets.MarketType;
import backend.tx.Match;
import backend.tx.Transaction;

@SuppressWarnings("serial")
public class TxHistoryTable extends JTable {

	private DefaultTableModel tableModel;
	
	@SuppressWarnings("rawtypes")
	private final static Class[] COLUMN_CLASSES = new Class[]{ Integer.class, 
		Integer.class, MarketType.class, String.class, String.class, String.class,
			String.class, String.class, String.class };
	
	public TxHistoryTable() {
		
		this.tableModel = new DefaultTableModel(
				new Object[] { "TX", "Sweeps", "Market",
						"Asker", "Bider", 
						"Asset Price", "Asset",
						"Loan Price", "Loan" }, 0 ) {

		    @Override
		    public boolean isCellEditable(int row, int column) {
		       return false;
		    }

			@Override
			public Class<?> getColumnClass(int columnIndex) {
				return COLUMN_CLASSES[ columnIndex ];
			}
		};
		
		TableRowSorter<DefaultTableModel> rowSorter = new TableRowSorter<DefaultTableModel>( this.tableModel );
		new TXColumnComparator( 5, rowSorter );
		new TXColumnComparator( 6, rowSorter );
		new TXColumnComparator( 7, rowSorter );
		new TXColumnComparator( 8, rowSorter );
		
		this.setModel( this.tableModel );
		this.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		this.setAutoCreateRowSorter( true );
		this.setRowSorter( rowSorter );
	}
	
	public void restore( List<Transaction> successfulTx ) {
		if ( this.tableModel.getRowCount() != successfulTx.size() ) {
			this.clearSelection();
			
			this.tableModel.setRowCount( 0 );
			this.revalidate();
			
			for ( Transaction tx : successfulTx ) {
				this.addTx( tx );
			}
		}
	}
	
	public void clearAll() {
		// if there are still items in table-model, delete them
		if ( 0 < this.tableModel.getRowCount() ) {
			this.tableModel.setRowCount( 0 );
			this.revalidate();
		}
	}
	
	public void addTx( Transaction tx ) {
		/*
		"TX", "Sweeps", "Market",
		"Asker", "Bider", 
		"Asset Price", "Asset",
		"Loan Price", "Loan" 
		*/
		
		Match match = tx.getMatch();
		
		int txId = tx.getTransNum();
		int sweepCount = tx.getSweepCount();
		MarketType market = match.getMarket();
		String askerH = Utils.DECIMAL_2_DIGITS_FORMATTER.format( match.getSellOffer().getAgent().getH() );
		String biderH = Utils.DECIMAL_2_DIGITS_FORMATTER.format( match.getBuyOffer().getAgent().getH() );
		
		String assetAmount = "-";
		String assetPrice = "-";
		String loanAmount = "-";
		String loanPrice = "-";
		
		if ( MarketType.ASSET_CASH == match.getMarket() ) {
			assetAmount = Utils.DECIMAL_4_DIGITS_FORMATTER.format( match.getAmount() );
			assetPrice = Utils.DECIMAL_4_DIGITS_FORMATTER.format( match.getPrice()  );

		} else if ( MarketType.LOAN_CASH == match.getMarket() ) {
			loanAmount = Utils.DECIMAL_4_DIGITS_FORMATTER.format( match.getAmount() );
			loanPrice = Utils.DECIMAL_4_DIGITS_FORMATTER.format( match.getPrice()  );

		} else if ( MarketType.ASSET_LOAN == match.getMarket() ) {
			assetAmount = Utils.DECIMAL_4_DIGITS_FORMATTER.format( match.getAmount() );
			assetPrice = Utils.DECIMAL_4_DIGITS_FORMATTER.format( match.getPrice()  );

			loanAmount = Utils.DECIMAL_4_DIGITS_FORMATTER.format( match.getPrice() );
		
		} else if ( MarketType.COLLATERAL_CASH == match.getMarket() ) {
			assetAmount = Utils.DECIMAL_4_DIGITS_FORMATTER.format( match.getAmount() );
			assetPrice = Utils.DECIMAL_4_DIGITS_FORMATTER.format( match.getPrice() );
		}
		
		this.tableModel.addRow( new Object[] {
				txId,
				sweepCount,
				market,
				askerH,
				biderH,
				assetPrice,
				assetAmount,
				loanPrice,
				loanAmount
		});
	}
}
