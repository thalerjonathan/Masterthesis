package gui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;

import agents.Agent;
import agents.network.AgentNetwork;
import doubleAuction.offer.AskOffering;
import doubleAuction.offer.AskOfferingWithLoans;
import doubleAuction.offer.BidOffering;
import doubleAuction.offer.BidOfferingWithLoans;

@SuppressWarnings("serial")
public class OfferBookFrame extends JFrame {
	private AgentNetwork agents;

	private JButton refreshButton;
	private JButton cloneButton;
	
	private JSpinner agentIndexSpinner;

	private JTabbedPane marketTabPane;
	
	private JTable[] askOffersBookTable;
	private JTable[] bidOffersBookTable;
	private DefaultTableModel[] askOffersBookModel;
	private DefaultTableModel[] bidOffersBookModel;
	
	public OfferBookFrame( AgentNetwork agents ) {
		this( agents, 0, 0 );
	}
	
	// NOTE: used for cloning
	private OfferBookFrame( AgentNetwork agents, int agentIndex, int tabIndex ) {
		super( "Offer-Book" );
		
		this.agents = agents;

		this.getContentPane().setLayout( new BorderLayout() );
		
		this.createControls( agentIndex, tabIndex );

		this.setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
		this.pack();
	}
	
	private void createControls( int agentIndex, int tabIndex ) {
		int numMarkets = 3;
		
		JPanel[] marketPanels = new JPanel[ numMarkets ];
		
		this.askOffersBookTable = new JTable[ numMarkets ];
		this.bidOffersBookTable = new JTable[ numMarkets ];
		this.askOffersBookModel = new DefaultTableModel[ numMarkets ];
		this.bidOffersBookModel = new DefaultTableModel[ numMarkets ];
		
		Object[] columnLabels = new Object[] { "h", 
				"Asset Amount", "Asset Price", 
				"Loan Amount", "Loan Price" };
		
		for ( int i = 0; i < numMarkets; ++i ) {
			this.askOffersBookModel[ i ] = new DefaultTableModel( columnLabels, 0 ) {
			    @Override
			    public boolean isCellEditable(int row, int column) {
			       return false;
			    }
			};
			
			this.bidOffersBookModel[ i ] = new DefaultTableModel( columnLabels, 0 ) {
			    @Override
			    public boolean isCellEditable(int row, int column) {
			       return false;
			    }
			};
			
			this.askOffersBookTable[ i ] = new JTable( this.askOffersBookModel[ i ] );
			this.askOffersBookTable[ i ].setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
			this.askOffersBookTable[ i ].setAutoCreateRowSorter( true );
	
			this.bidOffersBookTable[ i ] = new JTable( this.bidOffersBookModel[ i ] );
			this.bidOffersBookTable[ i ].setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
			this.bidOffersBookTable[ i ].setAutoCreateRowSorter( true );
	
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
			
			c.gridy = 0;
			panel.add( askLabel, c );
			c.gridy = 1;
			panel.add( askOfferBookScrollPane, c );
			
			c.gridy = 2;
			panel.add( bidLabel, c );
			c.gridy = 3;
			panel.add( bidOfferBookScrollPane, c );
			
			marketPanels[ i ] = panel;
		}
		
		this.refreshButton = new JButton( "Refresh" );
		this.cloneButton = new JButton( "Clone" );
		
		this.agentIndexSpinner = new JSpinner( new SpinnerNumberModel( agentIndex + 1, 1, this.agents.size(), 1 ) );
		this.agentIndexSpinner.addChangeListener( new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				OfferBookFrame.this.offerBookChanged();
			}
		});
		
		this.refreshButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				OfferBookFrame.this.offerBookChanged();
			}
		});
		
		this.cloneButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				OfferBookFrame clone = new OfferBookFrame( OfferBookFrame.this.agents, 
						(int) OfferBookFrame.this.agentIndexSpinner.getValue() - 1, 
						OfferBookFrame.this.marketTabPane.getSelectedIndex() );
				clone.offerBookChanged();
				clone.setVisible( true );
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
		
		this.getContentPane().add( controlsPanel, BorderLayout.NORTH );
		this.getContentPane().add( this.marketTabPane, BorderLayout.CENTER );
	}
	
	public void offerBookChanged() {
		int agentIndex = (int) this.agentIndexSpinner.getValue() - 1;
		Agent a = this.agents.get( agentIndex );
		
		List<List<AskOffering>> askOfferings = a.getBestAskOfferings();
		List<List<BidOffering>> bidOfferings = a.getBestBidOfferings();
		
		int numMarkets = 3;
		
		// clear previously set data
		for ( int i = 0; i < numMarkets; ++i ) {
			this.askOffersBookTable[ i ].clearSelection();
			this.askOffersBookModel[ i ].setRowCount( 0 );
			this.askOffersBookTable[ i ].revalidate();
			
			this.bidOffersBookTable[ i ].clearSelection();
			this.bidOffersBookModel[ i ].setRowCount( 0 );
			this.bidOffersBookTable[ i ].revalidate();
		}
		
		if ( null == askOfferings || null == bidOfferings ) {
			return;
		}
		
		for ( int i = 0; i < askOfferings.size(); ++i ) {
			List<AskOffering> askOfferingsMarket = askOfferings.get( i );
			List<BidOffering> bidOfferingsMarket = bidOfferings.get( i );
			
			for ( int j = 0; j < askOfferingsMarket.size(); ++j ) {
				AskOffering ask = askOfferingsMarket.get( j );
				
				if ( ask instanceof AskOfferingWithLoans ) {
					this.askOffersBookModel[ i ].addRow( new Object[] {
							MainWindow.AGENT_H_FORMAT.format( a.getH() ),
							MainWindow.TRADING_VALUES_FORMAT.format( ask.getAssetAmount() ),
							MainWindow.TRADING_VALUES_FORMAT.format( ask.getAssetPrice() ),
							MainWindow.TRADING_VALUES_FORMAT.format( ((AskOfferingWithLoans) ask).getLoanAmount() ),
							MainWindow.TRADING_VALUES_FORMAT.format( ((AskOfferingWithLoans) ask).getLoanPrice() ),
					});
					
				} else {
					this.askOffersBookModel[ i ].addRow( new Object[] {
							MainWindow.AGENT_H_FORMAT.format( a.getH() ),
							MainWindow.TRADING_VALUES_FORMAT.format( ask.getAssetAmount() ),
							MainWindow.TRADING_VALUES_FORMAT.format( ask.getAssetPrice() ),
							"-", "-", "-", "-"
					});
				}
			}
			
			for ( int j = 0; j < bidOfferingsMarket.size(); ++j ) {
				BidOffering bid = bidOfferingsMarket.get( j );
				
				if ( bid instanceof BidOfferingWithLoans ) {
					this.bidOffersBookModel[ i ].addRow( new Object[] {
							MainWindow.AGENT_H_FORMAT.format( a.getH() ),
							MainWindow.TRADING_VALUES_FORMAT.format( bid.getAssetAmount() ),
							MainWindow.TRADING_VALUES_FORMAT.format( bid.getAssetPrice() ),
							MainWindow.TRADING_VALUES_FORMAT.format( ((BidOfferingWithLoans) bid).getLoanAmount() ),
							MainWindow.TRADING_VALUES_FORMAT.format( ((BidOfferingWithLoans) bid).getLoanPrice() )
					});
					
				} else {
					this.askOffersBookModel[ i ].addRow( new Object[] {
							MainWindow.AGENT_H_FORMAT.format( a.getH() ),
							MainWindow.TRADING_VALUES_FORMAT.format( bid.getAssetAmount() ),
							MainWindow.TRADING_VALUES_FORMAT.format( bid.getAssetPrice() ),
							"-", "-", "-", "-"
					});
				}
			}
		}
	}
}
