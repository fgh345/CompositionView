package com.hkz.compositionview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import kotlinx.android.synthetic.main.activity_main3.*

class Main3Activity : AppCompatActivity() {


    /**
     * 行高(包括行间距) 行高减去行间距等于<格子宽>
     */
    private var lineHeight: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main3)

        lineHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50f, resources.displayMetrics).toInt()
        cv_core.setScrollLinister(object : CompositionView.CtrlScroll {
            override fun scrolTo(y: Int) {
                if (y < sv_scr.scrollY)
                    sv_scr.scrollTo(0, y)
                else if (y - sv_scr.scrollY > lineHeight * 5) {
                    sv_scr.scrollTo(0, sv_scr.scrollY + lineHeight)
                }

            }

        })

    }
}
