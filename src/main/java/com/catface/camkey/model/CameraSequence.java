package com.catface.camkey.model;

import java.util.ArrayList;
import java.util.List;

public class CameraSequence {
    private final String name;
    private final List<Keyframe> frames;

    public CameraSequence(String name) {
        this.name = name;
        this.frames = new ArrayList<>();
    }

    public void addKeyframe(Keyframe frame) {
        this.frames.add(frame);
    }

    public List<Keyframe> getFrames() {
        return frames;
    }

    public String getName() {
        return name;
    }
}