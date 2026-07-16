package cloud.apposs.robot.harness.schedule;

import java.io.File;
import java.nio.file.Path;

public class SchedulePath {
    private final String id;

    private final Path path;

    public SchedulePath(String id, Path path) {
        this.id = id;
        this.path = path;
    }

    public static SchedulePath of(String id, Path path) {
        return new SchedulePath(id, path);
    }

    public String getId() {
        return id;
    }

    public Path getPath() {
        return path;
    }

    public boolean isModified(File file) {
        return file.lastModified() > path.toFile().lastModified();
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SchedulePath other = (SchedulePath) obj;
        return id.equals(other.id);
    }
}
