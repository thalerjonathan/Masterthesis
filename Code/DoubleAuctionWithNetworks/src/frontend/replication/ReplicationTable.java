package frontend.replication;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

@SuppressWarnings("serial")
public class ReplicationTable extends JTable {

	private DefaultTableModel tableModel;
	
	@SuppressWarnings("rawtypes")
	public ReplicationTable() {
		Class[] columnClasses = new Class[]{ Integer.class };
		
		this.tableModel = new DefaultTableModel(
				new Object[] { "Number", "Thread" }, 0 ) {

		    @Override
		    public boolean isCellEditable(int row, int column) {
		       return false;
		    }

			@Override
			public Class<?> getColumnClass(int columnIndex) {
				return columnClasses[ columnIndex ];
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
	
	public void addReplication( int number, int thread ) {
		this.tableModel.addRow( new Object[] { number, thread } );
	}
}
