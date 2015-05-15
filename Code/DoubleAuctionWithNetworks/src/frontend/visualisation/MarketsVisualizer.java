package frontend.visualisation;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JPanel;

import backend.markets.MarketType;
import frontend.Utils;

@SuppressWarnings("serial")
public class MarketsVisualizer extends JPanel {

	private final static int SCALA_X_WIDTH = 60;
	private final static int SCALA_Y_WIDTH = 30;

	private final static int X_ACHSIS_GRID = 15;
	private final static int Y_ACHSIS_GRID = 15;
	
	private final static int LEGEND_BOX_X = SCALA_X_WIDTH + 15;
	private final static int LEGEND_BOX_Y = 15;

	private final static int[] MARKET_TX_COUNT = new int[ MarketType.values().length ];
	private final static double[] MARKET_Y = new double[ MarketType.values().length ];
	private final static double[] MOVING_AVG = new double[ MarketType.values().length ];
	
	private final static Color[] MARKET_COLORS;
	private final static Color DARK_GREEN = new Color( 0, 150, 0 );
	private final static Color DARK_CYAN = new Color(0, 180, 180);
	
	static {
		MARKET_COLORS = new Color[ MarketType.values().length ];
		MARKET_COLORS[ MarketType.ASSET_CASH.ordinal() ] =  Color.BLUE;
		MARKET_COLORS[ MarketType.LOAN_CASH.ordinal() ] = Color.RED;
		MARKET_COLORS[ MarketType.ASSET_LOAN.ordinal() ] = DARK_GREEN;
		MARKET_COLORS[ MarketType.COLLATERAL_CASH.ordinal() ] = DARK_CYAN;
	}
		
	private final static int WINDOW_SIZE = 100;
	private final static int MOVING_AVG_SIZE = 50;
	
	private List<MarketType> successfulMarkets;

	public MarketsVisualizer() {
		this( new ArrayList<>() );
	}
	
	public MarketsVisualizer( List<MarketType> successfulMatches ) {
		this.successfulMarkets = successfulMatches;
		
		this.setPreferredSize( new Dimension( (int) (this.getToolkit().getScreenSize().getWidth() / 2 ), 
				(int) (0.75 * this.getToolkit().getScreenSize().getHeight() ) ) );
		
		this.setBackground( Color.WHITE );
	}
	
	/*
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		int maxTx = 0;
		int[] totalTxInMarket = new int[ MarketType.values().length ];
		
		for ( MarketType market : this.successfulMarkets ) {
			totalTxInMarket[ market.ordinal() ]++;
			
			if ( totalTxInMarket[ market.ordinal() ] > maxTx ) {
				maxTx = totalTxInMarket[ market.ordinal() ];
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
			str = TX_COUNT_FORMATTER.format( ( int ) ( h * this.successfulMarkets.size() ) );

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
			int y = (int) (height - height * h );
			str = TX_COUNT_FORMATTER.format( ( int ) ( h * ( double ) maxTx ) );

			if ( i != 0 ) {
				g.drawChars( str.toCharArray(), 0, str.length(), SCALA_X_WIDTH - 45, y + 5 );
				g.drawLine( SCALA_X_WIDTH - 5, y, (int) d.width, y );
				
			} else {
				g.drawLine( SCALA_X_WIDTH, y, (int) d.width, y );
				
			}
		}
		
		Arrays.fill( MARKET_TX_COUNT, 0 );
		Arrays.fill( MARKET_X, SCALA_X_WIDTH );
		Arrays.fill( MARKET_Y, height );
		
		double xPixelPerTx = ( double ) width / ( double ) this.successfulMarkets.size();
		double yPixelPerTx = ( double ) height / ( double ) maxTx;
		
		( ( Graphics2D ) g ).setStroke( new BasicStroke( 2 ) );
		
		// draw grid
		( ( Graphics2D ) g ).setStroke( new BasicStroke( 2 ) );
		// draw x-achsis
		g.drawLine( SCALA_X_WIDTH, ( int ) height, d.width, ( int ) height );
		// draw y-achsis
		g.drawLine( SCALA_X_WIDTH, ( int ) height, SCALA_X_WIDTH, 0 );
		
		for ( MarketType market : this.successfulMarkets ) {
			MARKET_TX_COUNT[ market.ordinal() ]++;
			
			for ( int i = 0; i < MarketType.values().length; ++i ) {
				double newMarketX = MARKET_X[ i ] + xPixelPerTx;
				double newMarketY = height - MARKET_TX_COUNT[ i ] * yPixelPerTx;
				
				int x = ( int ) MARKET_X[ i ];
				int y = ( int ) MARKET_Y[ i ];
				
				g.setColor( MARKET_COLORS[ i ] );
				g.drawLine( x, y, ( int ) newMarketX, ( int ) newMarketY );
				
				MARKET_X[ i ] = newMarketX; 
				MARKET_Y[ i ] = newMarketY;
			}
		}
		
		// draw legend-box
		g.setColor( Color.WHITE );
		g.fillRect( LEGEND_BOX_X, LEGEND_BOX_Y, 155, 85 );
		
		// draw legend
		g.setColor( MarketsVisualizer.MARKET_COLORS[ MarketType.ASSET_CASH.ordinal() ] );
		g.drawLine( LEGEND_BOX_X + 5, LEGEND_BOX_Y + 13, LEGEND_BOX_X + 50, LEGEND_BOX_Y + 13 );
		g.setColor( Color.BLACK );
		g.drawChars( "Asset/Cash".toCharArray(), 0, "Asset/Cash".length(), LEGEND_BOX_X + 60, LEGEND_BOX_Y + 18 );
		
		g.setColor( MarketsVisualizer.MARKET_COLORS[ MarketType.LOAN_CASH.ordinal() ] );
		g.drawLine( LEGEND_BOX_X + 5, LEGEND_BOX_Y + 33, LEGEND_BOX_X + 50, LEGEND_BOX_Y + 33 );
		g.setColor( Color.BLACK );
		g.drawChars( "Loan/Cash".toCharArray(), 0, "Loan/Cash".length(), LEGEND_BOX_X + 60, LEGEND_BOX_Y + 38 );
		
		g.setColor( MarketsVisualizer.MARKET_COLORS[ MarketType.ASSET_LOAN.ordinal() ] );
		g.drawLine( LEGEND_BOX_X + 5, LEGEND_BOX_Y + 53, LEGEND_BOX_X + 50, LEGEND_BOX_Y + 53 );
		g.setColor( Color.BLACK );
		g.drawChars( "Asset/Loans".toCharArray(), 0, "Asset/Loans".length(), LEGEND_BOX_X + 60, LEGEND_BOX_Y + 58 );

		g.setColor( MarketsVisualizer.MARKET_COLORS[ MarketType.COLLATERAL_CASH.ordinal() ] );
		g.drawLine( LEGEND_BOX_X + 5, LEGEND_BOX_Y + 73, LEGEND_BOX_X + 50, LEGEND_BOX_Y + 73 );
		g.setColor( Color.BLACK );
		g.drawChars( "Collateral/Cash".toCharArray(), 0, "Collateral/Cash".length(), LEGEND_BOX_X + 60, LEGEND_BOX_Y + 78 );

		// draw border of legend-box
		( ( Graphics2D ) g ).setStroke( new BasicStroke( 1 ) );
		g.setColor( Color.BLACK );
		g.drawRect( LEGEND_BOX_X, LEGEND_BOX_Y, 155, 85 );
	}
	*/
	
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

		Arrays.fill( MARKET_Y, height );
		
		double xPixelPerTx = ( double ) width / ( double ) this.successfulMarkets.size();
		double xAchsisCurrent = SCALA_X_WIDTH;
		double xAchsisNext = 0.0;
		
		( ( Graphics2D ) g ).setStroke( new BasicStroke( 2 ) );
		
		// draw grid
		( ( Graphics2D ) g ).setStroke( new BasicStroke( 2 ) );
		// draw x-achsis
		g.drawLine( SCALA_X_WIDTH, ( int ) height, d.width, ( int ) height );
		// draw y-achsis
		g.drawLine( SCALA_X_WIDTH, ( int ) height, SCALA_X_WIDTH, 0 );
		
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
		
		// draw legend-box
		g.setColor( Color.WHITE );
		g.fillRect( LEGEND_BOX_X, LEGEND_BOX_Y, 155, 85 );
		
		// draw legend
		g.setColor( MarketsVisualizer.MARKET_COLORS[ MarketType.ASSET_CASH.ordinal() ] );
		g.drawLine( LEGEND_BOX_X + 5, LEGEND_BOX_Y + 13, LEGEND_BOX_X + 50, LEGEND_BOX_Y + 13 );
		g.setColor( Color.BLACK );
		g.drawChars( "Asset/Cash".toCharArray(), 0, "Asset/Cash".length(), LEGEND_BOX_X + 60, LEGEND_BOX_Y + 18 );
		
		g.setColor( MarketsVisualizer.MARKET_COLORS[ MarketType.LOAN_CASH.ordinal() ] );
		g.drawLine( LEGEND_BOX_X + 5, LEGEND_BOX_Y + 33, LEGEND_BOX_X + 50, LEGEND_BOX_Y + 33 );
		g.setColor( Color.BLACK );
		g.drawChars( "Loan/Cash".toCharArray(), 0, "Loan/Cash".length(), LEGEND_BOX_X + 60, LEGEND_BOX_Y + 38 );
		
		g.setColor( MarketsVisualizer.MARKET_COLORS[ MarketType.ASSET_LOAN.ordinal() ] );
		g.drawLine( LEGEND_BOX_X + 5, LEGEND_BOX_Y + 53, LEGEND_BOX_X + 50, LEGEND_BOX_Y + 53 );
		g.setColor( Color.BLACK );
		g.drawChars( "Asset/Loans".toCharArray(), 0, "Asset/Loans".length(), LEGEND_BOX_X + 60, LEGEND_BOX_Y + 58 );

		g.setColor( MarketsVisualizer.MARKET_COLORS[ MarketType.COLLATERAL_CASH.ordinal() ] );
		g.drawLine( LEGEND_BOX_X + 5, LEGEND_BOX_Y + 73, LEGEND_BOX_X + 50, LEGEND_BOX_Y + 73 );
		g.setColor( Color.BLACK );
		g.drawChars( "Collateral/Cash".toCharArray(), 0, "Collateral/Cash".length(), LEGEND_BOX_X + 60, LEGEND_BOX_Y + 78 );

		// draw border of legend-box
		( ( Graphics2D ) g ).setStroke( new BasicStroke( 1 ) );
		g.setColor( Color.BLACK );
		g.drawRect( LEGEND_BOX_X, LEGEND_BOX_Y, 155, 85 );
	}
}
