package gui.visualisation;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;

import javax.swing.JPanel;

import agents.Agent;

@SuppressWarnings("serial")
public class WealthVisualizer extends JPanel {
	private final static int X_ACHSIS_GRID = 10;
	private final static double Y_ACHSIS_RANGE = 10.0;
	
	private final static int POINT_RADIUS = 3;
	private final static int POINT_DIAMETER = POINT_RADIUS * 2;

	private final static int LEGEND_BOX_X = 45;
	private final static int LEGEND_BOX_Y = 100;
	
	private final static int SCALA_WIDTH = 30;
	
	private final static Color DARK_GREEN = new Color( 0, 150, 0 );
	
	private List<Agent> orderedAgents;
	
	public WealthVisualizer(List<Agent> orderedAgents) {
		this.orderedAgents = orderedAgents;

		this.setPreferredSize( new Dimension( (int) (this.getToolkit().getScreenSize().getWidth() / 2), 
				(int) (0.75 * this.getToolkit().getScreenSize().getHeight() ) ) );
		this.setBackground( Color.WHITE );
	}
	
	public void setAgents( List<Agent> orderedAgents ) {
		this.orderedAgents = orderedAgents;
		this.repaint();
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
		
		double width = d.getWidth() - SCALA_WIDTH;
		double yHalf = d.height / 2.0;

		// draw grid
		g.drawLine( SCALA_WIDTH, ( int ) yHalf, d.width, ( int ) yHalf );
		for ( int i = 0; i < X_ACHSIS_GRID; i++ ) {
			double h = i / ( double ) X_ACHSIS_GRID;
			int x = ( int ) ( width * h ) + SCALA_WIDTH;
			String str = " " + h;
			
			g.drawLine( x, 0, x, d.height );
			g.drawChars( str.toCharArray(), 0, str.length(), x, d.height - 2 );
		}
		
		// draw scala 
		g.drawChars( "0.0".toCharArray(), 0, "0.0".length(), 5, (int) yHalf + 5 );
		
		for ( int i = 1; i < 10; i++ ) {
			double r = i / Y_ACHSIS_RANGE;
			int yPos = ( int ) ( yHalf - ( yHalf * r ) );
			int yNeg = ( int ) ( yHalf + ( yHalf * r ) );
			String strPos = "" + ( r * Y_ACHSIS_RANGE );
			String strNeg = "-" + ( r * Y_ACHSIS_RANGE );
			
			g.drawLine( SCALA_WIDTH, yPos, d.width, yPos );
			g.drawLine( SCALA_WIDTH, yNeg, d.width, yNeg );
			
			g.drawChars( strPos.toCharArray(), 0, strPos.length(), 5, yPos + 5 );
			g.drawChars( strNeg.toCharArray(), 0, strNeg.length(), 5, yNeg + 5 );
		}
		
		( ( Graphics2D ) g ).setStroke( new BasicStroke( 2 ) );
		
		// draw points and lines of agents
		for ( int i = 0; i < orderedAgents.size(); ++i ) {
			Agent a = orderedAgents.get( i );
			double cash = a.getConumEndow();
			double assets = a.getAssetEndow();
			double optimism = a.getH();
			//double bonds = aLoans.getLoanGiven()[0];
			double unpledgedAssets = a.getFreeAssetEndow(); //assets - a.getLoanTaken();
			double bonds = a.getLoanGiven() - a.getLoanTaken();

			int x = ( int ) ( width * optimism ) + SCALA_WIDTH;
			int yCash = ( int ) ( yHalf - ( yHalf  * ( cash / Y_ACHSIS_RANGE ) ) );
			int yAssets = ( int ) ( yHalf - ( yHalf  * ( assets / Y_ACHSIS_RANGE ) ) );
			int yBonds = ( int ) ( yHalf - ( yHalf  * ( bonds / Y_ACHSIS_RANGE ) ) );
			int yUnpledged = ( int ) ( yHalf - ( yHalf  * ( unpledgedAssets / Y_ACHSIS_RANGE ) ) );

			g.setColor( Color.BLUE );
			if ( i > 0 )
				g.drawLine( lastX, lastYCash, x, yCash );
			g.fillOval( x - POINT_RADIUS, yCash - POINT_RADIUS, POINT_DIAMETER, POINT_DIAMETER );
			
			g.setColor( Color.YELLOW );
			if ( i > 0 )
				g.drawLine( lastX, lastYAsset, x, yAssets );
			g.fillOval( x - POINT_RADIUS, yAssets - POINT_RADIUS, POINT_DIAMETER, POINT_DIAMETER );

			g.setColor( Color.RED );
			if ( i > 0 )
				g.drawLine( lastX, lastYBonds, x, yBonds );
			g.fillOval( x - POINT_RADIUS, yBonds - POINT_RADIUS, POINT_DIAMETER, POINT_DIAMETER );
			
			g.setColor( DARK_GREEN );
			if ( i > 0 )
				g.drawLine( lastX, lastYUnpledged, x, yUnpledged );
			g.fillOval( x - POINT_RADIUS, yUnpledged - POINT_RADIUS, POINT_DIAMETER, POINT_DIAMETER );
			
			lastYCash = yCash;
			lastYAsset = yAssets;
			lastYBonds = yBonds;
			lastYUnpledged = yUnpledged;
			
			lastX = x;
		}

		// draw legend-box
		g.setColor( Color.WHITE );
		g.fillRect( LEGEND_BOX_X, d.height - LEGEND_BOX_Y, 155, 85 );

		// draw legend
		g.setColor( Color.BLUE );
		g.drawLine( LEGEND_BOX_X + 5, d.height - LEGEND_BOX_Y + 13, LEGEND_BOX_X + 50, d.height - LEGEND_BOX_Y + 13 );
		g.setColor( Color.BLACK );
		g.drawChars( "Cash".toCharArray(), 0, "Cash".length(), LEGEND_BOX_X + 60, d.height - LEGEND_BOX_Y + 18 );
		
		g.setColor( Color.YELLOW );
		g.drawLine( LEGEND_BOX_X + 5, d.height - LEGEND_BOX_Y + 33, LEGEND_BOX_X + 50, d.height - LEGEND_BOX_Y + 33 );
		g.setColor( Color.BLACK );
		g.drawChars( "Assets".toCharArray(), 0, "Assets".length(), LEGEND_BOX_X + 60, d.height - LEGEND_BOX_Y + 38 );
		
		g.setColor( Color.RED );
		g.drawLine( LEGEND_BOX_X + 5, d.height - LEGEND_BOX_Y + 53, LEGEND_BOX_X + 50, d.height - LEGEND_BOX_Y + 53 );
		g.setColor( Color.BLACK );
		g.drawChars( "Bonds".toCharArray(), 0, "Bonds".length(), LEGEND_BOX_X + 60, d.height - LEGEND_BOX_Y + 58 );
		
		g.setColor( DARK_GREEN );
		g.drawLine( LEGEND_BOX_X + 5, d.height - LEGEND_BOX_Y + 73, LEGEND_BOX_X + 50, d.height - LEGEND_BOX_Y + 73 );
		g.setColor( Color.BLACK );
		g.drawChars( "free Assets".toCharArray(), 0, "free Assets".length(), LEGEND_BOX_X + 60, d.height - LEGEND_BOX_Y + 78 );
		
		// draw border of legend-box
		( ( Graphics2D ) g ).setStroke( new BasicStroke( 1 ) );
		g.setColor( Color.BLACK );
		g.drawRect( LEGEND_BOX_X, d.height - LEGEND_BOX_Y, 155, 85 );
	}
}

