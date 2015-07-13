package frontend.visualisation;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Arrays;
import java.util.List;

import utils.Utils;
import backend.markets.MarketType;

@SuppressWarnings("serial")
public class MarketsTimeOnlineVisualizer extends MarketsVisualizer {

	private final static int LEGEND_BOX_X = SCALA_X_WIDTH + 15;
	private final static int LEGEND_BOX_Y = 45;

	protected final static int[] MARKET_TX_COUNT = new int[ MarketType.values().length ];
	
	private final static double[] MOVING_AVG = new double[ MarketType.values().length ];
		
	private final static int WINDOW_SIZE = 100;
	private final static int MOVING_AVG_SIZE = 50;
	
	protected List<MarketType> successfulMarkets;

	public MarketsTimeOnlineVisualizer(List<MarketType> successfulMatches ) {
		super();
		
		this.successfulMarkets = successfulMatches;
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		// NOTE: Center Of Origin is TOP-LEFT => need to transform y-achsis to origin of CENTER-LEFT (x-achsis origin is already left)
		Dimension d = this.getSize();
	
		double width = d.getWidth() - SCALA_X_WIDTH;
		double height = d.getHeight() - SCALA_Y_WIDTH;
		
		String str = null;
		
		g.setFont( RENDER_FONT );
		
		// draw x-achsis sections
		for ( int i = 0; i < X_ACHSIS_GRID; i++ ) {
			double h = i / ( double ) X_ACHSIS_GRID;
			int x = ( int ) ( width * h ) + SCALA_X_WIDTH;
			str = Utils.DECIMAL_LARGEVALUES_FORMATTER.format( ( int ) ( h * this.successfulMarkets.size() ) );

			if ( i != 0 ) {
				g.drawChars( str.toCharArray(), 0, str.length(), x - 15, (int) (height + 20) );
				g.drawLine( x, SCALA_Y_WIDTH, x, (int) (height + 5) );
				
			} else {
				g.drawLine( x, SCALA_Y_WIDTH, x, (int) height );
			}
		}
		
		// draw y-achsis sections
		for ( int i = 0; i <= Y_ACHSIS_GRID; ++i ) {
			double h = i / ( double ) Y_ACHSIS_GRID;
			int y = (int) ( height - ( ( height - SCALA_Y_WIDTH ) * h ) );
			str = Utils.DECIMAL_2_DIGITS_FORMATTER.format( h );
			g.drawChars( str.toCharArray(), 0, str.length(), SCALA_X_WIDTH - 45, y + 5 );
			
			if ( i != 0 && i != Y_ACHSIS_GRID ) {
				g.drawLine( SCALA_X_WIDTH - 5, y, (int) d.width, y );
				
			} else {
				g.drawLine( SCALA_X_WIDTH, y, (int) d.width, y );
				
			}
		}

		// draw grid
		( ( Graphics2D ) g ).setStroke( new BasicStroke( 2 ) );
		// draw x-achsis
		g.drawLine( SCALA_X_WIDTH, ( int ) height, d.width, ( int ) height );
		// draw lower y-achsis 
		g.drawLine( SCALA_X_WIDTH, ( int ) height, SCALA_X_WIDTH, SCALA_Y_WIDTH );
		// draw upper y-achsis 
		g.drawLine( SCALA_X_WIDTH, ( int ) SCALA_Y_WIDTH, d.width, SCALA_Y_WIDTH );
				
		// transactions displayed on screen: subtract WINDOW_SIZE because need at least WINDOW_SIZE 
		// transactions because WINDOW_SIZE will contribute to each pixel
		int txCount = this.successfulMarkets.size() - WINDOW_SIZE;
		// calculate ratio of transactions-count to widths in pixel (how many transactions fit on one pixel)
		int txToWidthRatio = Math.max( 1, (int) (txCount / width) );
		// adaptive moving-average kernel window: for each tx-to-pixel ratio increase by MOVING_AVG_SIZE
		// this will keep the curve about the same smoothness independent of the amount of transactions
		int movingAvgWindow = MOVING_AVG_SIZE * txToWidthRatio;

		Arrays.fill( MARKET_Y, height );
		
		double xPixelPerTx = ( double ) width / ( double ) ( txCount - movingAvgWindow );
		double xAchsisCurrent = SCALA_X_WIDTH;
		double xAchsisNext = 0.0;
		
		// don't visit each transaction: do steps of txToWidthRatio, will do moving average anyway
		for ( int i = 0; i < txCount - movingAvgWindow; i += txToWidthRatio ) {
			// advance x-achsis
			xAchsisNext = xAchsisCurrent + txToWidthRatio * xPixelPerTx;
			
			Arrays.fill( MOVING_AVG, 0.0 );
			
			// calculate moving average
			for ( int avg = 0; avg < movingAvgWindow; ++avg ) {
				Arrays.fill( MARKET_TX_COUNT, 0 );
				
				// accumulate over WINDOW_SIZE transactions
				for ( int w = i + avg; w < i + WINDOW_SIZE + avg; ++w ) {
					MarketType market = this.successfulMarkets.get( w );
					MARKET_TX_COUNT[ market.ordinal() ]++;
				}
				
				for ( int m = 0; m < MarketType.values().length; ++m ) {
					MOVING_AVG[ m ] += ( double ) MARKET_TX_COUNT[ m ] / ( double ) movingAvgWindow; 
				}
			}
		
			for ( int m = 0; m < MarketType.values().length; ++m ) {
				double ratio = ( double ) MOVING_AVG[ m ] / ( double ) WINDOW_SIZE;
				double yAchsisMarketNext = height - ( ( height - SCALA_Y_WIDTH ) * ratio ) ;
				
				g.setColor( MARKET_COLORS[ m ] );
				g.drawLine( (int) xAchsisCurrent, ( int ) MARKET_Y[ m ], ( int ) xAchsisNext, ( int ) yAchsisMarketNext );
				
				MARKET_Y[ m ] = yAchsisMarketNext;
			}
				
			xAchsisCurrent = xAchsisNext;
		}
		
		this.renderLegend( g, LEGEND_BOX_X, LEGEND_BOX_Y );
	}
}
