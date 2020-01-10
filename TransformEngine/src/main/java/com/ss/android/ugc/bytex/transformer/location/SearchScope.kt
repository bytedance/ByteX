package com.ss.android.ugc.bytex.transformer.location

/**
 * Created by yangzhiqian on 2019-12-10<br/>
 */
enum class SearchScope {
    /**
     * All input of the transform<br/>
     * equals to INPUT_JAR+INPUT_DIR
     */
    INPUT,
    /**
     * All input jar of the transform<br/>
     */
    INPUT_JAR,
    /**
     * All input dir(classes) of the transform<br/>
     */
    INPUT_DIR,

    /**
     * All origin input of the project.Same as all inputs(jar+dir) of the <br/>
     * first Transform with FullProject as Scope<br/>
     * equals to ORIGIN_JAR+ORIGIN_DIR
     */
    ORIGIN,
    /**
     * All input jar of the project<br/>
     */
    ORIGIN_JAR,
    /**
     * All input dir(classes) of the project<br/>
     */
    ORIGIN_DIR,

    /**
     * All aar of the project depends on<br/>
     */
    ORIGIN_AAR
}