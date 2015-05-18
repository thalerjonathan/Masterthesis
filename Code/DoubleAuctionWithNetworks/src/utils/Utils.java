package utils;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

public class Utils {
	public final static SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat( "dd.MM.yyyy HH:mm:ss" );
	
	public final static DecimalFormat DECIMAL_2_DIGITS_FORMATTER = new DecimalFormat( "0.00" );
	public final static DecimalFormat DECIMAL_3_DIGITS_FORMATTER = new DecimalFormat("0.000");
	public final static DecimalFormat DECIMAL_4_DIGITS_FORMATTER = new DecimalFormat("0.0000" );

	public final static DecimalFormat DECIMAL_LARGEVALUES_FORMATTER = new DecimalFormat( "###,###.###" );
	
	public final static File EXPERIMENTS_DIRECTORY = new File( System.getProperty( "user.dir" ) + File.separator + "experiments" );
	public final static File RESULTS_DIRECTORY = new File( System.getProperty( "user.dir" ) + File.separator + "results" );
}
