package frontend.visualisation;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Arrays;
import java.util.List;

import backend.markets.MarketType;
import frontend.Utils;

@SuppressWarnings("serial")
public class MarketsTimeVisualizer extends MarketsVisualizer {

	private final static double[] MOVING_AVG = new double[ MarketType.values().length ];
		
	private final static int WINDOW_SIZE = 100;
	private final static int MOVING_AVG_SIZE = 50;
	
	public MarketsTimeVisualizer(List<MarketType> successfulMatches) {
		super(successfulMatches);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		// NOTE: Center Of Origin is TOP-LEFT => need to transform y-achsis to origin of CENTER-LEFT (x-achsis origin is already left)
		Dimension d = this.getSize();
	
		double width = d.getWidth() - SCALA_X_WIDTH;
		double height = d.getHeight() - SCALA_Y_WIDTH;
		
		String str = null;
		
		// draw x-achsis sections
		for ( int i = 0; i < X_ACHSIS_GRID; i++ ) {
			double h = i / ( double ) X_ACHSIS_GRID;
			int x = ( int ) ( width * h ) + SCALA_X_WIDTH;
			str = Utils.DECIMAL_LARGEVALUES_FORMATTER.format( ( int ) ( h * this.successfulMarkets.size() ) );

			if ( i != 0 ) {
				g.drawChars( str.toCharArray(), 0, str.length(), x - 15, (int) (height + 20) );
				g.drawLine( x, 0, x, (int) (height + 5) );
				
			} else {
				g.drawLine( x, 0, x, (int) height );
			}
		}
		
		// draw y-achsis sections
		for ( int i = 0; i < Y_ACHSIS_GRID; ++i ) {
			double h = i / ( double ) Y_ACHSIS_GRID;
			int y = (int) ( height - ( height * h ) );
			str = Utils.DECIMAL_2_DIGITS_FORMATTER.format( h );
			
			if ( i != 0 ) {
				g.drawChars( str.toCharArray(), 0, str.length(), SCALA_X_WIDTH - 45, y + 5 );
				g.drawLine( SCALA_X_WIDTH - 5, y, (int) d.width, y );
				
			} else {
				g.drawLine( SCALA_X_WIDTH, y, (int) d.width, y );
				
			}
		}

		
		( ( Graphics2D ) g ).setStroke( new BasicStroke( 2 ) );
		
		// draw grid
		( ( Graphics2D ) g ).setStroke( new BasicStroke( 2 ) );
		// draw x-achsis
		g.drawLine( SCALA_X_WIDTH, ( int ) height, d.width, ( int ) height );
		// draw y-achsis
		g.drawLine( SCALA_X_WIDTH, ( int ) height, SCALA_X_WIDTH, 0 );
		
		Arrays.fill( MARKET_Y, height );
		
		double xPixelPerTx = ( double ) width / ( double ) ( this.successfulMarkets.size() - WINDOW_SIZE - MOVING_AVG_SIZE );
		double xAchsisCurrent = SCALA_X_WIDTH;
		double xAchsisNext = 0.0;
		
		for ( int i = 0; i < this.successfulMarkets.size() - WINDOW_SIZE - MOVING_AVG_SIZE; ++i ) {
			xAchsisNext = xAchsisCurrent + xPixelPerTx;
			
			Arrays.fill( MOVING_AVG, 0.0 );
			
			for ( int avg = 0; avg < MOVING_AVG_SIZE; ++avg ) {
				Arrays.fill( MARKET_TX_COUNT, 0 );
				
				for ( int w = i + avg; w < i + WINDOW_SIZE + avg; ++w ) {
					MarketType market = this.successfulMarkets.get( w );
					MARKET_TX_COUNT[ market.ordinal() ]++;
				}
				
				for ( int m = 0; m < MarketType.values().length; ++m ) {
					MOVING_AVG[ m ] += ( double ) MARKET_TX_COUNT[ m ] / ( double ) MOVING_AVG_SIZE; 
				}
			}
		
			for ( int m = 0; m < MarketType.values().length; ++m ) {
				double ratio = ( double ) MOVING_AVG[ m ] / ( double ) WINDOW_SIZE;
				double yAchsisMarketNext = height - ( height * ratio ) ;
				
				g.setColor( MARKET_COLORS[ m ] );
				g.drawLine( (int) xAchsisCurrent, ( int ) MARKET_Y[ m ], ( int ) xAchsisNext, ( int ) yAchsisMarketNext );
				
				MARKET_Y[ m ] = yAchsisMarketNext;
			}
				
			xAchsisCurrent = xAchsisNext;
		}
		
		this.renderLegend( g );
	}
}
