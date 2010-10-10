/*
 * Copyright 2009 Andre Pfeiler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.twodividedbyzero.idea.findbugs.gui.preferences;

import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.DetectorFactory;
import edu.umd.cs.findbugs.I18N;
import edu.umd.cs.findbugs.config.ProjectFilterSettings;
import info.clearthought.layout.TableLayout;
import org.twodividedbyzero.idea.findbugs.gui.common.TableSorter;
import org.twodividedbyzero.idea.findbugs.gui.preferences.model.BugPatternTableModel;
import org.twodividedbyzero.idea.findbugs.preferences.FindBugsPreferences;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;


/**
 * $Date$
 *
 * @author Andre Pfeiler<andrepdo@dev.java.net>
 * @version $Revision$
 * @since 0.9.90-dev
 */
@SuppressWarnings({"HardCodedStringLiteral", "AnonymousInnerClass", "AnonymousInnerClassMayBeStatic"})
public class DetectorConfiguration implements ConfigurationPage {

	private final FindBugsPreferences _preferences;
	private JTable _detectorsTable;
	private Component _component;
	private JTextArea _textArea;
	private JPanel _detailsPanel;
	private BugPatternTableModel _bugPatternModel;
	private TableSorter _tableSorter;

	private Map<DetectorFactory, String> _factoriesToBugAbbrev;
	private JCheckBox _hiddenCheckbox;
	private JCheckBox _enableAllCheckbox;
	private final ConfigurationPanel _parent;


	public DetectorConfiguration(final ConfigurationPanel parent, final FindBugsPreferences preferences) {
		//noinspection AssignmentToCollectionOrArrayFieldFromParameter
		_preferences = preferences;
		_parent = parent;
	}


	public Component getComponent() {
		if (_component == null) {

			final double border = 5;
			final double rowsGap = 5;
			final double colsGap = 10;
			final double[][] size = {{border, TableLayout.FILL, border}, // Columns
									 {border, TableLayout.PREFERRED, rowsGap, TableLayout.PREFERRED, rowsGap, TableLayout.PREFERRED, rowsGap, TableLayout.FILL, 15, 0, border}};// Rows
			final TableLayout tbl = new TableLayout(size);

			final Container mainPanel = new JPanel(tbl);
			mainPanel.add(new JLabel("Disabled detectors will not participate in the analysis."), "1, 1, 1, 1");
			mainPanel.add(new JLabel("'Grayed out' detector will run, however they will not report any result to the UI."), "1, 3, 1, 3");


			final Container checkboxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
			checkboxPanel.add(getHiddenCheckBox());
			checkboxPanel.add(getEnableAllBox());
			mainPanel.add(checkboxPanel, "1, 5, 1, 5");


			final double[][] size1 = {{TableLayout.FILL}, // Columns
									  {TableLayout.FILL}};// Rows
			final TableLayout tbl1 = new TableLayout(size1);
			final JPanel detectorPanel = new JPanel(tbl1);
			detectorPanel.add(new JScrollPane(getDetectorsTable(), JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), "0, 0, 0, 0");
			//mainPanel.add(detectorPanel, "1, 7, 1, 7");

			final JComponent scrollPane = new JScrollPane(getTextArea(), JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			getDetectorTextPanel().add(scrollPane, BorderLayout.CENTER);
			final Dimension preferredSize = getDetectorTextPanel().getPreferredSize();
			preferredSize.height = 150;
			getDetectorTextPanel().setPreferredSize(preferredSize);
			scrollPane.setBorder(null);
			//mainPanel.add(getDetectorTextPanel(), "1, 9, 1, 9");

			final Component splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, detectorPanel, getDetectorTextPanel());
			mainPanel.add(splitPane, "1, 7, 1, 7");

			_component = mainPanel;
		}
		return _component;
	}


	public void updatePreferences() {
		getHiddenCheckBox().setSelected(Boolean.valueOf(_preferences.getProperty(FindBugsPreferences.SHOW_HIDDEN_DETECTORS)));
		getModel().clear();
		populateAvailableRulesTable();
	}


	BugPatternTableModel getModel() {
		getDetectorsTable(); // init table model first
		return _bugPatternModel;
	}


	private AbstractButton getHiddenCheckBox() {
		if (_hiddenCheckbox == null) {
			_hiddenCheckbox = new JCheckBox("show hidden detector");
			_hiddenCheckbox.addItemListener(new ItemListener() {
				public void itemStateChanged(final ItemEvent e) {
					_preferences.setProperty(FindBugsPreferences.SHOW_HIDDEN_DETECTORS, e.getStateChange() == ItemEvent.SELECTED);
					getModel().clear();
					populateAvailableRulesTable();
				}
			});
		}
		return _hiddenCheckbox;
	}


	private Component getEnableAllBox() {
		if (_enableAllCheckbox == null) {
			_enableAllCheckbox = new JCheckBox("enable/disable all detectors");
			_enableAllCheckbox.addItemListener(new ItemListener() {
				public void itemStateChanged(final ItemEvent e) {
					getModel().selectAll(e.getStateChange() == ItemEvent.SELECTED);
				}
			});
		}
		return _enableAllCheckbox;
	}


	private JTable getDetectorsTable() {
		if (_detectorsTable == null) {
			_bugPatternModel = new BugPatternTableModel(_preferences);
			_tableSorter = new TableSorter(_bugPatternModel);

			_detectorsTable = new JTable(_tableSorter);

			final TableCellRenderer colorRenderer = new ColorRenderer(getModel(), _preferences);
			_detectorsTable.setDefaultRenderer(String.class, colorRenderer);

			_bugPatternModel.addTableModelListener(new TableModelListener() {
				public void tableChanged(final TableModelEvent e) {
					if (e.getColumn() == 0 && TableModelEvent.UPDATE == e.getType()) {
						final List<DetectorFactory> detectorFactoryList = _bugPatternModel.getEntries();
						final DetectorFactory detectorFactory = detectorFactoryList.get(e.getFirstRow());
						updatePreferences(detectorFactory);

						/*if(detectorFactoryList.size() <= getModel().getCheckedEntries().size()) {
							getEnableAllBox().setSelected(true);
						} else {
							getEnableAllBox().setSelected(false);
						}*/
					}
				}
			});

			_detectorsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
				public void valueChanged(final ListSelectionEvent e) {
					if (!e.getValueIsAdjusting() && e.getFirstIndex() > -1) {
						final DetectorFactory detectorFactory = _bugPatternModel.getEntries().get(e.getFirstIndex());
						final String description = getDetailedText(detectorFactory);
						getTextArea().setText(description);
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								getTextArea().scrollRectToVisible(new Rectangle(0, 0));
							}
						});
					}
				}
			});

			populateAvailableRulesTable();
			_tableSorter.setTableHeader(_detectorsTable.getTableHeader());
			_tableSorter.setSortingStatus(1, TableSorter.ASCENDING);
			TableSorter.makeSortable(_detectorsTable);

			_detectorsTable.setCellSelectionEnabled(false);
			_detectorsTable.setRowSelectionAllowed(true);
			_detectorsTable.getColumnModel().getColumn(0).setPreferredWidth(30);
			_detectorsTable.getColumnModel().getColumn(1).setPreferredWidth(350);
			_detectorsTable.getColumnModel().getColumn(2).setPreferredWidth(100);
			_detectorsTable.getColumnModel().getColumn(4).setPreferredWidth(400);
			_detectorsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
			_detectorsTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		}
		return _detectorsTable;
	}


	private JPanel getDetectorTextPanel() {
		if (_detailsPanel == null) {
			_detailsPanel = new JPanel(new BorderLayout());
			_detailsPanel.setBorder(BorderFactory.createTitledBorder("Detector details"));
		}
		return _detailsPanel;
	}


	JTextArea getTextArea() {
		if (_textArea == null) {
			_textArea = new JTextArea();
			_textArea.setEditable(false);
			_textArea.setBackground(Color.WHITE);
		}
		return _textArea;
	}


	private void populateAvailableRulesTable() {
		/*final List<DetectorFactory> allAvailableList = new ArrayList<DetectorFactory>();
		_factoriesToBugAbbrev = new HashMap<DetectorFactory, String>();
		Iterator<DetectorFactory> iterator = DetectorFactoryCollection.instance().factoryIterator();
		while (iterator.hasNext()) {
			final DetectorFactory factory = iterator.next();

			// Only configure non-hidden factories
			*//*if (factory.isHidden() && !isHiddenVisible()) {
				continue;
			}*//*

			allAvailableList.add(factory);
			addBugsAbbreviation(factory);
		}*/

		for (final Entry<String, String> entry : _preferences.getDetectors().entrySet()) {
			final DetectorFactory factory = FindBugsPreferences.getDetectorFactorCollection().getFactory(entry.getKey());
			if (factory != null) {
				// Only configure non-hidden factories
				if (factory.isHidden() && !getHiddenCheckBox().isSelected()) {
					continue;
				}
				_bugPatternModel.add(factory);
			}
		}
	}


	private void updatePreferences(final DetectorFactory factory) {
		for (final Entry<String, String> entry : _preferences.getDetectors().entrySet()) {
			if (entry.getKey().equals(factory.getShortName())) {
				entry.setValue(String.valueOf(_preferences.getUserPreferences().isDetectorEnabled(factory)));
				_preferences.setModified(true);
			}
		}
	}

	/*public static boolean isDetectorEnabled(final FindBugsPreferences preferences, final DetectorFactory factory) {
		final Map<String, String> detectors = preferences.getDetectors();
		final String shortName = factory.getShortName();
		if(detectors.containsKey(shortName)) {
			for (final Entry<String, String> entry : detectors.entrySet()) {
				if(shortName.equals(entry.getKey()) && "true".equals(entry.getValue())) {
					return true;
				}
			}
		}
		return false;
	}*/


	private static String getDetailedText(final DetectorFactory factory) {
		if (factory == null) {
			return "";
		}
		final StringBuilder sb = new StringBuilder(factory.getFullName());
		sb.append("\n");
		sb.append(getDescriptionWithoutHtml(factory));
		sb.append("\n\nReported patterns:\n");
		final Collection<BugPattern> patterns = factory.getReportedBugPatterns();
		for (Iterator<BugPattern> iter = patterns.iterator(); iter.hasNext();) {
			final BugPattern pattern = iter.next();
			sb.append(pattern.getType()).append(" ").append(" (").append(pattern.getAbbrev()).append(", ").append(pattern.getCategory()).append("):").append("  ");
			sb.append(pattern.getShortDescription());
			if (iter.hasNext()) {
				sb.append("\n");
			}
		}
		if (patterns.isEmpty()) {
			sb.append("none");
		}
		return sb.toString();
	}


	/**
	 * Tries to trim all the html out of the {@link DetectorFactory#getDetailHTML()}
	 * return value. See also private PluginLoader .init() method.
	 * @param factory
	 * @return
	 */
	private static String getDescriptionWithoutHtml(final DetectorFactory factory) {
		String detailHTML = factory.getDetailHTML();
		// cut beginning and the end of the html document
		detailHTML = trimHtml(detailHTML, "<BODY>", "</BODY>");
		// replace any amount of white space with newline inbetween through one space
		detailHTML = detailHTML.replaceAll("\\s*[\\n]+\\s*", " ");
		// remove all valid html tags
		detailHTML = detailHTML.replaceAll("<[a-zA-Z]+>", "");
		detailHTML = detailHTML.replaceAll("</[a-zA-Z]+>", "");
		// convert some of the entities which are used in current FB messages.xml
		detailHTML = detailHTML.replaceAll("&nbsp;", "");
		detailHTML = detailHTML.replaceAll("&lt;", "<");
		detailHTML = detailHTML.replaceAll("&gt;", ">");
		detailHTML = detailHTML.replaceAll("&amp;", "&");
		return detailHTML.trim();
	}


	private static String trimHtml(final String detailHTML, final String startTag, final String endTag) {
		String detailHTML1 = detailHTML;
		if (detailHTML1.contains(startTag)) {
			detailHTML1 = detailHTML1.substring(detailHTML1.indexOf(startTag) + startTag.length());
		}
		if (detailHTML1.contains(endTag)) {
			detailHTML1 = detailHTML1.substring(0, detailHTML1.lastIndexOf(endTag));
		}
		return detailHTML1;
	}


	public static String getBugsCategories(final DetectorFactory factory) {
		final Collection<BugPattern> patterns = factory.getReportedBugPatterns();
		String category = null;
		final Collection<String> categories = new TreeSet<String>();
		for (final BugPattern bugPattern : patterns) {
			final String category2 = bugPattern.getCategory();
			if (category == null) {
				category = category2;
			} else if (!category.equals(category2)) {
				categories.add(category);
				categories.add(category2);
			}
		}
		if (!categories.isEmpty()) {
			final StringBuilder sb = new StringBuilder();
			for (final String string : categories) {
				sb.append(I18N.instance().getBugCategoryDescription(string)).append("|");
			}
			category = sb.toString();
		} else {
			category = I18N.instance().getBugCategoryDescription(category);
		}
		return category == null ? "" : category;
	}


	public void addBugsAbbreviation(final DetectorFactory factory) {
		_factoriesToBugAbbrev.put(factory, createBugsAbbreviation(factory));
	}


	public String getBugsAbbreviation(final DetectorFactory factory) {
		String abbr = _factoriesToBugAbbrev.get(factory);
		if (abbr == null) {
			abbr = createBugsAbbreviation(factory);
		}
		return abbr;
	}


	public static String createBugsAbbreviation(final DetectorFactory factory) {
		final StringBuilder sb = new StringBuilder();
		final Collection<BugPattern> patterns = factory.getReportedBugPatterns();
		final HashSet<String> abbrs = new LinkedHashSet<String>();
		for (final BugPattern pattern : patterns) {
			final String abbr = pattern.getAbbrev();
			abbrs.add(abbr);
		}
		for (Iterator<String> iter = abbrs.iterator(); iter.hasNext();) {
			final String element = iter.next();
			sb.append(element);
			if (iter.hasNext()) {
				sb.append("|");
			}
		}
		return sb.toString();
	}


	public void setEnabled(final boolean enabled) {
		getDetectorTextPanel().setEnabled(enabled);
		getDetectorsTable().setEnabled(enabled);
		getEnableAllBox().setEnabled(enabled);
		getHiddenCheckBox().setEnabled(enabled);
		getTextArea().setEnabled(enabled);
	}


	public boolean showInModulePreferences() {
		return true;
	}


	private static class ColorRenderer extends JLabel implements TableCellRenderer {

		private static final Color BG_COLOR = new Color(226, 225, 225);
		private static final Color FG_COLOR = new Color(125, 124, 124);
		private static final Color ROW_COLOR = new Color(249, 247, 247);

		private final BugPatternTableModel _model;
		private final FindBugsPreferences _preferences;


		private ColorRenderer(final BugPatternTableModel model, final FindBugsPreferences preferences) {
			_model = model;
			//noinspection AssignmentToCollectionOrArrayFieldFromParameter
			_preferences = preferences;
			setOpaque(true);
		}


		public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
			final DetectorFactory factory = _model.getEntries().get(row);
			if (value != null) {
				setText(value.toString());
			}

			if (isSelected) {
				setBackground(table.getSelectionBackground());
				setForeground(table.getSelectionForeground());
			} else {
				setBackground(table.getBackground());
				setForeground(table.getForeground());

				if (row % 2 == 0 && !isSelected) {
					setBackground(ROW_COLOR);
				} else {
					setBackground(getBackground());
				}

				if (factory != null && !isFactoryVisible(factory)) {
					setBackground(BG_COLOR);
					setForeground(FG_COLOR);
				}
			}

			return this;
		}


		/**
		 * Return whether or not given DetectorFactory reports bug patterns
		 * in one of the currently-enabled set of bug categories.
		 *
		 * @param factory the DetectorFactory
		 * @return true if the factory reports bug patterns in one of the
		 *         currently-enabled bug categories, false if not
		 */
		private boolean isFactoryVisible(final DetectorFactory factory) {
			//final Map<DetectorFactory, Boolean> enabledDetectors = tab.propertyPage.getVisibleDetectors();
			//final Boolean enabled = enabledDetectors.get(factory);
			final List<DetectorFactory> enableDetectors = _model.getCheckedEntries();
			final Boolean enabled = enableDetectors.contains(factory);

			if (enabled != null) {
				return enabled;
			}
			final ProjectFilterSettings filterSettings = _preferences.getUserPreferences().getFilterSettings();
			for (final BugPattern pattern : factory.getReportedBugPatterns()) {
				if (filterSettings.containsCategory(pattern.getCategory())) {
					//enabledDetectors.put(factory, Boolean.TRUE);
					return true;
				}
			}
			//enabledDetectors.put(factory, Boolean.FALSE);
			return false;
		}
	}
}