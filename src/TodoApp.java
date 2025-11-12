import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Single-file ready-to-run Todo application with improved GUI layout.
 * - All functionality (add/edit/delete/toggle/search/save/clear) preserved.
 * - Uses serialization to save/load tasks to todo_data.ser in user home.
 *
 * To run:
 *   javac TodoApp.java
 *   java TodoApp
 */
public class TodoApp extends JFrame {

    // ---------- Data layer: Task, Category, Priority, TaskManager ----------

    public enum Priority { LOW, MEDIUM, HIGH }

    public static class Category implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;

        public Category(String name) { this.name = name; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class Task implements Serializable {
        private static final long serialVersionUID = 1L;
        private final UUID id;
        private String title;
        private String description;
        private Category category;
        private Priority priority;
        private LocalDate dueDate;
        private LocalDateTime createdAt;
        private boolean completed;

        public Task(String title, String description, Category category, Priority priority, LocalDate dueDate) {
            this.id = UUID.randomUUID();
            this.title = title;
            this.description = description;
            this.category = category;
            this.priority = priority == null ? Priority.MEDIUM : priority;
            this.dueDate = dueDate;
            this.createdAt = LocalDateTime.now();
            this.completed = false;
        }

        public UUID getId() { return id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Category getCategory() { return category; }
        public void setCategory(Category category) { this.category = category; }
        public Priority getPriority() { return priority; }
        public void setPriority(Priority priority) { this.priority = priority; }
        public LocalDate getDueDate() { return dueDate; }
        public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }
        public void toggleCompleted() { this.completed = !this.completed; }

        @Override
        public String toString() { return title; }
    }

    public static class TaskManager implements Serializable {
        private static final long serialVersionUID = 1L;
        private final List<Task> tasks = new ArrayList<>();

        public List<Task> getTasks() { return new ArrayList<>(tasks); }
        public void addTask(Task t) { tasks.add(t); }
        public void updateTask(Task t) {
            for (int i = 0; i < tasks.size(); i++) {
                if (tasks.get(i).getId().equals(t.getId())) { tasks.set(i, t); return; }
            }
        }
        public void removeTask(UUID id) {
            tasks.removeIf(t -> t.getId().equals(id));
        }
        public void clearAllTasks() { tasks.clear(); }
        public List<Task> search(String q) {
            String ql = q.toLowerCase();
            List<Task> out = new ArrayList<>();
            for (Task t : tasks) {
                if ((t.getTitle() != null && t.getTitle().toLowerCase().contains(ql)) ||
                    (t.getDescription() != null && t.getDescription().toLowerCase().contains(ql)) ||
                    (t.getCategory() != null && t.getCategory().getName().toLowerCase().contains(ql))) {
                    out.add(t);
                }
            }
            return out;
        }

        // persistence helpers
        public void saveToFile(File f) throws IOException {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f))) {
                oos.writeObject(this);
            }
        }
        public static TaskManager loadFromFile(File f) throws IOException, ClassNotFoundException {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
                Object o = ois.readObject();
                if (o instanceof TaskManager) return (TaskManager) o;
                else throw new IOException("Invalid data");
            }
        }
    }

    // ---------- UI helper components: RoundedButton, RoundedPanel ----------

    static class RoundedButton extends JButton {
        private int radius = 14;
        public RoundedButton(String text) {
            super(text);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        }
        public void setButtonColor(Color c) { setBackground(c); }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            super.paintComponent(g);
            g2.dispose();
        }
    }

    static class RoundedPanel extends JPanel {
        private int radius = 18;
        public RoundedPanel(LayoutManager layout) { super(layout); setOpaque(false); }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
        }
    }

    // ---------- Instance fields (GUI + model) ----------

    private TaskManager manager = new TaskManager();
    private DefaultListModel<Task> listModel = new DefaultListModel<>();
    private JList<Task> taskJList = new JList<>(listModel);
    private File storageFile = new File(System.getProperty("user.home"), "todo_data.ser");

    private boolean darkMode = false;

    private Color LIGHT_BG = new Color(245, 245, 250);
    private Color LIGHT_SIDE = new Color(235, 238, 245);
    private Color DARK_BG = new Color(40, 42, 48);
    private Color DARK_SIDE = new Color(52, 55, 63);
    private Color DARK_TEXT = new Color(230, 230, 230);
    private Color LIGHT_TEXT = Color.BLACK;

    private JPanel rightPanel;
    private JTextArea detailsArea;
    private JScrollPane listScroll;
    private JTextField searchField;

    // ---------- Constructor ----------

    public TodoApp() {
        super("Beautiful TODO List");
        loadIfExists();
        initComponents();
        refreshList();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 560);
        setLocationRelativeTo(null);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) { saveTasks(); }
        });
    }

    // ---------- UI Initialization (redesigned) ----------

    private void initComponents() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(LIGHT_BG);

        // LEFT SIDEBAR (search + list)
        RoundedPanel leftSidebar = new RoundedPanel(new BorderLayout());
        leftSidebar.setPreferredSize(new Dimension(280, 0));
        leftSidebar.setBackground(new Color(238, 238, 245));
        leftSidebar.setBorder(new EmptyBorder(12, 12, 12, 12));

        searchField = new JTextField();
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 225)),
                BorderFactory.createEmptyBorder(8,10,8,10)));
        searchField.setBackground(new Color(250,250,255));
        searchField.setToolTipText("Type and press Enter to search (title/description/category)");

        searchField.addActionListener(e -> {
            String q = searchField.getText().trim();
            if (q.isEmpty()) refreshList();
            else {
                var results = manager.search(q);
                listModel.clear();
                for (Task t : results) listModel.addElement(t);
            }
        });

        leftSidebar.add(searchField, BorderLayout.NORTH);

        // Task list
        taskJList.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        taskJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Renderer: priority colored dot + bold/plain based on completed
        taskJList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Task) {
                    Task t = (Task) value;
                    lbl.setText("  " + t.getTitle());
                    Font f = lbl.getFont();
                    lbl.setFont(t.isCompleted() ? f.deriveFont(Font.PLAIN) : f.deriveFont(Font.BOLD));
                    Color dotColor;
                    if (t.getPriority() == Priority.HIGH) dotColor = new Color(0xE53935);
                    else if (t.getPriority() == Priority.MEDIUM) dotColor = new Color(0xFFB300);
                    else dotColor = new Color(0x43A047);

                    // icon: small colored circle
                    BufferedImage img = new BufferedImage(12,12,BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2 = img.createGraphics();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(dotColor);
                    g2.fillOval(0,0,10,10);
                    g2.dispose();
                    lbl.setIcon(new ImageIcon(img));
                }
                return lbl;
            }
        });

        listScroll = new JScrollPane(taskJList);
        listScroll.setBorder(BorderFactory.createEmptyBorder());
        leftSidebar.add(listScroll, BorderLayout.CENTER);

        add(leftSidebar, BorderLayout.WEST);

        // TOP TOOLBAR (Add, Edit, Delete, Toggle)
        RoundedPanel topBar = new RoundedPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        topBar.setBackground(new Color(255,255,255));
        topBar.setBorder(new EmptyBorder(8,8,8,8));
        topBar.setPreferredSize(new Dimension(0, 64));

        RoundedButton addBtn = makeButton(" Add", new Color(0x42A5F5));
        RoundedButton editBtn = makeButton(" Edit", new Color(0x7E57C2));
        RoundedButton deleteBtn = makeButton(" Delete", new Color(0xEF5350));
        RoundedButton toggleBtn = makeButton("Toggle", new Color(0x8D6E63));

        addHoverEffect(addBtn);
        addHoverEffect(editBtn);
        addHoverEffect(deleteBtn);
        addHoverEffect(toggleBtn);

        topBar.add(addBtn);
        topBar.add(editBtn);
        topBar.add(deleteBtn);
        topBar.add(toggleBtn);

        add(topBar, BorderLayout.NORTH);

        // RIGHT DETAIL PANEL
        rightPanel = new RoundedPanel(new BorderLayout());
        rightPanel.setPreferredSize(new Dimension(320, 0));
        rightPanel.setBackground(new Color(245,245,248));
        rightPanel.setBorder(new EmptyBorder(12,12,12,12));

        detailsArea = new JTextArea();
        detailsArea.setEditable(false);
        detailsArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        detailsArea.setLineWrap(true);
        detailsArea.setWrapStyleWord(true);
        detailsArea.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));

        JScrollPane detailScroll = new JScrollPane(detailsArea);
        detailScroll.setBorder(BorderFactory.createEmptyBorder());
        rightPanel.add(detailScroll, BorderLayout.CENTER);

        // Right bottom buttons
        RoundedPanel sideButtons = new RoundedPanel(new GridLayout(0,1,10,10));
        sideButtons.setBackground(new Color(255,255,255));
        sideButtons.setBorder(new EmptyBorder(10,10,10,10));

        RoundedButton saveBtn = makeButton("Save", new Color(0xFFA726));
        RoundedButton clearBtn = makeButton(" Clear All", new Color(0xFB8C00));
        RoundedButton darkBtn = makeButton(" Dark Mode", new Color(0x546E7A));

        addHoverEffect(saveBtn);
        addHoverEffect(clearBtn);
        addHoverEffect(darkBtn);

        sideButtons.add(saveBtn);
        sideButtons.add(clearBtn);
        sideButtons.add(darkBtn);

        rightPanel.add(sideButtons, BorderLayout.SOUTH);

        add(rightPanel, BorderLayout.EAST);

        // --- Selection listener: show details ---
        taskJList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            Task sel = taskJList.getSelectedValue();
            if (sel == null) { detailsArea.setText(""); return; }
            StringBuilder sb = new StringBuilder();
            sb.append("Title: ").append(sel.getTitle()).append("\n\n");
            sb.append("Description: ").append(sel.getDescription() == null ? "" : sel.getDescription()).append("\n\n");
            sb.append("Category: ").append(sel.getCategory() != null ? sel.getCategory().getName() : "General").append("\n");
            sb.append("Priority: ").append(sel.getPriority()).append("\n");
            sb.append("Due: ").append(sel.getDueDate() != null ? sel.getDueDate().format(DateTimeFormatter.ISO_DATE) : "n/a").append("\n");
            sb.append("Created: ").append(sel.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("\n");
            sb.append("Completed: ").append(sel.isCompleted()).append("\n");
            detailsArea.setText(sb.toString());
        });

        // ---------- Button actions (preserve original behavior) ----------
        addBtn.addActionListener(e -> {
            TaskDialog d = new TaskDialog(this);
            d.setTitle("Add Task");
            d.setTask(new Task("", "", null, Priority.MEDIUM, null));
            d.setVisible(true);
            if (d.isSaved()) {
                manager.addTask(d.buildTask());
                saveTasks();
                refreshList();
            }
        });

        editBtn.addActionListener(e -> {
            Task sel = taskJList.getSelectedValue();
            if (sel == null) { JOptionPane.showMessageDialog(this, "Select a task to edit."); return; }
            TaskDialog d = new TaskDialog(this);
            d.setTitle("Edit Task");
            d.setTask(sel);
            d.setVisible(true);
            if (d.isSaved()) {
                d.applyTo(sel);
                manager.updateTask(sel);
                saveTasks();
                refreshList();
            }
        });

        deleteBtn.addActionListener(e -> {
            Task sel = taskJList.getSelectedValue();
            if (sel == null) { JOptionPane.showMessageDialog(this, "Select a task to delete."); return; }
            int ok = JOptionPane.showConfirmDialog(this, "Delete this task?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) {
                manager.removeTask(sel.getId());
                saveTasks();
                refreshList();
            }
        });

        toggleBtn.addActionListener(e -> {
            Task sel = taskJList.getSelectedValue();
            if (sel == null) { JOptionPane.showMessageDialog(this, "Select a task."); return; }
            sel.toggleCompleted();
            manager.updateTask(sel);
            saveTasks();
            refreshList();
        });

        saveBtn.addActionListener(e -> { saveTasks(); JOptionPane.showMessageDialog(this, "Saved!"); });

        clearBtn.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to clear all tasks?",
                    "Confirm Clear All",
                    JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) {
                manager.clearAllTasks();
                saveTasks();
                refreshList();
            }
        });

        darkBtn.addActionListener(e -> toggleDarkMode());

        // apply colors initially
        applyColors();
    }

    // ---------- small UI helpers ----------

    private RoundedButton makeButton(String name, Color color) {
        RoundedButton b = new RoundedButton(name);
        b.setButtonColor(color);
        return b;
    }

    private void addHoverEffect(RoundedButton b) {
        Color base = b.getBackground();
        b.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                b.setBackground(brighten(base, 0.08f));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                b.setBackground(base);
            }
        });
    }

    private Color brighten(Color c, float fraction) {
        int r = c.getRed(), g = c.getGreen(), b = c.getBlue();
        int i = (int)(1.0 / (1.0 - fraction));
        if (r == 0 && g == 0 && b == 0) return new Color(i, i, i);
        if (r > 0 && r < i) r = i;
        if (g > 0 && g < i) g = i;
        if (b > 0 && b < i) b = i;
        return new Color(Math.min((int)(r / (1.0 - fraction)), 255),
                Math.min((int)(g / (1.0 - fraction)), 255),
                Math.min((int)(b / (1.0 - fraction)), 255));
    }

    private void applyColors() {
        if (darkMode) {
            getContentPane().setBackground(DARK_BG);
            rightPanel.setBackground(DARK_SIDE);
            taskJList.setBackground(new Color(55,58,66));
            taskJList.setForeground(DARK_TEXT);
            detailsArea.setBackground(new Color(55,58,66));
            detailsArea.setForeground(DARK_TEXT);
            listScroll.getViewport().setBackground(DARK_BG);
            searchField.setBackground(new Color(65,65,70));
            searchField.setForeground(DARK_TEXT);
        } else {
            getContentPane().setBackground(LIGHT_BG);
            rightPanel.setBackground(LIGHT_SIDE);
            taskJList.setBackground(Color.WHITE);
            taskJList.setForeground(LIGHT_TEXT);
            detailsArea.setBackground(Color.WHITE);
            detailsArea.setForeground(LIGHT_TEXT);
            listScroll.getViewport().setBackground(LIGHT_BG);
            searchField.setBackground(new Color(250,250,255));
            searchField.setForeground(LIGHT_TEXT);
        }
        repaint();
    }

    private void toggleDarkMode() {
        darkMode = !darkMode;
        applyColors();
    }

    // ---------- persistence ----------

    private void loadIfExists() {
        if (storageFile.exists()) {
            try {
                manager = TaskManager.loadFromFile(storageFile);
            } catch (Exception ex) {
                System.out.println("Load failed: " + ex.getMessage());
                manager = new TaskManager();
            }
        }
    }

    private void saveTasks() {
        try { manager.saveToFile(storageFile); }
        catch (Exception ex) { System.out.println("Auto-save failed: " + ex.getMessage()); }
    }

    // ---------- list refresh (completed tasks moved to bottom) ----------

    private void refreshList() {
        List<Task> incomplete = new ArrayList<>();
        List<Task> completed = new ArrayList<>();
        for (Task t : manager.getTasks()) {
            if (t.isCompleted()) completed.add(t);
            else incomplete.add(t);
        }
        listModel.clear();
        for (Task t : incomplete) listModel.addElement(t);
        for (Task t : completed) listModel.addElement(t);
    }

    // ---------- TaskDialog (modal) ----------

    private static class TaskDialog extends JDialog {
        private boolean saved = false;
        private Task task;

        private JTextField titleField;
        private JTextArea descArea;
        private JComboBox<String> categoryCombo;
        private JComboBox<Priority> priorityCombo;
        private JTextField dueField; // format yyyy-MM-dd

        public TaskDialog(Frame owner) {
            super(owner, true);
            setSize(420, 400);
            setLayout(new BorderLayout());
            setLocationRelativeTo(owner);

            JPanel main = new JPanel(new BorderLayout(8,8));
            main.setBorder(new EmptyBorder(12,12,12,12));
            add(main, BorderLayout.CENTER);

            // fields
            titleField = new JTextField();
            titleField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            descArea = new JTextArea(6, 20);
            descArea.setLineWrap(true);
            descArea.setWrapStyleWord(true);

            categoryCombo = new JComboBox<>(new String[]{"General", "Work", "Home", "School", "Other"});
            priorityCombo = new JComboBox<>(Priority.values());
            dueField = new JTextField();
            dueField.setToolTipText("yyyy-MM-dd (leave empty if none)");

            JPanel form = new JPanel(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(6,6,6,6);
            c.gridx = 0; c.gridy = 0; c.anchor = GridBagConstraints.WEST;
            form.add(new JLabel("Title:"), c);
            c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
            form.add(titleField, c);

          
            c.gridy++;
            c.gridx = 0; c.fill = GridBagConstraints.NONE;
            form.add(new JLabel("Priority:"), c);
            c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL;
            form.add(priorityCombo, c);

            c.gridy++;
            c.gridx = 0; c.fill = GridBagConstraints.NONE;
            form.add(new JLabel("Due (yyyy-MM-dd):"), c);
            c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL;
            form.add(dueField, c);

            main.add(form, BorderLayout.CENTER);

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton save = new JButton("Save");
            JButton cancel = new JButton("Cancel");
            buttons.add(save);
            buttons.add(cancel);
            add(buttons, BorderLayout.SOUTH);

            save.addActionListener(e -> {
                if (titleField.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Title required.");
                    return;
                }
                saved = true;
                setVisible(false);
            });

            cancel.addActionListener(e -> {
                saved = false;
                setVisible(false);
            });
        }

        public void setTask(Task t) {
            this.task = t;
            titleField.setText(t.getTitle());
            descArea.setText(t.getDescription());
            String cat = t.getCategory() != null ? t.getCategory().getName() : "General";
            categoryCombo.setSelectedItem(cat);
            priorityCombo.setSelectedItem(t.getPriority());
            dueField.setText(t.getDueDate() != null ? t.getDueDate().toString() : "");
        }

        public boolean isSaved() { return saved; }

        public Task buildTask() {
            String title = titleField.getText().trim();
            String desc = descArea.getText().trim();
            String cat = (String) categoryCombo.getSelectedItem();
            Priority p = (Priority) priorityCombo.getSelectedItem();
            LocalDate due = null;
            String dueText = dueField.getText().trim();
            if (!dueText.isEmpty()) {
                try { due = LocalDate.parse(dueText); }
                catch (Exception ex) { /* ignore parse error, just leave null */ }
            }
            Task newTask = new Task(title, desc, new Category(cat), p, due);
            return newTask;
        }

        public void applyTo(Task t) {
            t.setTitle(titleField.getText().trim());
            t.setDescription(descArea.getText().trim());
            t.setCategory(new Category((String) categoryCombo.getSelectedItem()));
            t.setPriority((Priority) priorityCombo.getSelectedItem());
            String dueText = dueField.getText().trim();
            if (!dueText.isEmpty()) {
                try { t.setDueDate(LocalDate.parse(dueText)); }
                catch (Exception ignored) { t.setDueDate(null); }
            } else t.setDueDate(null);
        }
    }

    // ---------- main ----------

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TodoApp().setVisible(true));
    }
}
