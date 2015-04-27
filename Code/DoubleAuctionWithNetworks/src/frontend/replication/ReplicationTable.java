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
						"TXs", "p", "q", "pq", "i0", "i1", "i2", "P", "M", "O",
						"Equilibrium", "Canceled", "Trading Halted", "Termination Mode", "Finished at" }, 0 ) {

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
		// "TXs", "p", "q", "pq", "i0", "i1", "i2", "P", "M", "O",
		// "Equilibrium", "Canceled", "Trading Halted", "Termination Mode", "Finished at"
		EquilibriumStatistics stats = data.getStats();
		
		this.tableModel.addRow( new Object[] { data.getNumber(), data.getTaskId(), 
				data.getTxCount(), 
				ReplicationPanel.VALUES_FORMAT.format( stats.p ), 
				ReplicationPanel.VALUES_FORMAT.format( stats.q ), 
				ReplicationPanel.VALUES_FORMAT.format( stats.pq ),
				ReplicationPanel.VALUES_FORMAT.format( stats.i0 ),
				ReplicationPanel.VALUES_FORMAT.format( stats.i1 ),
				ReplicationPanel.VALUES_FORMAT.format( stats.i2 ),
				ReplicationPanel.VALUES_FORMAT.format( stats.P ),
				ReplicationPanel.VALUES_FORMAT.format( stats.M ),
				ReplicationPanel.VALUES_FORMAT.format( stats.O ),
				data.isEquilibrium(), data.isCanceled(),  data.isTradingHalted(), data.getTermination(), 
				ReplicationPanel.DATE_FORMATTER.format( data.getFinishTime() ) } );
	}
}
