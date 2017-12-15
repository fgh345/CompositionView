package com.tangce.wisdom.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.*
import android.util.AttributeSet
import android.view.Gravity
import com.tangce.wisdom.widget.helper.CompositionSpan


/**
 * Created by TangCE-Z on 2017/12/11.
 */

class CompositionEditText : android.support.v7.widget.AppCompatEditText {

    //格子画笔
    private val paintB = Paint()
    private var horCoun: Int = 0//每行字数
    private var verCoun: Int = 0//行数
    private val limSize = 1000 //字数限制

    constructor(context: Context) : super(context) {
        initView()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initView()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initView()
    }


    private fun initView() {

        background = null
        setPadding(0, 0, 0, 0)//去除默认边距
        setLineSpacing(0f, 2f)

        gravity = Gravity.START
        isLongClickable = false

        filters = arrayOf<InputFilter>(InputFilter.LengthFilter(limSize))

        paintB.color = Color.GREEN

    }

    override fun onTextChanged(text: CharSequence, start: Int, lengthBefore: Int, lengthAfter: Int) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
        if (lengthAfter > 0) {//添加字符
            for (index in start until start + lengthAfter) {
                getText().setSpan(CompositionSpan(lineHeight - 10, paint), index, index + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        for (linesNum in 0 until verCoun) {
            canvas.drawLine(0f, linesNum * lineHeight.toFloat() + 5, (lineHeight - 10f) * horCoun, linesNum * lineHeight.toFloat() + 5, paintB)
            canvas.drawLine(0f, (linesNum + 1) * lineHeight.toFloat() - 5, (lineHeight - 10f) * horCoun, (linesNum + 1) * lineHeight.toFloat() - 5, paintB)
            for (num in 0..horCoun + 1) {
                canvas.drawLine(num * (lineHeight - 10f), linesNum * lineHeight.toFloat() + 5, num * (lineHeight - 10f), (linesNum + 1) * lineHeight.toFloat() - 5, paintB)
            }
        }
        super.onDraw(canvas)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        horCoun = width / (lineHeight - 10)//一行最多几个格子
        verCoun = if (limSize % horCoun == 0) limSize / horCoun else limSize / horCoun + 1
    }
}

