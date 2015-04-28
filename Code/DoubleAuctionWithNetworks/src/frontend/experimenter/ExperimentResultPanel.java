package frontend.experimenter;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JPanel;

import backend.Auction.EquilibriumStatistics;
import backend.agents.Agent;
import frontend.experimenter.xml.result.AgentBean;
import frontend.experimenter.xml.result.ResultBean;
import frontend.replication.EquilibriumInfoPanel;
import frontend.visualisation.WealthVisualizer;

@SuppressWarnings("serial")
public class ExperimentResultPanel extends JPanel {

	private ExperimentPanel experimentPanel;
	private EquilibriumInfoPanel equilibriumInfoPanel;
	private WealthVisualizer wealthvisualizer;
	
	public ExperimentResultPanel( ResultBean bean ) {
		this.setLayout( new BorderLayout() );
		
		this.createControls( bean );
	}
	
	private void createControls( ResultBean bean ) {
		List<Agent> agents = new ArrayList<Agent>();
		EquilibriumStatistics stats = new EquilibriumStatistics();
		stats.p = bean.getEquilibrium().getAssetPrice();
		stats.q = bean.getEquilibrium().getLoanPrice();
		stats.pq = bean.getEquilibrium().getAssetLoanPrice();
		stats.i0 = bean.getEquilibrium().getI0();
		stats.i1 = bean.getEquilibrium().getI1();
		stats.i2 = bean.getEquilibrium().getI2();
		stats.P = bean.getEquilibrium().getP();
		stats.M = bean.getEquilibrium().getM();
		stats.O = bean.getEquilibrium().getO();
		
		Iterator<AgentBean> iter = bean.getAgents().iterator();
		while ( iter.hasNext() ) {
			AgentBean agentBean = iter.next();
			Agent a = new Agent( agentBean ) {
				public double getCollateral() {
					double collateral = this.getLoanTaken();
					if ( bean.getExperiment().isBondsPledgeability() ) {
						collateral -= this.getLoanGiven();
					}
					
					return collateral;
				}
			};
			
			agents.add( a );
		}
		
		this.experimentPanel = new ExperimentPanel( bean.getExperiment(), true );
		this.equilibriumInfoPanel = new EquilibriumInfoPanel();
		
		this.wealthvisualizer = new WealthVisualizer();
		this.wealthvisualizer.setAgents( agents );
		
		this.equilibriumInfoPanel.setStats( stats );

		this.add( this.experimentPanel, BorderLayout.NORTH );
		this.add( this.wealthvisualizer, BorderLayout.CENTER );
		this.add( this.equilibriumInfoPanel, BorderLayout.SOUTH );
	}
}
