package gui;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.SwingUtilities;

import doubleAuction.Auction;
import doubleAuction.tx.Transaction;

public class SimulationThread implements Runnable {
	private Lock lock = new ReentrantLock();
	private Condition nextTXCondition = lock.newCondition();
	private Condition simulationCondition = lock.newCondition();
	  
	private Auction auction;

	private MainWindow mainWindow;
	
	private int succTXCounter;
	private int totalTXCounter;
	private int noSuccTXCounter;
	
	private long computationTimeMs;
	
	private Transaction lastTX;
	
	private Thread awaitNextTxThread;
	private Thread simulationThread;
	
	private SimulationState state;

	private boolean successfulTXOnly;
	
	private enum SimulationState {
		EXIT,
		RUNNING,
		PAUSED,
		NEXT_TX;
	}
	
	public SimulationThread( Auction auction, MainWindow mainWindow ) {
		this.auction = auction;
		this.mainWindow = mainWindow;
		
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
		return this.state == SimulationState.NEXT_TX;
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
	public void nextTX( boolean successfulOnly ) {
		// switch state to next-tx (will always be already in paused-mode at this point, next-tx can only be reached from paused-mode)
		this.state = SimulationState.NEXT_TX;
		this.successfulTXOnly = successfulOnly;
		
		// PROBLEM: this call could block for a very long time or forever because 
		// it could take a very long time or forever for the next (successful) transaction to occur
		// => need another thread otherwise would block the GUI-thread!
		this.awaitNextTxThread = new Thread( new Runnable() {
			@Override
			public void run() {
				// need to lock section bevore we can signal and to prevent concurrent modification of data
				SimulationThread.this.lock.lock();
				
				try {
					// (re-) set to null to wait for TX
					SimulationThread.this.lastTX = null;
					
					// signal the blocking simulation-thread because nextTX can only be called from pause-state 
					// thus in pause-state simulation-thread is blocking already
					SimulationThread.this.simulationCondition.signal();

					// wait blocking till either a next TX has been found OR the state has changed 
					// if TX has been found: simulation-thread will set lastTX to the given TX and switch state back to pause and give signal
					// if state-switch occured through GUI e.g. back to pause or exit, signal came from GUI-Thread and lastSuccTX will be null
					while ( null == SimulationThread.this.lastTX || SimulationThread.this.state == SimulationState.NEXT_TX ) {
						SimulationThread.this.nextTXCondition.await();
					}
					
				} catch (InterruptedException e) {
					if ( SimulationThread.this.state == SimulationState.NEXT_TX ) {
						e.printStackTrace();
					}
					
				} finally {
					SimulationThread.this.lock.unlock();
					SimulationThread.this.awaitNextTxThread = null;

					// running in thread => need to update SWING through SwingUtilities.invokeLater
					SwingUtilities.invokeLater( new Runnable() {
						@Override
						public void run() {
							SimulationThread.this.mainWindow.nextTXFinished();
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
				Transaction tx = this.auction.executeSingleTransactionByType( SimulationThread.this.mainWindow.getSelectedMatchingType() );
				
				// increment time
				this.computationTimeMs += System.currentTimeMillis() - ts;
				boolean notifyTX = true;
				
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
					
					// repaint MainWindow upon the 10th unsuccessful TX because
					// previously successful tx notification could have ignored repaint because
					// when full-speed running a repaint will only happen when the delta
					// between two successful TX is > 1000ms => there could have been a
					// last successful TX which ist but not reflected in the visualisation
					if ( this.noSuccTXCounter == 10 ) {
						this.mainWindow.repaint();
					}
					
					notifyTX = ! this.successfulTXOnly;
				}
				
				// we are in nextTX-state and found one (successful) TX => switch back to paused-state
				if ( SimulationState.NEXT_TX == this.state && notifyTX ) {
					// found (successfull) transaction: store in consumer-data to 
					this.lastTX = tx;
					// next-tx can only happen in paused-state, switch back to paused when finished
					this.state = SimulationState.PAUSED;
					// signal the waiting GUI/next-TX-thread (if any)
					this.nextTXCondition.signalAll();
				}

				
				// running in thread => need to update SWING through SwingUtilities.invokeLater
				SwingUtilities.invokeLater( new Runnable() {
					@Override
					public void run() {
						// add tx to gui when successful
						if ( tx.wasSuccessful() ) {
							// force redraw when NEXT_TX-state to reflect change immediately
							SimulationThread.this.mainWindow.addSuccessfulTX( tx, SimulationState.RUNNING != stateBevoreTX );
						}

						SimulationThread.this.updateTXCounter();
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
		} else if ( SimulationState.NEXT_TX == this.state ) {
			// this will lead the simulation-thread to exit
			this.state = state;
			// if next-tx thread is active, it will be blocking on its signal, interrupt it
			this.interruptNextTXThread();
			
		// simulation-thread is blocked, switch state and signal to continue
		} else if ( SimulationState.PAUSED == this.state ) {
			this.mainWindow.restoreTXHistoryList();
			
			// signal the thread to exit
			this.lock.lock();
			this.state = state;
			this.simulationCondition.signal();
			this.lock.unlock();
		}
	}
	
	private void updateTXCounter() {
		this.mainWindow.updateTXCounter( this.succTXCounter, this.noSuccTXCounter, this.totalTXCounter, this.computationTimeMs );
	}
	
	private void interruptNextTXThread() {
		// if next-tx thread exists, interrupt it because it is blocked waiting
		if ( null != this.awaitNextTxThread ) {
			this.awaitNextTxThread.interrupt();
		}
	}
}