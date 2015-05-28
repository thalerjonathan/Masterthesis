package backend.agents.network.export;

import java.io.File;
import java.util.Iterator;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import backend.agents.Agent;
import backend.agents.network.AgentNetwork;

public class NetworkExporter {

	public static GraphBean createGraphBean( AgentNetwork network ) {
		GraphBean graphBean = new GraphBean();
		
		Iterator<Agent> nodesIter = network.iterator();
		while( nodesIter.hasNext() ) {
			Agent a = nodesIter.next();
			
			NodeBean node = new NodeBean();
			node.setId( a.getId() );
			node.setLabel( a.getH() );
		
			graphBean.getNodes().add( node );
		}
		
		int edgeId = 0;
		nodesIter = network.iterator();
		while( nodesIter.hasNext() ) {
			Agent source = nodesIter.next();
			
			Iterator<Agent> neighborsIter = network.getNeighbors( source );
			while( neighborsIter.hasNext() ) {
				Agent target = neighborsIter.next();
				
				EdgeBean edge = new EdgeBean();
				edge.setId( edgeId );
				edge.setSource( source.getId() );
				edge.setTarget( target.getId() );
				
				graphBean.getEdges().add( edge );
				
				edgeId++;
			}
		}
		
		return graphBean;
	}
	
	public static void exportAsGEXF( AgentNetwork network, String fileName ) {
		GraphBean graphBean = createGraphBean( network );
		GexfBean gefxBean = new GexfBean(); 
		gefxBean.setGraph( graphBean );
		
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance( GexfBean.class );
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
			 
		    jaxbMarshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, true );
		    jaxbMarshaller.marshal( gefxBean, new File( fileName  ) );
		    
		} catch (JAXBException e) {
			e.printStackTrace();
		}
		
		/*
		BufferedWriter out = null;

		try {
			out = new BufferedWriter( new FileWriter( new File( fileName ) ) );
			out.write( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" );
			out.write( "<gexf xmlns=\"http://www.gexf.net/1.2draft\" version=\"1.2\">" );
			out.write( "<graph mode=\"static\" defaultedgetype=\"undirected\">");
			
			out.write( "<nodes>");
			Iterator<Agent> nodesIter = network.iterator();
			while( nodesIter.hasNext() ) {
				Agent a = nodesIter.next();
				out.write( "<node id=\"" + a.getId() + "\" label=\"" + Utils.DECIMAL_3_DIGITS_FORMATTER.format( a.getH() ) + "\" />" );
			}
			out.write( "</nodes>");
			
			out.write( "<edges>");
			int edgeId = 0;
			nodesIter = network.iterator();
			while( nodesIter.hasNext() ) {
				Agent source = nodesIter.next();
				
				Iterator<Agent> neighborsIter = network.getNeighbors( source );
				while( neighborsIter.hasNext() ) {
					Agent target = neighborsIter.next();
					out.write( "<edge id=\"" + edgeId + "\" source=\"" + source.getId() + "\" target=\"" + target.getId() + "\" />" );
					edgeId++;
				}
			}
			out.write( "</edges>");
			
			out.write( "</graph>");
			out.write( "</gexf>");
			out.flush();
			
		} catch (IOException e) {
			e.printStackTrace();
			
		} finally {
			if ( out != null ) {
				try {
					out.close();
				} catch (IOException e) {
				}
			}
		}
		*/
	}
}
