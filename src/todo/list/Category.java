import java.io.Serializable;
import java.util.Objects;

public class Category implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;

    public Category(String name) { this.name = name == null ? "General" : name; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public String toString() { return name; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Category)) return false;
        return Objects.equals(name, ((Category)o).name);
    }

    @Override
    public int hashCode() { return Objects.hash(name); }
}
