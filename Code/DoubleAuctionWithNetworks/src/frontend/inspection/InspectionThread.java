package frontend.inspection;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.SwingUtilities;

import backend.Auction;
import backend.markets.MarketType;
import backend.tx.Transaction;

public class InspectionThread implements Runnable {
	private Lock lock = new ReentrantLock();
	private Condition nextTXCondition = lock.newCondition();
	private Condition simulationCondition = lock.newCondition();
	  
	private Auction auction;

	private InspectionPanel inspectorPanel;
	
	private int succTXCounter;
	private int noSuccTXCounter;
	
	private int totalTXCounter;
	private int totalNotSuccTXCounter;
	
	private long computationTimeMs;
	
	private int advanceTxCountCurrent;
	private int advanceTxCountTarget;
	
	private Thread awaitNextTxThread;
	private Thread simulationThread;
	
	private SimulationState state;

	private AdvanceMode advanceMode;
	
	private enum SimulationState {
		EXIT,
		RUNNING,
		PAUSED,
		ADVANCE_TX;
	}
	
	public enum AdvanceMode {
		ALL_TX,
		SUCCESSFUL_TX,
		SUCCESSFUL_LOAN_TX
	}
	
	public InspectionThread( Auction auction, InspectionPanel inspectorPanel ) {
		this.auction = auction;
		this.inspectorPanel = inspectorPanel;
		
		// start in pause-mode: simulation-thread must block bevore doing first transaction
		this.state = SimulationState.PAUSED;
		
		this.simulationThread = new Thread( this );
		// give nice name for debugging purposes
		this.simulationThread.setName( "Simulation Thread" );
		
		this.updateTXCounter();
	}
	
	public void startSimulation() {
		this.simulationThread.start();
	}
	
	public boolean isPause() {
		return this.state == SimulationState.PAUSED;
	}
	
	public boolean awaitsNextTX() {
		return this.state == SimulationState.ADVANCE_TX;
	}
	
	// NOTE: must be called from other thread than SimulationThread
	public void stopSimulation() {
		this.switchState( SimulationState.EXIT );

		try {
			// wait for the simulation-thread to join
			this.simulationThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	// NOTE: must be called from other thread than SimulationThread
	public void togglePause() {
		if ( this.state == SimulationState.PAUSED ) {
			this.switchState( SimulationState.RUNNING );
		} else {
			this.switchState( SimulationState.PAUSED );
		}
	}
	
	// NOTE: must be called from other thread than SimulationThread
	public void advanceTX( AdvanceMode advanceMode, int count ) {
		// switch state to advance-tx (will always be already in paused-mode at this point, advance-tx can only be reached from paused-mode)
		this.state = SimulationState.ADVANCE_TX;
		this.advanceMode = advanceMode;
		this.advanceTxCountTarget = count;
		
		// PROBLEM: this call could block for a very long time or forever because 
		// it could take a very long time or forever for the next (successful) transaction to occur
		// => need another thread otherwise would block the GUI-thread!
		this.awaitNextTxThread = new Thread( new Runnable() {
			@Override
			public void run() {
				// need to lock section bevore we can signal and to prevent concurrent modification of data
				InspectionThread.this.lock.lock();
				
				try {
					// (re-) set to null to wait for TX
					InspectionThread.this.advanceTxCountCurrent = 0;
					
					// signal the blocking simulation-thread because nextTX can only be called from pause-state 
					// thus in pause-state simulation-thread is blocking already
					InspectionThread.this.simulationCondition.signal();

					// wait blocking till either the number of TX been advanced OR the state has changed 
					// if number of TX has been advanced: simulation-thread has already incremented counter and switch state back to pause and give signal
					// if state-switch occured through GUI e.g. back to pause or exit, signal came from GUI-Thread and counter-condition will most probably be not satisfied
					while ( InspectionThread.this.advanceTxCountCurrent < InspectionThread.this.advanceTxCountTarget  || 
							InspectionThread.this.state == SimulationState.ADVANCE_TX ) {
						InspectionThread.this.nextTXCondition.await();
					}
					
				} catch (InterruptedException e) {
					if ( InspectionThread.this.state == SimulationState.ADVANCE_TX ) {
						e.printStackTrace();
					}
					
				} finally {
					InspectionThread.this.lock.unlock();
					InspectionThread.this.awaitNextTxThread = null;

					// running in thread => need to update SWING through SwingUtilities.invokeLater
					SwingUtilities.invokeLater( new Runnable() {
						@Override
						public void run() {
							InspectionThread.this.inspectorPanel.advanceTxFinished();
						}
					});
				}
			}
		});
		
		// give nice name for debugging purposes
		this.awaitNextTxThread.setName( "Next-TX Thread" );
		this.awaitNextTxThread.start();
	}
	
	@Override
	public void run() {
		// run this thread until simulation-state tells to exit
		while ( SimulationState.EXIT != this.state ) {
			// need to lock section bevore we can signal and to prevent concurrent modification of data
			this.lock.lock();
			
			try {
				// wait blocking while in pause mode. GUI-Thread or NextTX-thread will change state and give signal
				while ( SimulationState.PAUSED == this.state ) {
					this.simulationCondition.await();
				}

				// switched to exit after signaled, don't calculate a transaction anymore, exit immediately
				if ( SimulationState.EXIT == this.state ) {
					return;
				}
				
				// count total number of TX so far
				this.totalTXCounter++;
				
				SimulationState stateBevoreTX = this.state;
				
				// take current millis to calculate delta time
				long ts = System.currentTimeMillis();
				
				// execute the next transaction
				Transaction tx = this.auction.executeSingleTransaction( 
						InspectionThread.this.inspectorPanel.getSelectedMatchingType(),
						InspectionThread.this.inspectorPanel.isKeepAgentHistory() );
				
				// increment time
				this.computationTimeMs += System.currentTimeMillis() - ts;
				
				// tx was successful
				if ( tx.wasSuccessful() ) {
					// count number of successful TX so far
					this.succTXCounter++;
					// reset counter of how many unsuccessful TX in a row occured
					this.noSuccTXCounter = 0;

				// not successful
				} else {
					// count how many unsuccessful TX in a row occured
					this.noSuccTXCounter++;
					this.totalNotSuccTXCounter++;
					
					// repaint MainWindow upon the 10th unsuccessful TX because
					// previously successful tx notification could have ignored repaint because
					// when full-speed running a repaint will only happen when the delta
					// between two successful TX is > 1000ms => there could have been a
					// last successful TX which ist but not reflected in the visualisation
					if ( this.noSuccTXCounter == 10 ) {
						this.inspectorPanel.repaint();
					}
				}
				
				// we are in advance-tx state and found one (successful) TX => switch back to paused-state
				if ( SimulationState.ADVANCE_TX == this.state ) {
					if ( AdvanceMode.ALL_TX == this.advanceMode ) {
						this.advanceTxCountCurrent++;
						
					} else if ( AdvanceMode.SUCCESSFUL_TX == this.advanceMode && 
							tx.wasSuccessful() ) {
						this.advanceTxCountCurrent++;
						
					} else if ( AdvanceMode.SUCCESSFUL_LOAN_TX == this.advanceMode && 
							tx.wasSuccessful()  ) {
						if ( MarketType.ASSET_LOAN == tx.getMatch().getMarket() ) {
							this.advanceTxCountCurrent++;
						}
					}

					if ( this.advanceTxCountCurrent >= this.advanceTxCountTarget ||
							tx.hasTradingHalted() ) {
						// advancetx can only happen in paused-state, switch back to paused when finished
						this.state = SimulationState.PAUSED;
						// signal the waiting GUI/next-TX-thread (if any)
						this.nextTXCondition.signalAll();
					}
				} else {
					// terminate simulation-thread when reached the equilibrium
					if ( tx.hasTradingHalted() ) {
						this.state = SimulationState.EXIT;
					}
				}
				
				// running in thread => need to update SWING through SwingUtilities.invokeLater
				SwingUtilities.invokeLater( new Runnable() {
					@Override
					public void run() {
						// add tx to gui when successful
						if ( tx.wasSuccessful() ) {
							// force redraw when NEXT_TX-state to reflect change immediately
							InspectionThread.this.inspectorPanel.addSuccessfulTX( tx, SimulationState.RUNNING != stateBevoreTX );
						
						// 
						} else if ( tx.hasTradingHalted() ) {
							InspectionThread.this.inspectorPanel.simulationTerminated();
						}

						InspectionThread.this.updateTXCounter();
					}
				} );
				
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				// need to unlock
				this.lock.unlock();
			}
		}
	}
	
	private void switchState( SimulationState state ) {
		// simulation-thread is running and no nextTX-thread exists, just switch state to exit
		if ( SimulationState.RUNNING == this.state ) {
			this.state = state;
			
		// simulation-thread is running but need to stop nextTX-thread
		} else if ( SimulationState.ADVANCE_TX == this.state ) {
			// this will lead the simulation-thread to exit
			this.state = state;
			// if next-tx thread is active, it will be blocking on its signal, interrupt it
			this.interruptNextTXThread();
			
		// simulation-thread is blocked, switch state and signal to continue
		} else if ( SimulationState.PAUSED == this.state ) {
			this.inspectorPanel.restoreTXHistoryTable();
			
			// signal the thread to exit
			this.lock.lock();
			this.state = state;
			this.simulationCondition.signal();
			this.lock.unlock();
		}
	}
	
	private void updateTXCounter() {
		this.inspectorPanel.updateTXCounter( this.succTXCounter, this.noSuccTXCounter, this.totalTXCounter, this.totalNotSuccTXCounter, this.computationTimeMs );
	}
	
	private void interruptNextTXThread() {
		// if next-tx thread exists, interrupt it because it is blocked waiting
		if ( null != this.awaitNextTxThread ) {
			this.awaitNextTxThread.interrupt();
		}
	}
}