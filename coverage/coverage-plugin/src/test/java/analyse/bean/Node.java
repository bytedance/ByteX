package analyse.bean;

import java.io.Serializable;
import java.util.List;

public class Node implements Serializable {

    private String className = "";
    private List<String> children;

    //parent 可能是interface
    private String parent = "";

    private List<Node> childrenNodes;

    private int hitCount = 0;

    public Node(String className, List<String> children, String parent) {
        this.className = className;
        this.children = children;
        this.parent = parent;
    }

    public int getHitCount() {
        return hitCount;
    }

    public void setHitCount(int hitCount) {
        this.hitCount = hitCount;
    }

    public void setChildren(List<String> children) {
        this.children = children;
    }

    public String getClassName() {
        return className;
    }

    public List<String> getChildren() {
        return children;
    }

    public List<Node> getChildrenNodes() {
        return childrenNodes;
    }

    public void setChildrenNodes(List<Node> childrenNodes) {
        this.childrenNodes = childrenNodes;
    }

    public String getParent() {
        return parent;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    @Override
    public String toString() {
        return "Node{" +
                "className='" + className + '\'' +
                ", children=" + children +
                ", parent='" + parent + '\'' +
                ", childrenNodes=" + childrenNodes +
                ", hitCount=" + hitCount +
                '}';
    }
}