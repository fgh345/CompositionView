package com.hkz.compositionview

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.ReplacementSpan

class CompositionSpan(private val with: Int, private val mTextPaint: Paint) : ReplacementSpan() {

    /**
     * 设置宽度，宽度=背景宽度+右边距
     */
    override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
        return with
    }

    /**
     * draw
     *
     * @param text   完整文本
     * @param start  setSpan里设置的start
     * @param end    setSpan里设置的start
     * @param x
     * @param top    当前span所在行的上方y
     * @param y      y其实就是metric里baseline的位置
     * @param bottom 当前span所在行的下方y(包含了行间距)，会和下一行的top重合
     * //     * @param paint  使用此span的画笔
     */
    override fun draw(canv: Canvas, text: CharSequence, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, p: Paint) {

        val metrics = mTextPaint.fontMetrics

        //把字画在背景中间
        mTextPaint.isAntiAlias = true
        mTextPaint.textAlign = Paint.Align.CENTER  //这个只针对x有效

        canv.drawText(text.subSequence(start, end).toString(), x + with / 2, y.toFloat() + (with - Math.abs(metrics.bottom - metrics.top)) / 2, mTextPaint)

    }


}