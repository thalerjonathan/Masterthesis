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

	private JSpinner agentIdSpinner;
	private AgentInfoPanel agentInfoPanel;
	private List<Agent> agents;
	
	public AgentInfoFrame() {
		super( "Agent-Info" );
		
		this.agentInfoPanel = new AgentInfoPanel();
		
		this.agentIdSpinner = new JSpinner();
		this.agentIdSpinner.addChangeListener( new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				int agentId = (Integer) AgentInfoFrame.this.agentIdSpinner.getValue();
				Agent a = agents.get( agentId - 1 );
				
				AgentInfoFrame.this.agentInfoPanel.setAgent( a );
			}
		});
		
		this.getContentPane().setLayout( new BorderLayout() );
		this.getContentPane().add( this.agentIdSpinner, BorderLayout.NORTH );
		this.getContentPane().add( this.agentInfoPanel, BorderLayout.CENTER );
		this.getContentPane().setPreferredSize( new Dimension( 650, 100 ) );
		
		this.setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
		this.setResizable( false );
		this.pack();
	}
	
	public void setAgents( List<Agent> agents ) {
		int agentId = 1;
		
		if ( null == this.agents ) {
			this.agentIdSpinner.setModel( new SpinnerNumberModel( 1, 1, agents.size(), 1 ) );
			
		} else {
			// if number of agents has changed, do a complete reset
			if ( this.agents.size() == agents.size() ) {
				agentId = (Integer) AgentInfoFrame.this.agentIdSpinner.getValue();
			} else {
				this.agentIdSpinner.setModel( new SpinnerNumberModel( 1, 1, agents.size(), 1 ) );
			}
		}
	
		this.agents = agents;
		this.agentInfoPanel.setAgent( agents.get( agentId - 1 ) );
	}
}
