package frontend.visualisation;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import utils.Utils;
import backend.markets.MarketType;

@SuppressWarnings("serial")
public class MarketsTimeOfflineVisualizer extends MarketsVisualizer {

	private final static int LEGEND_BOX_X = SCALA_X_WIDTH + 15;
	private final static int LEGEND_BOX_Y = 45;
	
	private final static int WINDOW_SIZE = 100;
	private final static int MOVING_AVG_SIZE = 50;
	
	private int medianMarketsSize;
	private List<DoublePoint[]> preprocessedCoords;

	private class DoublePoint {
		double x;
		double y;
		
		DoublePoint( double x, double y ) {
			this.x = x;
			this.y = y;
		}
	}
	
	public MarketsTimeOfflineVisualizer() {
		this(new ArrayList<double[]>() );
	}

	public MarketsTimeOfflineVisualizer( List<double[]> medianMarkets ) {
		super();
		
		this.preprocessData( medianMarkets );
	} 
	
	public void setMarkets( List<double[]> medianMarkets ) {
		this.preprocessData( medianMarkets );
	}
	
	private synchronized void preprocessData( List<double[]> medianMarkets ) {
		this.medianMarketsSize = medianMarkets.size();
		
		// NOTE: Center Of Origin is TOP-LEFT => need to transform y-achsis to origin of CENTER-LEFT (x-achsis origin is already left)
		Dimension d = this.getPreferredSize();
	
		double width = d.getWidth() - SCALA_X_WIDTH;
		
		// transactions displayed on screen: subtract WINDOW_SIZE because need at least WINDOW_SIZE 
		// transactions because WINDOW_SIZE will contribute to each pixel
		int txCount = medianMarkets.size() - WINDOW_SIZE;
		// calculate ratio of transactions-count to widths in pixel (how many transactions fit on one pixel)
		int txToWidthRatio = Math.max( 1, (int) ( txCount / width) );
		// adaptive moving-average kernel window: for each tx-to-pixel ratio increase by MOVING_AVG_SIZE
		// this will keep the curve about the same smoothness independent of the amount of transactions
		int movingAvgWindow = MOVING_AVG_SIZE * txToWidthRatio;

		double[] MARKET_TX_COUNT = new double[ MarketType.values().length ];
		double[] MOVING_AVG = new double[ MarketType.values().length ];
		
		this.preprocessedCoords = new ArrayList<DoublePoint[]>( );
		
		// don't visit each transaction: do steps of txToWidthRatio, will do moving average anyway
		for ( int i = 0; i < txCount - movingAvgWindow; i += txToWidthRatio ) {
			Arrays.fill( MOVING_AVG, 0.0 );
			
			// calculate moving average
			for ( int avg = 0; avg < movingAvgWindow; ++avg ) {
				Arrays.fill( MARKET_TX_COUNT, 0 );
				
				// accumulate over WINDOW_SIZE transactions
				for ( int w = i + avg; w < i + WINDOW_SIZE + avg; ++w ) {
					double[] marketCounts = medianMarkets.get( w );
					
					for ( int m = 0; m < MarketType.values().length; ++m ) {
						MOVING_AVG[ m ] += ( double ) marketCounts[ m ] / ( double ) movingAvgWindow; 
					}
				}
			}
		
			DoublePoint[] relativeMarketCoords = new DoublePoint[  MarketType.values().length  ];
			
			for ( int m = 0; m < MarketType.values().length; ++m ) {
				double relativeYCoord = ( double ) MOVING_AVG[ m ] / ( double ) WINDOW_SIZE;
				double relativeXCoord = ( double ) i / ( double ) txCount;
				
				relativeMarketCoords[ m ] = new DoublePoint( relativeXCoord, relativeYCoord );
			}
			
			this.preprocessedCoords.add( relativeMarketCoords );
		}		
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
			str = Utils.DECIMAL_LARGEVALUES_FORMATTER.format( ( int ) ( h * this.medianMarketsSize ) );

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

		double[] lastAbsoluteXCoord = new double[ MarketType.values().length ];
		double[] lastAbsoluteYCoord = new double[ MarketType.values().length ];
		Arrays.fill( lastAbsoluteXCoord , SCALA_X_WIDTH );
		Arrays.fill( lastAbsoluteYCoord , height );
		
		// synchronize on preprocessed coords because preprocessData is synchronized and 
		// is working on preprocessedCoords
		synchronized( this ) {
			// don't visit each transaction: do steps of txToWidthRatio, will do moving average anyway
			for ( DoublePoint[] marketCoords : this.preprocessedCoords ) {
				for ( int m = 0; m < MarketType.values().length; ++m ) {
					DoublePoint p = marketCoords[ m ];
	
					double absoluteXCoord = SCALA_X_WIDTH + ( p.x * width );
					double absoluteYCoord = height - ( ( height - SCALA_Y_WIDTH ) * p.y );
					
					g.setColor( MARKET_COLORS[ m ] );
					g.drawLine( ( int ) lastAbsoluteXCoord[ m ], ( int ) lastAbsoluteYCoord[ m ], ( int ) absoluteXCoord, ( int ) absoluteYCoord );
					
					lastAbsoluteXCoord[ m ] = absoluteXCoord;
					lastAbsoluteYCoord[ m ] = absoluteYCoord;
				}
			}
		}
		
		this.renderLegend( g, LEGEND_BOX_X, LEGEND_BOX_Y );
	}
}
