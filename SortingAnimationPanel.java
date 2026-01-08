// Import modules
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Random;
import javax.swing.*;

//A JPanel that visualizes various sorting algorithms with animation capabilities
class SortingAnimationPanel extends JPanel implements Runnable {
    // Array to be sorted and visualized
    private int[] array;
    // Thread for running the sorting animation
    private Thread thread;
    // Default size of the array
    private int size = 50;
    // Delay between animation steps (in milliseconds)
    private int animationDelay = 30;
    // Random number generator for creating arrays
    private static final Random RAND = new Random();
    // Flag to track if sorting is paused
    private volatile boolean paused = false;
    
    // Color scheme for the visualization
    private final Color barBaseColor = new Color(66, 165, 245); // Base blue color
    private final Color barHighlightColor = new Color(33, 150, 243); // Highlight color
    private final Color backgroundColor = new Color(245, 250, 255); // Very light blue background
    private final Color textColor = new Color(33, 33, 33); // Dark text
    private final Color buttonColor = new Color(25, 118, 210); // Darker blue for buttons
    private final Font titleFont = new Font("Segoe UI", Font.BOLD, 16);
    private final Font regularFont = new Font("Segoe UI", Font.PLAIN, 12);
    private final Font buttonFont = new Font("Segoe UI", Font.BOLD, 12);
    
    // Variables for tracking animation state
    private int currentIndex = -1;        // Currently processed element
    private int comparisonIndex = -1;     // Element being compared
    private int[] tempArray;              // Auxiliary array for merge sort
    
    // Enum representing the available sorting algorithms
    private enum SortingAlgorithm {
        BUBBLE("Bubble Sort"),
        SELECTION("Selection Sort"),
        INSERTION("Insertion Sort"),
        MERGE("Merge Sort"),
        HEAP("Heap Sort");
        
        private final String name;
        
        SortingAlgorithm(String name) {
            this.name = name;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
    
    // Currently selected algorithm (defaults to Bubble Sort)
    private SortingAlgorithm selectedAlgorithm = SortingAlgorithm.BUBBLE;
    
    // UI components
    private final JComboBox<SortingAlgorithm> algorithmSelector;
    private final JButton startButton;
    private final JButton resetButton;
    private final JButton pauseButton;
    private final JSlider sizeSlider;
    private final JSlider speedSlider;
    private final JLabel sizeLabel;
    private final JLabel speedLabel;
    
    // State tracking variables
    private boolean isSorting = false;    // Whether sorting is in progress
    private long startTime = 0;           // When sorting started
    private long elapsedTime = 0;         // Time taken for sorting

    // Constructor - sets up the UI components and initializes the array
    public SortingAnimationPanel() {
        // Set up the panel properties
        setPreferredSize(new Dimension(800, 400));
        setLayout(new BorderLayout());
        setBackground(backgroundColor);
        
        // Create the main control panel with a border layout
        JPanel mainControlPanel = new JPanel(new BorderLayout(10, 5));
        mainControlPanel.setBackground(new Color(240, 245, 250));
        mainControlPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        
        // Create algorithm selection and buttons panel (top section)
        JPanel topPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        topPanel.setBackground(new Color(240, 245, 250));
        
        // Algorithm selection panel (left side of top panel)
        JPanel algorithmPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        algorithmPanel.setBackground(new Color(240, 245, 250));
        
        // Add algorithm label and dropdown
        JLabel algorithmLabel = new JLabel("Algorithm: ");
        algorithmLabel.setFont(regularFont);
        algorithmSelector = new JComboBox<>(SortingAlgorithm.values());
        algorithmSelector.setSelectedItem(SortingAlgorithm.BUBBLE);
        algorithmSelector.setFont(regularFont);
        algorithmSelector.setPreferredSize(new Dimension(130, 25));
        
        algorithmPanel.add(algorithmLabel);
        algorithmPanel.add(algorithmSelector);
        
        // Buttons panel (right side of top panel)
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonsPanel.setBackground(new Color(240, 245, 250));
        
        // Create and add control buttons
        startButton = createStyledButton("Start", buttonColor);
        startButton.addActionListener(e -> startSorting());
        startButton.setPreferredSize(new Dimension(65, 25));
        
        pauseButton = createStyledButton("Pause", buttonColor);
        pauseButton.addActionListener(e -> togglePause());
        pauseButton.setPreferredSize(new Dimension(65, 25));
        pauseButton.setEnabled(false);
        
        resetButton = createStyledButton("Reset", buttonColor);
        resetButton.addActionListener(e -> resetArray());
        resetButton.setEnabled(true);
        resetButton.setPreferredSize(new Dimension(65, 25));
        
        buttonsPanel.add(startButton);
        buttonsPanel.add(pauseButton);
        buttonsPanel.add(resetButton);
        
        // Add components to top panel
        topPanel.add(algorithmPanel);
        topPanel.add(buttonsPanel);
        
        // Create sliders panel (bottom section)
        JPanel slidersPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        slidersPanel.setBackground(new Color(240, 245, 250));
        
        // Size slider panel (left side)
        JPanel sizePanel = new JPanel(new BorderLayout(5, 3));
        sizePanel.setBackground(new Color(240, 245, 250));
        
        JLabel sizeTitleLabel = new JLabel("Array Size:", JLabel.LEFT);
        sizeTitleLabel.setFont(regularFont);
        
        sizeSlider = new JSlider(JSlider.HORIZONTAL, 10, 250, 50);
        sizeSlider.setBackground(new Color(240, 245, 250));
        sizeSlider.setMajorTickSpacing(50);
        sizeSlider.setMinorTickSpacing(10);
        sizeSlider.setPaintTicks(true);
        sizeSlider.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        
        sizeLabel = new JLabel("50", JLabel.RIGHT);
        sizeLabel.setFont(regularFont);
        
        sizePanel.add(sizeTitleLabel, BorderLayout.WEST);
        sizePanel.add(sizeSlider, BorderLayout.CENTER);
        sizePanel.add(sizeLabel, BorderLayout.EAST);
        
        // Speed slider panel (right side)
        JPanel speedPanel = new JPanel(new BorderLayout(5, 3));
        speedPanel.setBackground(new Color(240, 245, 250));
        
        JLabel speedTitleLabel = new JLabel("Speed:", JLabel.LEFT);
        speedTitleLabel.setFont(regularFont);
        
        speedSlider = new JSlider(JSlider.HORIZONTAL, 1, 100, 70);
        speedSlider.setBackground(new Color(240, 245, 250));
        speedSlider.setMajorTickSpacing(25);
        speedSlider.setMinorTickSpacing(5);
        speedSlider.setPaintTicks(true);
        speedSlider.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        
        speedLabel = new JLabel("Medium", JLabel.RIGHT);
        speedLabel.setFont(regularFont);
        
        speedPanel.add(speedTitleLabel, BorderLayout.WEST);
        speedPanel.add(speedSlider, BorderLayout.CENTER);
        speedPanel.add(speedLabel, BorderLayout.EAST);
        
        // Add sliders to their container
        slidersPanel.add(sizePanel);
        slidersPanel.add(speedPanel);
        
        // Add all control elements to the main control panel
        mainControlPanel.add(topPanel, BorderLayout.NORTH);
        mainControlPanel.add(slidersPanel, BorderLayout.CENTER);
        
        // Add control panel to the top of the main panel
        add(mainControlPanel, BorderLayout.NORTH);
        
        // Initialize array and set up event listeners
        resetArray();
        
        // Size slider change listener
        sizeSlider.addChangeListener(e -> {
            int newSize = sizeSlider.getValue();
            
            // Update the label
            sizeLabel.setText(String.valueOf(newSize));
            
            // Show warning for large arrays
            if (newSize > 200) {
                sizeLabel.setForeground(Color.ORANGE);
                sizeLabel.setText(newSize + " (large)");
            } else {
                sizeLabel.setForeground(textColor);
                sizeLabel.setText(String.valueOf(newSize));
            }
            
            // Only recreate the array when the slider is released
            if (!sizeSlider.getValueIsAdjusting() && !isSorting) {
                size = newSize;
                resetArray();
            }
        });
        
        // Speed slider change listener
        speedSlider.addChangeListener(e -> {
            // Convert slider value to animation delay
            int value = speedSlider.getValue();
            if (value < 60) {
                // Linear scaling for slower speeds
                animationDelay = 100 - value;
            } else {
                // Exponential scaling for faster speeds
                double factor = (100.0 - value) / 40.0;
                animationDelay = (int)(factor * factor * 40);
                if (animationDelay < 1) animationDelay = 1;
            }
            
            // Update speed label text
            if (speedSlider.getValue() < 25) {
                speedLabel.setText("Slow");
            } else if (speedSlider.getValue() < 50) {
                speedLabel.setText("Medium Slow");
            } else if (speedSlider.getValue() < 75) {
                speedLabel.setText("Medium");
            } else if (speedSlider.getValue() < 90) {
                speedLabel.setText("Fast");
            } else {
                speedLabel.setText("Very Fast");
            }
        });
        
        // Component listener for responsive UI adjustments
        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                Dimension size = getSize();
                // Adjust UI elements based on component size
                if (size.width < 500) {
                    // Compact mode for small widths
                    algorithmSelector.setPreferredSize(new Dimension(100, 25));
                    startButton.setPreferredSize(new Dimension(60, 25));
                    pauseButton.setPreferredSize(new Dimension(60, 25));
                    resetButton.setPreferredSize(new Dimension(60, 25));
                } else {
                    // Regular size for normal widths
                    algorithmSelector.setPreferredSize(new Dimension(130, 25));
                    startButton.setPreferredSize(new Dimension(65, 25));
                    pauseButton.setPreferredSize(new Dimension(65, 25));
                    resetButton.setPreferredSize(new Dimension(65, 25));
                }
            }
        });
    }
    
    // Styled buttons
    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Paint background with different states
                if (getModel().isPressed()) {
                    g2.setColor(getBackground().darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(getBackground().brighter());
                } else {
                    g2.setColor(getBackground());
                }
                
                // Draw rounded rectangle background
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                
                // Draw border
                g2.setColor(getBackground().darker());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                
                // Center text properly
                FontMetrics fm = g2.getFontMetrics();
                Rectangle2D rect = fm.getStringBounds(getText(), g2);
                int textX = (int) (getWidth() - rect.getWidth()) / 2;
                int textY = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                
                g2.setColor(getForeground());
                g2.drawString(getText(), textX, textY);
                g2.dispose();
            }
        };
        
        // Configure button appearance
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFont(buttonFont);
        
        return button;
    }
    
    // Resets the array and UI to initial state
    private void resetArray() {
        // Enable the reset button
        resetButton.setEnabled(true);
        
        // Stop any ongoing sorting
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
            try {
                thread.join(500);
            } catch (InterruptedException e) {
                System.out.println("Reset process was interrupted");
            }
        }
        
        // Reset state variables
        thread = null;
        isSorting = false;
        paused = false;
        
        // Create new random array
        array = new int[size];
        for (int i = 0; i < size; i++) {
            array[i] = RAND.nextInt(280) + 20; // Values between 20 and 300
        }
        
        // Update UI components
        SwingUtilities.invokeLater(() -> {
            startButton.setText("Start");
            startButton.setEnabled(true);
            pauseButton.setEnabled(false);
            pauseButton.setText("Pause");
            pauseButton.setBackground(buttonColor);
            resetButton.setEnabled(false);
            
            // Enable controls
            algorithmSelector.setEnabled(true);
            sizeSlider.setEnabled(true);
            
            // Reset visualization state
            currentIndex = -1;
            comparisonIndex = -1;
            elapsedTime = 0;
            
            repaint();
        });
    }

    // Toggles the pause state of the sorting animation
    private void togglePause() {
        paused = !paused;
        pauseButton.setText(paused ? "Resume" : "Pause");
        if (paused) {
            pauseButton.setBackground(new Color(0, 150, 136)); // Teal for resume
            resetButton.setEnabled(true);
        } else {
            pauseButton.setBackground(buttonColor); // Blue for pause
            resetButton.setEnabled(true);
        }
    }

    // Starts the sorting process with the selected algorithm
    private void startSorting() {
        if (isSorting) return;
        
        // Set up UI for sorting
        selectedAlgorithm = (SortingAlgorithm) algorithmSelector.getSelectedItem();
        startButton.setText("Sorting");
        startButton.setEnabled(false);
        resetButton.setEnabled(true);
        pauseButton.setEnabled(true);
        paused = false;
        pauseButton.setText("Pause");
        pauseButton.setBackground(buttonColor);
        algorithmSelector.setEnabled(false);
        sizeSlider.setEnabled(false);
        isSorting = true;
        
        // Start sorting thread
        startTime = System.currentTimeMillis();
        thread = new Thread(this);
        thread.start();
    }

    // Main sorting thread entry point
    @Override
    public void run() {
        try {
            // Execute the selected sorting algorithm
            switch (selectedAlgorithm) {
                case BUBBLE:
                    bubbleSort();
                    break;
                case SELECTION:
                    selectionSort();
                    break;
                case INSERTION:
                    insertionSort();
                    break;
                case MERGE:
                    tempArray = new int[array.length];
                    mergeSort(array, 0, array.length - 1);
                    break;
                case HEAP:
                    heapSort();
                    break;
            }
        } catch (InterruptedException e) {
            System.out.println("Sorting interrupted");
            return;
        } catch (Exception e) {
            System.out.println("Error during sorting: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Update timing information
        long currentTime = System.currentTimeMillis();
        elapsedTime = currentTime - startTime;
        
        // Clear highlights
        currentIndex = -1;
        comparisonIndex = -1;
        repaint();
        
        // Reset UI after sorting
        SwingUtilities.invokeLater(() -> {
            startButton.setText("Start");
            startButton.setEnabled(true);
            resetButton.setEnabled(true);
            pauseButton.setEnabled(false);
            algorithmSelector.setEnabled(true);
            sizeSlider.setEnabled(true);
            isSorting = false;
        });
    }
    
    // Checks pause state and handles thread interruption
    private void checkPause() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("Thread interrupted during sorting");
        }
        
        // Handle pause state
        while (paused) {
            Thread.sleep(100);
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Thread interrupted during pause");
            }
        }
    }
    
    // Sorting algorithm implementations with animation support

    //Bubble sort implementation with animation
    @SuppressWarnings("BusyWait")
    private void bubbleSort() throws InterruptedException {
        for (int i = 0; i < array.length - 1; i++) {
            for (int j = 0; j < array.length - i - 1; j++) {
                checkPause();
                currentIndex = j;
                comparisonIndex = j + 1;
                if (array[j] > array[j + 1]) {
                    // Swap elements
                    int tmp = array[j];
                    array[j] = array[j + 1];
                    array[j + 1] = tmp;
                    repaint();
                    Thread.sleep(animationDelay);
                } else {
                    repaint();
                    Thread.sleep(animationDelay / 2);
                }
            }
        }
    }
    
    // Selection sort implementation with animation
    @SuppressWarnings("BusyWait")
    private void selectionSort() throws InterruptedException {
        for (int i = 0; i < array.length - 1; i++) {
            int min = i;
            currentIndex = i;
            
            // Find minimum element in unsorted portion
            for (int j = i + 1; j < array.length; j++) {
                checkPause();
                comparisonIndex = j;
                if (array[j] < array[min]) min = j;
                repaint();
                Thread.sleep(animationDelay / 2);
            }
            
            // Swap found minimum with first element
            checkPause();
            comparisonIndex = min;
            int tmp = array[i]; 
            array[i] = array[min]; 
            array[min] = tmp;
            repaint();
            Thread.sleep(animationDelay);
        }
    }
    
    // Insertion sort implementation with animation
    @SuppressWarnings("BusyWait")
    private void insertionSort() throws InterruptedException {
        for (int i = 1; i < array.length; i++) {
            int key = array[i], j = i - 1;
            currentIndex = i;
            
            // Move elements greater than key to one position ahead
            while (j >= 0 && array[j] > key) {
                checkPause();
                comparisonIndex = j;
                array[j + 1] = array[j--];
                repaint();
                Thread.sleep(animationDelay);
            }
            
            checkPause();
            comparisonIndex = j + 1;
            array[j + 1] = key;
            repaint();
            Thread.sleep(animationDelay);
        }
    }
    
    // Merge sort implementation with animation
    @SuppressWarnings("BusyWait")
    private void mergeSort(int[] a, int left, int right) throws InterruptedException {
        checkPause();
        if (left >= right) return;
        
        int mid = (left + right) / 2;
        mergeSort(a, left, mid);
        mergeSort(a, mid + 1, right);
        merge(a, left, mid, right);
        repaint();
        Thread.sleep(animationDelay);
    }
    
    // Merge helper function for merge sort
    private void merge(int[] a, int left, int mid, int right) throws InterruptedException {
        int i = left, j = mid + 1, k = 0;
        
        // Copy elements to temp array
        for (int idx = left; idx <= right; idx++) {
            tempArray[idx - left] = a[idx];
        }
        
        // Merge the two halves
        while (i <= mid && j <= right) {
            checkPause();
            currentIndex = i;
            comparisonIndex = j;
            repaint();
            Thread.sleep(animationDelay / 2);
            
            if (tempArray[i - left] <= tempArray[j - left]) {
                a[left + k++] = tempArray[i++ - left];
            } else {
                a[left + k++] = tempArray[j++ - left];
            }
        }
        
        // Copy remaining elements from left half
        while (i <= mid) {
            checkPause();
            currentIndex = i;
            a[left + k++] = tempArray[i++ - left];
            repaint();
            Thread.sleep(animationDelay / 3);
        }
        
        // Copy remaining elements from right half
        while (j <= right) {
            checkPause();
            comparisonIndex = j;
            a[left + k++] = tempArray[j++ - left];
            repaint();
            Thread.sleep(animationDelay / 3);
        }
    }
    
    // Heap sort implementation with animation
    @SuppressWarnings("BusyWait")
    private void heapSort() throws InterruptedException {
        int n = array.length;
        
        // Build heap (rearrange array)
        for (int i = n / 2 - 1; i >= 0; i--) {
            checkPause();
            currentIndex = i;
            heapify(n, i);
            repaint();
            Thread.sleep(animationDelay);
        }
        
        // Extract elements from heap one by one
        for (int i = n - 1; i > 0; i--) {
            checkPause();
            currentIndex = 0;
            comparisonIndex = i;
            
            // Move current root to end
            int temp = array[0];
            array[0] = array[i];
            array[i] = temp;
            
            repaint();
            Thread.sleep(animationDelay);
            
            // Call heapify on the reduced heap
            heapify(i, 0);
        }
    }
    
    // Heapify helper function for heap sort
    private void heapify(int n, int i) throws InterruptedException {
        int largest = i; // Initialize largest as root
        int left = 2 * i + 1;
        int right = 2 * i + 2;
        
        // If left child is larger than root
        if (left < n) {
            checkPause();
            comparisonIndex = left;
            repaint();
            Thread.sleep(animationDelay / 2);
            
            if (array[left] > array[largest]) {
                largest = left;
            }
        }
        
        // If right child is larger than largest so far
        if (right < n) {
            checkPause();
            comparisonIndex = right;
            repaint();
            Thread.sleep(animationDelay / 2);
            
            if (array[right] > array[largest]) {
                largest = right;
            }
        }
        
        // If largest is not root
        if (largest != i) {
            checkPause();
            comparisonIndex = largest;
            
            // Swap and continue heapifying
            int swap = array[i];
            array[i] = array[largest];
            array[largest] = swap;
            
            repaint();
            Thread.sleep(animationDelay);
            
            heapify(n, largest);
        }
    }

    // Paint the visualization of the sorting process
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (array == null) {
            resetArray();
            return;
        }
        
        Graphics2D g2 = (Graphics2D) g;
        // Enable anti-aliasing for smoother graphics
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Get dimensions and calculate margins
        int width = getWidth();
        int height = getHeight();
        int topMargin = 80;
        int bottomMargin = 30;
        int leftMargin = 40;
        int rightMargin = 40;
        
        // Calculate available drawing area
        int drawingAreaHeight = height - topMargin - bottomMargin;
        int drawingAreaWidth = width - leftMargin - rightMargin;
        
        // Calculate bar dimensions based on array size
        int barWidth;
        int barSpacing;
        
        if (array.length <= 50) {
            barWidth = Math.max(4, Math.min(15, drawingAreaWidth / array.length - 2));
            barSpacing = 2;
        } else if (array.length <= 200) {
            barWidth = Math.max(2, Math.min(8, drawingAreaWidth / array.length - 1));
            barSpacing = 1;
        } else {
            barWidth = Math.max(1, Math.min(4, drawingAreaWidth / array.length));
            barSpacing = 0;
        }
        
        // Center the bars in the available drawing area
        int availableWidth = array.length * (barWidth + barSpacing);
        int xOffset = leftMargin + (drawingAreaWidth - availableWidth) / 2;
        
        // Find maximum value for scaling
        int maxValue = 0;
        for (int value : array) {
            maxValue = Math.max(maxValue, value);
        }
        
        // Calculate scaling factor
        double scaleFactor = (double) drawingAreaHeight / (maxValue + 20);
        int cornerRadius = Math.max(1, Math.min(4, barWidth / 2));
        
        // Draw algorithm title and status
        g2.setFont(titleFont);
        g2.setColor(textColor);
        String title = selectedAlgorithm.toString();
        if (isSorting) {
            title += " - Sorting in progress...";
        } else if (elapsedTime > 0) {
            title += String.format(" - Completed in %.2f seconds", elapsedTime / 1000.0);
        }
        g2.drawString(title, leftMargin, topMargin - 40);
        
        // Draw array size information
        g2.setFont(regularFont);
        g2.drawString("Array Size: " + array.length, width - 150, topMargin - 40);
        
        // Draw the array elements as bars
        for (int i = 0; i < array.length; i++) {
            // Determine bar color based on state
            Color barColor;
            if (i == currentIndex) {
                barColor = Color.RED; // Current element
            } else if (i == comparisonIndex) {
                barColor = Color.GREEN; // Comparison element
            } else {
                // Gradient based on value
                float ratio = array[i] / 300.0f;
                barColor = new Color(
                    (int)(barBaseColor.getRed() - ratio * 30),
                    (int)(barBaseColor.getGreen() - ratio * 20),
                    (int)(barBaseColor.getBlue() + ratio * 10)
                );
            }
            
            // Calculate bar height
            int scaledHeight = (int)(array[i] * scaleFactor);
            if (scaledHeight < 1) scaledHeight = 1;
            
            int barX = xOffset + i * (barWidth + barSpacing);
            int barY = height - bottomMargin - scaledHeight;
            
            // Draw the bar
            if (barWidth <= 2) {
                // Simple rectangle for very thin bars
                g2.setColor(barColor);
                g2.fillRect(barX, barY, barWidth, scaledHeight);
            } else {
                // Rounded rectangle with shadow for wider bars
                // Draw shadow
                g2.setColor(new Color(0, 0, 0, 30));
                RoundRectangle2D shadowRect = new RoundRectangle2D.Float(
                    barX + 1, barY + 1, barWidth - 1, scaledHeight, cornerRadius, cornerRadius);
                g2.fill(shadowRect);
                
                // Draw bar
                g2.setColor(barColor);
                RoundRectangle2D roundedBar = new RoundRectangle2D.Float(
                    barX, barY, barWidth - 1, scaledHeight, cornerRadius, cornerRadius);
                g2.fill(roundedBar);
                
                // Add highlight for 3D effect
                if (barWidth > 4) {
                    g2.setColor(new Color(255, 255, 255, 100));
                    g2.setStroke(new BasicStroke(1.0f));
                    g2.drawLine(barX, barY + cornerRadius/2, barX + barWidth - 1, barY + cornerRadius/2);
                }
            }
        }
    }
}