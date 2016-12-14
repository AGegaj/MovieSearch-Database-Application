import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;


public class hw3 extends MouseAdapter 
implements ActionListener, FocusListener, PopupMenuListener {
    
    /*
     * Setup connection string
     */
	  	private static final String DB_HOST = "localhost";
	    private static final int DB_PORT = 1521;
	    private static final String DB_NAME = "orcl";
	    private static final String DB_USER = "SH";
	    private static final String DB_PWD = "please create your own password (account) for Oracle DB";
    
	private static final int FONT_SIZE = 16;
    
    /** The connection to database */
    private Connection conn = null;
    
	/** The main frame of the GUI */
    private JFrame mainFrame;
    
    /** Indicate that whether a background task is running */
    private boolean workerRunning;
    
    /** List of genres for selecting in the search */
    private DefaultListModel<SelectableItem> genres = new DefaultListModel<>();
    
    /** List of countries for selecting in the search */
    private DefaultListModel<SelectableItem> countries = new DefaultListModel<>();
    
    /** Array of text field to search by actors */
    private List<JComboBox<String>> actorFields = new ArrayList<>(4);
    
    /** Text field for filling in director to search */
    private JComboBox<String> directorField = new JComboBox<>(new DefaultComboBoxModel<>());
    
    /** Text field for filling in rating value to search */
    private JTextField ratingField = new JTextField("0", 2);
    
    /** Combo box to select a rating comparator */
    private JComboBox<String> ratingComparatorBox = 
    		new JComboBox<String>(new String[] {"=", "<", ">", "<=", ">="});

    /** Text field for filling in number of ratings to search */
    private JTextField numRatingsField = new JTextField("0", 2);

    /** Combo box to select a comparator to compare number of ratings */
    private JComboBox<String> numRatingsComparatorBox = 
    		new JComboBox<>(new String[] {"=", "<", ">", "<=", ">="});
    
    /** Text field for filling in year value (from) to search */
    private JTextField fromYearField = new JTextField("", 4);
    
    /** Text field for filling in year value (to) to search */
    private JTextField toYearField = new JTextField("", 4);
    
    /** Text field for filling in user ID to search */
    private JTextField userIdField = new JTextField("", 8);
    
    private JTextField fromUserTimeField = new JTextField("mm/dd/yyyy", 8);
    
    private JTextField toUserTimeField = new JTextField("mm/dd/yyyy", 8);
    
    private JComboBox<String> userRatingsComparatorBox = 
    		new JComboBox<>(new String[] {"=", "<", ">", "<=", ">="});
    
    private JTextField userRatingField = new JTextField("", 8);
    
    /** Combo box to select a search operator */
    private JComboBox<String> searchOperatorBox = new JComboBox<>(new String[] {
			"Select AND between attributes",
			"Select OR between attributes"
	});
	
    /** Text area to display the query */
	private JTextArea queryArea = new JTextArea();
	
	/** Label for the search result */
	private JLabel resultLabel = new JLabel("Result");
	
	/** List to store search result */
	private MovieTableModel movieList = new MovieTableModel();
	
	class MovieItem {
		int id;
		String title;
		int year;
		double topCriticsRating;
		int numTopCriticsReviews;
		double allCriticsRating;
		int numAllCriticsReviews;
		double audienceRating;
		int numAudicenceReviews;
		
		String countries;
		String genres;
		String tags;

		public MovieItem(ResultSet rs) throws SQLException {
			id = rs.getInt("ID");
			title = rs.getString("title");
			year = rs.getInt("year");
			
			topCriticsRating = rs.getDouble("all_critics_rating");
			numTopCriticsReviews = rs.getInt("all_critics_num_reviews");
			
			allCriticsRating = rs.getDouble("top_critics_rating");
			numAllCriticsReviews = rs.getInt("top_critics_num_reviews");

			audienceRating = rs.getDouble("audience_rating");
			numAudicenceReviews = rs.getInt("audience_num_ratings");
			
			countries = "...";
			genres = "...";
			tags = "...";
		}
		
		public String toString() {
			return title + " (ID: " + id + ")";
		}
	}
	
	class MovieTableModel extends AbstractTableModel {
		
		private String[] columnNames = new String[] {
				"ID", "Title", "Year", "Countries", "Genres", "Tags",
				"Top Critics Rating", "Top Critics Reviews",
				"All Critics Rating", "All Critics Reviews" ,
				"Audience Rating", "Audience Reviews"
		};
		
		private List<MovieItem> data = new ArrayList<>();

		@Override
		public int getRowCount() {
			return data.size();
		}

		@Override
		public int getColumnCount() {
			return columnNames.length;
		}

		@Override
		public String getColumnName(int column) {
			return columnNames[column];
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			MovieItem item = data.get(rowIndex);
			switch (columnIndex) {
			case 0:
				return item.id;
			case 1:
				return item.title;
			case 2:
				return item.year;
			case 3:
				return item.countries;
			case 4:
				return item.genres;
			case 5:
				return item.tags;
			case 6:
				return item.topCriticsRating;
			case 7:
				return item.numTopCriticsReviews;
			case 8:
				return item.allCriticsRating;
			case 9:
				return item.numAllCriticsReviews;
			case 10:
				return item.audienceRating;
			case 11:
				return item.numAudicenceReviews;
			default:
				return null;
			}
		}

		public void clear() {
			data.clear();
		}

		public void addElement(MovieItem item) {
			data.add(item);
			
			int row = data.size() - 1;
			
			// Get movie's locations (country)
			SwingUtilities.invokeLater(new Runnable() {
				
				@Override
				public void run() {
					String sql = "SELECT DISTINCT l.country "
							+ "FROM Movie m INNER JOIN MovieCountry l ON m.ID = l.movie_id "
							+ "WHERE m.ID = " + item.id + " ORDER BY l.country";
					
					new ListValuesQuery(sql) {
						@Override
						public void onResult(List<String> result) {
							StringBuilder sb = new StringBuilder();
							for (String v: result) {
								sb.append(v).append(", ");
							}
							item.countries = (sb.length() > 0) ? sb.substring(0, sb.length() - 2) : "";
							fireTableCellUpdated(row, 3);
						}
					};
				}
			});
			// Get movie's genres
			SwingUtilities.invokeLater(new Runnable() {
				
				@Override
				public void run() {
					String sql = "SELECT g.genre "
							+ "FROM Movie m INNER JOIN Genres g ON m.ID = g.movie_id "
							+ "WHERE m.ID = " + item.id + " ORDER BY g.genre";
					
					new ListValuesQuery(sql) {
						@Override
						public void onResult(List<String> result) {
							StringBuilder sb = new StringBuilder();
							for (String v: result) {
								sb.append(v).append(", ");
							}
							item.genres = (sb.length() > 0) ? sb.substring(0, sb.length() - 2) : "";
							fireTableCellUpdated(row, 4);
						}
					};
				}
			});
			// Get movie's tags
			SwingUtilities.invokeLater(new Runnable() {
				
				@Override
				public void run() {
					String sql = "SELECT DISTINCT t.value "
			    			+ "FROM Movie m INNER JOIN UserTaggedMovie u ON m.ID = u.movie_id "
			    			+ "INNER JOIN Tag t ON u.tag_id = t.ID "
			    			+ "WHERE m.ID = " + item.id + " ORDER BY t.value";
					
					new ListValuesQuery(sql) {
						@Override
						public void onResult(List<String> result) {
							StringBuilder sb = new StringBuilder();
							for (String v: result) {
								sb.append(v).append(", ");
							}
							item.tags = (sb.length() > 0) ? sb.substring(0, sb.length() - 2) : "";
							fireTableCellUpdated(row, 5);
						}
					};
				}
			});
		}
	}
	
	class SelectableItem {
		String value;
		boolean selected;
		
		public SelectableItem(String v) {
			value = v;
			selected = false;
		}
	}
	
	class SelectableItemRenderer extends JCheckBox 
	implements ListCellRenderer<SelectableItem> {

		@Override
		public Component getListCellRendererComponent(
				JList<? extends SelectableItem> list, SelectableItem value,
				int index, boolean isSelected, boolean cellHasFocus) {

			setEnabled(list.isEnabled());
			setSelected(value.selected);
			setText(value.value);
			
			return this;
		}
		
	}
	
	abstract class DropDownSearch implements DocumentListener {
		
		JComboBox<String> actorField;
				
		boolean searching = false;
		
		public DropDownSearch(JComboBox<String> combo) {
			actorField = combo;
			
			((JTextComponent) combo.getEditor().getEditorComponent())
			.getDocument().addDocumentListener(this);
		}

		private synchronized void searchText(String text) {
			
			if (text.length() == 0)
				return;

			if (searching)
				return;

			// Making sure the typed text does not appear in the dropdown
			DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) actorField.getModel();
			if (model.getIndexOf(text) > 0) {
				return;
			}
			
			searching = true;
			
			new ListValuesQuery(buildQuery(text)) {
				
				@Override
				public void onResult(List<String> result) {
					actorField.hidePopup();

					DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) actorField.getModel();
					model.removeAllElements();
					model.addElement(text);
					
					if (result != null) {
						for (String name: result) {
							model.addElement(name);
						}
					}
					
					actorField.showPopup();
					searching = false;
				}
			};
		}
		
		protected abstract String buildQuery(String text);
		
		@Override
		public void insertUpdate(DocumentEvent e) {
			try {
				Document d = e.getDocument();
				searchText(d.getText(0, d.getLength()));
			} catch (BadLocationException ex) {
			}
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			try {
				Document d = e.getDocument();
				searchText(d.getText(0, d.getLength()));
			} catch (BadLocationException ex) {
			}
		}

		@Override
		public void changedUpdate(DocumentEvent e) {
		}		
	}
	
	class ActorSearch extends DropDownSearch {

		public ActorSearch(JComboBox<String> combo) {
			super(combo);
		}

		@Override
		protected String buildQuery(String text) {
			String select = "SELECT DISTINCT full_name FROM Person p, Cast c";
			String where = " WHERE p.ID = c.actor_id AND full_name LIKE '" + text + "%'";
			
			if (searchOperatorBox.getSelectedIndex() == 0) {
				// Search actors by genres and countries
				
				// List of selected genres and countries
				String selectedGenres = join(genres);
				String selectedCountries = join(countries);
				
				if (selectedGenres.length() > 0 || selectedCountries.length() > 0) {
					select += ", Movie m";
					where += " AND c.movie_id = m.ID";
				}
				
				// select actors by genres
				if (selectedGenres.length() > 0) {
					select += ", Genres g";
					where += " AND g.movie_id = m.ID AND g.genre IN (" + selectedGenres + ")";
				}
				
				// select actor by countries
				if (selectedCountries.length() > 0) {
					select += ", MovieCountry l";
					where += " AND l.movie_id=m.ID AND l.country IN (" + selectedCountries + ")";
				}
			}
			
			String sql = select + where + " ORDER BY full_name";
			return sql;
		}
	}
	
	class DirectorSearch extends DropDownSearch {

		public DirectorSearch(JComboBox<String> combo) {
			super(combo);
		}

		@Override
		protected String buildQuery(String text) {
			String select = "SELECT DISTINCT full_name FROM Person p, Movie m";
			String where = " WHERE p.ID = m.director_id AND full_name LIKE '"+ text + "%'";
			
			if (searchOperatorBox.getSelectedIndex() == 0) {
				// Search director by genres and countries
				
				// List of selected genres and countries
				String selectedGenres = join(genres);
				String selectedCountries = join(countries);
				
				// select director by genres
				if (selectedGenres.length() > 0) {
					select += ", Genres g";
					where += " AND g.movie_id = m.ID AND g.genre IN (" + selectedGenres + ")";
				}
				
				// select director by countries
				if (selectedCountries.length() > 0) {
					select += ", MovieCountry l";
					where += " AND l.movie_id=m.ID AND l.country IN (" + selectedCountries + ")";
				}
			}
			
			String sql = select + where + " ORDER BY full_name";
			return sql;
		}
	}
	
    public hw3() {
    	
    	connectDB();
    	
    	setupGUI();
    }

    private void setupGUI() {
    	
		mainFrame = new JFrame("Movie Search");
		mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		mainFrame.setLayout(new BorderLayout());
		mainFrame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				closeDB();
			}
		});
		
		mainFrame.add(createSearchPanel(), BorderLayout.PAGE_START);
		mainFrame.add(createResultPanel(), BorderLayout.CENTER);
		
		listGenres();
		listCountries();
		buildQuery();
		
		mainFrame.pack();
		mainFrame.setVisible(true);
	}

	private JPanel createSearchPanel() {
		JPanel panel = new JPanel(new GridLayout(1, 5));
		
		JPanel container;

		// Panel to select Genres
		container = new JPanel();
		container.setBorder(new TitledBorder("Genres"));
		container.setLayout(new GridLayout(1, 1));
		container.add(createCheckboxList(genres));
		panel.add(container);
		
		// Panel to select Countries
		container = new JPanel();
		container.setBorder(new TitledBorder("Country"));
		container.setLayout(new GridLayout(1, 1));
		container.add(createCheckboxList(countries));
		panel.add(container);
		
		// Panel to search Cast
		container = new JPanel(new BorderLayout());
		{
			JPanel inner = new JPanel();
			inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
			createActorSearchPanel(inner);
			container.add(inner, BorderLayout.NORTH);
		}
		panel.add(container);
		
		// Panel to search by Rating
		container = new JPanel(new BorderLayout());
		{
			JPanel inner = new JPanel();
			inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
			createRatingSearchPanel(inner);
			container.add(inner, BorderLayout.NORTH);
		}
		panel.add(container);

		// Panel to search for Users' Tags and Rating
		container = new JPanel(new BorderLayout());
		container.setBorder(new TitledBorder("Users' Tags and Rating"));
		{
			JPanel inner = new JPanel();
			inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
			createUserSearchPanel(inner);
			container.add(inner, BorderLayout.NORTH);
		}
		panel.add(container);
		
		return panel;
	}

	private Component createResultPanel() {
		
		JPanel queryPane = new JPanel(new BorderLayout());

		queryArea.setEditable(false);
		queryArea.setFont(new Font("Courier", Font.PLAIN, FONT_SIZE));
		queryArea.setLineWrap(true);
		queryArea.setWrapStyleWord(true);
		
		queryPane.add(new JScrollPane(queryArea), BorderLayout.CENTER);
		
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		panel.add(new JLabel("Search for:"));
		searchOperatorBox.addActionListener(this);
		panel.add(searchOperatorBox);
		queryPane.add(panel, BorderLayout.NORTH);

		JButton execButton = new JButton("Execute Query");
		execButton.setActionCommand("ExecuteQuery");
		execButton.addActionListener(this);
		queryPane.add(execButton, BorderLayout.SOUTH);
		
		JPanel resultPane = new JPanel(new BorderLayout());	
		resultPane.add(resultLabel, BorderLayout.NORTH);
		
		JTable resultTable = new JTable(movieList);
		resultPane.add(new JScrollPane(resultTable), BorderLayout.CENTER);
		
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				queryPane, resultPane);
		splitPane.setPreferredSize(new Dimension(1000, 500));
		splitPane.setDividerLocation(300);
		splitPane.setResizeWeight(0.3);
		return splitPane;
	}

	/* Create a scrollable list box that shows list of SelectableItems */
	private Component createCheckboxList(ListModel<SelectableItem> data) {
		
		JList<SelectableItem> list = new JList<>(data);
		
		list.setCellRenderer(new SelectableItemRenderer());
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.addMouseListener(this);
		
		JScrollPane listScroller = new JScrollPane(list);
		listScroller.setPreferredSize(new Dimension(100, 250));
		return listScroller;
	}
	
	/* Create a panel containing components to search for actors */
	private void createActorSearchPanel(JPanel container) {
		
		JPanel castPanel = new JPanel(new GridLayout(4, 1));
		castPanel.setBorder(new TitledBorder("Actor / Actress"));
		container.add(castPanel);
		
		for (int i = 0; i < 4; i++) {			
			JComboBox<String> actorField = new JComboBox<>(new DefaultComboBoxModel<String>());
			actorFields.add(actorField);
			
			actorField.setEditable(true);
			actorField.getEditor().getEditorComponent().addFocusListener(this);
			new ActorSearch(actorField);
			
			castPanel.add(actorField);
		}
		
		JPanel directorPanel = new JPanel(new GridLayout(1, 1));
		directorPanel.setBorder(new TitledBorder("Director"));
		container.add(directorPanel);
		{
			directorField = new JComboBox<>(new DefaultComboBoxModel<String>());
			directorField.setEditable(true);
			directorField.getEditor().getEditorComponent().addFocusListener(this);
			new DirectorSearch(directorField);

			directorPanel.add(directorField);
		}
	}
	
	/* Create a panel containing components to search for rating */
	private void createRatingSearchPanel(JPanel container) {
		
		JPanel panel;
		
		// Add listeners
		ratingComparatorBox.addActionListener(this);
		numRatingsComparatorBox.addActionListener(this);
		ratingField.addActionListener(this);
		ratingField.addFocusListener(this);
		
		numRatingsField.addActionListener(this);
		numRatingsField.addFocusListener(this);
		
		fromYearField.addActionListener(this);
		fromYearField.addFocusListener(this);
		
		toYearField.addActionListener(this);
		toYearField.addFocusListener(this);

		
		panel = new JPanel();
		panel.setBorder(new TitledBorder("Rating"));
		panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
		panel.add(ratingComparatorBox);
		panel.add(ratingField);
		container.add(panel);
		
		panel = new JPanel();
		panel.setBorder(new TitledBorder("# Ratings"));
		panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
		panel.add(numRatingsComparatorBox);
		panel.add(numRatingsField);
		container.add(panel);
		
		panel = new JPanel();
		panel.setBorder(new TitledBorder("Movie Year"));
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		panel.add(new JLabel("From:"));
		panel.add(fromYearField);
		panel.add(new JLabel("To:"));
		panel.add(toYearField);
		container.add(panel);
		
		container.add(Box.createVerticalGlue());
	}

	/* Create a panel containing components to search for users */
	private void createUserSearchPanel(JPanel container) {
		JPanel panel;
		
		userIdField.addActionListener(this);
		userIdField.addFocusListener(this);
		
		fromUserTimeField.addActionListener(this);
		fromUserTimeField.addFocusListener(this);
		
		toUserTimeField.addActionListener(this);
		toUserTimeField.addFocusListener(this);
		
		userRatingsComparatorBox.addActionListener(this);
		
		userRatingField.addActionListener(this);
		userRatingField.addFocusListener(this);
		
		panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
		panel.add(new JLabel("User Id:"));
		panel.add(userIdField);
		container.add(panel);
		
		panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
		panel.add(new JLabel("From:"));
		panel.add(fromUserTimeField);
		container.add(panel);
		
		panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
		panel.add(new JLabel("To:"));
		panel.add(toUserTimeField);
		container.add(panel);
		
		panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
		panel.add(new JLabel("User's Rating"));
		panel.add(userRatingsComparatorBox);
		container.add(panel);
		
		panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
		panel.add(new JLabel("Value:"));
		panel.add(userRatingField);
		container.add(panel);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		
		if ("ExecuteQuery".equals(cmd)) {
			executeQuery();
			
		} else {
			buildQuery();
						
			if (e.getSource() == searchOperatorBox) {
				listCountries();
			}
		}
	}

	@Override
	public void focusLost(FocusEvent e) {
		if (e.isTemporary()) {
			return;
		}
	
		Object src = e.getSource();
		if (src == ratingField) {
			getInt(ratingField.getText(), true);
		} else if (src == numRatingsField) {
			getInt(numRatingsField.getText(), true);
		} else if (src == fromYearField) {
			getInt(fromYearField.getText(), true);
		} else if (src == toYearField) {
			getInt(toYearField.getText(), true);
		} else if (src == userIdField) {
			getInt(userIdField.getText(), true);
		} else if (src == fromUserTimeField) {
			getDate(fromUserTimeField.getText(), true);
		} else if (src == toUserTimeField) {
			getDate(toUserTimeField.getText(), true);
		} else if (src == userRatingField) {
			getInt(userRatingField.getText(), true);

		} else if (src == directorField.getEditor().getEditorComponent()) {
			
			String director = (String) directorField.getSelectedItem();
			if (director != null && director.length() > 0)
			new ListValuesQuery("SELECT full_name FROM Person WHERE full_name='" + escapeSQL(director) + "'") {
				@Override
				public void onResult(List<String> result) {
					if (result == null || result.size() == 0) {
						JOptionPane.showMessageDialog(mainFrame, 
								"Director name: " + director + " not found",
								"Invalid Input", JOptionPane.ERROR_MESSAGE);
					}
				}
			};
			
		} else {
			
			for (JComboBox<String> actorField: actorFields) {
				if (src == actorField.getEditor().getEditorComponent()) {
					String actor = (String) actorField.getSelectedItem();
					if (actor != null && actor.length() > 0)
					new ListValuesQuery("SELECT full_name FROM Person WHERE full_name='" + escapeSQL(actor) + "'") {
						@Override
						public void onResult(List<String> result) {
							if (result == null || result.size() == 0) {
								JOptionPane.showMessageDialog(mainFrame, 
										"Actor name: " + actor + " not found",
										"Invalid Input", JOptionPane.ERROR_MESSAGE);
							}
						}
					};
				}
			}
		}
		
		buildQuery();
	}


	@Override
	public void focusGained(FocusEvent e) {}
	
	@Override
	public void mouseClicked(MouseEvent event) {
		if (workerRunning) {
			return;
		}
		
		Object source = event.getSource();
		if (source instanceof JList) {
			JList list = (JList) source;
			
			int index = list.locationToIndex(event.getPoint());
			if (index >= 0) {
				ListModel model = list.getModel();
				
				SelectableItem item = (SelectableItem) model.getElementAt(index);
				item.selected = !item.selected;
				
				list.repaint(list.getCellBounds(index, index));
				
				buildQuery();
				
				if (model == genres) {
					// update countries list
					listCountries();
				}
			}
		}
		
	}
	
	/**
	 * Build the query from current state of the search parameters
	 */
	private void buildQuery() {
		List<String> conditions = new ArrayList<String>();		
		
		// List of selected genres
		String selectedGenres = join(genres);
		if (selectedGenres.length() == 0) {
			queryArea.setText("");
			return; // at least one genre is required
		}
		
		String select = "SELECT DISTINCT m.ID, m.title, m.year, "
	    			+ "m.all_critics_rating, m.top_critics_rating, m.audience_rating, "
	    			+ "m.all_critics_num_reviews, m.top_critics_num_reviews, m.audience_num_ratings "
	    			+ "FROM Movie m, Genres g";
		conditions.add("(m.ID = g.movie_id AND g.genre IN (" + selectedGenres + "))");
		
		// List of selected countries
		String selectedCountries = join(countries);
		if (selectedCountries.length() > 0) {
			select += ", MovieCountry l";
			conditions.add("l.movie_id=m.ID AND l.country IN (" + selectedCountries + ")");
		}
		
		// Director
		if (directorField.getSelectedItem() != null) {
			String director = directorField.getSelectedItem().toString();
			if (director.length() > 0) {
				select += ", Person d";
				conditions.add("(m.director_id = d.ID AND d.full_name = '" + escapeSQL(director) + "')");
			}
		}
		
		// Actor list
		String selectedActors = joinActors(actorFields);
		if (selectedActors.length() > 0) {
			select += ", Person a, Cast c";
			conditions.add("(c.movie_id = m.ID AND c.actor_id = a.ID AND a.full_name IN (" + selectedActors + "))");
		}
		
		// Movie Year
		int fromYear = getInt(fromYearField.getText(), false);
		int toYear = getInt(toYearField.getText(), false);
		if (fromYear > 0) {
			conditions.add("m.year >= " + fromYear);
		}
		if (toYear > 0) {
			conditions.add("m.year <= " + toYear);
		}
		
		// Rating
		int rating = getInt(ratingField.getText(), false);
		if (rating > 0) {
			String op = String.valueOf(ratingComparatorBox.getSelectedItem());
			conditions.add("(m.all_critics_rating + m.top_critics_rating + m.audience_rating) / 3 " + op + " " + rating);
		}
		
		// Number of Ratings
		int numRating = getInt(numRatingsField.getText(), false);
		if (numRating > 0) {
			String op = String.valueOf(numRatingsComparatorBox.getSelectedItem());
			conditions.add("(m.all_critics_num_reviews + m.top_critics_num_reviews + m.audience_num_ratings) / 3 " + op + " " + numRating);
		}
		
		// User Rating
		int userId = getInt(userIdField.getText(), false);
		int userRating = getInt(userRatingField.getText(), false);
		String fromDate = getDate(fromUserTimeField.getText(), false);
		String toDate = getDate(toUserTimeField.getText(), false);		
		if (userId > 0 || userRating > 0 || fromDate.length() > 0 || toDate.length() > 0) {
			
			select += ", Rating r";
			String condition = "r.movie_id = m.ID";
			
			if (userId > 0) {
				condition += " AND r.user_id = " + userId;
			}
			
			if (fromDate.length() > 0) {
				condition += " AND r.ts >= TO_TIMESTAMP('" + fromDate + "', 'MM/DD/YYYY')";
			}
			if (toDate.length() > 0) {
				condition += " AND r.ts <= TO_TIMESTAMP('" + toDate + "', 'MM/DD/YYYY')";
			}
			
			if (userRating > 0) {
				String op = String.valueOf(userRatingsComparatorBox.getSelectedItem());
				condition += " AND r.stars " + op + " " + userRating;
			}
			
			conditions.add("(" + condition + ")");
		}
		
		// AND or OR between conditions
		String conditionOp = searchOperatorBox.getSelectedIndex() == 0 ? "AND" : "OR";
		
		// Show the query
		queryArea.setText(select);
		
		if (conditions.size() > 0) {
			queryArea.append("\nWHERE " + conditions.get(0));
			for (int i = 1; i < conditions.size(); i++) {
				queryArea.append("\n  " + conditionOp + " " + conditions.get(i));
			}
		}
	}
	

	/**
	 * Execute the query in the query text area
	 */
	private void executeQuery() {
		buildQuery();
		
		String sql = queryArea.getText();
		if (sql.isEmpty()) {
			JOptionPane.showMessageDialog(null, "Please select at least one genre", 
					"Executing Query", JOptionPane.WARNING_MESSAGE);
			return;
		}
		
		movieList.clear();
		try {
			Statement stmt = conn.createStatement();
	    	ResultSet rs = stmt.executeQuery(sql);
	    	
	    	while (rs.next()) {
	    		MovieItem item = new MovieItem(rs);
	    		movieList.addElement(item);
	    	}
	    	
	    	rs.close();
	    	stmt.close();
	    	
	    	resultLabel.setText(movieList.getRowCount() + " Movies Found");
	    	movieList.fireTableDataChanged();

		} catch (SQLException e) {
			//e.printStackTrace();
			JOptionPane.showMessageDialog(null, e.getMessage(), 
					"Query Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
     * Open connection to database
     */
    private void connectDB() {        
        try {
            
            String dbURL = "jdbc:oracle:thin:@" + DB_HOST + ":" + DB_PORT 
                    + ":" + DB_NAME;
            
            DriverManager.registerDriver(new oracle.jdbc.OracleDriver()); 
            conn = DriverManager.getConnection(dbURL, DB_USER, DB_PWD);
            
        } catch (SQLException e) {
            System.out.println("Error connecting database: " + e.getMessage());
            System.exit(1);
        }
    }
    
    /**
     * Close the database connection
     */
    private void closeDB() {
        if (conn != null) {
            try {
                conn.close();
             } catch (SQLException e) {
                System.out.println("Error closing connection: " + e.getMessage());
            }   
        }
    }
    
    private List<String> getListValues(String query) throws SQLException {
    	List<String> values = new ArrayList<String>();
    	
		Statement stmt = conn.createStatement();
    	ResultSet rs = stmt.executeQuery(query);
    	
    	while (rs.next()) {
    		values.add(rs.getString(1));
    	}
    	
    	rs.close();
    	stmt.close();
    	
    	return values;
    }
    
    private abstract class ListValuesQuery extends SwingWorker<List<String>, String> {
    	
    	private final String query;

		public ListValuesQuery(String query) {
			this.query = query;
			
			synchronized (mainFrame) {
				mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				workerRunning = true;
			}
			execute();
		}
		
		public abstract void onResult(List<String> result);

		@Override
		protected void done() {
			try {
				onResult(get());
			} catch (Exception e) {
			} finally {
				synchronized (mainFrame) {
					mainFrame.setCursor(Cursor.getDefaultCursor());
					workerRunning = false;
				}
			}
		}

		@Override
		protected List<String> doInBackground() throws Exception {
			return (query.length() > 0) ? getListValues(query) : null;
		}
    }
    
    private void listGenres() {
    	genres.clear();
    	
    	new ListValuesQuery("SELECT DISTINCT genre FROM Genres ORDER BY genre") {
			@Override
			public void onResult(List<String> result) {
		    	for (String genre: result) {
		    		genres.addElement(new SelectableItem(genre));
		    	}
			}
		};
    }
    
    private void listCountries() {
    	
    	String query;
    	
    	String selectedGenres = join(genres);
    	if (selectedGenres.length() > 0 && searchOperatorBox.getSelectedIndex() == 0) {
    		query = "SELECT DISTINCT l.country FROM Movie m, MovieCountry l, Genres g "
    				+ "WHERE m.ID=l.movie_id AND m.ID=g.movie_id AND l.country IS NOT NULL "
    				+ "AND g.genre IN (" + selectedGenres + ") ORDER BY l.country";
    	} else {
    		query = "SELECT DISTINCT country FROM MovieCountry "
    				+ "WHERE country IS NOT NULL ORDER BY country";
    	}
    	
    	countries.clear();
    	new ListValuesQuery(query) {
			@Override
			public void onResult(List<String> result) {
				for (String country: result) {
		    		countries.addElement(new SelectableItem(country));
		    	}
			}
		};
    }
    
    /*
     * Helper functions
     */
    
    private static String join(ListModel<SelectableItem> items) {
    	StringBuilder sb = new StringBuilder();
    	for (int i = 0; i < items.getSize(); i++) {
    		SelectableItem item = items.getElementAt(i);
    		if (item.selected) {
    			sb.append("'").append(escapeSQL(item.value)).append("',");
    		}
    	}
    	return sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "";
    }
    
    private static String joinActors(List<JComboBox<String>> items) {
    	StringBuilder sb = new StringBuilder();
    	for (JComboBox<String> item: items) {
    		if (item.getSelectedItem() != null) {
	    		String text = String.valueOf(item.getSelectedItem()).trim();
	    		if (text.length() > 0) {
	    			sb.append("'").append(escapeSQL(text)).append("',");
	    		}
    		}
    	}
    	return sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "";
    }
    
	private static int getInt(String text, boolean showError) {
		if (text == null || text.trim().length() == 0) {
			return 0;
		}
		
		try {
			return Integer.parseInt(text);
		} catch (Exception e) {
			if (showError) {
				JOptionPane.showMessageDialog(null, 
						"The entered input: " + text + " is not a valid",
						"Invalid Input", JOptionPane.ERROR_MESSAGE);
			}
			return 0;
		}
	}

	private static String getDate(String text, boolean showError) {
		if (text == null || text.trim().length() == 0 || text.equals("mm/dd/yyyy")) {
			return "";
		}
		
		try {
			String[] fields = text.split("/");
			
			int month = Integer.parseInt(fields[0]);
			int day = Integer.parseInt(fields[1]);
			int year = Integer.parseInt(fields[2]);
			
			return String.format("%2d/%2d/%4d", month, day, year);
			
		} catch (Exception e) {
			if (showError) {
				JOptionPane.showMessageDialog(null, 
						"The entered input: " + text + " is not a valid",
						"Invalid Input", JOptionPane.ERROR_MESSAGE);
			}
			return "";
		}
	}
	
	private static String escapeSQL(String value) {
    	return value != null ? value.replaceAll("'", "''") : "";
    }
    
	public static void main(String[] args) {
		// Create and run the GUI
		// Setup default font for all UI components
    	// http://stackoverflow.com/questions/7434845/setting-the-default-font-of-swing-program
    	FontUIResource defFont = new FontUIResource("Arial", Font.PLAIN, FONT_SIZE);
    	Enumeration keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
          Object key = keys.nextElement();
          Object value = UIManager.get(key);
          if (value != null && value instanceof FontUIResource) {
            UIManager.put (key, defFont);
          }
        } 
        
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new hw3();
			}
		});
	}

	@Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
		System.out.println(e);
	}

	@Override
	public void popupMenuCanceled(PopupMenuEvent e) {
		// TODO Auto-generated method stub
		
	}
}
