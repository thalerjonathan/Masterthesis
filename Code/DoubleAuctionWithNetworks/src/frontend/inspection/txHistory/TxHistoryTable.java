package frontend.inspection.txHistory;

import frontend.inspection.InspectionPanel;

import java.util.List;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import backend.markets.MarketType;
import backend.tx.Match;
import backend.tx.Transaction;

@SuppressWarnings("serial")
public class TxHistoryTable extends JTable {

	private DefaultTableModel tableModel;
	
	@SuppressWarnings("rawtypes")
	public TxHistoryTable() {
		Class[] columnClasses = new Class[]{ Integer.class, Integer.class, String.class, String.class, String.class,
				String.class, String.class, String.class, String.class, String.class, String.class, String.class };
		
		this.tableModel = new DefaultTableModel(
				new Object[] { "TX", "Sweeps",
						"Asker", "Bider", 
						"Asset Price", "Asset Ask", "Asset Bid", "Asset",
						"Loan Price", "Loan Ask", "Loan Bid", "Loan" }, 0 ) {

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
		new TXColumnComparator( 8, rowSorter );
		new TXColumnComparator( 9, rowSorter );
		new TXColumnComparator( 10, rowSorter );
		new TXColumnComparator( 11, rowSorter );
		
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
		"TX", "Sweeps",
		"Asker", "Bider", 
		"Asset Price", "Asset Ask", "Asset Bid", "Asset",
		"Loan Price", "Loan Ask", "Loan Bid", "Loan" 
		*/
		
		Match match = tx.getMatch();
		
		int txId = tx.getTransNum();
		int sweepCount = tx.getSweepCount();
		String askerH = InspectionPanel.AGENT_H_FORMAT.format( match.getSellOffer().getAgent().getH() );
		String biderH = InspectionPanel.AGENT_H_FORMAT.format( match.getBuyOffer().getAgent().getH() );
		
		String assetAmount = "-";
		String assetPrice = "-";
		String assetAskPrice = "-";
		String assetBidPrice = "-";
		String loanAmount = "-";
		String loanPrice = "-";
		String loanAskPrice = "-";
		String loanBidPrice = "-";
		
		if ( MarketType.ASSET_CASH == match.getMarket() ) {
			assetAmount = InspectionPanel.TRADING_VALUES_FORMAT.format( match.getAmount() );
			assetPrice = InspectionPanel.TRADING_VALUES_FORMAT.format( match.getPrice()  );
			assetAskPrice = InspectionPanel.TRADING_VALUES_FORMAT.format( match.getSellOffer().getPrice() );
			assetBidPrice = InspectionPanel.TRADING_VALUES_FORMAT.format( match.getBuyOffer().getPrice() );
			
		} else if ( MarketType.LOAN_CASH == match.getMarket() ) {
			loanAmount = InspectionPanel.TRADING_VALUES_FORMAT.format( match.getAmount() );
			loanPrice = InspectionPanel.TRADING_VALUES_FORMAT.format( match.getPrice()  );
			loanAskPrice = InspectionPanel.TRADING_VALUES_FORMAT.format( match.getSellOffer().getPrice() );
			loanBidPrice = InspectionPanel.TRADING_VALUES_FORMAT.format( match.getBuyOffer().getPrice() );
		}
		
		this.tableModel.addRow( new Object[] {
				txId,
				sweepCount,
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
