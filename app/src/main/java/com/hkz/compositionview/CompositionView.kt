package com.hkz.compositionview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Handler
import android.os.Message
import android.text.Editable
import android.text.TextPaint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import java.lang.ref.WeakReference

/**
 * Created by TangCE-Z on 2017/12/11.
 */

class CompositionView : View, View.OnClickListener {

    /**
     * 格子画笔
     */
    private val paintA = Paint()
    /**
     * 字画笔
     */
    private val paintB = TextPaint()
    /**
     * 游标画笔
     */
    private val paintC = Paint()

    /**
     *  行间距  上下各 5f
     */
    private val lineSpacing = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6f, resources.displayMetrics).toInt()
    /**
     * 行高(包括行间距) 行高减去行间距等于<格子宽>
     */
    private val lineHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50f, resources.displayMetrics).toInt()
    /**
     * 每行最大字数 （屏幕宽 / 格子宽）
     */
    private var horCoun: Int = 0
    /**
     * 限制最大行数  （也是绘制行数）
     */
    private var verCounMax: Int = 0
    /**
     * 字数限制  通过字数限制 算出最大行数
     */
    private val limSize = 1000
    /**
     * 可编辑字符串
     */
    private lateinit var textEdit: Editable
    /**
     * 字高 由画笔算出
     */
    private var textHeight: Float = 0.0f
    /**
     *  游标 字插入位置
     */
    private var cursor: Int = 0
    /**
     *  游标绘制坐标
     */
    private var cursorDrawPos: IntArray = intArrayOf(0, 0)
    /**
     *  输入法管理器
     */
    private lateinit var input: InputMethodManager
    /**
     *  文本绘制信息集合
     */
    private val wordInfoList = ArrayList<ArrayList<Word>>()
    /**
     *  控制光标闪烁
     */
    private var isTwinkle = false
    /**
     *  scrollview 滚动接口
     */
    private var mCtrlScroll: CtrlScroll? = null

    /**
     *  view 刷新handler
     */
    private val mHandler = CursorHandler(this)

    class CursorHandler(cView: CompositionView) : Handler() {

        private val weakReference: WeakReference<CompositionView> = WeakReference(cView)
        private val view = weakReference.get() as CompositionView

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            removeCallbacksAndMessages(null)

            if (!view.isFocused)
                return

            when {
                msg.what == 0 ->
                    view.isTwinkle = true
                msg.what == 500 ->
                    view.isTwinkle = !view.isTwinkle
            }
            view.invalidate()
            sendEmptyMessageDelayed(500, 500)

        }
    }

    constructor(context: Context) : super(context) {
        initView()
        this.horCoun = horCoun
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initView()
        this.horCoun = horCoun
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initView()
    }


    private fun initView() {

        this.isFocusable = true
        this.isFocusableInTouchMode = true
        input = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        paintA.color = Color.GREEN
        paintB.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 20f, resources.displayMetrics)
        //把字画在背景中间
        paintB.isAntiAlias = true
        paintB.textAlign = Paint.Align.CENTER  //这个只针对x有效
        textHeight = paintB.fontMetrics.bottom - paintB.fontMetrics.top
        textEdit = Editable.Factory.getInstance().newEditable("")

        paintC.strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, resources.displayMetrics)

        isVerticalScrollBarEnabled = true

        mHandler.sendEmptyMessage(0)

        setOnClickListener(this)
        analysisCharacter()
    }

    override fun onDraw(canvas: Canvas) {
        for (linesNum in 0 until verCounMax) {
            canvas.drawLine(paddingStart.toFloat(), linesNum * lineHeight.toFloat() + lineSpacing / 2, paddingStart.toFloat() + (lineHeight - lineSpacing) * horCoun, linesNum * lineHeight.toFloat() + lineSpacing / 2, paintA)
            canvas.drawLine(paddingStart.toFloat(), (linesNum + 1) * lineHeight.toFloat() - lineSpacing / 2, paddingStart.toFloat() + (lineHeight - lineSpacing) * horCoun, (linesNum + 1) * lineHeight.toFloat() - lineSpacing / 2, paintA)
            for (num in 0..horCoun + 1) {

                if (isTwinkle && linesNum == cursorDrawPos[0] && num == cursorDrawPos[1])
                    canvas.drawLine(paddingStart.toFloat() + num * (lineHeight - lineSpacing), linesNum * lineHeight.toFloat() + lineSpacing / 2, paddingStart.toFloat() + num * (lineHeight - lineSpacing), (linesNum + 1) * lineHeight.toFloat() - lineSpacing / 2, paintC)
                else
                    canvas.drawLine(paddingStart.toFloat() + num * (lineHeight - lineSpacing), linesNum * lineHeight.toFloat() + lineSpacing / 2, paddingStart.toFloat() + num * (lineHeight - lineSpacing), (linesNum + 1) * lineHeight.toFloat() - lineSpacing / 2, paintA)

            }

        }

        wordInfoList.forEach {
            it.forEach {
                val cX = paddingStart.toFloat() + it.col * (lineHeight - lineSpacing) + (lineHeight - lineSpacing) / 2
                val cY = it.row * lineHeight.toFloat() + textHeight + lineSpacing

                canvas.drawText(it.mhar.toString(), cX, cY, paintB)
            }
        }

        super.onDraw(canvas)

    }


    private fun analysisCharacter() {
        var row = 0//行
        var col = 0//列
        var index = 0
        wordInfoList.clear()
        wordInfoList.add(ArrayList())//默认给一行
        cursorDrawPos = intArrayOf(row, col)//游标默认位置

        textEdit.forEach {
            when {
                it == '\n' -> {
                    row++
                    index++
                    col = 0
                    wordInfoList.add(ArrayList())
                    if (cursor == index)
                        cursorDrawPos = intArrayOf(row, col)
                }
                else -> {
                    if (col >= horCoun) {
                        row++
                        col = 0
                        wordInfoList.add(ArrayList())
                    }
                    wordInfoList[row].add(Word(it, index++, row, col++))
                    if (cursor == index)
                        cursorDrawPos = intArrayOf(row, col)
                }
            }
        }
        mCtrlScroll?.scrolTo(lineHeight * (cursorDrawPos[0]))
        mHandler.sendEmptyMessage(0)
    }

    class Word(val mhar: Char, val mIndex: Int, var row: Int, var col: Int)

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        horCoun = width / (lineHeight - lineSpacing)//一行最多几个格子
        verCounMax = if (limSize % horCoun == 0) limSize / horCoun else limSize / horCoun + 1

        val padl = width % (lineHeight - lineSpacing) / 2
        setPadding(padl, 0, 0, 0)
    }

    /**
     * 重写该方法，达到使ListView适应ScrollView的效果
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val expandSpec = View.MeasureSpec.makeMeasureSpec(lineHeight * 100 + lineSpacing / 2,
                View.MeasureSpec.AT_MOST)
        super.onMeasure(widthMeasureSpec, expandSpec)
    }

    //首先我们得重写View中的一个方法，返回true，就是让这个View变成文本可编辑的状态，默认返回false
    override fun onCheckIsTextEditor(): Boolean {
        return true
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        return object : BaseInputConnection(this, false) {
            override fun commitText(text: CharSequence, newCursorPosition: Int): Boolean {

                textEdit.insert(cursor, text)
                cursor += text.length
                analysisCharacter()
                return super.setComposingText(text, newCursorPosition)
            }
        }
    }

    override fun onClick(v: View?) {
        //点击控件时获取焦点弹出软键盘输入
        requestFocus()
        input.showSoftInput(this, InputMethodManager.SHOW_FORCED)

    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val posY = event.y

            val row = posY.toInt() / lineHeight

            val col = ((event.x - paddingLeft) / (lineHeight - lineSpacing) + 0.5).toInt()

            val relRow = if (row > wordInfoList.size - 1) wordInfoList.size - 1 else row

            val relCol = if (col > wordInfoList[relRow].size) wordInfoList[relRow].size else col

            cursorDrawPos = intArrayOf(relRow, relCol)

            cursor = when {
                wordInfoList[relRow].size == 0 -> sumCurso(relRow)
                else -> {
                    wordInfoList[relRow][0].mIndex + relCol
                }
            }
            mHandler.sendEmptyMessage(0)
        }
        return super.onTouchEvent(event)
    }

    fun sumCurso(rows: Int): Int {
        var sum = 0
        for (row in rows downTo 0) {
            if (wordInfoList[row].size != 0)
                return wordInfoList[row][wordInfoList[row].size - 1].mIndex + 2// 加2 是补上换行符号‘\n’
        }
        return sum
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        if (gainFocus) {
            input.showSoftInput(this, InputMethodManager.RESULT_SHOWN)

            mCtrlScroll?.scrolTo(lineHeight * (cursorDrawPos[0]))
        } else {
            input.hideSoftInputFromWindow(windowToken, 0)
        }
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (!hasWindowFocus) {
            input.hideSoftInputFromWindow(windowToken, 0)
        }
    }


    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DEL) {
            if (cursor <= 0)
                return true

            textEdit.delete(cursor - 1, cursor)
            cursor--
            analysisCharacter()
            return true
        }

        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            textEdit.insert(cursor, "\n  ")
            cursor += 3
            analysisCharacter()
        }

        return super.onKeyUp(keyCode, event)
    }

    interface CtrlScroll {
        fun scrolTo(y: Int)
    }

    fun setScrollLinister(lin: CtrlScroll) {
        mCtrlScroll = lin
    }
}

