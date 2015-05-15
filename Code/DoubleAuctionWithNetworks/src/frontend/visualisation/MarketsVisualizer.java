package frontend.visualisation;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;

import javax.swing.JPanel;

import backend.markets.MarketType;

@SuppressWarnings("serial")
public abstract class MarketsVisualizer extends JPanel {

	protected final static int SCALA_X_WIDTH = 60;
	protected final static int SCALA_Y_WIDTH = 30;

	protected final static int X_ACHSIS_GRID = 15;
	protected final static int Y_ACHSIS_GRID = 10;
	
	protected final static int[] MARKET_TX_COUNT = new int[ MarketType.values().length ];
	protected final static double[] MARKET_Y = new double[ MarketType.values().length ];
	
	protected final static Color[] MARKET_COLORS;
	protected final static Color DARK_GREEN = new Color( 0, 150, 0 );
	protected final static Color DARK_CYAN = new Color(0, 180, 180);
	
	static {
		MARKET_COLORS = new Color[ MarketType.values().length ];
		MARKET_COLORS[ MarketType.ASSET_CASH.ordinal() ] =  Color.BLUE;
		MARKET_COLORS[ MarketType.LOAN_CASH.ordinal() ] = Color.RED;
		MARKET_COLORS[ MarketType.ASSET_LOAN.ordinal() ] = DARK_GREEN;
		MARKET_COLORS[ MarketType.COLLATERAL_CASH.ordinal() ] = DARK_CYAN;
	}
	
	protected List<MarketType> successfulMarkets;

	public MarketsVisualizer( List<MarketType> successfulMatches ) {
		this.successfulMarkets = successfulMatches;
		
		this.setPreferredSize( new Dimension( (int) (this.getToolkit().getScreenSize().getWidth() / 2 ), 
				(int) (0.75 * this.getToolkit().getScreenSize().getHeight() ) ) );
		
		this.setBackground( Color.WHITE );
	}
	
	protected void renderLegend( Graphics g, int topX, int topY ) {
		// draw legend-box
		g.setColor( Color.WHITE );
		g.fillRect( topX, topY, 155, 85 );
		
		// draw legend
		g.setColor( MarketsAccumulatedVisualizer.MARKET_COLORS[ MarketType.ASSET_CASH.ordinal() ] );
		g.drawLine( topX + 5, topY + 13, topX + 50, topY + 13 );
		g.setColor( Color.BLACK );
		g.drawChars( "Asset/Cash".toCharArray(), 0, "Asset/Cash".length(), topX + 60, topY + 18 );
		
		g.setColor( MarketsAccumulatedVisualizer.MARKET_COLORS[ MarketType.LOAN_CASH.ordinal() ] );
		g.drawLine( topX + 5, topY + 33, topX + 50, topY + 33 );
		g.setColor( Color.BLACK );
		g.drawChars( "Loan/Cash".toCharArray(), 0, "Loan/Cash".length(), topX + 60, topY + 38 );
		
		g.setColor( MarketsAccumulatedVisualizer.MARKET_COLORS[ MarketType.ASSET_LOAN.ordinal() ] );
		g.drawLine( topX + 5, topY + 53, topX + 50, topY + 53 );
		g.setColor( Color.BLACK );
		g.drawChars( "Asset/Loans".toCharArray(), 0, "Asset/Loans".length(), topX + 60, topY + 58 );

		g.setColor( MarketsAccumulatedVisualizer.MARKET_COLORS[ MarketType.COLLATERAL_CASH.ordinal() ] );
		g.drawLine( topX + 5, topY + 73, topX + 50, topY + 73 );
		g.setColor( Color.BLACK );
		g.drawChars( "Collateral/Cash".toCharArray(), 0, "Collateral/Cash".length(), topX + 60, topY + 78 );

		// draw border of legend-box
		( ( Graphics2D ) g ).setStroke( new BasicStroke( 1 ) );
		g.setColor( Color.BLACK );
		g.drawRect( topX, topY, 155, 85 );
	}
}
