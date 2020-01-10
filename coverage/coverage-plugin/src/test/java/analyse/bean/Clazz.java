package analyse.bean;

import java.io.Serializable;

public class Clazz implements Serializable {
    private String id;
    private String className;
    private String methodName;
    private String desc;

    public static final String INIT = "<init>";
    public static final String CLINIT = "<clinit>";


    public Clazz(String id, String className, String methodName, String desc) {
        this.id = id;
        this.className = className;
        this.methodName = methodName;
        this.desc = desc;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }
}
