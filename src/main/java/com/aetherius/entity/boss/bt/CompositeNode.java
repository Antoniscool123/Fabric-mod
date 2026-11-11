package com.aetherius.entity.boss.bt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Base class for nodes with ordered children. */
public abstract class CompositeNode implements BehaviorNode {

    protected final List<BehaviorNode> children = new ArrayList<>();

    protected CompositeNode(BehaviorNode... children) {
        Collections.addAll(this.children, children);
    }

    public CompositeNode add(BehaviorNode child) {
        children.add(child);
        return this;
    }

    public List<BehaviorNode> children() {
        return Collections.unmodifiableList(children);
    }

    @Override
    public void reset() {
        for (BehaviorNode child : children) {
            child.reset();
        }
    }
}
