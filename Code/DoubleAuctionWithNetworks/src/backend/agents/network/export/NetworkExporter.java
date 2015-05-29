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

		int edgeId = 0;
		Iterator<Agent> nodesIter = network.iterator();
		while( nodesIter.hasNext() ) {
			Agent source = nodesIter.next();
			
			NodeBean node = new NodeBean();
			node.setId( source.getId() );
			node.setLabel( source.getH() );
		
			graphBean.getNodes().add( node );
			
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
	}
}
