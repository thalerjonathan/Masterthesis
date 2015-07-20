package backend.agents.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections15.Transformer;

import controller.network.export.EdgeBean;
import controller.network.export.GraphBean;
import controller.network.export.NodeBean;
import utils.Utils;
import backend.agents.Agent;
import backend.agents.IAgentFactory;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseGraph;
import frontend.networkVisualisation.INetworkSelectionObserver;
import frontend.networkVisualisation.NetworkRenderPanel;
import frontend.visualisation.WealthVisualizer;

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
	private boolean fullyConnectedFlag;

	// NOTE: use only to visualize results
	public static AgentNetwork createByGraphBean( GraphBean graphBean ) {
		AgentNetwork network = new AgentNetwork( "N/A", false );
		
		HashMap<Integer, Agent> agentsById = new HashMap<Integer, Agent>();
		
		Iterator<NodeBean> beanIter = graphBean.getNodes().iterator();
		while ( beanIter.hasNext() ) {
			NodeBean node = beanIter.next();
			double h = node.getLabel();
			// note: violates the agent-creation delegation through agent-factory, but this is
			// only for visualization purposes anyway
			Agent a = new Agent( node.getId(), h );
			
			agentsById.put( a.getId(), a );
			
			network.orderedAgents.add( a );
			
			network.graph.addVertex( a );
		}
		
		Iterator<EdgeBean> edgeIter = graphBean.getEdges().iterator();
		while ( edgeIter.hasNext() ) {
			EdgeBean edge = edgeIter.next();
			
			Agent source = agentsById.get( edge.getSource() );
			Agent target = agentsById.get( edge.getTarget() );
			
			// only add if not already neighbours (undirected graph)
			if ( false == network.graph.isNeighbor( source, target ) ) {
				network.graph.addEdge( new AgentConnection(), source, target );
			}
		}
		
		return network;
	}
	
	public static AgentNetwork createFullyConnected( IAgentFactory agentFactory ) {
		AgentNetwork network = new AgentNetwork( "FullyConnected", false );
		network.fullyConnectedFlag = true;
		network.populate( agentFactory );
		network.connectCompleted( 0, network.orderedAgents.size() );
		
		return network;
	}
	
	public static AgentNetwork createAscendingConnected( IAgentFactory agentFactory ) {
		return AgentNetwork.createAscendingConnectedWithRandomShortcuts( 0.0, agentFactory );
	}
	
	public static AgentNetwork createAscendingConnectedWithRandomShortcuts( double p, IAgentFactory agentFactory ) {
		AgentNetwork network = new AgentNetwork( "AscendingConnected", p != 0.0 );
		network.populate( agentFactory );
		
		for ( int i = 0; i < network.orderedAgents.size() - 1; ++i ) {
			Agent from = network.orderedAgents.get( i );
			Agent to = network.orderedAgents.get( ( i + 1 ) % network.orderedAgents.size() );
			
			network.graph.addEdge( new AgentConnection(), from, to );
			
			// search random-neighbor with probability p
			double r = Math.random();
			// the lower p, the more unlikely it should be
			if ( p >= r ) {
				// random neighbor must satisfy:
				// 1. is different from SELF
				// 2. no double-edges between the self and neighbour
				
				// need to prevent endless-loop through counter. if used up: no neighbour
				int maxRetries = 50;
				
				while ( maxRetries > 0 ) {
					int randomForwardIndex = (int) ( Math.random() * network.orderedAgents.size() );
					to = network.orderedAgents.get( randomForwardIndex );
					
					// ommit self-loops AND double-edges
					if ( to == from || network.graph.isNeighbor( from, to ) ) {
						maxRetries--;
						continue;
					}
					
					network.graph.addEdge( new AgentConnection(), from, to );
					
					break;
				}
			}
		}
		
		return network;
	}
	
	public static AgentNetwork createAscendingConnectedWithRegularShortcuts( int n, IAgentFactory agentFactory ) {
		AgentNetwork network = new AgentNetwork( "AscendingConnectedWithShortcuts", false );
		network.populate( agentFactory );
		
		// would lead to double-edges and self-loops, avoid them at any cost
		if ( n >= network.orderedAgents.size() / 2 ) {
			n = network.orderedAgents.size() / 2;
		}
		
		for ( int i = 0; i < network.orderedAgents.size() - 1; ++i ) {
			Agent from = network.orderedAgents.get( i );
			Agent to1 = network.orderedAgents.get( ( i + 1 ) % network.orderedAgents.size() );
			Agent to2 = network.orderedAgents.get( ( i + n ) % network.orderedAgents.size() );
			
			network.graph.addEdge( new AgentConnection(), from, to1 );
			network.graph.addEdge( new AgentConnection(), from, to2 );
		}
		
		return network;
	}
	
	public static AgentNetwork createAscendingConnectedWithFullShortcuts( int n, IAgentFactory agentFactory ) {
		AgentNetwork network = new AgentNetwork( "AscendingConnectedWithFullShortcuts", false );
		network.populate( agentFactory );
		
		// would lead to double-edges and self-loops, avoid them at any cost
		if ( n >= network.orderedAgents.size() / 2 ) {
			n = network.orderedAgents.size() / 2;
		}
		
		for ( int i = 0; i < network.orderedAgents.size(); ++i ) {
			Agent from = network.orderedAgents.get( i );

			for ( int j = 0; j < n; ++j ) {
				Agent to = network.orderedAgents.get( ( i + 1 + j ) % network.orderedAgents.size() );
				network.graph.addEdge( new AgentConnection(), from, to );
			}
		}
		
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
	
	public static AgentNetwork createHalfFullyConnected( IAgentFactory agentFactory ) {
		AgentNetwork network = new AgentNetwork( "HalfFullyConnected", false );
		network.populate( agentFactory );
		
		for ( int i = 0; i < network.orderedAgents.size(); ++i ) {
			Agent from = network.orderedAgents.get( i );
			Agent to1 = network.orderedAgents.get( ( i + 1 ) % network.orderedAgents.size() );
			
			network.graph.addEdge( new AgentConnection(), from, to1 );
		}
		
		int medianIndex = network.orderedAgents.size() / 2;
		network.connectCompleted( medianIndex, network.orderedAgents.size() );
		
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

	public static AgentNetwork createSellerHas2Buyers( IAgentFactory agentFactory, int index ) {
		AgentNetwork network = AgentNetwork.createAscendingConnectedWithRandomShortcuts( 0.0, agentFactory );
		
		if ( index >= network.size() ) {
			index = 0;
		}
		
		Agent a1 = network.get( index );
		Agent a2 = network.get( index + 1 );
		Agent a3 = network.get( index + 2 );
		
		AgentConnection conn = network.graph.findEdge( a2, a3 );
		network.graph.removeEdge( conn );
		
		network.graph.addEdge( new AgentConnection(), a1, a3 );
		
		return network;
	}
	
	public static AgentNetwork createBuyerHas2Sellers( IAgentFactory agentFactory, int index ) {
		AgentNetwork network = AgentNetwork.createAscendingConnectedWithRandomShortcuts( 0.0, agentFactory );

		if ( index >= network.size() ) {
			index = 0;
		}
		
		Agent a1 = network.get( index );
		Agent a2 = network.get( index + 1 );
		Agent a3 = network.get( index + 2 );
		
		AgentConnection conn = network.graph.findEdge( a1, a2 );
		network.graph.removeEdge( conn );
		
		network.graph.addEdge( new AgentConnection(), a1, a3 );
		
		return network;
	}
	
	public static AgentNetwork create2BuyersAnd2Sellers( IAgentFactory agentFactory, int index ) {
		AgentNetwork network = AgentNetwork.createAscendingConnectedWithRandomShortcuts( 0.0, agentFactory );
		
		if ( index >= network.size() ) {
			index = 0;
		}
		
		Agent a1 = network.get( index );
		Agent a2 = network.get( index + 1 );
		Agent a3 = network.get( index + 2 );
		Agent a4 = network.get( index + 4 );
		
		AgentConnection conn = network.graph.findEdge( a2, a3 );
		network.graph.removeEdge( conn );
		
		network.graph.addEdge( new AgentConnection(), a1, a4 );
		
		return network;
	}
	
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
	
	public AgentNetwork( AgentNetwork parent ) {
		this( parent.networkName, parent.randomNetworkFlag );
		this.fullyConnectedFlag = parent.fullyConnectedFlag;
		
		// use a hashmap to reduce search overhead
		HashMap<Integer, Agent> agentsById = new HashMap<Integer, Agent>();
		
		for ( Agent a : parent.orderedAgents ) {
			Agent agentClone = (Agent) a.clone();
			this.orderedAgents.add( agentClone );
			this.graph.addVertex( agentClone );
			
			agentsById.put( agentClone.getId(), agentClone );
		}
		
		// asuming every Agent in orderedAgents is included in the graph
		for ( Agent parentAgent : parent.orderedAgents ) {
			Iterator<Agent> parentNeighbours = parent.graph.getNeighbors( parentAgent ).iterator();
			Agent copyAgent = agentsById.get( parentAgent.getId() );
			
			while ( parentNeighbours.hasNext() ) {
				Agent parentNeighbour = parentNeighbours.next();
				Agent copyNeighbour = agentsById.get( parentNeighbour.getId() );
				
				this.graph.addEdge( new AgentConnection(), copyAgent, copyNeighbour );
			}
		}
	}
	
	public String getNetworkName() {
		return networkName;
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
	
	public Iterator<Agent> randomIterator( boolean reshuffle ) {
		if ( null == this.randomOrderAgents ) {
			this.randomOrderAgents = new ArrayList<Agent>( this.orderedAgents );
			Collections.shuffle( this.randomOrderAgents );
			
		} else {
			if ( reshuffle ) {
				Collections.shuffle( this.randomOrderAgents );
			}
		}
		
		return this.randomOrderAgents.iterator();
	}
	
	public List<Agent> getOrderedList() {
		// NOTE: returns an unmodifiable list to prohibit the change of the list itself
		return Collections.unmodifiableList( this.orderedAgents );
	}
	
	public List<Agent> cloneAgents() {
		List<Agent> clonedAgents = new ArrayList<Agent>( this.orderedAgents.size() );
		for ( Agent a : this.orderedAgents ) {
			Agent clone = ( Agent ) a.clone();
			clonedAgents.add( clone );
		}
		
		return clonedAgents;
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
	
	public void reset() {
		for ( Agent a : this.orderedAgents ) {
			a.reset();
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

		return neighborArray[ (int) (Utils.THREADLOCAL_RANDOM.get().nextDouble() * neighbors.size()) ];
	}
	
	public boolean isFullyConnected() {
		return this.fullyConnectedFlag;
	}
	
	public NetworkRenderPanel getNetworkRenderingPanel( Class<? extends Layout<Agent, AgentConnection>> layoutClazz, 
			INetworkSelectionObserver selectionObserver ) {
		return new NetworkRenderPanel( this.graph, layoutClazz, selectionObserver );
	}
	
	public WealthVisualizer getWealthVisualizer() {
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

				if ( false == this.graph.isNeighbor( from, to ) ) {
					this.graph.addEdge( new AgentConnection(), from, to );
				}
			}
		}
	}
}
