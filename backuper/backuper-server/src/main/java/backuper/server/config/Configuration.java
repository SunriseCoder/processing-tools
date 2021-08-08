package backuper.server.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Configuration {
    private int serverPort;

    private List<Resource> resources;
    private List<User> users;

    private Map<String, Resource> resourceMap;
    private Map<String, User> usersByLoginMap;
    private Map<String, User> usersByTokenMap;

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public List<Resource> getResources() {
        return resources;
    }

    public void setResources(List<Resource> resources) {
        this.resources = resources;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public void linkObjects() {
        resourceMap = new HashMap<>();
        resources.forEach(resource -> resourceMap.put(resource.getName(), resource));

        usersByLoginMap = new HashMap<>();
        users.forEach(user -> {
            usersByLoginMap.put(user.getLogin(), user);
            usersByTokenMap.put(user.getToken(), user);
        });
    }
}
