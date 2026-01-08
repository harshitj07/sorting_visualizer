// Import modules
import java.text.DecimalFormat;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

class SortingTableModel extends AbstractTableModel {
    // Column names for the table
    private final String[] columnNames = {
        "Array Size", "Bubble Sort (ms)", "Selection Sort (ms)",
        "Insertion Sort (ms)", "Merge Sort (ms)", "Heap Sort (ms)"
    };

    // Benchmark sizes and data storage
    private static final int[] SIZES = {100, 1000, 10000, 100000};
    private final Object[][] data = new Object[SIZES.length][columnNames.length];
    private static final Random RAND = new Random();
    private final DecimalFormat df = new DecimalFormat("#,##0.00");

    // Executor for running long tasks in background
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public SortingTableModel() {
        for (int i = 0; i < SIZES.length; i++) {
            int size = SIZES[i];
            int[] baseArray = randArray(size);
            data[i][0] = size;

            // For small arrays, run all sorts directly
            if (size <= 10000) {
                data[i][1] = timeSort(baseArray.clone(), this::bubbleSort);
                data[i][2] = timeSort(baseArray.clone(), this::selectionSort);
                data[i][3] = timeSort(baseArray.clone(), this::insertionSort);
            } else {
                // For large arrays, run slow sorts in background
                for (int col = 1; col <= 3; col++) {
                    data[i][col] = "Running...";
                    scheduleBenchmark(i, col);
                }
            }

            // Always run merge and heap sorts directly
            data[i][4] = timeSort(baseArray.clone(), this::mergeSort);
            data[i][5] = timeSort(baseArray.clone(), this::heapSort);
        }
    }

    // Schedules slow sorts (bubble, selection, insertion) in background
    private void scheduleBenchmark(int row, int col) {
        executor.submit(() -> {
            Object result;
            try {
                result = runBenchmark(row, col);
            } catch (Exception e) {
                result = "Error";
            }
            Object finalResult = result;
            SwingUtilities.invokeLater(() -> {
                data[row][col] = finalResult;
                fireTableCellUpdated(row, col);
            });
        });
    }

    // Runs benchmark for the specific sort algorithm
    public Object runBenchmark(int row, int col) {
        if (row < 0 || row >= data.length || col <= 0 || col >= columnNames.length) {
            return "Error";
        }

        int size = (int) data[row][0];
        int[] array = randArray(size);

        switch (col) {
            case 1: return timeSort(array, this::bubbleSort);
            case 2: return timeSort(array, this::selectionSort);
            case 3: return timeSort(array, this::insertionSort);
            case 4: return timeSort(array, this::mergeSort);
            case 5: return timeSort(array, this::heapSort);
            default: return "Error";
        }
    }

    // Generates a random integer array
    private int[] randArray(int n) {
        int[] arr = new int[n];
        for (int i = 0; i < n; i++) arr[i] = RAND.nextInt();
        return arr;
    }

    // Measures the time (in ms) to sort an array with the given sorter
    private Object timeSort(int[] arr, Sorter sorter) {
        try {
            long start = System.nanoTime();
            sorter.sort(arr);
            double timeMs = (System.nanoTime() - start) / 1_000_000.0;
            return timeMs;
        } catch (Exception e) {
            return "Error";
        }
    }

    @Override public int getRowCount() { return data.length; }
    @Override public int getColumnCount() { return columnNames.length; }
    @Override public String getColumnName(int col) { return columnNames[col]; }

    // Formats table values for display
    @Override
    public Object getValueAt(int row, int col) {
        if (col == 0) {
            int size = (int) data[row][col];
            return df.format(size);
        }
        Object value = data[row][col];
        if (value instanceof Double) {
            return df.format((Double) value);
        }
        return value;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        // Treat all columns as String for rendering consistency
        return String.class;
    }

    public Object[][] getData() {
        return data;
    }

    public void shutdown() {
        executor.shutdown();
    }

    // Sorter functional interface
    private interface Sorter { void sort(int[] a); }

    // Bubble Sort
    private void bubbleSort(int[] a) {
        for (int i = 0; i < a.length - 1; i++) {
            boolean swapped = false;
            for (int j = 0; j < a.length - i - 1; j++) {
                if (a[j] > a[j + 1]) {
                    int tmp = a[j]; a[j] = a[j + 1]; a[j + 1] = tmp;
                    swapped = true;
                }
            }
            if (!swapped) break;
        }
    }

    // Selection Sort
    private void selectionSort(int[] a) {
        for (int i = 0; i < a.length - 1; i++) {
            int min = i;
            for (int j = i + 1; j < a.length; j++) if (a[j] < a[min]) min = j;
            int tmp = a[i]; a[i] = a[min]; a[min] = tmp;
        }
    }

    // Insertion Sort
    private void insertionSort(int[] a) {
        for (int i = 1; i < a.length; i++) {
            int key = a[i], j = i - 1;
            while (j >= 0 && a[j] > key) a[j + 1] = a[j--];
            a[j + 1] = key;
        }
    }

    // Merge Sort
    private void mergeSort(int[] a) {
        mergeSort(a, new int[a.length], 0, a.length - 1);
    }

    private void mergeSort(int[] a, int[] tmp, int l, int r) {
        if (l >= r) return;
        int m = (l + r) / 2;
        mergeSort(a, tmp, l, m);
        mergeSort(a, tmp, m + 1, r);
        merge(a, tmp, l, m, r);
    }

    private void merge(int[] a, int[] tmp, int l, int m, int r) {
        int i = l, j = m + 1, k = l;
        while (i <= m && j <= r) tmp[k++] = a[i] <= a[j] ? a[i++] : a[j++];
        while (i <= m) tmp[k++] = a[i++];
        while (j <= r) tmp[k++] = a[j++];
        System.arraycopy(tmp, l, a, l, r - l + 1);
    }

    // Heap Sort
    private void heapSort(int[] a) {
        int n = a.length;
        for (int i = n / 2 - 1; i >= 0; i--) heapify(a, n, i);
        for (int i = n - 1; i > 0; i--) {
            int tmp = a[0]; a[0] = a[i]; a[i] = tmp;
            heapify(a, i, 0);
        }
    }

    private void heapify(int[] a, int n, int i) {
        int largest = i, l = 2 * i + 1, r = 2 * i + 2;
        if (l < n && a[l] > a[largest]) largest = l;
        if (r < n && a[r] > a[largest]) largest = r;
        if (largest != i) {
            int tmp = a[i]; a[i] = a[largest]; a[largest] = tmp;
            heapify(a, n, largest);
        }
    }
}