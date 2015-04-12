import agents.markets.Markets;
import gui.MainWindow;

public class Main {
	public static void main(String[] args) {
		Markets markets = new Markets( 0.2, 1.0, 0.2 );
		
		new MainWindow( markets );
	}

}
