package agents.network;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.JPanel;

import org.apache.commons.collections15.Transformer;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import agents.Agent;
import agents.IAgentFactory;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseGraph;
import gui.visualisation.INetworkSelectionObserver;
import gui.visualisation.NetworkRenderPanel;
import gui.visualisation.WealthVisualizer;

public class AgentNetwork {
	private String networkName;
	private Graph<Agent, AgentConnection> graph;
	// need to keep track of agents in the insertion-order by ourself,
	// graph won't store them in this order and extracting them and transforming
	// to array and sorting is a bit overhead
	private List<Agent> orderedAgents;
	private List<Agent> randomOrderAgents;
	
	private DijkstraShortestPath<Agent, AgentConnection> pathCalculator;
	
	private boolean randomNetworkFlag;
	
	// NOTE: SATISFIES HYPOTHESIS
	public static AgentNetwork createFullyConnected( IAgentFactory agentFactory ) {
		AgentNetwork network = new AgentNetwork( "FullyConnected", false );
		network.populate( agentFactory );
		network.connectCompleted( 0, network.orderedAgents.size() );
		
		return network;
	}
	
	// NOTE: SATISFIES HYPOTHESIS
	public static AgentNetwork createAscendingConnected( IAgentFactory agentFactory ) {
		return AgentNetwork.createAscendingShortcutsConnected( 0.0, agentFactory );
	}
	
	// NOTE: SATISFIES HYPOTHESIS
	public static AgentNetwork createAscendingShortcutsConnected( double p, IAgentFactory agentFactory ) {
		AgentNetwork network = new AgentNetwork( "AscendingConnected", p != 0.0 );
		network.populate( agentFactory );
		
		for ( int i = 0; i < network.orderedAgents.size() - 1; ++i ) {
			Agent from = network.orderedAgents.get( i );
			Agent to = network.orderedAgents.get( i + 1 );
			
			network.graph.addEdge( new AgentConnection(), from, to );
			
			while ( true ) {
				double r = Math.random();
				if ( p > r ) {
					int randomForwardIndex = (int) ( r * ( network.orderedAgents.size() - i ) );
					to = network.orderedAgents.get( i + randomForwardIndex );
					
					if ( to == from ) {
						continue;
					}
					
					network.graph.addEdge( new AgentConnection(), from, to );
				}
				
				break;
			}
		}
		
		// TODO: optional connect first with last: circle
		//network.graph.addEdge( new AgentConnection(), network.orderedAgents.get( 0 ), network.orderedAgents.get( network.orderedAgents.size() - 1 ) );

		return network;
	}
	
	public static AgentNetwork createWithHubs( int hubs, IAgentFactory agentFactory ) {
		AgentNetwork network = new AgentNetwork( "ThreeHubs", false );
		network.populate( agentFactory );
		
		int lastHubIndex = 0;
		int[] hubIndices = new int[ hubs ];
		
		for ( int i = 1; i <= hubs; ++i ) {
			int hubIndex = ( int ) ( i * ( network.orderedAgents.size() / hubs ) );
			if ( 0 != network.orderedAgents.size() % hubs ) {
				// TODO: fix
				hubIndex++;
			}
			
			network.connectCompleted( lastHubIndex, hubIndex );
			
			hubIndices[ i - 1 ] = hubIndex - 1;
			lastHubIndex = hubIndex;
		}
		
		for ( int i = 0; i < hubIndices.length - 1; ++i ) {
			Agent from = network.orderedAgents.get( hubIndices[ i ] );
			
			for ( int j = i + 1; j < hubIndices.length; ++j ) {
				Agent to = network.orderedAgents.get( hubIndices[ j ] );
				
				network.graph.addEdge( new AgentConnection(), from, to );
			}
		}
		
		return network;
	}
	
	public static AgentNetwork createWithMedianHub( IAgentFactory agentFactory ) {
		AgentNetwork network = new AgentNetwork( "MedianHub", false );
		network.populate( agentFactory );
		
		int medianIndex = network.orderedAgents.size() / 2;
		Agent medianAgent = network.orderedAgents.get( medianIndex );
		
		for ( int i = 0; i < network.orderedAgents.size(); ++i ) {
			Agent from = network.orderedAgents.get( i );
			
			// no self-loops
			if ( medianAgent == from ) {
				continue;
			}
			
			network.graph.addEdge( new AgentConnection(), from, medianAgent );
		}
		
		return network;
	}
	
	public static AgentNetwork createWith3MedianHubs( IAgentFactory agentFactory ) {
		AgentNetwork network = new AgentNetwork( "3MedianHubs", false );
		network.populate( agentFactory );
		
		int medianIndex = network.orderedAgents.size() / 2;
		Agent[] medianAgents = new Agent[ 3 ];
		medianAgents[ 0 ] = network.orderedAgents.get( medianIndex - 1 );
		medianAgents[ 1 ] = network.orderedAgents.get( medianIndex );
		medianAgents[ 2 ] = network.orderedAgents.get( medianIndex + 1 );
		
		network.graph.addEdge( new AgentConnection(), medianAgents[ 0 ], medianAgents[ 1 ] );
		network.graph.addEdge( new AgentConnection(), medianAgents[ 1 ], medianAgents[ 2 ] );
		network.graph.addEdge( new AgentConnection(), medianAgents[ 2 ], medianAgents[ 0 ]);
		
		int counter = 0;
		
		Iterator<Agent> randIter = network.randomIterator( false );
		while ( randIter.hasNext() ) {
			Agent from = randIter.next();
			
			boolean selfLoop = false;
			// no self-loops
			for ( int j = 0; j < medianAgents.length; ++j ) {
				if ( medianAgents[ j ] == from ) {
					selfLoop = true;
					break;
				}
			}
			
			if ( false == selfLoop ) {
				network.graph.addEdge( new AgentConnection(), from, medianAgents[ counter % 3 ] );
				counter++;
			}
		}
		
		return network;
	}
	
	public static AgentNetwork createWithMaximumHub( IAgentFactory agentFactory ) {
		AgentNetwork network = new AgentNetwork( "MaximumHub", false );
		network.populate( agentFactory );
		
		Agent maximumAgent = network.orderedAgents.get( network.orderedAgents.size() - 1 );
		
		for ( int i = 0; i < network.orderedAgents.size(); ++i ) {
			Agent from = network.orderedAgents.get( i );
			
			// no self-loops
			if ( maximumAgent == from ) {
				continue;
			}
			
			network.graph.addEdge( new AgentConnection(), from, maximumAgent );
		}
		
		return network;
	}
	// NOTE: DOES NOT SATISFIES HYPOTHESIS 
	public static AgentNetwork createErdosRenyiConnected( double p, IAgentFactory agentFactory ) {
		AgentNetwork network = new AgentNetwork( "ErdosRenyi", true );
		network.populate( agentFactory );
		
		// NOTE: graph must be one single component - see theory for selecting p the according way

		// iterate through all EDGES and include each with a probability of p
		for ( int i = 0; i < network.orderedAgents.size() - 1; ++i) {
			for ( int j = i + 1; j <= network.orderedAgents.size() - 1; ++j ) {
				if ( p >= Math.random() ) {
					Agent from = network.orderedAgents.get( i );
					Agent to = network.orderedAgents.get( j );
					
					network.graph.addEdge( new AgentConnection(), from, to );
				}
			}
		}
		
		return network;
	}
	
	// NOTE: DOES NOT SATISFIES HYPOTHESIS
	public static AgentNetwork createBarbasiAlbertConnected( int m0, int m, IAgentFactory agentFactory ) {
		AgentNetwork network = new AgentNetwork( "BarbasiAlbert", true );
		
		Agent previousAgent = null;
		// performance: arraylist is by far better than linked list: problem is bound by random-access instead of insertion
		List<Integer> degreeDistribution = new ArrayList<Integer>();
		
		// 1. create initial m0 nodes and connect them in some way
		for ( int i = 0; i < m0; ++i) {
			Agent a = agentFactory.createAgent();
			network.orderedAgents.add( a );
			
			if ( null != previousAgent ) {
				network.graph.addEdge(new AgentConnection(), a, previousAgent );
			}
			
			previousAgent = a;
		}
		
		// 2. construct initial degree distribution
		for ( int i = 0; i < network.orderedAgents.size(); ++i) {
			Agent a = network.orderedAgents.get(i);
			
			int degree = network.graph.degree( a );
			
			for ( int j = 0; j < degree; ++j ) {
				degreeDistribution.add(i);
			}
		}
		
		Agent newAgent = null;
		List<Integer> selectedIndices = new ArrayList<Integer>();
	
		// 3. create n new nodes and attach them to m existing nodes
		while( ( newAgent = agentFactory.createAgent() ) != null ) {
			network.graph.addVertex( newAgent );
			selectedIndices.clear();
			
			// select m existing nodes, but don't add yet as it would change distribution!
			for ( int j = 0; j < m; ++j ) {
				while ( true ) {
					int distributionIndex = ( int ) ( Math.random() * degreeDistribution.size() );
					int nodeIndex = degreeDistribution.get( distributionIndex );
					Agent toAgent = network.orderedAgents.get( nodeIndex );
				
					// prevent multi-graph
					if ( false == network.graph.isNeighbor( newAgent, toAgent ) ) {
						network.graph.addEdge( new AgentConnection(), newAgent, toAgent );
						selectedIndices.add( nodeIndex );
						
						break;
					}
				}
			}

			// adjust the degree-distributions of the selected nodes
			degreeDistribution.addAll( selectedIndices );
			// extend the degree-distribution by including the newly created node
			degreeDistribution.add( network.orderedAgents.size() );
			// add new node to existing nodes
			network.orderedAgents.add( newAgent );
		}
		
		return network;
	}
	
	// NOTE: SATISFIES HYPOTHESIS IF k == 2
	public static AgentNetwork createWattsStrogatzConnected( int k, double b, IAgentFactory agentFactory ) {
		AgentNetwork network = new AgentNetwork( "WattsStrongatz", true );
		network.populate( agentFactory );
		
		// NOTE: should create ascending order AND shortcuts!
		
		// 1. connect each node to K neighbours
		for ( int i = 0; i < network.orderedAgents.size(); ++i ) {
			Agent from = network.orderedAgents.get( i );
			
			for ( int j = 1; j <= k; ++j ) {
				int neighborIndex = ( i + j ) % network.orderedAgents.size();
				Agent to = network.orderedAgents.get( neighborIndex );

				network.graph.addEdge( new AgentConnection(), from, to );
			}
		}

		// 2. rewire edges
		for ( int i = 0; i < network.orderedAgents.size(); ++i ) {
			Agent a = network.orderedAgents.get( i );
			// contains the graphNodes to which to create a new edge from node
			List<Agent> newEdges = new ArrayList<Agent>();
			// contains the edges to be removed
			List<AgentConnection> removeEdges = new ArrayList<AgentConnection>();
			
			// undirected graph => in- and out-edges are the same, thus neighborhood is always the same
			Collection<Agent> neighbors = network.graph.getNeighbors( a );
			Iterator<Agent> neighborsIter = neighbors.iterator();
			while ( neighborsIter.hasNext() ) {
				Agent neighbor = neighborsIter.next();
				
				// ignore connections to larger neighbors
				if ( neighbor.getId() > a.getId() ) {
					continue;
				}
				
				// MUST NOT be null, otherwise not neighbors
				AgentConnection edge = network.graph.findEdge( a, neighbor);
			
				if ( b >= Math.random() ) {
					Agent randomAgent = null;
					
					// pick a random node 
					while ( true ) {
						randomAgent = network.orderedAgents.get( ( int ) ( Math.random() * network.orderedAgents.size() ) );
						
						// avoid self-loop...  
						if ( randomAgent != a && 
								// ... and link-duplication ...
								null == network.graph.findEdge( a, randomAgent ) && 
								// ... and need also to check newEdges because those weren't added to the graph yet
								false == newEdges.contains( randomAgent ) ) {
							break;
						}
					}
					
					removeEdges.add( edge );
					newEdges.add( randomAgent );
				}
			}
			
			// add and remove edages to/from graph. 
			// need to be done this way otherwise concurrentmodification exception in iterator!
			for ( int j = 0; j < newEdges.size(); ++j ) {
				network.graph.removeEdge( removeEdges.get( j ) );
				network.graph.addEdge( new AgentConnection(), a, newEdges.get( j ) );
			}
		}
		
		return network;
	}
	
	private AgentNetwork(String name, boolean randomNetwork) {
		this.networkName = name;
		this.randomNetworkFlag = randomNetwork;
		
		this.orderedAgents = new ArrayList<Agent>();
		this.graph = new SparseGraph<Agent, AgentConnection>();
	}
	
	public Agent get( int i ) {
		return this.orderedAgents.get( i );
	}
	
	public int size() {
		return this.orderedAgents.size();
	}
	
	public Iterator<Agent> iterator() {
		return this.orderedAgents.iterator();
	}
	
	public Iterator<Agent> randomIterator(boolean reshuffle) {
		if ( null == this.randomOrderAgents ) {
			this.randomOrderAgents = new ArrayList<Agent>( this.orderedAgents.size() );
			for (Agent a : this.orderedAgents ) {
				this.randomOrderAgents.add( a );
			}
			
			Collections.shuffle( this.randomOrderAgents );
			
		} else {
			if ( reshuffle ) {
				Collections.shuffle( this.randomOrderAgents );
			}
		}
		
		return this.randomOrderAgents.iterator();
	}
	
	public Iterator<AgentConnection> connectionIterator() {
		return this.graph.getEdges().iterator();
	}
	
	public boolean isNeighbor(Agent a1, Agent a2) {
		return this.graph.isNeighbor( a1, a2 );
	}

	public AgentConnection getConnection( Agent a1, Agent a2 ) {
		return this.graph.findEdge( a1, a2 );
	}
	
	public List<AgentConnection> getPath( Agent a1, Agent a2 ) {
		if ( null == this.pathCalculator ) {
			this.pathCalculator = new DijkstraShortestPath<Agent, AgentConnection>( this.graph, new Transformer<AgentConnection, Number>() {
				@Override
				public Number transform( AgentConnection arg ) {
					return arg.getWeight();
				}
			} );
			
			this.pathCalculator.setMaxDistance( Double.MAX_VALUE );
		}
		
		this.pathCalculator.reset();
		
		Number distance = this.pathCalculator.getDistance( a1, a2 );
		if ( null == distance || distance.doubleValue() == Double.MAX_VALUE ) {
			return null;
		}
		
		return this.pathCalculator.getPath( a1, a2 );
	}
	
	public void reset( double consumEndow, double assetEndow ) {
		for ( Agent a : this.orderedAgents ) {
			a.reset( consumEndow, assetEndow );
		}
		
		Iterator<AgentConnection> connIter = this.connectionIterator();
		while ( connIter.hasNext() ) {
			AgentConnection c = connIter.next();
			c.reset();
		}
	}

	public boolean isRandomNetwork() {
		return this.randomNetworkFlag;
	}

	public Iterator<Agent> getNeighbors( Agent a ) {
		return this.graph.getNeighbors( a ).iterator();
	}
	
	public Agent getRandomNeighbor( Agent a ) {
		Collection<Agent> neighbors = this.graph.getNeighbors( a );
		if ( 0 == neighbors.size() ) {
			return null;
		}

		Agent[] neighborArray = neighbors.toArray( new Agent[ neighbors.size() ] );
		
		return neighborArray[ (int) (Math.random() * neighbors.size()) ];
	}
	
	public void createHistogramm() {
		int i = 0;
		
		double[] totalDegree = new double[ graph.getVertexCount() ];
		double[] inDegree = new double[ graph.getVertexCount() ];
		double[] outDegree = new double[ graph.getVertexCount() ];
		
		double totalDegreeMin = Double.MAX_VALUE;
		double totalDegreeMax = 0.0;
		
		double inDegreeMin = Double.MAX_VALUE;
		double inDegreeMax = 0.0;
		
		double outDegreeMin = Double.MAX_VALUE;
		double outDegreeMax = 0.0;
		
		Iterator<Agent> iter = graph.getVertices().iterator();
		while ( iter.hasNext() ) {
			Agent a = iter.next();
			
			totalDegree[ i ] = graph.degree( a );
			inDegree[ i ] = graph.inDegree( a );
			outDegree[ i ] = graph.outDegree( a );
			
			if ( totalDegree[ i ] < totalDegreeMin )
				totalDegreeMin = totalDegree[ i ];
			else if ( totalDegree[ i ] > totalDegreeMax )
				totalDegreeMax = totalDegree[ i ];
			
			if ( inDegree[ i ] < inDegreeMin )
				inDegreeMin = inDegree[ i ];
			else if ( inDegree[ i ] > inDegreeMax )
				inDegreeMax = inDegree[ i ];
			
			if ( outDegree[ i ] < outDegreeMin )
				outDegreeMin = outDegree[ i ];
			else if ( outDegree[ i ] > outDegreeMax )
				outDegreeMax = outDegree[ i ];
			
			++i;
		}
		
		HistogramDataset totalDegreeDS = new HistogramDataset();
		HistogramDataset inDegreeDS = new HistogramDataset();
		HistogramDataset outDegreeDS = new HistogramDataset();
		
		totalDegreeDS.addSeries("Total Degree Distribution", totalDegree, 20, totalDegreeMin, totalDegreeMax);
		inDegreeDS.addSeries("In-Degree Distribution", inDegree, 20, inDegreeMin, inDegreeMax);
		outDegreeDS.addSeries("Out-Degree Distribution", outDegree, 20, outDegreeMin, outDegreeMax);

		writeHistogramm( this.networkName + " Total-Degree Distribution", this.networkName + "_totalDegreeHist.png", totalDegreeDS );
		writeHistogramm( this.networkName + " In-Degree Distribution", this.networkName + "_inDegreeHist.png", inDegreeDS );
		writeHistogramm( this.networkName + " Out-Degree Distribution", this.networkName + "_outDegreeHist.png", outDegreeDS );
	}
	
	public ChartPanel createDegreeToOptDiagram() {
		XYSeries totalDegreeSeries = new XYSeries("Total-Degre");
		XYSeries inDegreeSeries = new XYSeries("In-Degree");
		XYSeries outDegreeSeries = new XYSeries("Out-Degree");
		
		XYSeriesCollection dataset = new XYSeriesCollection();

		Iterator<Agent> iter = graph.getVertices().iterator();
		while ( iter.hasNext() ) {
			Agent a = iter.next();
			
			totalDegreeSeries.add( a.getH(), graph.degree( a ) );
			inDegreeSeries.add( a.getH(), graph.inDegree( a ) );
			outDegreeSeries.add( a.getH(), graph.outDegree( a ) );
		}
		
		dataset.addSeries(totalDegreeSeries);
		dataset.addSeries(inDegreeSeries);
		dataset.addSeries(outDegreeSeries);
		
		JFreeChart chart = writeChart( this.networkName + " Degree To Optimism-Factor", this.networkName + "_degreeToOptFactor.PNG", dataset );
		
		return new ChartPanel( chart );
	}

	public ChartPanel createWeightsToOptDiagram() {
		XYSeries totalWeightsSeries = new XYSeries("Total-Weights");
		XYSeries inWeightsSeries = new XYSeries("In-Weights");
		XYSeries outWeightsSeries = new XYSeries("Out-Weights");
		
		XYSeriesCollection dataset = new XYSeriesCollection();
		
		Iterator<Agent> iter = graph.getVertices().iterator();
		while ( iter.hasNext() ) {
			Agent a = iter.next();
			double inWeight = 0.0;
			double outWeight = 0.0;

        	Iterator<AgentConnection> connIter = graph.getIncidentEdges( a ).iterator();
        	while ( connIter.hasNext() ) {
        		inWeight += connIter.next().getWeight();
        	}
        	
        	connIter = graph.getOutEdges( a ).iterator();
        	while ( connIter.hasNext() ) {
        		outWeight += connIter.next().getWeight();
        	}

        	inWeightsSeries.add( a.getH(), inWeight );
        	outWeightsSeries.add( a.getH(), outWeight );
        	totalWeightsSeries.add( a.getH(), inWeight + outWeight );
		}
		
		dataset.addSeries(totalWeightsSeries);
		dataset.addSeries(inWeightsSeries);
		dataset.addSeries(outWeightsSeries);
		
		String plotTitle = this.networkName + " Connection-Weights To Optimism-Factor"; 
		String xaxis = "Optimism-Factor";
		String yaxis = "Weights"; 
		JFreeChart chart = ChartFactory.createXYLineChart( plotTitle, xaxis, yaxis, dataset);
		int width = 800;
		int height = 600; 
		try {
			ChartUtilities.saveChartAsPNG(new File("img\\" + this.networkName + "_weightsToOptFactor.PNG"), chart, width, height);
		} catch (IOException e) {
		}
		
		return new ChartPanel( chart );
	}
	
	public NetworkRenderPanel getNetworkRenderingPanel( Class<? extends Layout<Agent, AgentConnection>> layoutClazz, 
			INetworkSelectionObserver selectionObserver ) {
		return new NetworkRenderPanel( this.graph, layoutClazz, selectionObserver );
	}
	
	public JPanel getWealthVisualizer() {
		return new WealthVisualizer( this.orderedAgents );
	}
	
	private void populate( IAgentFactory agentFactory ) {
		Agent a = null;
		
		while ( ( a = agentFactory.createAgent() ) != null ) {
			this.orderedAgents.add( a );
			this.graph.addVertex( a );
		}
	}
	
	private void connectCompleted(int fromIndex, int toIndex) {
		for ( int i = fromIndex; i < toIndex - 1; ++i ) {
			Agent from = this.orderedAgents.get( i );
			
			for ( int j = i + 1; j < toIndex; ++j ) {
				Agent to = this.orderedAgents.get( j );

				this.graph.addEdge( new AgentConnection(), from, to );
			}
		}
	}
	
	private JFreeChart writeHistogramm(String plotTitle, String fileName, HistogramDataset dataset) {
		String xaxis = "Degree";
		String yaxis = "Count"; 
		PlotOrientation orientation = PlotOrientation.VERTICAL; 
		boolean show = false; 
		boolean toolTips = false;
		boolean urls = false; 
		JFreeChart chart = ChartFactory.createHistogram( plotTitle, xaxis, yaxis, dataset, orientation, show, toolTips, urls);
		int width = 800;
		int height = 600; 
		try {
			ChartUtilities.saveChartAsPNG(new File("img\\" + fileName), chart, width, height);
		} catch (IOException e) {
		}
		
		return chart;
	}
	
	private JFreeChart writeChart(String plotTitle, String fileName, XYSeriesCollection dataset) {
		String xaxis = "Optimism-Factor";
		String yaxis = "Degree"; 
		JFreeChart chart = ChartFactory.createXYLineChart( plotTitle, xaxis, yaxis, dataset);
		int width = 800;
		int height = 600; 
		try {
			ChartUtilities.saveChartAsPNG(new File("img\\" + fileName), chart, width, height);
		} catch (IOException e) {
		}
		
		return chart;
	}
}
