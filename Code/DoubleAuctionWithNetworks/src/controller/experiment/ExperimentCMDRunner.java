package controller.experiment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import controller.experiment.data.ExperimentBean;
import controller.experiment.data.ExperimentListBean;
import controller.replication.ReplicationsRunner;
import controller.replication.ReplicationsRunner.ReplicationsListener;
import controller.replication.data.ReplicationData;
import backend.EquilibriumStatistics;
import backend.agents.AgentFactoryImpl;
import backend.agents.network.AgentNetwork;
import backend.markets.Markets;
import frontend.networkCreators.AscendingConnectedCreator;
import frontend.networkCreators.AscendingFullShortcutsCreator;
import frontend.networkCreators.AscendingRandomShortcutsCreator;
import frontend.networkCreators.AscendingRegularShortcutsCreator;
import frontend.networkCreators.BarbasiAlbertCreator;
import frontend.networkCreators.ErdosRenyiCreator;
import frontend.networkCreators.FullyConnectedCreator;
import frontend.networkCreators.HalfFullyConnectedCreator;
import frontend.networkCreators.HubConnectedCreator;
import frontend.networkCreators.MaximumHubCreator;
import frontend.networkCreators.MedianHubCreator;
import frontend.networkCreators.NetworkCreator;
import frontend.networkCreators.ThreeMedianHubsCreator;
import frontend.networkCreators.WattStrogatzCreator;

public class ExperimentCMDRunner {

	private int maxThreads;
	private ExperimentListBean experiments;
	private List<NetworkCreator> networkCreators;
	
	public static ExperimentCMDRunner openExperiment( File file, int maxThreads ) {
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance( ExperimentListBean.class );
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			ExperimentListBean experiments = ( ExperimentListBean ) jaxbUnmarshaller.unmarshal( file );
	
			System.out.println( "Loaded " + experiments.getExperiments().size() + " experiments from " + file.getName() + ": ");
			for ( ExperimentBean experimentBean : experiments.getExperiments() ) {
				printExperimentInfo( experimentBean );
				System.out.println();
			}
			
			return new ExperimentCMDRunner( experiments, maxThreads );
		} catch ( JAXBException e ) {
			e.printStackTrace();
			
			System.out.println( "An Error occured parsing XML-File \"" + file.getAbsoluteFile() + "\": " + e.getMessage() );
			return null;
		}
	}
	
	private ExperimentCMDRunner( ExperimentListBean experiments, int maxThreads ) {
		this.maxThreads = maxThreads;
		this.experiments = experiments;
		this.networkCreators = new ArrayList<NetworkCreator>();
		
		this.networkCreators.add( new AscendingConnectedCreator() );
		this.networkCreators.add( new AscendingFullShortcutsCreator() );
		this.networkCreators.add( new AscendingRegularShortcutsCreator() );
		this.networkCreators.add( new AscendingRandomShortcutsCreator() );
		this.networkCreators.add( new FullyConnectedCreator() );
		this.networkCreators.add( new HalfFullyConnectedCreator() );
		this.networkCreators.add( new HubConnectedCreator() );
		this.networkCreators.add( new MedianHubCreator() );
		this.networkCreators.add( new MaximumHubCreator() );
		this.networkCreators.add( new ThreeMedianHubsCreator() );
		this.networkCreators.add( new ErdosRenyiCreator() );
		this.networkCreators.add( new BarbasiAlbertCreator() );
		this.networkCreators.add( new WattStrogatzCreator() );
	}
	
	public void runAllExperiments() {
		for ( ExperimentBean experimentBean : this.experiments.getExperiments() ) {
			Markets markets = createMarkets( experimentBean );
			AgentNetwork agentNetwork = this.createAgentNetwork( experimentBean, markets );
			
			if ( null != agentNetwork ) {
				this.runExperiment( experimentBean, agentNetwork, markets );
			}
		}
		
		System.out.println( "\nAll Experiments finished." );
	}
	
	private void runExperiment( final ExperimentBean experiment, final AgentNetwork agentNetwork, final Markets markets ) {
		System.out.print( "\nRunning " );
		printExperimentInfo( experiment );
		System.out.println( "..." );
		
		ReplicationsRunner replications = new ReplicationsRunner( agentNetwork, markets );
		replications.startAndWaitFinish( experiment, new ReplicationsListener() {
			@Override
			public void replicationFinished( ReplicationData data,
					ReplicationData meanData, EquilibriumStatistics variance,
					List<double[]> medianMarkets)  {
				System.out.println( "	Finished Replication #" + data.getNumber() + " / " +
						experiment.getReplications() );
			}
			
			@Override
			public void allReplicationsFinished() {
				System.out.println( "Experiment " + experiment.getName() + " finished." );
			}
		}, this.maxThreads );
	}
	
	private AgentNetwork createAgentNetwork( ExperimentBean bean, Markets markets ) {
		NetworkCreator networkCreator = null;
		
		for ( NetworkCreator creator : this.networkCreators ) {
			if ( creator.getClass().getName().equals( bean.getTopology().getClazz() ) ) {
				networkCreator = creator;
				break;
			}
		}

		if ( null == networkCreator ) {
			System.out.println( "Couldn't find topology-creator class \"" + bean.getTopology().getClazz() + "\" skipping experiment." );
			return null;
		}
		
		Map<String, String> creatorParams = bean.getTopology().getParams();
		if ( null != creatorParams ) {
			networkCreator.setParams( creatorParams );
		}
		
		AgentNetwork agentNetwork = networkCreator.createNetwork( new AgentFactoryImpl( bean.getAgentCount(), markets ) );
		if ( bean.isImportanceSampling() ) {
			networkCreator.createImportanceSampling( agentNetwork, markets );
		}
		
		return agentNetwork;
	}
	
	private static Markets createMarkets( ExperimentBean bean ) {
		Markets markets = new Markets();
		markets.setABM( bean.isAssetLoanMarket() );
		markets.setLoanMarket( bean.isLoanCashMarket() );
		markets.setBP( bean.isBondsPledgeability() );
		markets.setLoanType( bean.getLoanType() );
		markets.setCollateralMarket( bean.isCollateralCashMarket() );
		
		return markets;
	}
	
	private static void printExperimentInfo( ExperimentBean experiment ) {
		System.out.print( "Experiment \"" + experiment.getName() + "\", " + experiment.getAgentCount() + " Agents"
				+ ", Topology \"" + experiment.getTopology().getClazz() + "\""
				+ ", " + experiment.getReplications() + " Replications" );
	}
}
