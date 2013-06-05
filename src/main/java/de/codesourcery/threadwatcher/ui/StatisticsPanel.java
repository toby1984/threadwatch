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
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

import org.apache.commons.lang.ObjectUtils;

import de.codesourcery.threadwatcher.FileReader;
import de.codesourcery.threadwatcher.FileReader.FileVisitor;
import de.codesourcery.threadwatcher.HiResInterval;
import de.codesourcery.threadwatcher.PerThreadStatistics;
import de.codesourcery.threadwatcher.ThreadEvent;
import de.codesourcery.threadwatcher.ui.PercentageBar.IPercentageProvider;

public final class StatisticsPanel extends JPanel
{
	private static final String[] COLUMN_NAMES;
	
	static {
		List<String> columnNames = new ArrayList<>();
		columnNames.add( "Thread" );
		columnNames.add( "States" );
		for ( LegendItem item : UIConstants.LEGEND_ITEMS ) {
			columnNames.add( item.title+" %" );
		}
		COLUMN_NAMES=new String[ columnNames.size() ];
		for ( int i = 0 ; i < columnNames.size() ; i++ ) {
			COLUMN_NAMES[i] = columnNames.get(i);
		}
	}
	
	private static final int COLUMN_THREAD_NAME = 0;
	private static final int COLUMN_PERCENTAGE_BAR = 1;

	private FileReader reader;
	private HiResInterval interval;

	private final List<TableEntry> tableData=new ArrayList<>();
	private final MyTableModel tableModel = new MyTableModel();
	private final JTable table = new JTable(tableModel);
	
    private final MouseAdapter mouseListener = new MouseAdapter() 
    {
    	public void mouseMoved(java.awt.event.MouseEvent e) 
    	{
			int col = table.columnAtPoint( e.getPoint() );
			if ( col == COLUMN_PERCENTAGE_BAR ) {
				int row = table.rowAtPoint( e.getPoint() );
				if ( row >= 0 && row < tableData.size() ) 
				{
					final TableEntry entry = tableData.get( row );
					final Rectangle rectangle = table.getCellRect( row , col , false );
					Point translated = new Point( e.getPoint().x - rectangle.x , e.getPoint().y - rectangle.y );
					final LegendItemProvider value = (LegendItemProvider) entry.percentageBar.getValueForPoint( translated );
					String tooltip = null;
					if ( value != null ) 
					{
						tooltip = value.item.title+" : "+formatPercentageValue( value.getPercentageValue() ) +" %";
					}
					if ( ! ObjectUtils.equals( tooltip , entry.percentageBar.getToolTipText() ) ) {
						entry.percentageBar.setToolTipText( tooltip );
					}
				}
			}
    	}
    };	
	
	protected final class MyTableModel implements TableModel {

		private final List<TableModelListener> listeners = new ArrayList<>();
		
		@Override
		public int getRowCount() { return tableData.size(); }

		@Override
		public int getColumnCount() { return COLUMN_NAMES.length; }

		@Override
		public String getColumnName(int columnIndex) { return COLUMN_NAMES[columnIndex]; }

		@Override
		public Class<?> getColumnClass(int columnIndex) { return TableEntry.class; }

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {  return false; }

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) { return tableData.get(rowIndex); }

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) { /* NOP */ }

		public void fireDataChanged() 
		{
			final TableModelEvent ev=new TableModelEvent(this);
			for ( TableModelListener l : listeners ) {
				l.tableChanged( ev );
			}
		}
		
		@Override
		public void addTableModelListener(TableModelListener l) {
			listeners.add(l);
		}

		@Override
		public void removeTableModelListener(TableModelListener l) {
			listeners.remove(l);
		}
	}
	
	protected static final class TableEntry 
	{
		public final PerThreadStatistics statistics;
		public final PercentageBar percentageBar;
		
		public TableEntry(PerThreadStatistics statistics, PercentageBar percentageBar) 
		{
			this.statistics = statistics;
			this.percentageBar = percentageBar;
		}
	}

	protected final class CellRenderer extends DefaultTableCellRenderer {
		
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) 
		{
			Component result = super.getTableCellRendererComponent(table, value, isSelected, hasFocus,row, column);
			final TableEntry entry = (TableEntry) value;
			switch( column ) 
			{
				case COLUMN_THREAD_NAME:
					final String name = reader.getThreadNamesByID().get( entry.statistics.threadId );
					setText( name+" ("+entry.statistics.threadId+")" );
					break;
				case COLUMN_PERCENTAGE_BAR:
					return entry.percentageBar;
				default:
					int legendItem = column-2;
					LegendItem item = UIConstants.LEGEND_ITEMS.get(legendItem);
					for ( IPercentageProvider i : entry.percentageBar.getValues() ) 
					{
						LegendItemProvider current = (LegendItemProvider) i;
						if ( current.item == item ) 
						{
							setText( formatPercentageValue( current.percentageValue ) );		
							setHorizontalAlignment( JLabel.RIGHT );
							break;
						}
					}

			}
			return result;
		}
	}
	
	private static String formatPercentageValue(double value) {
		return new DecimalFormat("##0.##").format(value);
	}
	
	public StatisticsPanel(FileReader reader) 
	{
		this.reader = reader;
		table.setDefaultRenderer( TableEntry.class , new CellRenderer() );
		table.addMouseMotionListener( mouseListener );
		table.setBackground(Color.WHITE);
		table.setFillsViewportHeight(true);
		setLayout( new GridBagLayout() );
		GridBagConstraints cnstrs = new GridBagConstraints();
		cnstrs.gridheight=GridBagConstraints.REMAINDER;
		cnstrs.gridwidth=GridBagConstraints.REMAINDER;
		cnstrs.fill = GridBagConstraints.BOTH;
		cnstrs.weightx=1.0;
		cnstrs.weighty=1.0;
		JScrollPane pane = new JScrollPane(table);
		pane.setBackground(Color.WHITE);
		add( pane, cnstrs );
	}

	public void setInterval(HiResInterval interval) throws IOException 
	{
		this.interval = interval;
		recalcStatistics();
		repaint();
	}

	private void recalcStatistics() throws IOException {

		tableData.clear();
		
		if ( interval == null ) {
			return;
		}

		final Map<Integer,PerThreadStatistics> statistics = new HashMap<>();

		final Set<Integer> threadIds = reader.getAliveThreadsInInterval( interval );
		for ( int threadId : threadIds ) {
			statistics.put(threadId,new PerThreadStatistics(threadId));
		}

		final FileVisitor statisticsVisitor = new FileVisitor() {

			@Override
			public void visit(ThreadEvent event)
			{
				final PerThreadStatistics stat = statistics.get(event.threadId);
				stat.processEvent( event );
			}
		};
		reader.visit( statisticsVisitor , interval , threadIds );

		final double totalIntervalDurationMillis = interval.getDurationInMilliseconds();		
		for ( PerThreadStatistics s : statistics.values() ) 
		{
			s.finish( interval );
			
			if ( s.containsData ) 
			{
				final PercentageBar bar = new PercentageBar();

				for ( Entry<LegendItem, Double> v : s.sumDurationInMillis.entrySet() ) 
				{
					double percentage = 100.0*( v.getValue() / totalIntervalDurationMillis );
					LegendItemProvider provider = new LegendItemProvider( v.getKey() , percentage );
					bar.setPercentage( v.getKey().color , provider );
				}
				tableData.add( new TableEntry( s , bar ) );
			}
		}       
		tableModel.fireDataChanged();
	}

	protected static final class LegendItemProvider implements IPercentageProvider {

		public final LegendItem item;
		public final double percentageValue;

		private LegendItemProvider(LegendItem item, double percentageValue)
		{
			this.item = item;
			this.percentageValue = percentageValue;
		}

		@Override
		public double getPercentageValue()
		{
			return percentageValue;
		}
	}
}