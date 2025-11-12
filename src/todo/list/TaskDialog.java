import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

/**
 * TaskDialog for TodoApp
 * - Only Title, Priority, Due Date, Completed fields
 * - Black/dark background
 * - No description or category
 */
public class TaskDialog extends JDialog {

    private boolean saved = false;

    private JTextField titleField = new JTextField(20);
    private JComboBox<TodoApp.Priority> priorityBox = new JComboBox<>(TodoApp.Priority.values());
    private JTextField dueDateField = new JTextField(10); // format: YYYY-MM-DD
    private JCheckBox completedBox = new JCheckBox("Completed");

    public TaskDialog(Frame owner) {
        super(owner, "Task", true);
        setup();
    }

    private void setup() {
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(Color.BLACK);

        // Center panel for inputs
        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(Color.BLACK);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);

        Font labelFont = new Font("Segoe UI", Font.BOLD, 14);
        Font inputFont = new Font("Segoe UI", Font.PLAIN, 14);

        // Title
        c.gridx = 0; c.gridy = 0; c.anchor = GridBagConstraints.EAST;
        JLabel titleLabel = new JLabel("Title:");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(labelFont);
        center.add(titleLabel, c);

        c.gridx = 1; c.anchor = GridBagConstraints.WEST;
        titleField.setFont(inputFont);
        titleField.setBackground(Color.DARK_GRAY);
        titleField.setForeground(Color.WHITE);
        titleField.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        center.add(titleField, c);

        // Priority
        c.gridx = 0; c.gridy++; c.anchor = GridBagConstraints.EAST;
        JLabel priorityLabel = new JLabel("Priority:");
        priorityLabel.setForeground(Color.WHITE);
        priorityLabel.setFont(labelFont);
        center.add(priorityLabel, c);

        c.gridx = 1; c.anchor = GridBagConstraints.WEST;
        priorityBox.setFont(inputFont);
        priorityBox.setBackground(Color.DARK_GRAY);
        priorityBox.setForeground(Color.WHITE);
        center.add(priorityBox, c);

        // Due Date
        c.gridx = 0; c.gridy++; c.anchor = GridBagConstraints.EAST;
        JLabel dueLabel = new JLabel("Due (YYYY-MM-DD):");
        dueLabel.setForeground(Color.WHITE);
        dueLabel.setFont(labelFont);
        center.add(dueLabel, c);

        c.gridx = 1; c.anchor = GridBagConstraints.WEST;
        dueDateField.setFont(inputFont);
        dueDateField.setBackground(Color.DARK_GRAY);
        dueDateField.setForeground(Color.WHITE);
        dueDateField.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        center.add(dueDateField, c);

        // Completed
        c.gridx = 1; c.gridy++;
        completedBox.setForeground(Color.WHITE);
        completedBox.setBackground(Color.BLACK);
        completedBox.setFont(labelFont);
        center.add(completedBox, c);

        add(center, BorderLayout.CENTER);

        // Buttons panel
        JPanel south = new JPanel();
        south.setBackground(Color.BLACK);
        south.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        TodoApp.RoundedButton saveBtn = new TodoApp.RoundedButton("Save");
        saveBtn.setButtonColor(new Color(76, 175, 80));
        TodoApp.RoundedButton cancelBtn = new TodoApp.RoundedButton("Cancel");
        cancelBtn.setButtonColor(new Color(244, 67, 54));

        south.add(saveBtn);
        south.add(cancelBtn);

        add(south, BorderLayout.SOUTH);

        // Button actions
        saveBtn.addActionListener(e -> {
            if (titleField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Title required.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            saved = true;
            setVisible(false);
        });

        cancelBtn.addActionListener(e -> {
            saved = false;
            setVisible(false);
        });

        pack();
        setResizable(false);
        setLocationRelativeTo(getOwner());
    }

    // Fill fields when editing
    public void setTask(TodoApp.Task t) {
        titleField.setText(t.getTitle());
        priorityBox.setSelectedItem(t.getPriority());
        dueDateField.setText(t.getDueDate() != null ? t.getDueDate().toString() : "");
        completedBox.setSelected(t.isCompleted());
    }

    // Create new task
    public TodoApp.Task buildTask() {
        String title = titleField.getText().trim();
        TodoApp.Priority p = (TodoApp.Priority) priorityBox.getSelectedItem();

        LocalDate due = null;
        try {
            if (!dueDateField.getText().trim().isEmpty()) {
                due = LocalDate.parse(dueDateField.getText().trim());
            }
        } catch (Exception ignored) {}

        TodoApp.Task t = new TodoApp.Task(title, "", null, p, due);
        t.setCompleted(completedBox.isSelected());
        return t;
    }

    // Apply edited values
    public void applyTo(TodoApp.Task t) {
        t.setTitle(titleField.getText().trim());
        t.setPriority((TodoApp.Priority) priorityBox.getSelectedItem());

        try {
            if (!dueDateField.getText().trim().isEmpty()) {
                t.setDueDate(LocalDate.parse(dueDateField.getText().trim()));
            } else {
                t.setDueDate(null);
            }
        } catch (Exception ignored) {}

        t.setCompleted(completedBox.isSelected());
    }

    public boolean isSaved() { return saved; }
}
