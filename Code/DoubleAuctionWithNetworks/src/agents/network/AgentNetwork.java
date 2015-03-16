package agents.network;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
import agents.AgentWithLoans;
import agents.IAgentFactory;
import edu.uci.ics.jung.algorithms.layout.CircleLayout;
import edu.uci.ics.jung.algorithms.layout.GraphElementAccessor;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseGraph;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.renderers.Renderer;

public class AgentNetwork {
	private String networkName;
	private Graph<Agent, AgentConnection> graph;
	// need to keep track of agents in the insertion-order by ourself,
	// graph won't store them in this order and extracting them and transforming
	// to array and sorting is a bit overhead
	private List<Agent> orderedAgents;
	private List<Agent> randomOrderAgents;
	
	private DijkstraShortestPath<Agent, AgentConnection> pathCalculator;
	
	// NOTE: SATISFIES HYPOTHESIS
	public static AgentNetwork createFullyConnected( IAgentFactory agentFactory ) {
		AgentNetwork network = new AgentNetwork("FullyConnected");
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
		AgentNetwork network = new AgentNetwork("AscendingConnected");
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
		AgentNetwork network = new AgentNetwork("ThreeHubs");
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
	
	// NOTE: DOES NOT SATISFIES HYPOTHESIS 
	public static AgentNetwork createErdosRenyiConnected( double p, IAgentFactory agentFactory ) {
		AgentNetwork network = new AgentNetwork("ErdosRenyi");
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
		AgentNetwork network = new AgentNetwork("BarbasiAlbert");
		
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
		AgentNetwork network = new AgentNetwork("WattsStrongatz");
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
	
	private AgentNetwork(String name) {
		this.networkName = name;
		
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
	
	public Iterator<Agent> randomIterator() {
		if ( null == this.randomOrderAgents ) {
			this.randomOrderAgents = new ArrayList<Agent>( this.orderedAgents.size() );
			for (Agent a : this.orderedAgents ) {
				this.randomOrderAgents.add( a );
			}
		}
		
		Collections.shuffle( this.randomOrderAgents ); 
		
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
	
	public NetworkRenderPanel getNetworkRenderingPanel( Class<? extends Layout<Agent, AgentConnection>> layoutClazz, INetworkSelectionObserver selectionObserver ) {
		return new NetworkRenderPanel( layoutClazz, selectionObserver );
	}
	
	public JPanel getWealthVisualizer() {
		return new WealthVisualizer();
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
	
	@SuppressWarnings("serial")
	private class WealthVisualizer extends JPanel {
		private final static double Y_ACHSIS_RANGE = 10.0;
		
		private final static int POINT_RADIUS = 2;
		private final static int POINT_DIAMETER = POINT_RADIUS * 2;

		private final static int LEGEND_BOX_X = 10;
		private final static int LEGEND_BOX_Y = 100;
		
		public WealthVisualizer() {
			this.setPreferredSize( new Dimension( 640, 300 ) );
			this.setBackground( Color.WHITE );
		}
		
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			
			int lastX = 0;
			int lastYCash = 0;
			int lastYAsset = 0;
			int lastYBonds = 0;
			int lastYUnpledged = 0;
			
			// NOTE: Center Of Origin is TOP-LEFT => need to transform y-achsis to origin of CENTER-LEFT (x-achsis origin is already left)
			Dimension d = this.getSize();
			double yHalf = d.height / 2.0;
	
			// draw grid 
			g.drawLine( 0, ( int ) yHalf, d.width, ( int ) yHalf ); 
			for ( int i = 0; i < 10; i++ ) {
				double h = i / 10.0;
				int x = ( int ) ( d.width * h );
				String str = "" + h;
				
				g.drawLine( x, 0, x, d.height );
				g.drawChars( str.toCharArray(), 0, str.length(), x, d.height );
			}
			
			// draw points and lines of agents
			for ( int i = 0; i < orderedAgents.size(); ++i ) {
				Agent a = orderedAgents.get( i );
				double cash = a.getCE();
				double assets = a.getAE();
				double optimism = a.getH();
				
				int x = ( int ) ( d.width * optimism );
				int yCash = ( int ) ( yHalf - ( yHalf  * ( cash / Y_ACHSIS_RANGE ) ) );
				int yAssets = ( int ) ( yHalf - ( yHalf  * ( assets / Y_ACHSIS_RANGE ) ) );
				
				g.setColor( Color.BLUE );
				if ( i > 0 )
					g.drawLine( lastX, lastYCash, x, yCash );
				g.fillOval( x - POINT_RADIUS, yCash - POINT_RADIUS, POINT_DIAMETER, POINT_DIAMETER );
				
				g.setColor( Color.GREEN );
				if ( i > 0 )
					g.drawLine( lastX, lastYAsset, x, yAssets );
				g.fillOval( x - POINT_RADIUS, yAssets - POINT_RADIUS, POINT_DIAMETER, POINT_DIAMETER );
				
				// TODO: replace by dynamic-binding: use visitor pattern
				if ( a instanceof AgentWithLoans ) {
					AgentWithLoans aLoans = ( AgentWithLoans ) a;
					//double bonds = aLoans.getLoanGiven()[0] - aLoans.getLoanTaken()[0];
					double bonds = aLoans.getLoanGiven()[0];
					double unpledgedAssets = assets - aLoans.getLoanTaken()[0];
					
					int yBonds = ( int ) ( yHalf - ( yHalf  * ( bonds / Y_ACHSIS_RANGE ) ) );
					int yUnpledged = ( int ) ( yHalf - ( yHalf  * ( unpledgedAssets / Y_ACHSIS_RANGE ) ) );
					
					g.setColor( Color.RED );
					if ( i > 0 )
						g.drawLine( lastX, lastYBonds, x, yBonds );
					g.fillOval( x - POINT_RADIUS, yBonds - POINT_RADIUS, POINT_DIAMETER, POINT_DIAMETER );
					
					g.setColor( Color.CYAN );
					if ( i > 0 )
						g.drawLine( lastX, lastYUnpledged, x, yUnpledged );
					g.fillOval( x - POINT_RADIUS, yUnpledged - POINT_RADIUS, POINT_DIAMETER, POINT_DIAMETER );
					
					lastYBonds = yBonds;
					lastYUnpledged = yUnpledged;
				}
				
				lastX = x;
				lastYCash = yCash;
				lastYAsset = yAssets;
			}

			// draw legend-box
			g.setColor( Color.WHITE );
			g.fillRect( LEGEND_BOX_X, d.height - LEGEND_BOX_Y, 155, 85 );
			g.setColor( Color.BLACK );
			g.drawRect( LEGEND_BOX_X, d.height - LEGEND_BOX_Y, 155, 85 );
			
			// draw legend
			g.setColor( Color.BLUE );
			g.drawLine( LEGEND_BOX_X + 5, d.height - LEGEND_BOX_Y + 15, 50, d.height - LEGEND_BOX_Y + 15 );
			g.setColor( Color.BLACK );
			g.drawChars( "cash".toCharArray(), 0, "cash".length(), 60, d.height - LEGEND_BOX_Y + 18 );
			
			g.setColor( Color.GREEN );
			g.drawLine( LEGEND_BOX_X + 5, d.height - LEGEND_BOX_Y + 35, 50, d.height - LEGEND_BOX_Y + 35 );
			g.setColor( Color.BLACK );
			g.drawChars( "assets".toCharArray(), 0, "assets".length(), 60, d.height - LEGEND_BOX_Y + 38 );
			
			g.setColor( Color.RED );
			g.drawLine( LEGEND_BOX_X + 5, d.height - LEGEND_BOX_Y + 55, 50, d.height - LEGEND_BOX_Y + 55 );
			g.setColor( Color.BLACK );
			g.drawChars( "bonds".toCharArray(), 0, "bonds".length(), 60, d.height - LEGEND_BOX_Y + 58 );
			
			g.setColor( Color.CYAN );
			g.drawLine( LEGEND_BOX_X + 5, d.height - LEGEND_BOX_Y + 75, 50, d.height - LEGEND_BOX_Y + 75 );
			g.setColor( Color.BLACK );
			g.drawChars( "unpledged assets".toCharArray(), 0, "unpledged assets".length(), 60, d.height - LEGEND_BOX_Y + 78 );
		}
	}

	public interface INetworkSelectionObserver {
		public void agentSeleted( AgentSelectedEvent agentSelectedEvent );
		public void connectionSeleted( ConnectionSelectedEvent connSelectedEvent );
		
	}
	
	public class SelectionEvent {
		private boolean ctrlDownFlag;
		
		public SelectionEvent( boolean b ) {
			this.ctrlDownFlag = b;
		}

		public boolean isCtrlDownFlag() {
			return ctrlDownFlag;
		}
	}
	
	public class AgentSelectedEvent extends SelectionEvent {
		private Agent selectedAgent;
		
		public AgentSelectedEvent( boolean b, Agent a ) {
			super(b);

			this.selectedAgent = a;
		}

		public Agent getSelectedAgent() {
			return selectedAgent;
		}
	}
	
	public class ConnectionSelectedEvent extends SelectionEvent {
		private AgentConnection selectedConnection;
		
		public ConnectionSelectedEvent(boolean b, AgentConnection c ) {
			super(b);
			
			this.selectedConnection = c;
		}

		public AgentConnection getSelectedConnection() {
			return selectedConnection;
		}
	}
	
	@SuppressWarnings("serial")
	public class NetworkRenderPanel extends JPanel {
		private boolean keepTXHighlighted;
		private VisualizationViewer<Agent, AgentConnection> visualizationViewer;
		private Class<? extends Layout<Agent, AgentConnection>> layoutClazz;
		private INetworkSelectionObserver selectionObserver;
		
		private final Shape EDGE_SHAPE = AffineTransform.getScaleInstance(3, 3).createTransformedShape( new Ellipse2D.Double(-5, -5, 10, 10 ) );
		private final BasicStroke HIGHLIGHTED_CONNECTION = new BasicStroke( 3.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f );
		private final BasicStroke NORMAL_CONNECTION = new BasicStroke( 1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f );
	
		private NetworkRenderPanel( Class<? extends Layout<Agent, AgentConnection>> layoutClazz, INetworkSelectionObserver selectionObserver ) {
			this.layoutClazz = layoutClazz;
			this.selectionObserver = selectionObserver;
			this.keepTXHighlighted = false;
			this.initVisualizationViewer();
		}
		
		public void setKeepTXHighlighted( boolean flag ) {
			this.keepTXHighlighted = flag;
		}
		
		private void initVisualizationViewer() {
			final DecimalFormat df = new DecimalFormat("#.##");
			Layout<Agent, AgentConnection> layout = this.createLayout();
			
			this.visualizationViewer = new VisualizationViewer<Agent, AgentConnection>( layout );
			
			this.visualizationViewer.getRenderer().getVertexLabelRenderer().setPosition(Renderer.VertexLabel.Position.CNTR);
			this.visualizationViewer.getRenderContext().setVertexLabelTransformer(new Transformer<Agent, String>() {
				@Override
				public String transform( Agent arg ) {	
					return "" + df.format( arg.getH() );
				}
			});

			this.visualizationViewer.getRenderContext().setVertexFillPaintTransformer( new Transformer<Agent, Paint>() {
				@Override
				public Paint transform( Agent arg ) {
					if ( arg.isHighlighted() )
						return Color.BLUE;
					
					return Color.RED;
				}
			});
			
			this.visualizationViewer.getRenderContext().setVertexShapeTransformer(new Transformer<Agent, Shape>(){
				@Override
				public Shape transform(Agent arg0) {
	                return NetworkRenderPanel.this.EDGE_SHAPE;
				}
	        });
			
			this.visualizationViewer.getRenderContext().setEdgeDrawPaintTransformer( new Transformer<AgentConnection, Paint>() {
				@Override
				public Paint transform(AgentConnection arg) {
					if ( false == NetworkRenderPanel.this.keepTXHighlighted && arg.isHighlighted() )
						return Color.GREEN;
					
					// when weight is different from DOUBLE.MAX then this edge has a successful TX
					if ( NetworkRenderPanel.this.keepTXHighlighted && arg.getWeight() != Double.MAX_VALUE )
						return Color.GREEN;
					
					return Color.BLACK;
				}
			});
			
			this.visualizationViewer.getRenderContext().setEdgeStrokeTransformer( new Transformer<AgentConnection, Stroke>() {
				@Override
				public Stroke transform(AgentConnection arg) {
					if ( arg.isHighlighted() )
						return HIGHLIGHTED_CONNECTION;
					
					return NORMAL_CONNECTION;
				}
			});
			
			this.visualizationViewer.setForeground(Color.white);
			
			this.visualizationViewer.addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent arg) {
					GraphElementAccessor<Agent, AgentConnection> pickSupport = visualizationViewer.getPickSupport();
			        if(pickSupport != null) {
			        	Agent a = pickSupport.getVertex(visualizationViewer.getGraphLayout(), arg.getX(), arg.getY());
			            if(a != null) {
			            	NetworkRenderPanel.this.selectionObserver.agentSeleted( new AgentSelectedEvent( arg.isControlDown(), a) );
			            } else {
			            	AgentConnection conn = pickSupport.getEdge(visualizationViewer.getGraphLayout(), arg.getX(), arg.getY());
			            	if ( null != conn ) {
			            		NetworkRenderPanel.this.selectionObserver.connectionSeleted( new ConnectionSelectedEvent( arg.isControlDown(), conn ) );
			            	}
			            }
			        }
				}
	        } );
			
			this.add(this.visualizationViewer);
		}
		
		private Layout<Agent, AgentConnection> createLayout() {
			Layout<Agent, AgentConnection> layout = null;
			
			try {
				Constructor<? extends Layout<Agent, AgentConnection>> constr = this.layoutClazz.getConstructor( Graph.class );
				layout = constr.newInstance( graph );
				
			} catch (InstantiationException | IllegalAccessException | NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
				return null;
			}
			
			if ( this.layoutClazz.equals( CircleLayout.class ) ) {
				( ( CircleLayout<Agent, AgentConnection> ) layout ).setVertexOrder(new Comparator<Agent>() {
					@Override
					public int compare(Agent arg0, Agent arg1 ) {
						return arg0.getId() - arg1.getId();
					}			
				} );
			}
			
	        return layout;
		}
	}
}
