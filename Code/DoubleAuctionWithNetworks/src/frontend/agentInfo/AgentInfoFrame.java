package frontend.agentInfo;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import backend.agents.Agent;

@SuppressWarnings("serial")
public class AgentInfoFrame extends JFrame {

	private JSpinner agentIndexSpinner;
	private AgentInfoPanel agentInfoPanel;
	private List<Agent> agents;
	
	public AgentInfoFrame() {
		super( "Agent-Info" );
		
		this.agentInfoPanel = new AgentInfoPanel();
		
		this.agentIndexSpinner = new JSpinner();
		this.agentIndexSpinner.addChangeListener( new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				int agentIndex = (int) AgentInfoFrame.this.agentIndexSpinner.getValue();
				Agent a = agents.get( agentIndex );
				
				AgentInfoFrame.this.agentInfoPanel.setAgent( a );
			}
		});
		
		this.getContentPane().setLayout( new BorderLayout() );
		this.getContentPane().add( this.agentIndexSpinner, BorderLayout.NORTH );
		this.getContentPane().add( this.agentInfoPanel, BorderLayout.CENTER );
		this.getContentPane().setPreferredSize( new Dimension( 550, 100 ) );
		
		this.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		this.setResizable(false);
		this.pack();
	}
	
	public void setAgents( List<Agent> agents ) {
		this.agents = agents;
		this.agentIndexSpinner.setModel( new SpinnerNumberModel( 0, 0, agents.size() - 1, 1 ) );
		
		this.agentInfoPanel.setAgent( agents.get( 0 ) );
	}
}
