package frontend.visualisation;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

import utils.Utils;

@SuppressWarnings("serial")
public class Visualizer extends JPanel {

	protected final static Font RENDER_FONT = new Font( "Helvetica", Font.BOLD, 16 );
	
	public Visualizer() {
		this.addMouseListener( new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				storeAsPng();
			}
		} );
	}
	
	public void storeAsPng() {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileFilter( new FileNameExtensionFilter( "PNG-Files", "png" ) );
		fileChooser.setCurrentDirectory( Utils.VISUALS_DIRECTORY );
		
		int returnVal = fileChooser.showSaveDialog( this );
        if (returnVal != JFileChooser.APPROVE_OPTION) {
        	return;
        }
        
        File file = fileChooser.getSelectedFile();
        if ( false == file.getName().endsWith( ".png" ) ) {
        	file = new File( fileChooser.getSelectedFile() + ".png" );
        }
        
		try {
			BufferedImage image = new BufferedImage( this.getWidth(), this.getHeight(), BufferedImage.TYPE_3BYTE_BGR );
			Graphics g = image.getGraphics();
			this.paint(g);
			
			g.drawImage( image, 0, 0, null);
			ImageIO.write( image, "PNG", file );
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
