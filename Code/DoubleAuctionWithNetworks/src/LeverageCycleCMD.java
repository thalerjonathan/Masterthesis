import java.io.File;

import frontend.experimenter.ExperimentRunner;

public class LeverageCycleCMD {

	public static void main( String[] args ) {
		if ( 1 != args.length ) {
			System.out.println( "Usage: LeverageCycleCMD <experimentFileName>" );
			return;
		}
		
		File experimentFile = new File( args[ 0 ] );
		if ( false == experimentFile.exists() ) {
			System.out.println( "Experimentfile \"" + args[ 0 ] + "\" does not exist." );
			return;
		}
		
		ExperimentRunner eperimentRunner = ExperimentRunner.openExperiment( experimentFile );
		if ( null != eperimentRunner ) {
			eperimentRunner.runAllExperiments();
		}
	}
}
