/**
 * Copyright 2013 Tobias Gierke <tobias.gierke@code-sourcery.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.codesourcery.threadwatcher.ui;

import java.awt.Color;
import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import de.codesourcery.threadwatcher.HiResInterval;

public class IntervalPanel extends JPanel {

    private final JTextField viewIntervalStart = new JTextField();
    private final JTextField viewIntervalEnd = new JTextField();
    private final JTextField viewIntervalDuration= new JTextField();
    
    @Override
    public void setBackground(Color bg) {
    	super.setBackground(bg);
    	if ( viewIntervalStart != null ) {
	    	viewIntervalStart.setBackground( bg );
	    	viewIntervalEnd.setBackground( bg );
	    	viewIntervalDuration.setBackground( bg );
    	}
    }
    
	public IntervalPanel(HiResInterval interval) 
	{
        setFocusable( true );
        setLayout( new GridLayout(2, 3 ) );

        viewIntervalStart.setColumns( 20 );
        viewIntervalEnd.setColumns( 20 );
        viewIntervalDuration.setColumns( 20 );
        
        viewIntervalStart.setHorizontalAlignment(JTextField.CENTER);
        viewIntervalEnd.setHorizontalAlignment(JTextField.CENTER);
        viewIntervalDuration.setHorizontalAlignment(JTextField.CENTER);
        
        viewIntervalStart.setEditable( false );
        viewIntervalEnd.setEditable( false );
        viewIntervalDuration.setEditable( false );
        
        updateTextFields( interval );
        
        add( new JLabel("Start" , JLabel.CENTER ) );
        add( new JLabel("End" , JLabel.CENTER ) );
        add( new JLabel("Window Duration" , JLabel.CENTER ) );
        
        add( viewIntervalStart );
        add( viewIntervalEnd );
        add( viewIntervalDuration );		
	}
	
    public void updateTextFields(HiResInterval interval) {
        
    	if ( interval == null ) {
    		viewIntervalStart.setText( " -- " );
    		viewIntervalEnd.setText( " -- " );
    		viewIntervalDuration.setText( " -- " );
    	} 
    	else 
    	{
    		viewIntervalStart.setText( interval.start.toUIString() );
    		viewIntervalEnd.setText( interval.end.toUIString() );
    		viewIntervalDuration.setText( interval.getDurationInMilliseconds()+" ms");
    	}
    }	
}
