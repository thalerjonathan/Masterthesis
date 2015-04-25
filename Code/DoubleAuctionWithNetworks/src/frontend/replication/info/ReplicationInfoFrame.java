package frontend.replication.info;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import frontend.replication.ReplicationPanel.ReplicationTask;
import frontend.replication.ReplicationTable;

@SuppressWarnings("serial")
public class ReplicationInfoFrame extends JFrame {

	private JPanel tasksPanel;
	private JButton refreshButton;
	private JScrollPane txHistoryScrollPane;
	private List<ReplicationInfoPanel> infoPanels;
	
	public ReplicationInfoFrame( ReplicationTable replicationTable ) {
		super( "Replication-Info" );

		this.infoPanels = new ArrayList<>();

		this.createControls( replicationTable );
		
		this.getContentPane().setLayout( new BorderLayout() );
		this.getContentPane().setPreferredSize( new Dimension( 880, 580 ) );
		
		this.setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
		this.setResizable( false );
		this.pack();
	}
	
	private void createControls( ReplicationTable replicationTable ) {
		this.tasksPanel = new JPanel();
		
		this.refreshButton = new JButton( "Refresh" );
		this.refreshButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for ( ReplicationInfoPanel ip : infoPanels ) {
					ip.refreshInfo();
				}
			}
		});
		
		this.txHistoryScrollPane = new JScrollPane( replicationTable );
		this.txHistoryScrollPane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED );
		this.txHistoryScrollPane.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		this.txHistoryScrollPane.setBorder( BorderFactory.createTitledBorder( BorderFactory.createLineBorder( Color.black ), "Finished Replications" ) );
		this.txHistoryScrollPane.setPreferredSize( new Dimension( 880, 150 ) );
		/*
		// setting properties
		this.replicationTable.getSelectionModel().addListSelectionListener( new ListSelectionListener() {
			@Override
			public void valueChanged( ListSelectionEvent e ) {
				if (e.getValueIsAdjusting() == false) {
					int rowIndex = ReplicationPanel.this.replicationTable.getSelectedRow();
					if ( -1 == rowIndex ) {
						return;
					}
					
					int modelIndex = ReplicationPanel.this.replicationTable.getRowSorter().convertRowIndexToModel( rowIndex );
					
					ReplicationData data = ReplicationPanel.this.replicationData.get( modelIndex );
					ReplicationPanel.this.agentWealthPanel.setAgents( data.getFinalAgents() );
		        }
			}
		});
		*/
	}

	public void setTasks( List<ReplicationTask> tasks ) {
		this.infoPanels.clear();
		
		this.remove( this.tasksPanel );
		this.remove( this.txHistoryScrollPane );
		
		if ( tasks.size() > 0 ) {
			this.tasksPanel = new JPanel();
			this.tasksPanel.setBorder( BorderFactory.createTitledBorder( BorderFactory.createLineBorder( Color.black ), "Running Tasks " ) );
			this.tasksPanel.add( this.refreshButton );
			
			for ( ReplicationTask t : tasks ) {
				ReplicationInfoPanel ip = new ReplicationInfoPanel( t );
				ip.setBorder( BorderFactory.createTitledBorder( BorderFactory.createLineBorder( Color.black ), "Task " + t.getTaskId() ) );
				
				this.tasksPanel.add( ip );
				this.infoPanels.add( ip );
			}
			
			this.add( this.tasksPanel, BorderLayout.CENTER );
			this.add( this.txHistoryScrollPane, BorderLayout.SOUTH );
		} else {
			this.add( this.txHistoryScrollPane, BorderLayout.CENTER );
		}
		
		this.revalidate();
	}
}
