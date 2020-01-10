package com.ss.android.ugc.bytex.example.coverage

import com.ss.android.ugc.bytex.coverage_lib.CoverageHandler
import com.ss.android.ugc.bytex.coverage_lib.DemoCoverageImp

/**
 * Created by jiangzilai on 2019-12-18.
 */
object CoverageReportTask {
    fun init() {
        CoverageHandler.init(DemoCoverageImp.getInstance())
    }
}