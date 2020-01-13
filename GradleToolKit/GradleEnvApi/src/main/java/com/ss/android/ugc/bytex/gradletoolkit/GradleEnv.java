package com.ss.android.ugc.bytex.gradletoolkit;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

import javax.annotation.Nonnull;

/**
 * Created by tanlehua on 2019-04-29.
 */
public interface GradleEnv {
    @Nonnull
    default Collection<File> getArtifact(@Nonnull Artifact artifact) {
        return Collections.emptyList();
    }
}
