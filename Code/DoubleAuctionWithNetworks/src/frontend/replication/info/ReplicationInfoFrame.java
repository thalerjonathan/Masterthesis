package frontend.replication.info;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import backend.replications.ReplicationsRunner.ReplicationTask;
import frontend.replication.ReplicationTable;

@SuppressWarnings("serial")
public class ReplicationInfoFrame extends JFrame {

	private JPanel tasksPanel;
	private JButton refreshButton;
	private JScrollPane txHistoryScrollPane;
	private List<ReplicationInfoPanel> infoPanels;
	private JScrollPane scroll;
	
	public ReplicationInfoFrame( ReplicationTable replicationTable ) {
		super( "Replication-Info" );

		this.infoPanels = new ArrayList<>();

		this.createControls( replicationTable );
		
		this.getContentPane().setLayout( new BorderLayout() );
		this.getContentPane().setPreferredSize( new Dimension( 1300, 580 ) );
		
		this.setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
		this.setResizable( false );
		this.pack();
	}
	
	private void createControls( ReplicationTable replicationTable ) {
		this.tasksPanel = new JPanel( new GridBagLayout() );
		
		this.scroll = new JScrollPane( this.tasksPanel );
		this.scroll.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED );
		this.scroll.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		this.scroll.setBorder( BorderFactory.createTitledBorder( BorderFactory.createLineBorder( Color.black ), "Running Tasks" ) );
		
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
		
		this.getContentPane().add( this.txHistoryScrollPane );
	}

	public void setTasks( List<ReplicationTask> tasks ) {
		this.infoPanels.clear();
		this.tasksPanel.removeAll();
		//this.getContentPane().remove( this.scroll );
		
		if ( tasks.size() > 0 ) {
			GridBagConstraints c = new GridBagConstraints();
			c.gridwidth = 1;
			c.gridheight = 1;
			c.gridx = 0;
			c.gridy = 0;
			c.fill = GridBagConstraints.BOTH;

			this.tasksPanel.add( this.refreshButton, c );
			
			c.gridy++;
			
			for ( ReplicationTask t : tasks ) {
				ReplicationInfoPanel ip = new ReplicationInfoPanel( t );
				ip.setBorder( BorderFactory.createTitledBorder( BorderFactory.createLineBorder( Color.black ), "Task " + t.getTaskId() ) );
				
				this.tasksPanel.add( ip, c );
				this.infoPanels.add( ip );
				
				c.gridy++;
			}
			
			this.getContentPane().add( this.scroll, BorderLayout.CENTER );
		}
		
		this.revalidate();
		this.pack();
	}
}
