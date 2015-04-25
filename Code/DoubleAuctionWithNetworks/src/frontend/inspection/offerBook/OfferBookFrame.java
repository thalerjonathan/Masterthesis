package frontend.inspection.offerBook;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import backend.agents.Agent;
import backend.markets.MarketType;
import backend.markets.Markets;
import backend.offers.AskOffering;
import backend.offers.BidOffering;
import frontend.agentInfo.AgentInfoPanel;

@SuppressWarnings("serial")
public class OfferBookFrame extends JFrame {
	private JButton refreshButton;
	private JButton cloneButton;
	//private JButton visParetoFrontiersButton;
	
	private JSpinner agentIndexSpinner;

	private JTabbedPane marketTabPane;
	
	private OffersTable[] askOffersBookTable;
	private OffersTable[] bidOffersBookTable;
	
	private AgentInfoPanel agentInfoPanel;
	
	private OfferBook parent;
	
	// NOTE: used for cloning
	public OfferBookFrame( OfferBook parent, int agentIndex, int tabIndex ) {
		super( "Offer-Book" );

		this.parent = parent;
		
		this.createControls( agentIndex, tabIndex );
		
		this.setResizable(false);
		this.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		
		this.setPreferredSize( new Dimension( 550, 400 ) );
		
		this.pack();
	}

	public void refresh() {
		int agentIndex = (int) this.agentIndexSpinner.getValue();
		Agent a = this.parent.getAgents().get( agentIndex );
		
		this.agentInfoPanel.setAgent( a );
		
		AskOffering[] askOfferings = a.getBestAskOfferings();
		BidOffering[] bidOfferings = a.getBestBidOfferings();
		
		int numMarkets = 3;
		
		// clear previously set data
		for ( int i = 0; i < numMarkets; ++i ) {
			this.askOffersBookTable[ i ].clearAll();
			this.bidOffersBookTable[ i ].clearAll();
		}
		
		for ( int i = 0; i < Markets.NUMMARKETS; ++i ) {
			this.askOffersBookTable[ i ].addOffering( askOfferings[ i ] );
			this.bidOffersBookTable[ i ].addOffering( bidOfferings[ i ] );
		}
	}

	private void createControls( int agentIndex, int tabIndex ) {
		int numMarkets = 3;
		
		JPanel[] marketPanels = new JPanel[ numMarkets ];
		
		this.askOffersBookTable = new OffersTable[ numMarkets ];
		this.bidOffersBookTable = new OffersTable[ numMarkets ];
		
		for ( int i = 0; i < numMarkets; ++i ) {
			this.askOffersBookTable[ i ] = new OffersTable( MarketType.values()[ i ] );
			this.bidOffersBookTable[ i ] = new OffersTable( MarketType.values()[ i ] );
			
			JScrollPane askOfferBookScrollPane = new JScrollPane( this.askOffersBookTable[ i ] );
			askOfferBookScrollPane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED );
			askOfferBookScrollPane.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
			
			JScrollPane bidOfferBookScrollPane = new JScrollPane( this.bidOffersBookTable[ i ] );
			askOfferBookScrollPane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED );
			askOfferBookScrollPane.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
			
			JLabel askLabel = new JLabel( "Ask-Offers (Sell):" );
			JLabel bidLabel = new JLabel( "Bid-Offers (Buy):" );
			
			JPanel panel = new JPanel( new GridBagLayout() );
			
			GridBagConstraints c = new GridBagConstraints();
			
			c.fill = GridBagConstraints.BOTH;
			c.gridx = 0;
			c.weightx = 1.0;
			
			c.gridy = 0;
			c.gridheight = 1;
			c.weighty = 0.0;
			panel.add( askLabel, c );
			c.gridy = 1;
			c.gridheight = 10;
			c.weighty = 0.5;
			panel.add( askOfferBookScrollPane, c );
			
			c.gridy = 11;
			c.gridheight = 1;
			c.weighty = 0.0;
			panel.add( bidLabel, c );
			c.gridy = 12;
			c.gridheight = 10;
			c.weighty = 0.5;
			panel.add( bidOfferBookScrollPane, c );
			
			marketPanels[ i ] = panel;
		}
		
		this.refreshButton = new JButton( "Refresh" );
		this.cloneButton = new JButton( "Clone" );
		//this.visParetoFrontiersButton = new JButton( "Visualize Pareto-Frontiers" );

		this.agentIndexSpinner = new JSpinner( new SpinnerNumberModel( agentIndex, 0, 
				this.parent.getAgents().size() - 1, 1 ) );
		this.agentIndexSpinner.addChangeListener( new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				OfferBookFrame.this.refresh();
			}
		});
		
		this.refreshButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				OfferBookFrame.this.refresh();
			}
		});
		
		this.cloneButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				
				parent.createAndShowInstance( (int) OfferBookFrame.this.agentIndexSpinner.getValue(),
						OfferBookFrame.this.marketTabPane.getSelectedIndex() );
			}
		});
		
		/*
		this.visParetoFrontiersButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				visualizeParetoFrontiers();
			}
		});
		*/
		
		JPanel controlsPanel = new JPanel();
		controlsPanel.add( this.refreshButton );
		controlsPanel.add( this.cloneButton );
		//controlsPanel.add( this.visParetoFrontiersButton );
		controlsPanel.add( this.agentIndexSpinner );

		this.marketTabPane = new JTabbedPane();
		this.marketTabPane.addTab( "Cash / Asset", marketPanels[ 0 ] );
		this.marketTabPane.addTab( "Cash / Loan", marketPanels[ 1 ] );
		this.marketTabPane.addTab( "Asset / Loan", marketPanels[ 2 ] );

		this.marketTabPane.setSelectedIndex( tabIndex );
		
		this.agentInfoPanel = new AgentInfoPanel();
		this.agentInfoPanel.setBorder( BorderFactory.createTitledBorder( BorderFactory.createLineBorder( Color.black ), "Agent-Info") );
		
		this.getContentPane().setLayout( new GridBagLayout() );
		
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		
		c.gridy = 0;
		c.gridheight = 1;
		c.weighty = 0.0;
		c.weightx = 1.0;
		this.getContentPane().add( controlsPanel, c );
		c.gridy = 1;
		c.gridheight = 1;
		c.weighty = 0.0;
		c.weightx = 1.0;
		this.getContentPane().add( this.agentInfoPanel, c );
		c.gridy = 2;
		c.gridheight = 10;
		c.weighty = 1.0;
		c.weightx = 1.0;
		this.getContentPane().add( this.marketTabPane, c );
	}
	
	/*
	private void visualizeParetoFrontiers() {
		int agentIndex = (int) this.agentIndexSpinner.getValue();
		Agent a = OfferBookFrame.agents.get( agentIndex );
		
		List<List<AskOffering>> askOfferings = a.getBestAskOfferings();
		List<List<BidOffering>> bidOfferings = a.getBestBidOfferings();
		
		if ( null == askOfferings || askOfferings.size() < 1 ) {
			return;
		}
		
		List<AskOffering> askOfferingsMarket = askOfferings.get( 1 );
		List<BidOffering> bidOfferingsMarket = bidOfferings.get( 1 );
		
		if ( askOfferingsMarket.size() == 0 ) {
			return;
		}
		
		XYSeries askOffersSeries = new XYSeries("Ask-Offers");
		XYSeries bidOffersSeries = new XYSeries("Bid-Offers");
		
		XYSeriesCollection askParetoFrontier = new XYSeriesCollection();
		XYSeriesCollection bidParetoFrontier = new XYSeriesCollection();
		
		for ( int i = 0; i < askOfferingsMarket.size(); ++i ) {
			AskOfferingWithLoans ask = ( AskOfferingWithLoans ) askOfferingsMarket.get( i );
			
			askOffersSeries.add( ask.getPrice(), ask.getLoanPrice() );
		}

		for ( int i = 0; i < bidOfferingsMarket.size(); ++i ) {
			BidOfferingWithLoans bid = ( BidOfferingWithLoans ) bidOfferingsMarket.get( i );

			bidOffersSeries.add( bid.getPrice(), bid.getLoanPrice() );
		}

		askParetoFrontier.addSeries(askOffersSeries);
		bidParetoFrontier.addSeries(bidOffersSeries);
		
		JFrame paretoFrontierFrame = new JFrame( "Agent " + MainWindow.AGENT_H_FORMAT.format( a.getH() ) + " Offer Pareto-Frontiers" );
		paretoFrontierFrame.getContentPane().setLayout( new BorderLayout() );
		
		String xaxis = "Asset-Price";
		String yaxis = "Loan-Price";
		JFreeChart askOffersParetoChart = ChartFactory.createXYLineChart( "Ask-Offers", xaxis, yaxis, askParetoFrontier );
		JFreeChart bidOffersParetoChart = ChartFactory.createXYLineChart( "Bid-Offers", xaxis, yaxis, bidParetoFrontier );

		paretoFrontierFrame.getContentPane().add( new ChartPanel( askOffersParetoChart ), BorderLayout.NORTH );
		paretoFrontierFrame.getContentPane().add( new ChartPanel( bidOffersParetoChart ), BorderLayout.SOUTH );
		paretoFrontierFrame.pack();
		paretoFrontierFrame.setVisible( true );
	}
	*/
}
