package frontend.experimenter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import backend.EquilibriumStatistics;
import backend.agents.AgentFactoryImpl;
import backend.agents.network.AgentNetwork;
import backend.markets.Markets;
import backend.replications.ReplicationsRunner;
import backend.replications.ReplicationsRunner.ReplicationsListener;
import frontend.experimenter.xml.experiment.ExperimentBean;
import frontend.experimenter.xml.experiment.ExperimentListBean;
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
import frontend.replication.ReplicationData;

public class ExperimentRunner {

	private ExperimentListBean experiments;
	private List<NetworkCreator> networkCreators;
	
	public static void main( String[] args ) {
		if ( 1 != args.length ) {
			System.out.println( "Usage: ExperimentRunner <experimentFileName>" );
			return;
		}
		
		File experimentFile = new File( args[ 0 ] );
		if ( false == experimentFile.exists() ) {
			System.out.println( "Experimentfile \"" + args[ 0 ] + "\" does not exist." );
			return;
		}
		
		ExperimentRunner eperimentRunner = ExperimentRunner.openExperiment( experimentFile );
		if ( null != eperimentRunner ) {
			eperimentRunner.runAllExperiments();
		}
	}
	
	public static ExperimentRunner openExperiment( File file ) {
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance( ExperimentListBean.class );
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			ExperimentListBean experiments = ( ExperimentListBean ) jaxbUnmarshaller.unmarshal( file );
	
			System.out.println( "Loaded " + experiments.getExperiments().size() + " experiments from " + file.getName() + ": ");
			for ( ExperimentBean experimentBean : experiments.getExperiments() ) {
				printExperimentInfo( experimentBean );
				System.out.println();
			}
			
			return new ExperimentRunner( experiments );
		} catch ( JAXBException e ) {
			System.out.println( "An Error occured parsing XML-File \"" + file.getAbsoluteFile() + "\"" );
			return null;
		}
	}
	
	private ExperimentRunner( ExperimentListBean experiments ) {
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
			
			this.runExperiment( experimentBean, agentNetwork, markets );
		}
		
		System.out.println( "\nAll Experiments finished." );
	}
	
	private void runExperiment( final ExperimentBean experiment, final AgentNetwork agentNetwork, final Markets markets ) {
		System.out.print( "\nRunning " );
		printExperimentInfo( experiment );
		System.out.println( "..." );
		
		ReplicationsRunner replications = new ReplicationsRunner( agentNetwork, markets );
		replications.start( experiment, new ReplicationsListener() {
			@Override
			public void replicationFinished( ReplicationData data,
					ReplicationData meanData, EquilibriumStatistics variance,
					List<double[]> medianMarkets)  {
				System.out.println( "	Finished Replication #" + data.getNumber() + " of " +
						experiment.getReplications() + " total Replications ");
			}
			
			@Override
			public void allReplicationsFinished() {
				System.out.println( "Experiment " + experiment.getName() + " finished." );
			}
		}, false );
		
		replications.awaitFinished();
	}
	
	private AgentNetwork createAgentNetwork( ExperimentBean bean, Markets markets ) {
		NetworkCreator networkCreator = null;
		
		for ( NetworkCreator creator : this.networkCreators ) {
			if ( creator.name().equals( bean.getTopology() ) ) {
				networkCreator = creator;
				break;
			}
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
		
		return markets;
	}
	
	private static void printExperimentInfo( ExperimentBean experiment ) {
		System.out.print( "Experiment \"" + experiment.getName() + "\", " + experiment.getAgentCount() + " Agents"
				+ ", Topology \"" + experiment.getTopology() + "\""
				+ ", " + experiment.getReplications() + " Replications" );
	}
}
