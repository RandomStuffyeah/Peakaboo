package peakaboo.ui.swing;

import java.awt.Dimension;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.ezware.common.Strings;
import com.ezware.dialog.task.TaskDialog;

import commonenvironment.Env;
import peakaboo.common.PeakabooLog;
import peakaboo.common.Version;
import peakaboo.curvefit.peak.table.CombinedPeakTable;
import peakaboo.curvefit.peak.table.KrausePeakTable;
import peakaboo.curvefit.peak.table.PeakTable;
import peakaboo.curvefit.peak.table.XrayLibPeakTable;
import peakaboo.datasink.plugin.DataSinkPluginManager;
import peakaboo.datasource.plugin.DataSourcePluginManager;
import peakaboo.filter.model.FilterPluginManager;
import peakaboo.ui.swing.plotting.tabbed.TabbedPlotterFrame;
import stratus.StratusLookAndFeel;
import stratus.theme.LightTheme;
import swidget.Swidget;
import swidget.icons.IconFactory;
import swidget.icons.IconSize;
import swidget.icons.StockIcon;
import swidget.widgets.HButton;
import swidget.widgets.tabbedinterface.TabbedInterfaceDialog;



public class Peakaboo
{
	private static final Logger LOGGER = PeakabooLog.get();
	private static Timer gcTimer;
	

	private static void showError(Throwable e, String message) {
		showError(e, message, null);
	}
	
	private static void showError(Throwable e, String message, String text)
	{
		SwingUtilities.invokeLater(() -> {
			TaskDialog errorDialog = new TaskDialog("Peakaboo Error");
			errorDialog.setIcon(StockIcon.BADGE_WARNING.toImageIcon(IconSize.ICON));
			errorDialog.setInstruction(message);
			
			String realText = text;
			
			if (realText != null) {
				realText += "\n";
			} else if (e != null) {
				if (e.getMessage() != null) {
					realText = e.getMessage() + "\n";
				}
				realText += "The problem is of type " + e.getClass().getSimpleName();
			}
			
			errorDialog.setText(realText);
				
			JTextArea stacktrace = new JTextArea();
			stacktrace.setEditable(false);
			stacktrace.setText((e != null) ? Strings.stackStraceAsString(e) : "No additional information available");
			
			JScrollPane scroller = new JScrollPane(stacktrace);
			scroller.setPreferredSize(new Dimension(500, 200));
			errorDialog.getDetails().setExpandableComponent(scroller);
			errorDialog.getDetails().setExpanded(true);
		
			errorDialog.show();
		});
			
	}
	

	private static void warnDevRelease() {
		if (!Version.release){
			String message = "This build of Peakaboo is not a final release version.\nAny results you obtain should be treated accordingly.";
			String title = "Development Build of Peakaboo";
			JOptionPane optionPane = new TabbedInterfaceDialog(title, message, JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, v-> {}).getComponent();
			JDialog dialog = optionPane.createDialog(title);
			optionPane.setOptions(new Object[] {new HButton("OK", () -> dialog.setVisible(false))});
			dialog.setAlwaysOnTop(true);
			dialog.setVisible(true);
		}
	}
	
	private static void warnLowMemory() {
		LOGGER.log(Level.INFO, "Max heap size = " + Env.heapSize());
		
		if (Env.heapSize() <= 128){
			String message = "This system's Java VM is only allocated " + Env.heapSize()
			+ "MB of memory.\nProcessing large data sets may be quite slow, if not impossible.";
			String title = "Low Memory";
			JOptionPane optionPane = new TabbedInterfaceDialog(title, message, JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, v-> {}).getComponent();
			JDialog dialog = optionPane.createDialog(title);
			optionPane.setOptions(new Object[] {new HButton("OK", () -> dialog.setVisible(false))});
			dialog.setAlwaysOnTop(true);
			dialog.setVisible(true);
		}
	}
	
	private static void runPeakaboo()
	{

		//Any errors that don't get handled anywhere else come here and get shown
		//to the user and printed to standard out.
		try {
			new TabbedPlotterFrame();
		} catch (Exception e) {
			
			PeakabooLog.get().log(Level.SEVERE, "Critical Error in Peakaboo", e);
			
			//if the user chooses to close rather than restart, break out of the loop
			showError(e, "Peakaboo has encountered a problem and must exit");
			System.exit(1);
			
		}
		
	}
	
	private static void errorHook() {
		PeakabooLog.get().addHandler(new Handler() {
			
			@Override
			public void publish(LogRecord record) {
				if (record.getLevel() == Level.SEVERE) {
					Throwable t = record.getThrown();
					String m = record.getMessage();
					showError(t, m);
				}
			}
			
			@Override
			public void flush() {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void close() throws SecurityException {
				// TODO Auto-generated method stub
				
			}
		});
	}
	
	private static void setAppTitle(String title) {
		//This was broken with Java 8/9
//		try
//		{
//		    Toolkit toolkit = Toolkit.getDefaultToolkit();
//		    Field awtAppClassNameField = toolkit.getClass().getDeclaredField("awtAppClassName");
//		    awtAppClassNameField.setAccessible(true);
//		    awtAppClassNameField.set(toolkit, title);
//		}
//		catch (NoSuchFieldException | IllegalAccessException e)
//		{
//		    e.printStackTrace();
//		}
		
	}
	
	private static void startGCTimer() {
		gcTimer = new Timer(1000*60, e -> {  
			System.gc(); 
		});
		
		gcTimer.setRepeats(true);
		gcTimer.start();
	}
	
	private static void setLaF(LookAndFeel laf) {
		try {
			UIManager.setLookAndFeel(laf);
		} catch (UnsupportedLookAndFeelException e) {
			PeakabooLog.get().log(Level.WARNING, "Failed to set Look and Feel", e);
		}
	}
	
	public static void run() {
		
		//Needed to work around https://bugs.openjdk.java.net/browse/JDK-8130400
		//NEED TO SET THESE RIGHT AT THE START BEFORE ANY AWT/SWING STUFF HAPPENS.
		//THAT INCLUDES CREATING ANY ImageIcon DATA FOR SPLASH SCREEN
		System.setProperty("sun.java2d.xrender", "false");
		System.setProperty("sun.java2d.pmoffscreen", "false");
		
		
		
		LOGGER.log(Level.INFO, "Starting " + Version.longVersionNo + " - " + Version.buildDate);
		IconFactory.customPath = "/peakaboo/ui/swing/icons/";
		StratusLookAndFeel laf = new StratusLookAndFeel(new LightTheme());
		setAppTitle("Peakaboo 5");
			
		Swidget.initialize(Version.splash, Version.icon, "Peakaboo", () -> {
			setLaF(laf);
			PeakabooLog.init();
			errorHook();
			startGCTimer();
			warnLowMemory();
			warnDevRelease();
			//warm up the peak table, which is lazy
			PeakTable.SYSTEM.getAll();
			DataSourcePluginManager.SYSTEM.load();
			FilterPluginManager.SYSTEM.load();
			DataSinkPluginManager.SYSTEM.load();
			runPeakaboo();
		});
		
		
	}
	
	public static void main(String[] args)
	{	
		run();
	}


}
