package frontend.networkVisualisation;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.lang.reflect.Constructor;
import java.text.DecimalFormat;
import java.util.Comparator;

import javax.swing.JPanel;

import org.apache.commons.collections15.Transformer;

import backend.agents.Agent;
import backend.agents.network.AgentConnection;
import edu.uci.ics.jung.algorithms.layout.CircleLayout;
import edu.uci.ics.jung.algorithms.layout.GraphElementAccessor;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.renderers.Renderer;

@SuppressWarnings("serial")
public class NetworkRenderPanel extends JPanel {
	private Graph<Agent, AgentConnection> graph;
	
	private boolean keepTXHighlighted;
	private VisualizationViewer<Agent, AgentConnection> visualizationViewer;
	private Class<? extends Layout<Agent, AgentConnection>> layoutClazz;
	private INetworkSelectionObserver selectionObserver;
	
	private final Shape EDGE_SHAPE = AffineTransform.getScaleInstance(3, 3).createTransformedShape( new Ellipse2D.Double(-5, -5, 10, 10 ) );
	private final BasicStroke HIGHLIGHTED_CONNECTION = new BasicStroke( 3.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f );
	private final BasicStroke NORMAL_CONNECTION = new BasicStroke( 1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f );

	public NetworkRenderPanel( Graph<Agent, AgentConnection> graph,
			Class<? extends Layout<Agent, AgentConnection>> layoutClazz, 
			INetworkSelectionObserver selectionObserver ) {
		this.graph = graph;
		
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
				if ( arg.isCantTrade() ) 
					return Color.BLACK;
				
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
		
		if ( null != this.selectionObserver ) {
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
		}
		
		this.add( this.visualizationViewer );
	}
	
	private Layout<Agent, AgentConnection> createLayout() {
		Layout<Agent, AgentConnection> layout = null;
		
		try {
			Constructor<? extends Layout<Agent, AgentConnection>> constr = this.layoutClazz.getConstructor( Graph.class );
			layout = constr.newInstance( graph );
			
		} catch ( Exception e ) { 
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