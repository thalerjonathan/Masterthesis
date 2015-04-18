package frontend.replication;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

@SuppressWarnings("serial")
public class ReplicationTable extends JTable {

	private DefaultTableModel tableModel;
	
	@SuppressWarnings("rawtypes")
	public ReplicationTable() {
		Class[] columnClasses = new Class[]{ Integer.class, Integer.class, Integer.class, Boolean.class, Boolean.class, String.class };
		
		this.tableModel = new DefaultTableModel(
				new Object[] { "Replication-Number", "Task-Id", "TX Count", "Equilibrium", "Canceled", "Finished at" }, 0 ) {

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
		this.tableModel.addRow( new Object[] { data.getNumber(), data.getTaskId(), 
				data.getTxCount(), data.isReachedEquilibrium(), 
				data.isWasCanceled(), ReplicationPanel.DATE_FORMATTER.format( data.getFinishTime() ) } );
	}
}
