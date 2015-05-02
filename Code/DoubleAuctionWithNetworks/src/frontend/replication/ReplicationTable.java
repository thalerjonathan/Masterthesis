package frontend.replication;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

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
				ReplicationPanel.VALUES_FORMAT.format( stats.p ), 
				ReplicationPanel.VALUES_FORMAT.format( stats.q ), 
				ReplicationPanel.VALUES_FORMAT.format( stats.pq ),
				ReplicationPanel.VALUES_FORMAT.format( stats.i0 ),
				ReplicationPanel.VALUES_FORMAT.format( stats.i1 ),
				ReplicationPanel.VALUES_FORMAT.format( stats.i2 ),
				ReplicationPanel.VALUES_FORMAT.format( stats.P ),
				ReplicationPanel.VALUES_FORMAT.format( stats.M ),
				ReplicationPanel.VALUES_FORMAT.format( stats.O ),
				data.isCanceled(),  data.isTradingHalted(), durationSec } );
	}
}
