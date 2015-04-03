package gui.offerBook;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

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

import agents.Agent;
import agents.network.AgentNetwork;
import doubleAuction.offer.AskOffering;
import doubleAuction.offer.BidOffering;
import doubleAuction.offer.MarketType;

@SuppressWarnings("serial")
public class OfferBookFrame extends JFrame {
	private JButton refreshButton;
	private JButton cloneButton;
	
	private JSpinner agentIndexSpinner;

	private JTabbedPane marketTabPane;
	
	private OffersTable[] askOffersBookTable;
	private OffersTable[] bidOffersBookTable;
	
	private AgentInfoPanel agentInfoPanel;
	
	private static AgentNetwork agents;
	private static List<OfferBookFrame> offerBookInstances = new ArrayList<>();
	
	public static void agentsChanged( AgentNetwork agents  ) {
		OfferBookFrame.agents = agents;
		
		for ( OfferBookFrame obf : OfferBookFrame.offerBookInstances ) {
			obf.setVisible( false );
			obf.dispose();
		}
		
		OfferBookFrame.offerBookInstances.clear();
	}
	
	public static void showOfferBook() {
		OfferBookFrame.createAndShowInstance( 0, 0 );
	}

	public static void offerBookChanged() {
		for ( OfferBookFrame obf : OfferBookFrame.offerBookInstances ) {
			obf.refillTables();
		}
	}
	
	private void refillTables() {
		int agentIndex = (int) this.agentIndexSpinner.getValue() - 1;
		Agent a = OfferBookFrame.agents.get( agentIndex );
		
		this.agentInfoPanel.setAgent( a );
		
		List<List<AskOffering>> askOfferings = a.getBestAskOfferings();
		List<List<BidOffering>> bidOfferings = a.getBestBidOfferings();
		
		int numMarkets = 3;
		
		// clear previously set data
		for ( int i = 0; i < numMarkets; ++i ) {
			this.askOffersBookTable[ i ].clearAll();
			this.bidOffersBookTable[ i ].clearAll();
		}
		
		if ( null == askOfferings || null == bidOfferings ) {
			return;
		}
		
		for ( int i = 0; i < askOfferings.size(); ++i ) {
			List<AskOffering> askOfferingsMarket = askOfferings.get( i );
			List<BidOffering> bidOfferingsMarket = bidOfferings.get( i );
			
			for ( int j = 0; j < askOfferingsMarket.size(); ++j ) {
				AskOffering ask = askOfferingsMarket.get( j );
				this.askOffersBookTable[ i ].addAskOffering( ask );
			}
			
			for ( int j = 0; j < bidOfferingsMarket.size(); ++j ) {
				BidOffering bid = bidOfferingsMarket.get( j );
				this.bidOffersBookTable[ i ].addBidOffering( bid );
			}
		}
	}
	
	// NOTE: used for cloning
	private OfferBookFrame( int agentIndex, int tabIndex ) {
		super( "Offer-Book" );

		this.createControls( agentIndex, tabIndex );
		
		this.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		this.addWindowListener( new WindowAdapter() {
			@Override
			public void windowClosed( WindowEvent e ) {
				OfferBookFrame.offerBookInstances.remove( e.getComponent() );
			}
		});
		
		this.setPreferredSize( new Dimension( 640, 768 ) );
		
		this.pack();
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
		
		this.agentIndexSpinner = new JSpinner( new SpinnerNumberModel( agentIndex + 1, 1, OfferBookFrame.agents.size(), 1 ) );
		this.agentIndexSpinner.addChangeListener( new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				OfferBookFrame.this.refillTables();
			}
		});
		
		this.refreshButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				OfferBookFrame.this.refillTables();
			}
		});
		
		this.cloneButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				OfferBookFrame.createAndShowInstance( (int) OfferBookFrame.this.agentIndexSpinner.getValue() - 1,
						OfferBookFrame.this.marketTabPane.getSelectedIndex() );
			}
		});
		
		JPanel controlsPanel = new JPanel();
		controlsPanel.add( this.refreshButton );
		controlsPanel.add( this.cloneButton );
		controlsPanel.add( this.agentIndexSpinner );

		this.marketTabPane = new JTabbedPane();
		this.marketTabPane.addTab( "Asset -> Cash", marketPanels[ 0 ] );
		this.marketTabPane.addTab( "Asset -> Loan", marketPanels[ 1 ] );
		this.marketTabPane.addTab( "Loan -> Cash", marketPanels[ 2 ] );
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
	
	private static void createAndShowInstance( int agentIndex, int tabIndex ) {
		OfferBookFrame instance = new OfferBookFrame( agentIndex, tabIndex );
		instance.refillTables();
		instance.setVisible( true );
		OfferBookFrame.offerBookInstances.add( instance );
	}
}
