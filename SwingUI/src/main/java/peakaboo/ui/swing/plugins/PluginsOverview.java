package peakaboo.ui.swing.plugins;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import commonenvironment.Apps;
import commonenvironment.Env;
import net.sciencestudio.bolt.plugin.core.BoltPlugin;
import net.sciencestudio.bolt.plugin.core.BoltPluginController;
import net.sciencestudio.bolt.plugin.core.BoltPluginSet;
import net.sciencestudio.bolt.plugin.core.IBoltPluginSet;
import net.sciencestudio.bolt.plugin.core.exceptions.BoltImportException;
import net.sciencestudio.bolt.plugin.core.BoltPluginManager;
import peakaboo.datasink.plugin.DataSinkPluginManager;
import peakaboo.datasink.plugin.JavaDataSinkPlugin;
import peakaboo.common.Configuration;
import peakaboo.common.PeakabooLog;
import peakaboo.datasink.plugin.DataSinkPlugin;
import peakaboo.datasource.plugin.DataSourcePluginManager;
import peakaboo.datasource.plugin.JavaDataSourcePlugin;
import peakaboo.datasource.plugin.DataSourcePlugin;
import peakaboo.filter.model.FilterPluginManager;
import peakaboo.filter.plugins.FilterPlugin;
import peakaboo.filter.plugins.JavaFilterPlugin;
import peakaboo.ui.swing.plotting.FileDrop;
import swidget.dialogues.fileio.SimpleFileExtension;
import swidget.dialogues.fileio.SwidgetFilePanels;
import swidget.icons.IconSize;
import swidget.icons.StockIcon;
import swidget.widgets.ButtonBox;
import swidget.widgets.ClearPanel;
import swidget.widgets.HButton;
import swidget.widgets.HeaderBox;
import swidget.widgets.HeaderBoxPanel;
import swidget.widgets.Spacing;
import swidget.widgets.tabbedinterface.TabbedInterfaceDialog;
import swidget.widgets.tabbedinterface.TabbedInterfacePanel;

public class PluginsOverview extends JPanel {

	JPanel details;
	JTree tree;
	TabbedInterfacePanel parent;
	
	JButton close, add, remove, reload, browse, download;
	
	public PluginsOverview(TabbedInterfacePanel parent) {
		super(new BorderLayout());
		
		this.parent = parent;
		
		JPanel body = new JPanel(new BorderLayout());
		setPreferredSize(new Dimension(800, 350));
		body.add(pluginTree(), BorderLayout.WEST);
		details = new JPanel(new BorderLayout());
		body.add(details, BorderLayout.CENTER);
				
		close = new HButton("Close", () -> parent.popModalComponent());
		
		add = new HButton(StockIcon.EDIT_ADD, "Import Plugins", this::add);
		remove = new HButton(StockIcon.EDIT_REMOVE, "Remove Plugins", this::removeSelected);
		
		reload = new HButton(StockIcon.ACTION_REFRESH, "Reload Plugins", this::reload);
		browse = new HButton(StockIcon.PLACE_FOLDER_OPEN, "Open Plugins Folder", this::browse);
		download = new HButton(StockIcon.GO_DOWN, "Get More Plugins", this::download);
		
		ButtonBox left = new ButtonBox(Spacing.bNone(), Spacing.medium, false);
		left.setOpaque(false);
		left.addLeft(add);
		left.addLeft(remove);
		left.addLeft(reload);
		left.addLeft(new ClearPanel()); //spacing
		left.addLeft(browse);
		left.addLeft(download);
		
		HeaderBoxPanel main = new HeaderBoxPanel(new HeaderBox(left, "Manage Plugins", close), body);
		
		this.add(main, BorderLayout.CENTER);
		
		new FileDrop(body, files -> {
			for (File file : files) {
				addJar(file);
			}
		});

		
	}
	
	private void add() {
		SwidgetFilePanels.openFile(parent, "Import Plugins", Env.homeDirectory(), new SimpleFileExtension("Peakaboo Plugin", "jar"), result -> {
			if (!result.isPresent()) {
				return;
			}
			
			addJar(result.get());
			
		});
	}
	
	private boolean isRemovable(BoltPluginController<? extends BoltPlugin> plugin) {
		return plugin.getSource() != null;
	}
	
	private BoltPluginController<? extends BoltPlugin> selectedPlugin() {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
		
		if (!(node.getUserObject() instanceof BoltPluginController<?>)) {
			return null;
		}
		
		BoltPluginController<? extends BoltPlugin> plugin = (BoltPluginController<? extends BoltPlugin>) node.getUserObject();
		return plugin;
	}
	
	private void removeSelected() {	
		BoltPluginController<? extends BoltPlugin> plugin = selectedPlugin();
		if (plugin != null) {
			remove(plugin);	
		}
	}
	
	private void remove(BoltPluginController<? extends BoltPlugin> plugin) {
		/*
		 * This is a little tricky. There's no rule that says that each plugin is in 
		 * it's own jar file. We need to confirm with the user that they want to 
		 * remove the jar file and all plugins that it contains.
		 */
		
		BoltPluginManager<? extends BoltPlugin> manager = managerForPlugin(plugin);
		if (manager == null) {
			return;
		}
		
		if (!isRemovable(plugin)) {
			return;
		}
		
		File jar;
		BoltPluginSet<? extends BoltPlugin> set;
		try {
			jar = new File(plugin.getSource().toURI());
			set = manager.pluginsInJar(jar);
		} catch (URISyntaxException e) {
			PeakabooLog.get().log(Level.WARNING, "Cannot lookup jar for plugin", e);
			return;
		}
		
		if (set.getAll().size() == 0) {
			return;
		}
		
		new TabbedInterfaceDialog(
				"Delete Plugin Archive?", 
				"Are you sure you want to delete the archive containing the plugins:\n\n" + listToUL(set.getAll()), 
				JOptionPane.QUESTION_MESSAGE,
				new HButton("Yes", () -> {
					manager.removeJar(jar);
					this.reload();
				}),
				new HButton("No")
			).showIn(parent);
		
		
	}
	
	private String listToUL(List<?> stuff) {
		StringBuffer buff = new StringBuffer();
		buff.append("<ul>");
		for (Object o : stuff) {
			buff.append("<li>" + o.toString() + "</li>");
		}
		buff.append("</ul>");
		return buff.toString();
	}
	
	private BoltPluginManager<? extends BoltPlugin> managerForPlugin(BoltPluginController<? extends BoltPlugin> plugin) {
		Class<? extends BoltPlugin> pluginBaseClass = plugin.getPluginClass();
		
		if (pluginBaseClass == JavaDataSourcePlugin.class) {
			return DataSourcePluginManager.SYSTEM;
		}
		
		if (pluginBaseClass == JavaDataSinkPlugin.class) {
			return DataSinkPluginManager.SYSTEM;
		}
		
		if (pluginBaseClass == JavaFilterPlugin.class) {
			return FilterPluginManager.SYSTEM;
		}
		
		return null;
		
	}
	
	private void addJar(File jar) {
		
		boolean added = false;
		
		try {
			added |= addJarToManager(jar, DataSourcePluginManager.SYSTEM);
			added |= addJarToManager(jar, DataSinkPluginManager.SYSTEM);
			added |= addJarToManager(jar, FilterPluginManager.SYSTEM);		
		} catch (BoltImportException e) {
		
			PeakabooLog.get().log(Level.WARNING, e.getMessage(), e);
			new TabbedInterfaceDialog(
					"Import Failed", 
					"Peakboo was unable to import the plugin\n" + e.getMessage(), 
					JOptionPane.ERROR_MESSAGE).showIn(parent);
			added = true;
		}
		
		if (!added) {
			new TabbedInterfaceDialog(
					"No Plugins Found", 
					"Peakboo could not fint any plugins in the file(s) provided", 
					JOptionPane.ERROR_MESSAGE).showIn(parent);
		}
		
		reload();
		
		

	}
	
	private boolean addJarToManager(File jar, BoltPluginManager<? extends BoltPlugin> manager) throws BoltImportException {
		
		if (!manager.jarContainsPlugins(jar)) {
			return false;
		}
		BoltPluginSet<? extends BoltPlugin> plugins = manager.importJar(jar);
		
		this.reload();
		new TabbedInterfaceDialog(
				"Imported New Plugins", 
				"Peakboo successfully imported the following plugin(s):\n" + listToUL(plugins.getAll()), 
				JOptionPane.INFORMATION_MESSAGE).showIn(parent);

		return true;


	}
	
	
	private void reload() {
		DataSourcePluginManager.SYSTEM.reload();
		DataSinkPluginManager.SYSTEM.reload();
		FilterPluginManager.SYSTEM.reload();
		tree.setModel(buildTreeModel());
	}
	
	private void browse() {
		File appDataDir = Configuration.appDir("Plugins");
		appDataDir.mkdirs();
		Desktop desktop = Desktop.getDesktop();
		try {
			desktop.open(appDataDir);
		} catch (IOException e1) {
			PeakabooLog.get().log(Level.SEVERE, "Failed to open plugin folder", e1);
		}
	}
	
	private void download() {
		Apps.browser("https://github.com/nsherry4/PeakabooPlugins");
	}
	
	private TreeModel buildTreeModel() {
		
		DefaultMutableTreeNode plugins = new DefaultMutableTreeNode("Plugins");
		
		DefaultMutableTreeNode sourcesNode = new DefaultMutableTreeNode("Data Sources");
		plugins.add(sourcesNode);
		for (BoltPluginController<? extends DataSourcePlugin> source :  DataSourcePluginManager.SYSTEM.getPlugins().getAll()) {
			DefaultMutableTreeNode node = new DefaultMutableTreeNode(source);
			sourcesNode.add(node);
		}
		
		DefaultMutableTreeNode sinksNode = new DefaultMutableTreeNode("Data Sinks");
		plugins.add(sinksNode);
		for (BoltPluginController<? extends DataSinkPlugin> source :  DataSinkPluginManager.SYSTEM.getPlugins().getAll()) {
			DefaultMutableTreeNode node = new DefaultMutableTreeNode(source);
			sinksNode.add(node);
		}
		
		DefaultMutableTreeNode filtersNode = new DefaultMutableTreeNode("Filters");
		plugins.add(filtersNode);
		for (BoltPluginController<? extends FilterPlugin> source :  FilterPluginManager.SYSTEM.getPlugins().getAll()) {
			DefaultMutableTreeNode node = new DefaultMutableTreeNode(source);
			filtersNode.add(node);
		}
		
		
		return new DefaultTreeModel(plugins);
		
	}
	
	private JComponent pluginTree() {	
			
		tree = new JTree(buildTreeModel());

		DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
		renderer.setLeafIcon(StockIcon.MISC_EXECUTABLE.toImageIcon(IconSize.BUTTON));
		renderer.setBorder(Spacing.bSmall());
		tree.setCellRenderer(renderer);
		
		tree.setRootVisible(false);
		
		
		tree.addTreeSelectionListener(tse -> {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
			details.removeAll();
			if (node == null || !node.isLeaf()) { 
				details.add(new JPanel(), BorderLayout.CENTER);
				remove.setEnabled(false);
			} else {
				details.add(new PluginView((BoltPluginController<? extends BoltPlugin>) node.getUserObject()), BorderLayout.CENTER);
				remove.setEnabled(isRemovable(selectedPlugin()));
			}
			details.revalidate();
		});
		
		
		JScrollPane scroller = new JScrollPane(tree);
		scroller.setPreferredSize(new Dimension(200, 300));
		scroller.setBorder(new EmptyBorder(0, 0, 0, 0));
		return scroller;
		
	}
	
}
