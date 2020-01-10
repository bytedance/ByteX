package com.ss.android.ugc.bytex.example.consts

import android.content.Context
import android.content.Intent
import com.ss.android.ugc.bytex.example.MainActivity

object KtConsts {
    val CONST_1 = "CONST_1"
    val CONST_2 = "CONST_2"

    @JvmStatic
    fun test1(context: Context, eventType: String, pageType: Int) {
        val intent = Intent(context, MainActivity::class.java)
        intent.putExtra(CONST_1, eventType)
        intent.putExtra(CONST_2, pageType)
        context.startActivity(intent)
    }


    fun test2(context: Context, enable: Boolean) {
        val sp = context.getSharedPreferences(CONST_1, Context.MODE_PRIVATE)
        val editor = sp.edit()
        editor.putBoolean(CONST_2, enable)
        editor.apply()
    }

    fun test3(context: Context): Boolean {
        val sp = context.getSharedPreferences(CONST_1, Context.MODE_PRIVATE)
        return sp.getBoolean(CONST_2, false)
    }
}
