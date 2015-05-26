package backend.agents.network;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

import backend.agents.Agent;

public class NetworkExporter {

	public static void exportAsGEXF( AgentNetwork network, String fileName ) {
		BufferedWriter out = null;

		try {
			out = new BufferedWriter( new FileWriter( new File( fileName ) ) );
			out.write( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" );
			out.write( "<gexf xmlns=\"http://www.gexf.net/1.2draft\" version=\"1.2\">" );
			out.write( "<graph mode=\"static\" defaultedgetype=\"directed\">");
			
			out.write( "<nodes>");
			Iterator<Agent> nodesIter = network.iterator();
			while( nodesIter.hasNext() ) {
				Agent a = nodesIter.next();
				out.write( "<node id=\"" + a.getId() + "\" label=\"" + a.getH() + "\" />" );
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
	}
}
