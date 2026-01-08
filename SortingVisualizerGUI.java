// Import modules
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

public class SortingVisualizerGUI extends JFrame {
    // Colours for Visualizer
    private static final Color BACKGROUND_COLOR = new Color(245, 250, 255);
    private static final Color HEADER_COLOR = new Color(30, 136, 229);
    private static final Color GRID_COLOR = new Color(210, 230, 245);
    private static final Color TEXT_COLOR = new Color(33, 33, 33);
    private static final Color ALTERNATE_ROW_COLOR = new Color(240, 247, 255);
    private static final Color RUNNING_COLOR = new Color(0, 150, 136);
    private static final Color SUBTITLE_COLOR = new Color(100, 120, 140);
    
    // Panel for displaying benchmark data in graph form
    private BenchmarkGraphPanel graphPanel;
    // Data model for the benchmark results table
    private SortingTableModel model;

    // Custruct all elements
    public SortingVisualizerGUI() {
        // Configure main window properties
        setTitle("Sorting Benchmark and Visualizer");
        setSize(1000, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setBackground(BACKGROUND_COLOR);

        // Ensure executor service is shut down properly when closing the application
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (model != null) {
                    model.shutdown();
                }
            }
        });

        // Create main content panel with padding
        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.setBackground(BACKGROUND_COLOR);
        contentPane.setBorder(new EmptyBorder(15, 15, 15, 15));
        setContentPane(contentPane);

        // Initialize model and create benchmark table
        model = new SortingTableModel();
        JTable benchmarkTable = createStyledTable(model);

        // Initialize visualization components
        graphPanel = new BenchmarkGraphPanel(model.getData());
        SortingAnimationPanel animationPanel = new SortingAnimationPanel();

        // Create container for benchmark table and its info panel
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(BACKGROUND_COLOR);
        tablePanel.add(createTableInfoPanel(), BorderLayout.NORTH);
        tablePanel.add(new JScrollPane(benchmarkTable), BorderLayout.CENTER);

        // Set up timer for periodic graph updates
        Timer graphUpdateTimer = new Timer(1000, e -> graphPanel.repaint());
        graphUpdateTimer.start();

        // Create tabbed interface for different views
        JTabbedPane tabbedPane = createTabbedPane(tablePanel, graphPanel, animationPanel);

        // Add title/subtitle and tabbed pane to the main content area
        contentPane.add(createTitlePanel(), BorderLayout.NORTH);
        contentPane.add(tabbedPane, BorderLayout.CENTER);
    }

    // Create a styled JTable for displaying benchmark results
    private JTable createStyledTable(SortingTableModel model) {
        JTable table = new JTable(model);

        // Configure header appearance
        styleTableHeader(table);
        
        // Configure table rows
        table.setRowHeight(36);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setGridColor(GRID_COLOR);
        table.setShowGrid(true);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(true);

        // Create timer for animating "Running..." text
        Timer animationTimer = new Timer(500, e -> table.repaint());
        animationTimer.start();

        // Configure custom cell renderer for styling table cells
        configureTableCellRenderer(table);
        
        // Adjust column widths for better content display
        adjustColumnWidths(table);

        return table;
    }
    
    // Styles the table header with appropriate colors and fonts
    private void styleTableHeader(JTable table) {
        JTableHeader header = table.getTableHeader();
        header.setBackground(HEADER_COLOR);
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setReorderingAllowed(false);
        ((DefaultTableCellRenderer) header.getDefaultRenderer())
            .setHorizontalAlignment(SwingConstants.CENTER);
    }
    
    // Configures the custom cell renderer for the benchmark table
    private void configureTableCellRenderer(JTable table) {
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            // Counter for animating dots in "Running..." text
            private int dotCount = 0;

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);

                // Apply alternating row colors when not selected
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : ALTERNATE_ROW_COLOR);
                }

                // Center-align all cell contents
                setHorizontalAlignment(SwingConstants.CENTER);

                // Handle special "Running..." animation
                if (value != null && value.toString().startsWith("Running")) {
                    c.setForeground(RUNNING_COLOR);
                    c.setFont(new Font("Segoe UI", Font.BOLD, 13));

                    // Create animated dots (1-3 dots that cycle)
                    String dots = ".".repeat((dotCount % 3) + 1);
                    dotCount++;
                    setText("Running" + dots);
                } else {
                    c.setForeground(TEXT_COLOR);
                }

                return c;
            }
        });
    }
    
    // Adjusts column widths for the benchmark table
    private void adjustColumnWidths(JTable table) {
        // Array size column is narrower
        table.getColumnModel().getColumn(0).setPreferredWidth(80);
        
        // Algorithm columns are wider
        for (int i = 1; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(150);
        }
    }

    // Creates the informational panel displayed above the benchmark table
    private JPanel createTableInfoPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(5, 0, 10, 0));

        JLabel infoLabel = new JLabel("Benchmarks for large arrays will run automatically in the background");
        infoLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        infoLabel.setForeground(SUBTITLE_COLOR);

        panel.add(infoLabel, BorderLayout.WEST);
        return panel;
    }

    // Creates the tabbed pane containing all main application views
    private JTabbedPane createTabbedPane(JPanel tablePanel, BenchmarkGraphPanel graphPanel, 
                                        SortingAnimationPanel animationPanel) {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tabbedPane.setBackground(BACKGROUND_COLOR);
        tabbedPane.setForeground(TEXT_COLOR);

        // Add tabs for each view
        tabbedPane.addTab("Benchmark Table", null, tablePanel);

        // Wrap graph panel in scroll pane for large datasets
        JScrollPane graphScrollPane = new JScrollPane(graphPanel);
        graphScrollPane.setPreferredSize(new Dimension(800, 500));
        tabbedPane.addTab("Benchmark Graph", null, graphScrollPane);

        // Add animation view tab
        tabbedPane.addTab("Sorting Animation", null, animationPanel);

        return tabbedPane;
    }

    // Creates the title and subtitle panel displayed at the top of the application
    private JPanel createTitlePanel() {
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(BACKGROUND_COLOR);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        // Create main title with larger, bold font
        JLabel titleLabel = new JLabel("Sorting Algorithm Visualizer");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(HEADER_COLOR);

        // Create subtitle with smaller font
        JLabel subtitleLabel = new JLabel("Compare and visualize different sorting algorithms");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(SUBTITLE_COLOR);

        // Center the title and subtitle
        JPanel titleCenterPanel = createCenteredPanel(titleLabel);
        JPanel subtitleCenterPanel = createCenteredPanel(subtitleLabel);

        // Stack them vertically
        titlePanel.add(titleCenterPanel, BorderLayout.CENTER);
        titlePanel.add(subtitleCenterPanel, BorderLayout.SOUTH);

        return titlePanel;
    }
    
    // Helper method to create a panel that centers its content horizontally
    private JPanel createCenteredPanel(Component component) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.setBackground(BACKGROUND_COLOR);
        panel.add(component);
        return panel;
    }

    // Configures UI settings and launches the application
    public static void main(String[] args) {
        try {
            // Apply system look and feel with custom UI enhancements
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            // Apply global UI property overrides for consistent styling
            UIManager.put("Panel.background", BACKGROUND_COLOR);
            UIManager.put("Button.font", new Font("Segoe UI", Font.PLAIN, 13));
            UIManager.put("Label.font", new Font("Segoe UI", Font.PLAIN, 13));
            UIManager.put("TabbedPane.selected", new Color(225, 240, 250));
            UIManager.put("TabbedPane.contentAreaColor", BACKGROUND_COLOR);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Create and display the application on the EDT
        SwingUtilities.invokeLater(() -> new SortingVisualizerGUI().setVisible(true));
    }
}