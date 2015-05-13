package frontend.replication;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import frontend.Utils;
import backend.Auction.EquilibriumStatistics;

@SuppressWarnings("serial")
public class ReplicationTable extends JTable {

	private DefaultTableModel tableModel;

	public ReplicationTable() {
		this.tableModel = new DefaultTableModel(
				new Object[] { "Replication", "Task", 
						"Total TXs", "Failed TXs", "p", "q", "pq", "i0", "i1", "i2", "P", "M", "O",
						"Canceled", "Trading Halted", "Duration" }, 0 ) {

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
		// "Total TXs", "Failed TXs", "p", "q", "pq", "i0", "i1", "i2", "P", "M", "O",
		// "Canceled", "Trading Halted", "Duration"
		EquilibriumStatistics stats = data.getStats();
		
		long durationSec = ( data.getEndingTime().getTime() - data.getStartingTime().getTime() ) / 1000;
		
		this.tableModel.addRow( new Object[] { data.getNumber(), data.getTaskId(), 
				data.getTotalTxCount(),
				data.getFailedTxCount(),
				Utils.DECIMAL_2_DIGITS_FORMATTER.format( stats.p ), 
				Utils.DECIMAL_2_DIGITS_FORMATTER.format( stats.q ), 
				Utils.DECIMAL_2_DIGITS_FORMATTER.format( stats.pq ),
				Utils.DECIMAL_2_DIGITS_FORMATTER.format( stats.i0 ),
				Utils.DECIMAL_2_DIGITS_FORMATTER.format( stats.i1 ),
				Utils.DECIMAL_2_DIGITS_FORMATTER.format( stats.i2 ),
				Utils.DECIMAL_2_DIGITS_FORMATTER.format( stats.P ),
				Utils.DECIMAL_2_DIGITS_FORMATTER.format( stats.M ),
				Utils.DECIMAL_2_DIGITS_FORMATTER.format( stats.O ),
				data.isCanceled(),  data.isTradingHalted(), durationSec } );
	}
}
