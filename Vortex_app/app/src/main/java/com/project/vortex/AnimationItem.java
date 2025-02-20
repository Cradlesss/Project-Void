package com.project.vortex;

public class AnimationItem {
    private String name;
    private int command;
    public AnimationItem(String name, int command) {
        this.name = name;
        this.command = command;
    }
    public String getName() {
        return name;
    }
    public int getCommand() {
        return command;
    }
}
