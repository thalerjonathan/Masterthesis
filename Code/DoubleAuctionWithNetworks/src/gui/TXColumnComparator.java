package gui;

import java.util.Comparator;
import java.util.List;

import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.event.RowSorterEvent;
import javax.swing.event.RowSorterListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

public class TXColumnComparator implements Comparator<String>, RowSorterListener {

	private int column;
	private int ascDescValue;
	private TableRowSorter<DefaultTableModel> parent;
	
	public TXColumnComparator( int column, TableRowSorter<DefaultTableModel> parent ) {
		this.column = column;
		this.parent = parent;
		
		this.ascDescValue = 1;
		
		this.parent.setComparator( this.column, this );
		this.parent.addRowSorterListener( this );
	}
	
	@Override
	public int compare(String o1, String o2) {
		if ( o1.equals( o2 ) ) {
			return 0;
		}
		
		if ( o1.equals( "-" ) ) {
			return this.ascDescValue;
		}
		
		if ( o2.equals( "-" ) ) {
			return - this.ascDescValue;
		}
		
		return o1.compareTo( o2 );
	}

	@SuppressWarnings("unchecked")
	@Override
	public void sorterChanged(RowSorterEvent e) {
		if ( RowSorterEvent.Type.SORT_ORDER_CHANGED == e.getType() ) {
			List<RowSorter.SortKey> sortKeys = e.getSource().getSortKeys();
			
			for ( RowSorter.SortKey sorting : sortKeys ) {
				if ( this.column == sorting.getColumn() ) {
						
					if ( SortOrder.ASCENDING == sorting.getSortOrder() ) {
						this.ascDescValue = 1;
					} else {
						this.ascDescValue = -1;
					}
				}
			}
		}
	}
}
