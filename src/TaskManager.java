import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class TaskManager implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<Task> tasks = new ArrayList<>();
    private Set<Category> categories = new HashSet<>();

    public TaskManager() {
        categories.add(new Category("General"));
    }

    public void addTask(Task t) {
        tasks.add(t);
        if (t.getCategory() != null) categories.add(t.getCategory());
    }

    public void updateTask(Task t) {
        // tasks stored by reference; ensure category set contains it
        if (t.getCategory() != null) categories.add(t.getCategory());
    }

    public void removeTask(UUID id) {
        tasks.removeIf(t -> t.getId().equals(id));
    }

    public List<Task> getTasks() {
        List<Task> copy = new ArrayList<>(tasks);
        Collections.sort(copy);
        return copy;
    }

    public Task findById(UUID id) {
        for (Task t : tasks) if (t.getId().equals(id)) return t;
        return null;
    }

    public List<Task> filterByCategory(String name) {
        return tasks.stream()
            .filter(t -> t.getCategory() != null && t.getCategory().getName().equalsIgnoreCase(name))
            .sorted()
            .collect(Collectors.toList());
    }

    public List<Task> filterByPriority(Priority p) {
        return tasks.stream().filter(t -> t.getPriority() == p).sorted().collect(Collectors.toList());
    }

    public List<Task> search(String q) {
        String lower = q == null ? "" : q.toLowerCase();
        return tasks.stream()
            .filter(t -> t.getTitle().toLowerCase().contains(lower) || (t.getDescription()!=null && t.getDescription().toLowerCase().contains(lower)))
            .sorted()
            .collect(Collectors.toList());
    }

    public void saveToFile(File f) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(f))) {
            out.writeObject(this);
        }
    }

    public static TaskManager loadFromFile(File f) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(f))) {
            Object obj = in.readObject();
            if (obj instanceof TaskManager) return (TaskManager) obj;
            throw new IOException("Invalid data");
        }
    }

    public Set<Category> getCategories() { return categories; }

    void clearAllTasks() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}
