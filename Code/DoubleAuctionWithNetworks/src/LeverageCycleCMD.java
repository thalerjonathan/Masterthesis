import java.io.File;

import frontend.experimenter.ExperimentCMDRunner;

public class LeverageCycleCMD {

	public static void main( String[] args ) {
		File experimentFile = null;
		int maxThreads = Runtime.getRuntime().availableProcessors();
		
		if ( 0 == args.length || 2 < args.length ) {
			System.out.println( "Usage: LeverageCycleCMD <experimentFileName> <maxThreads>" );
			return;
		}
		
		experimentFile = new File( args[ 0 ] );
		if ( false == experimentFile.exists() ) {
			System.out.println( "Experimentfile \"" + args[ 0 ] + "\" does not exist." );
			return;
		}
		
		if ( 2 == args.length ) {
			maxThreads = Integer.parseInt( args[ 1 ] );
		} 
		
		ExperimentCMDRunner eperimentRunner = ExperimentCMDRunner.openExperiment( experimentFile, maxThreads );
		if ( null != eperimentRunner ) {
			eperimentRunner.runAllExperiments();
		}
	}
}
