import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class Task implements Serializable, Comparable<Task> {
    private static final long serialVersionUID = 1L;

    private UUID id;
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
        this.priority = (priority == null ? Priority.MEDIUM : priority);
        this.dueDate = dueDate;
        this.createdAt = LocalDateTime.now();
        this.completed = false;
    }

    // Getters / Setters
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
    public String toString() {
        String done = completed ? "[Completed] " : "[ ] ";
        String cat = (category != null ? " (" + category.getName() + ")" : "");
        String due = dueDate != null ? " - due " + dueDate.toString() : "";
        return done + title + cat + due + " - " + priority.name();
    }

    @Override
    public int compareTo(Task other) {
        // Primary: priority (HIGH first), then dueDate (earlier first), then createdAt
        int pcmp = other.priority.ordinal() - this.priority.ordinal(); // HIGH=2..LOW=0
        if (pcmp != 0) return pcmp;
        if (this.dueDate != null && other.dueDate != null) {
            int dcmp = this.dueDate.compareTo(other.dueDate);
            if (dcmp != 0) return dcmp;
        } else if (this.dueDate != null) {
            return -1;
        } else if (other.dueDate != null) {
            return 1;
        }
        return this.createdAt.compareTo(other.createdAt);
    }
}
