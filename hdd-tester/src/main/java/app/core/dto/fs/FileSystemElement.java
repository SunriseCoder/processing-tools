package app.core.dto.fs;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class FileSystemElement {
    private String name;
    @JsonIgnore
    private FileSystemElement parent;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FileSystemElement getParent() {
        return parent;
    }

    public void setParent(FileSystemElement parent) {
        this.parent = parent;
    }

    @JsonIgnore
    public String getPath() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName());

        FileSystemElement element = this;
        FileSystemElement parent;
        while ((parent = element.getParent()) != null) {
            // Adding parent with slash like: "parent/"
            sb.insert(0, "/").insert(0, parent.getName());
            element = parent;
        }
        return sb.toString();
    }

}
