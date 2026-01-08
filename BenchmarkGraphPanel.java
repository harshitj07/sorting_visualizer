// Import modules
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.text.DecimalFormat;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

class BenchmarkGraphPanel extends JPanel {
    // Raw benchmark data: rows correspond to array sizes, columns to algorithms
    private final Object[][] data;
    
    // Names of sorting algorithms displayed
    private final String[] algorithms = {"Bubble Sort", "Selection Sort", "Insertion Sort", "Merge Sort", "Heap Sort"};

    // Define custom colors and fonts for the graph display
    private final Color[] colors = {
        new Color(0, 0, 128),      // Navy color for Bubble Sort
        new Color(220, 20, 60),    // Red color for Selection Sort
        new Color(30, 144, 255),   // Blue color for Insertion Sort
        new Color(0, 168, 107),    // Jade Green color for Merge Sort
        new Color(255, 140, 0)     // Orange color for Heap Sort
    };
    private final Color backgroundColor = new Color(250, 252, 255); // Light background color
    private final Color gridColor = new Color(220, 235, 250);       // Soft blue grid lines
    private final Color textColor = new Color(44, 62, 80);          // Dark text color for readability
    private final Font titleFont = new Font("Segoe UI", Font.BOLD, 22);       // Font for graph title
    private final Font axisLabelFont = new Font("Segoe UI", Font.BOLD, 14);   // Font for axis labels
    private final Font labelFont = new Font("Segoe UI", Font.PLAIN, 13);      // Font for smaller labels
    private final DecimalFormat df = new DecimalFormat("#,##0.0");             // Decimal format for time labels

    // Currently selected array size index (0 = 100 elements, etc.)
    private int selectedSizeIndex = 0;
    
    // Array sizes corresponding to benchmark data rows
    private final int[] arraySizes = {100, 1000, 10000, 100000};
    private JPanel controlPanel; // Panel containing UI controls like size selector

    public BenchmarkGraphPanel(Object[][] benchmarkData) {
        this.data = benchmarkData;
        setLayout(new BorderLayout());
        setBackground(backgroundColor);

        // Create the control panel with array size selector dropdown
        createControlPanel();
        add(controlPanel, BorderLayout.NORTH);

        // Create the graph drawing panel with custom painting logic
        JPanel graphPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawGraph((Graphics2D) g);
            }
        };
        graphPanel.setBackground(backgroundColor);
        add(graphPanel, BorderLayout.CENTER);
    }

    // Setup the control panel UI elements
    private void createControlPanel() {
        controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.setBackground(backgroundColor);
        controlPanel.setBorder(new EmptyBorder(5, 10, 5, 10)); // Padding around controls

        JLabel sizeLabel = new JLabel("Select Array Size: ");
        sizeLabel.setFont(labelFont);
        controlPanel.add(sizeLabel);

        // Options for array sizes with formatted strings
        String[] sizeOptions = {"100 elements", "1,000 elements", "10,000 elements", "100,000 elements"};
        JComboBox<String> sizeSelector = new JComboBox<>(sizeOptions);
        sizeSelector.setFont(labelFont);
        sizeSelector.setPreferredSize(new Dimension(150, 28));
        sizeSelector.addActionListener(e -> {
            selectedSizeIndex = sizeSelector.getSelectedIndex();
            repaint(); // Redraw graph with new data
        });

        controlPanel.add(sizeSelector);
    }

    // Method to draw graph visualization
    private void drawGraph(Graphics2D g2) {
        if (data == null) return; // No data to display

        setRenderingHints(g2); // Enable anti-aliasing and high quality rendering

        int width = getWidth(), height = getHeight();
        
        // Padding around the graph area to leave space for labels and titles
        int padding = 80;
        int barWidth = 60;
        int gap = 25;
        
        // Calculate horizontal offset to center bars in available space
        int xOffset = padding + (width - 2 * padding - 5 * (barWidth + gap)) / 2;
        int yBase = height - padding;
        int cornerRadius = 12;

        // Vertical padding for title and axis labels
        int titleTopPadding = 50, titleBottomPadding = 50;
        int yAxisTitleTopPadding = 40, yAxisTitleBottomPadding = 20;
        int graphTopPadding = titleTopPadding + titleBottomPadding + yAxisTitleBottomPadding;

        int arraySize = arraySizes[selectedSizeIndex];

        // Calculate minimum and maximum time values for logarithmic scaling
        double[] scale = calculateScale(selectedSizeIndex);
        double minTime = scale[0], maxTime = scale[1];

        // Fill background of graph area
        g2.setColor(backgroundColor);
        g2.fillRect(padding, graphTopPadding, width - 2 * padding, yBase - graphTopPadding);

        // Draw graph title and axis labels at the top and bottom
        drawTitleAndLabels(g2, width, height, arraySize, titleTopPadding, yAxisTitleTopPadding);
       
        // Draw major grid lines corresponding to powers of ten on log scale
        drawMajorGridLines(g2, width, padding, yBase, graphTopPadding, minTime, maxTime);
        
        // Draw minor grid lines between major lines for finer scale reference
        drawMinorGridLines(g2, width, padding, yBase, graphTopPadding, minTime, maxTime);

        // Draw bars for each algorithm's execution time
        for (int col = 0; col < algorithms.length; col++) {
            if (!(data[selectedSizeIndex][col + 1] instanceof Double)) continue; // Skip invalid data

            double time = (Double) data[selectedSizeIndex][col + 1];
            if (time <= 0) continue; // Skip zero or negative times

            // Calculate bar X position based on index and spacing
            int barX = xOffset + col * (barWidth + gap);
            
            // Calculate bar top Y coordinate using logarithmic scaling
            int barY = logScaleY(time, minTime, maxTime, yBase, graphTopPadding);
            
            // Bar height is distance from baseline to top Y, minimum 3 pixels for visibility
            int barHeight = Math.max(yBase - barY, 3);

            // Draw the individual bar with shadow, color, outline, and labels
            drawBar(g2, barX, barY, barWidth, barHeight, colors[col], time, yBase, algorithms[col]);
        }
    }

    // Enable high-quality rendering hints for smooth graphics and text
    private void setRenderingHints(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    // Draw the graph's title and axis labels with centered alignment
    private void drawTitleAndLabels(Graphics2D g2, int width, int height, int arraySize, int titleTop, int yAxisTop) {
        
        // Format array size string with commas for readability
        String sizeText = arraySize == 100000 ? "100,000" : arraySize == 10000 ? "10,000" : String.valueOf(arraySize);
        String title = "Sorting Performance with " + sizeText + " Elements";

        g2.setFont(titleFont);
        g2.setColor(textColor);
        int titleWidth = g2.getFontMetrics().stringWidth(title);
        
        // Draw the main title centered horizontally near the top
        g2.drawString(title, (width - titleWidth) / 2, titleTop);

        g2.setFont(axisLabelFont);
        
        // Draw Y-axis label near top left
        g2.drawString("Execution Time (ms) - Log Scale", 20, titleTop + yAxisTop);
        
        // Draw X-axis label centered near bottom of panel
        String xAxisTitle = "Type of Sorting Algorithm";
        int xAxisWidth = g2.getFontMetrics().stringWidth(xAxisTitle);
        g2.drawString(xAxisTitle, (width - xAxisWidth) / 2, height - 20);
    }

    // Draw major horizontal grid lines at powers of 10 on the log scale
    private void drawMajorGridLines(Graphics2D g2, int width, int padding, int yBase, int top, double min, double max) {
        g2.setStroke(new BasicStroke(1.0f)); // Standard stroke thickness
        
        // Iterate over powers of 10 from min to max
        for (double power = Math.floor(Math.log10(min)); power <= Math.ceil(Math.log10(max)); power++) {
            double value = Math.pow(10, power);
            int y = logScaleY(value, min, max, yBase, top);
            g2.setColor(gridColor); // Light grid line color
            g2.drawLine(padding, y, width - padding, y); // Horizontal line across graph

            // Draw label for grid line value to left of graph area
            g2.setColor(textColor);
            g2.setFont(labelFont);
            g2.drawString(formatTimeValue(value), padding - 70, y + 5);
        }
    }

    // Draw minor grid lines between major grid lines for finer scale reference
    private void drawMinorGridLines(Graphics2D g2, int width, int padding, int yBase, int top, double min, double max) {
        // Iterate over powers of 10 between min and max (exclusive)
        for (double power = Math.floor(Math.log10(min)); power < Math.ceil(Math.log10(max)); power++) {
            // Draw 8 minor lines between each major line at multiples 2 through 9
            for (int i = 2; i <= 9; i++) {
                double value = i * Math.pow(10, power);
                int y = logScaleY(value, min, max, yBase, top);
                
                // Thicker line for 5 to highlight mid-point, thinner for others
                g2.setStroke(new BasicStroke(i == 5 ? 0.7f : 0.5f));
               
                // Use semi-transparent grid color with different alpha for emphasis
                g2.setColor(new Color(gridColor.getRed(), gridColor.getGreen(), gridColor.getBlue(), i == 5 ? 120 : 80));
                g2.drawLine(padding, y, width - padding, y);
            }
        }

        // Draw X-axis baseline line darker than grid lines for contrast
        g2.setColor(gridColor.darker());
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawLine(padding, yBase, width - padding, yBase);
    }

    // Draw an individual bar representing a sorting algorithm's execution time
    private void drawBar(Graphics2D g2, int x, int y, int width, int height, Color color, double time, int yBase, String algoName) {
        int cornerRadius = 12; // Rounded corners for bar and shadow

        // Draw subtle shadow offset by 3 pixels right and down
        g2.setColor(new Color(0, 0, 0, 20)); // Transparent black shadow
        g2.fill(new RoundRectangle2D.Float(x + 3, y + 3, width, height, cornerRadius, cornerRadius));

        // Draw main colored bar with rounded corners
        g2.setColor(color);
        RoundRectangle2D bar = new RoundRectangle2D.Float(x, y, width, height, cornerRadius, cornerRadius);
        g2.fill(bar);

        // Draw a slightly darker outline around the bar for definition
        g2.setColor(color.darker());
        g2.setStroke(new BasicStroke(0.8f));
        g2.draw(bar);

        // Draw execution time label on or above the bar
        g2.setColor(height > 40 ? Color.WHITE : textColor); // White text if bar tall enough, else dark text
        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        String timeText = formatTimeValue(time);
        int textWidth = g2.getFontMetrics().stringWidth(timeText);
        
        // Position label inside bar if tall, otherwise above bar
        int textY = height > 40 ? y + 25 : y - 10;
        g2.drawString(timeText, x + (width - textWidth) / 2, textY);

        // Draw algorithm name below the bar, shortened by removing " Sort"
        g2.setColor(textColor);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        String shortName = algoName.replace(" Sort", "");
        int algoWidth = g2.getFontMetrics().stringWidth(shortName);
        g2.drawString(shortName, x + (width - algoWidth) / 2, yBase + 20);
    }

    // Determine the minimum and maximum time values to set the logarithmic scale
    private double[] calculateScale(int index) {
        
        // Start with a very small minimum time (10^-4) and default max time (1)
        double minPower = Math.floor(Math.log10(0.0001));
        double maxPower = Math.ceil(Math.log10(1.0));

        // Iterate over all algorithms for the selected array size to find max time
        for (int i = 1; i <= algorithms.length; i++) {
            if (data[index][i] instanceof Double) {
                double time = (Double) data[index][i];
                if (time > 0) maxPower = Math.max(maxPower, Math.ceil(Math.log10(time)));
            }
        }
        // Ensure at least 3 orders of magnitude difference for scale clarity
        if (maxPower - minPower < 3) minPower = maxPower - 3;

        // Return actual min and max values as powers of 10
        return new double[]{Math.pow(10, minPower), Math.pow(10, maxPower)};
    }

    // Format time values with varying decimal precision based on magnitude
    private String formatTimeValue(double value) {
        if (value < 0.001) return String.format("%.4f", value);
        if (value < 0.01) return String.format("%.3f", value);
        if (value < 0.1) return String.format("%.2f", value);
        if (value < 1 || value < 10) return String.format("%.1f", value);
        if (value < 100) return String.format("%.0f", value);
        return df.format(value);
    }

    // Convert a time value to a Y coordinate on the graph using logarithmic scaling
    private int logScaleY(double value, double min, double max, int yBase, int topPadding) {
        if (value <= 0) return yBase; // Values <= 0 map to baseline
        
        // Calculate logarithmic ratio between min and max
        double ratio = (Math.log10(value) - Math.log10(min)) / (Math.log10(max) - Math.log10(min));
        
        // Map ratio to pixel coordinate between baseline and top padding
        return yBase - (int) (ratio * (yBase - topPadding));
    }

    @Override
    public Dimension getPreferredSize() {
        // Preferred size for the panel to ensure enough space for graph and labels
        return new Dimension(1000, 600);
    }
}