package de.codesourcery.threadwatcher;

import java.awt.Dimension;
import java.awt.Graphics;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class Main 
{

    public static void main(String[] args)
    {
        new Main().run(new File("/tmp/threadwatcher.out" ) );
    }

    public void run(File file)
    {
        final JFrame frame = new JFrame("Thread-Watcher V0.0");
        final RenderPanel panel = new RenderPanel(file);
        panel.setPreferredSize(new Dimension(640,480 ) );
        frame.add( panel );
        frame.pack();
        frame.setVisible( true );
    }
    
    protected final class RenderPanel extends JPanel 
    {
        private File inputFile;
        
        public RenderPanel(File inputFile) {
            this.inputFile = inputFile;
        }
        
        @Override
        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);
        }
    }
    
    
    
}
