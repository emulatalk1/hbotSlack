package core.bot.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 *
 * @author Spectre
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Edited {
    private String user;
    private String ts;

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getTs() {
        return ts;
    }

    public void setTs(String ts) {
        this.ts = ts;
    }
}
