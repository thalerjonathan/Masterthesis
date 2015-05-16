package frontend.visualisation;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import backend.markets.MarketType;
import frontend.Utils;

@SuppressWarnings("serial")
public class MarketsAccumulatedMedianVisualizer extends MarketsVisualizer {

	private final static int LEGEND_BOX_X = SCALA_X_WIDTH + 15;
	private final static int LEGEND_BOX_Y = 15;

	private final static double[] MARKET_TX_COUNT = new double[ MarketType.values().length ];
	
	private List<double[]> medianMarkets;

	public MarketsAccumulatedMedianVisualizer() {
		this(new ArrayList<>());
	}

	public MarketsAccumulatedMedianVisualizer( List<double[]> medianMarkets ) {
		super();
		
		this.medianMarkets = medianMarkets;
	} 

	public void setMarkets( List<double[]> successfulMatches ) {
		this.medianMarkets = successfulMatches;
		repaint();
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		double maxTx = 0;
		double[] totalTxInMarket = new double[ MarketType.values().length ];
		
		for ( double[] marketCounts : this.medianMarkets ) {
			for ( int i = 0; i < MarketType.values().length; ++i ) {
				totalTxInMarket[ i ] += marketCounts[ i ];
				
				if ( totalTxInMarket[ i ] >= maxTx ) {
					maxTx = totalTxInMarket[ i ];
				}
			}
		}
		
		// NOTE: Center Of Origin is TOP-LEFT => need to transform y-achsis to origin of CENTER-LEFT (x-achsis origin is already left)
		Dimension d = this.getSize();
	
		double width = d.getWidth() - SCALA_X_WIDTH;
		double height = d.getHeight() - SCALA_Y_WIDTH;
		
		String str = null;
		
		// draw x-achsis sections
		for ( int i = 0; i < X_ACHSIS_GRID; i++ ) {
			double h = i / ( double ) X_ACHSIS_GRID;
			int x = ( int ) ( width * h ) + SCALA_X_WIDTH;
			str = Utils.DECIMAL_LARGEVALUES_FORMATTER.format( ( int ) ( h * this.medianMarkets.size() ) );

			if ( i != 0 ) {
				g.drawChars( str.toCharArray(), 0, str.length(), x - 15, (int) (height + 20) );
				g.drawLine( x, 0, x, (int) (height + 5) );
				
			} else {
				g.drawLine( x, 0, x, (int) height );
			}
		}
		
		// draw y-achsis sections
		for ( int i = 0; i <= Y_ACHSIS_GRID; ++i ) {
			double h = i / ( double ) Y_ACHSIS_GRID;
			int y = (int) ( height - ( height - SCALA_Y_WIDTH ) * h );
			str = Utils.DECIMAL_LARGEVALUES_FORMATTER.format( ( int ) ( h * ( double ) maxTx ) );

			if ( i != 0 ) {
				g.drawChars( str.toCharArray(), 0, str.length(), SCALA_X_WIDTH - 45, y + 5 );
				g.drawLine( SCALA_X_WIDTH - 5, y, (int) d.width, y );
				
			} else {
				g.drawLine( SCALA_X_WIDTH, y, (int) d.width, y );
				
			}
		}
		
		Arrays.fill( MARKET_TX_COUNT, 0 );
		Arrays.fill( MARKET_Y, height );
		
		double xPixelPerTx = ( double ) width / ( double ) this.medianMarkets.size();
		double xAchsisCurrent = SCALA_X_WIDTH;
		double xAchsisNext = 0.0;
		double yPixelPerTx = ( double ) ( height - SCALA_Y_WIDTH ) / maxTx;
		
		// draw grid
		( ( Graphics2D ) g ).setStroke( new BasicStroke( 2 ) );
		// draw x-achsis
		g.drawLine( SCALA_X_WIDTH, ( int ) height, d.width, ( int ) height );
		// draw y-achsis
		g.drawLine( SCALA_X_WIDTH, ( int ) height, SCALA_X_WIDTH, 0 );
		
		for ( int i = 0; i < this.medianMarkets.size(); ++i ) {
			double[] marketCounts = this.medianMarkets.get( i );
			xAchsisNext = xAchsisCurrent + xPixelPerTx;
			
			for ( int m = 0; m < MarketType.values().length; ++m ) {
				MARKET_TX_COUNT[ m ] += marketCounts[ m ];
				double newMarketY = height - MARKET_TX_COUNT[ m ] * yPixelPerTx;
				
				g.setColor( MARKET_COLORS[ m ] );
				g.drawLine( (int) xAchsisCurrent, ( int ) MARKET_Y[ m ], ( int ) xAchsisNext, ( int ) newMarketY );

				MARKET_Y[ m ] = newMarketY;
			}
			
			xAchsisCurrent = xAchsisNext;
		}
		
		this.renderLegend( g, LEGEND_BOX_X, LEGEND_BOX_Y );
	}
}
