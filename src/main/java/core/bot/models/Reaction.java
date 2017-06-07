package core.bot.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 *
 * @author Spectre
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Reaction {
    private String name;
    private int count;
    private String[] users;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String[] getUsers() {
        return users;
    }

    public void setUsers(String[] users) {
        this.users = users;
    }
}
