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

import frontend.replication.ReplicationPanel.ReplicationTask;

@SuppressWarnings("serial")
public class ReplicationInfoFrame extends JFrame {

	private JPanel controlsPanel = new JPanel();
	private JPanel tasksPanel = new JPanel();
	
	private JButton refreshButton;
	
	private List<ReplicationInfoPanel> infoPanels;
	
	public ReplicationInfoFrame() {
		super( "Replication-Info" );

		this.infoPanels = new ArrayList<>();
		
		this.createControls();
		
		this.getContentPane().setLayout( new BorderLayout() );
		this.getContentPane().setPreferredSize( new Dimension( 750, 400 ) );
		
		this.setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
		this.setResizable( false );
		this.pack();
	}
	
	private void createControls() {
		this.controlsPanel = new JPanel();
			
		this.refreshButton = new JButton( "Refresh" );
		this.refreshButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for ( ReplicationInfoPanel ip : infoPanels ) {
					ip.refreshInfo();
				}
			}
		});
		
		this.controlsPanel.add( this.refreshButton );
	}
	
	public void setTasks( List<ReplicationTask> tasks ) {
		this.infoPanels.clear();
		
		this.remove( this.controlsPanel );
		this.remove( this.tasksPanel );
		
		this.tasksPanel = new JPanel();
		
		for ( ReplicationTask t : tasks ) {
			ReplicationInfoPanel ip = new ReplicationInfoPanel( t );
			ip.setBorder( BorderFactory.createTitledBorder( BorderFactory.createLineBorder( Color.black ), "Task " + t.getTaskId() ) );
			
			this.tasksPanel.add( ip );
			this.infoPanels.add( ip );
		}

		this.add( this.controlsPanel, BorderLayout.NORTH );
		this.add( this.tasksPanel, BorderLayout.CENTER );
	}
}
