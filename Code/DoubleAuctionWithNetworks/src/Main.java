import agents.markets.Markets;
import gui.MainWindow;

public class Main {
	public static void main(String[] args) {
		Markets markets = new Markets();
		
		new MainWindow( markets );
	}

}
