package frontend.replication;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import backend.EquilibriumStatistics;
import frontend.Utils;

@SuppressWarnings("serial")
public class ReplicationTable extends JTable {

	private DefaultTableModel tableModel;

	public ReplicationTable() {
		this.tableModel = new DefaultTableModel(
				new Object[] { "Replication", "Task", 
						"TXs", "Failed TXs", "Asset", "Loan", "Asset/Loan", "Collateral", "i0", "i1", "i2", "Pessimist", "Medianist", "Optimist",
						"Canceled", "Halted", "Duration" }, 0 ) {

		    @Override
		    public boolean isCellEditable(int row, int column) {
		       return false;
		    }

			@Override
			public Class<?> getColumnClass(int columnIndex) {
				return Integer.class;
			}
		};
		
		this.setModel( this.tableModel );
		this.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		this.setAutoCreateRowSorter( true );
	}
	
	public void clearAll() {
		// if there are still items in table-model, delete them
		if ( 0 < this.tableModel.getRowCount() ) {
			this.tableModel.setRowCount( 0 );
			this.revalidate();
		}
	}
	
	public void addReplication( ReplicationData data ) {
		// "Replication", "Task", 
		// "Total TXs", "Failed TXs", "Asset", "Loan", "Asset/Loan", "Collateral", "i0", "i1", "i2", "Pessimist", "Medianist", "Optimist",
		// "Canceled", "Trading Halted", "Duration"
		EquilibriumStatistics stats = data.getStats();
		
		long durationSec = ( data.getEndingTime().getTime() - data.getStartingTime().getTime() ) / 1000;
		
		this.tableModel.addRow( new Object[] { data.getNumber(), data.getTaskId(), 
				data.getTotalTxCount(),
				data.getFailedTxCount(),
				Utils.DECIMAL_3_DIGITS_FORMATTER.format( stats.assetPrice ), 
				Utils.DECIMAL_3_DIGITS_FORMATTER.format( stats.loanPrice ), 
				Utils.DECIMAL_3_DIGITS_FORMATTER.format( stats.assetLoanPrice ),
				Utils.DECIMAL_3_DIGITS_FORMATTER.format( stats.collateralPrice ),
				Utils.DECIMAL_3_DIGITS_FORMATTER.format( stats.i0 ),
				Utils.DECIMAL_3_DIGITS_FORMATTER.format( stats.i1 ),
				Utils.DECIMAL_3_DIGITS_FORMATTER.format( stats.i2 ),
				Utils.DECIMAL_3_DIGITS_FORMATTER.format( stats.pessimistWealth ),
				Utils.DECIMAL_3_DIGITS_FORMATTER.format( stats.medianistWealth ),
				Utils.DECIMAL_3_DIGITS_FORMATTER.format( stats.optimistWealth ),
				data.isCanceled(),  data.isTradingHalted(), durationSec } );
	}
}
