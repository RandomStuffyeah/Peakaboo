package peakaboo.ui.swing.plotting.filters;


import java.awt.Component;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import peakaboo.filter.model.Filter;
import swidget.icons.IconSize;
import swidget.icons.StockIcon;
import swidget.widgets.ImageButton;
import swidget.widgets.ImageButton.Layout;
import swidget.widgets.Spacing;


class EditButtonRenderer implements TableCellRenderer
{

	private ImageButton	edit;
	private JPanel container;


	public EditButtonRenderer()
	{

		edit = new ImageButton(StockIcon.MISC_PREFERENCES, "…", "Edit Filter", Layout.IMAGE, IconSize.TOOLBAR_SMALL);
		edit.setOpaque(false);
				
		container = new JPanel();
		container.setBorder(Spacing.bNone());

	}


	public Component getTableCellRendererComponent(JTable table, Object _filter, boolean isSelected, boolean hasFocus,
			int row, int column)
	{
		
		Filter filter = (Filter)_filter;
		
		int numParameters = filter.getParameters().size();

		if (table.getRowHeight() < container.getPreferredSize().height) {
			table.setRowHeight(container.getPreferredSize().height);
		}

		container.remove(edit);
		
		if (isSelected) {
			container.setBackground(table.getSelectionBackground());
			container.setOpaque(true);
		} else {
			container.setBackground(table.getBackground());
			container.setOpaque(false);
		}
		
		if (numParameters == 0)
		{
			return container;
		}
		container.add(edit);
		
		container.setToolTipText("Edit settings for this " + filter.getFilterName() + " Filter");
		
		return container;
	}
}
