package analyse.bean;

public class Hit {
    private String className;
    private int hitCount;

    public Hit(String className, int hitCount) {
        this.className = className;
        this.hitCount = hitCount;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public int getHitCount() {
        return hitCount;
    }

    public void setHitCount(int hitCount) {
        this.hitCount = hitCount;
    }
}
