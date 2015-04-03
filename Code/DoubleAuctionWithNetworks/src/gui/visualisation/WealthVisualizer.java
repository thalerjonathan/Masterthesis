package gui.visualisation;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.List;

import javax.swing.JPanel;

import agents.Agent;
import agents.AgentWithLoans;

@SuppressWarnings("serial")
public class WealthVisualizer extends JPanel {
	private final static double Y_ACHSIS_RANGE = 10.0;
	
	private final static int POINT_RADIUS = 2;
	private final static int POINT_DIAMETER = POINT_RADIUS * 2;

	private final static int LEGEND_BOX_X = 10;
	private final static int LEGEND_BOX_Y = 100;
	
	private List<Agent> orderedAgents;
	
	public WealthVisualizer(List<Agent> orderedAgents) {
		this.orderedAgents = orderedAgents;

		this.setPreferredSize( new Dimension( (int) (this.getToolkit().getScreenSize().getWidth() / 2), 
				(int) (0.75 * this.getToolkit().getScreenSize().getHeight() ) ) );
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
			String str = " " + h;
			
			g.drawLine( x, 0, x, d.height );
			g.drawChars( str.toCharArray(), 0, str.length(), x, d.height );
		}
		
		// draw points and lines of agents
		for ( int i = 0; i < orderedAgents.size(); ++i ) {
			Agent a = orderedAgents.get( i );
			double cash = a.getConumEndow();
			double assets = a.getAssetEndow();
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
				double bonds = aLoans.getLoanGiven()[0] - aLoans.getLoanTaken()[0];
				//double bonds = aLoans.getLoanGiven()[0];
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

