package frontend.replication.info;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import backend.replications.ReplicationsRunner.ReplicationTask;
import backend.replications.ReplicationsRunner.TerminationMode;
import frontend.visualisation.WealthVisualizer;

@SuppressWarnings("serial")
public class ReplicationInfoPanel extends JPanel {

	private JLabel terminationModeLabel;
	private JLabel maxTxLabel;
	private JLabel totalTxLabel;
	private JLabel failedTxLabel;
	private JLabel replicationNumberLabel;
	
	private JButton cancelButton;
	private JButton nextRepButton;
	private JButton showWealthButton;
	
	private JProgressBar progressBar;
	
	private ReplicationTask task;
	
	private final static DecimalFormat TX_COUNT_FORMATTER = new DecimalFormat( "###,###.###" );
	
	public ReplicationInfoPanel( ReplicationTask task ) {
		this.task = task;
		
		this.setLayout( new GridBagLayout() );
		
		this.createControls();
		this.refreshInfo();
	}
	
	private void createControls() {
		JLabel terminationModeInfoLabel = new JLabel( "Termination-Mode: " );
		JLabel replicationNumberInfoLabel = new JLabel( "Replication #: " );
		
		JLabel maxTxInfoLabel = new JLabel( "Max TX: " );
		JLabel totalTxInfoLabel = new JLabel( "Total TX-Count: " );
		JLabel failedTxInfoLabel = new JLabel( "Failed TX-Count: " );
		
		this.terminationModeLabel = new JLabel();
		this.maxTxLabel = new JLabel();
		this.totalTxLabel = new JLabel();
		this.failedTxLabel = new JLabel();
		this.replicationNumberLabel = new JLabel();
				
		this.cancelButton = new JButton( "Cancel" );
		this.nextRepButton = new JButton( "Next Rep." );
		this.showWealthButton = new JButton( "Show Wealth" );
		
		this.progressBar = new JProgressBar();
		this.progressBar.setMinimum( 0 );
		this.progressBar.setStringPainted( true );

		this.cancelButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ReplicationInfoPanel.this.task.cancel();
			}
		} );
		
		this.nextRepButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ReplicationInfoPanel.this.task.nextReplication();
			}
		} );
		
		this.showWealthButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ReplicationInfoPanel.this.showWealth();
			}
		});
		
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.ipadx = 25;
		c.ipady = 5;
		
		c.gridx = 0;
		c.gridy = 0;
		this.add( terminationModeInfoLabel, c );
		c.gridx = 1;
		c.gridy = 0;
		this.add( this.terminationModeLabel, c );
		c.gridx = 0;
		c.gridy = 1;
		this.add( replicationNumberInfoLabel, c );
		c.gridx = 1;
		c.gridy = 1;
		this.add( this.replicationNumberLabel, c );
		
		c.gridx = 2;
		c.gridy = 0;
		this.add( totalTxInfoLabel, c );
		c.gridx = 3;
		c.gridy = 0;
		this.add( this.totalTxLabel, c );
		c.gridx = 2;
		c.gridy = 1;
		this.add( failedTxInfoLabel, c );
		c.gridx = 3;
		c.gridy = 1;
		this.add( this.failedTxLabel, c );
		
		c.gridx = 4;
		c.gridy = 0;
		this.add( maxTxInfoLabel, c );
		c.gridx = 5;
		c.gridy = 0;
		this.add( this.maxTxLabel, c );
		
		c.gridx = 4;
		c.gridy = 1;
		c.gridwidth = 5;
		c.gridheight = 1;
		this.add( this.progressBar, c );
		
		c.gridx = 9;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		this.add( this.cancelButton, c );
		c.gridx = 9;
		c.gridy = 1;
		this.add( this.nextRepButton, c );
		c.gridx = 10;
		c.gridy = 0;
		c.gridwidth = 2;
		c.gridheight = 2;
		this.add( this.showWealthButton, c );
	}

	public void refreshInfo() {
		this.terminationModeLabel.setText( this.task.getTerminationMode().name() );
		this.totalTxLabel.setText( TX_COUNT_FORMATTER.format( this.task.getTotalTxCount() ) );
		this.failedTxLabel.setText( TX_COUNT_FORMATTER.format( this.task.getFailTxCount() ) );
		this.replicationNumberLabel.setText( "" + this.task.getCurrentReplication() );
		
		if ( TerminationMode.TRADING_HALTED != task.getTerminationMode()) {
			this.maxTxLabel.setText( TX_COUNT_FORMATTER.format( this.task.getMaxTx() )  );
			this.progressBar.setMaximum( this.task.getMaxTx() );
			
			if ( TerminationMode.TOTAL_TX == task.getTerminationMode() ) {
				this.progressBar.setValue( this.task.getTotalTxCount() );
			} else {
				this.progressBar.setValue( this.task.getFailTxCount() );
			}
			
		} else {
			this.maxTxLabel.setText( "-" );
		}
	}

	private void showWealth() {
		JFrame wealthFrame = new JFrame( "Current Wealth of Replication #" + task.getCurrentReplication() + " in Task " + task.getTaskId() );
		wealthFrame.add( new WealthVisualizer( this.task.getAgents() ) );
		wealthFrame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		wealthFrame.setResizable( false );
		wealthFrame.pack();
		wealthFrame.setVisible( true );
	}
}
