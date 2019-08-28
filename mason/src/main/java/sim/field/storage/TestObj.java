package sim.field.storage;

public class TestObj implements java.io.Serializable {
    public int id;

    public TestObj(int id) {
        this.id = id;
        }

    public String toString() {
        return String.format("%d", id);
        }

    public static int getMaxObjectSize() {
        return 128;
        }
    }
