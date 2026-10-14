package com.company.feature.editor.editor

import kotlin.test.Test
import kotlin.test.assertEquals

class StageFitTest {

    @Test
    fun fitStage_keepsStageThatEndsAboveBottom() {
        assertEquals(StageTransform.None, fitStage(stageTop = 100f, stageHeight = 400f, top = 90f, bottom = 500f, minScale = 0.3f))
    }

    @Test
    fun fitStage_movesStageUpWhenFreePartIsHighEnough() {
        val transform = fitStage(stageTop = 100f, stageHeight = 400f, top = 50f, bottom = 480f, minScale = 0.3f)

        assertEquals(1f, transform.scale, 1e-4f)
        assertEquals(-20f, transform.translationY, 1e-4f)
    }

    @Test
    fun fitStage_shrinksStageIntoFreePart() {
        val transform = fitStage(stageTop = 80f, stageHeight = 400f, top = 80f, bottom = 280f, minScale = 0.3f)

        assertEquals(0.5f, transform.scale, 1e-4f)
        // 줄인 무대(높이 200)의 가운데가 180에 오면 80~280을 채운다.
        assertEquals(-100f, transform.translationY, 1e-4f)
    }

    @Test
    fun fitStage_stopsAtMinScaleAndTouchesTop() {
        val transform = fitStage(stageTop = 80f, stageHeight = 400f, top = 80f, bottom = 120f, minScale = 0.3f)

        assertEquals(0.3f, transform.scale, 1e-4f)
        // 최소 크기(높이 120)에서는 위쪽 경계 80부터 그린다. 가운데는 140이다.
        assertEquals(-140f, transform.translationY, 1e-4f)
    }
}
