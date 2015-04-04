package gui.txHistory;

import gui.MainWindow;

import java.util.List;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import doubleAuction.offer.AskOfferingWithLoans;
import doubleAuction.offer.BidOfferingWithLoans;
import doubleAuction.tx.Transaction;
import doubleAuction.tx.TransactionWithLoans;

@SuppressWarnings("serial")
public class TxHistoryTable extends JTable {

	private DefaultTableModel tableModel;
	
	@SuppressWarnings("rawtypes")
	public TxHistoryTable() {
		Class[] columnClasses = new Class[]{ Integer.class, String.class, String.class, String.class,
				String.class, String.class, String.class, String.class, String.class, String.class, String.class };
		
		this.tableModel = new DefaultTableModel(
				new Object[] { "TX", 
						"Asker", "Bider", 
						"Asset Price", "Asset Ask Price", "Asset Bid Price", "Asset Amount",
						"Loan Price", "Loan Ask Price", "Loan Bid Price", "Loan Amount" }, 0 ) {

		    @Override
		    public boolean isCellEditable(int row, int column) {
		       return false;
		    }

			@Override
			public Class<?> getColumnClass(int columnIndex) {
				return columnClasses[ columnIndex ];
			}
		};
		
		TableRowSorter<DefaultTableModel> rowSorter = new TableRowSorter<>( this.tableModel );
		new TXColumnComparator( 7, rowSorter );
		new TXColumnComparator( 8, rowSorter );
		new TXColumnComparator( 9, rowSorter );
		new TXColumnComparator( 10, rowSorter );
		
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
		"TX", 
		"Asker", "Bider", 
		"Asset Price", "Asset Ask Price", "Asset Bid Price", "Asset Amount",
		"Loan Price", "Loan Ask Price", "Loan Bid Price", "Loan Amount"
		*/
		
		int txId = tx.getTransNum();
		String askerH = MainWindow.AGENT_H_FORMAT.format( tx.getFinalAskH() );
		String biderH = MainWindow.AGENT_H_FORMAT.format( tx.getFinalBidH() );
		String assetAmount = MainWindow.TRADING_VALUES_FORMAT.format( tx.getAssetAmount() );
		String assetPrice = MainWindow.TRADING_VALUES_FORMAT.format( tx.getAssetPrice() );
		String assetAskPrice = MainWindow.TRADING_VALUES_FORMAT.format( tx.getFinalAskAssetPrice() );
		String assetBidPrice = MainWindow.TRADING_VALUES_FORMAT.format( tx.getFinalBidAssetPrice() );
		String loanAmount = "-";
		String loanPrice = "-";
		String loanAskPrice = "-";
		String loanBidPrice = "-";
		
		if ( tx.getMatchingAskOffer() instanceof AskOfferingWithLoans ) {
			loanAmount = MainWindow.TRADING_VALUES_FORMAT.format( ( ( TransactionWithLoans ) tx ).getLoanAmount() );
			loanPrice = MainWindow.TRADING_VALUES_FORMAT.format( ( ( TransactionWithLoans ) tx ).getLoanPrice() );
			loanAskPrice = MainWindow.TRADING_VALUES_FORMAT.format( ( ( TransactionWithLoans ) tx ).getFinalAskLoanPrice() );
		}
		
		if ( tx.getMatchingBidOffer() instanceof BidOfferingWithLoans ) {
			loanBidPrice = MainWindow.TRADING_VALUES_FORMAT.format( ( ( TransactionWithLoans ) tx ).getFinalBidLoanPrice() );
		}

		this.tableModel.addRow( new Object[] {
				txId,
				askerH,
				biderH,
				assetPrice,
				assetAskPrice,
				assetBidPrice,
				assetAmount,
				loanPrice,
				loanAskPrice,
				loanBidPrice,
				loanAmount
		});
	}
}
