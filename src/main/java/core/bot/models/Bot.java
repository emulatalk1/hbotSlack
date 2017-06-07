/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package core.bot.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 *
 * @author Spectre
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class Bot {
    private String id;
    private String name;
    private Icon icons;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Icon getIcons() {
        return icons;
    }

    public void setIcons(Icon icons) {
        this.icons = icons;
    }
}
