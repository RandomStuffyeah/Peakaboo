package peakaboo.ui.swing.mapping.sidebar;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import peakaboo.controller.mapper.settings.MapSettingsController;
import peakaboo.display.map.MapDisplayMode;
import peakaboo.ui.swing.mapping.sidebar.modes.Composite;
import peakaboo.ui.swing.mapping.sidebar.modes.Overlay;
import peakaboo.ui.swing.mapping.sidebar.modes.Ratio;
import swidget.widgets.ClearPanel;


public class MapFittingPanel extends ClearPanel
{

	private CardLayout	card;

	private JPanel		cardPanel;
	
	private JPanel		compPanel, overPanel, ratioPanel;
	
	
	
	public MapFittingPanel(final MapSettingsController controller)
	{
		//create the card panel
		cardPanel = new ClearPanel();
		card = new CardLayout();
		cardPanel.setLayout(card);
		
		//create each of the three panels
		compPanel = new Composite(controller);
		overPanel = new Overlay(controller);
		ratioPanel = new Ratio(controller);
		
		//add each of the panels
		cardPanel.add(compPanel, MapDisplayMode.COMPOSITE.toString());
		cardPanel.add(overPanel, MapDisplayMode.OVERLAY.toString());
		cardPanel.add(ratioPanel, MapDisplayMode.RATIO.toString());
		
		
		//create combobox
		final JComboBox<MapDisplayMode> modeSelect = new JComboBox<>(MapDisplayMode.values());
		modeSelect.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
		modeSelect.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e)
			{
				MapDisplayMode mode = (MapDisplayMode)modeSelect.getSelectedItem();
				controller.getMapFittings().setMapDisplayMode(mode);
				card.show(cardPanel, mode.toString());
			}
		});
		
		
		//add the two components to this panel
		setLayout(new BorderLayout());
		add(cardPanel, BorderLayout.CENTER);
		add(modeSelect, BorderLayout.NORTH);
		setBorder(new TitledBorder("Fittings"));
		
		
	}
	
}

class AlignedListCellRenderer extends DefaultListCellRenderer {
    
    private int align;
    
    public AlignedListCellRenderer(int align) {
        this.align = align;
    }

    @Override
    public Component getListCellRendererComponent(JList list, 
                                                  Object value, 
                                                  int index, 
                                                  boolean isSelected, 
                                                  boolean cellHasFocus) {
        // DefaultListCellRenderer uses a JLabel as the rendering component:
        JLabel lbl = (JLabel)super.getListCellRendererComponent(
                list, value, index, isSelected, cellHasFocus);
        lbl.setHorizontalAlignment(align);
        return lbl;
    }
}
