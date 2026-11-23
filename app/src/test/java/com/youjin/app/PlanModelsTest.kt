package com.youjin.app

import com.youjin.app.domain.PlanQuadrant
import com.youjin.app.domain.planFraction
import com.youjin.app.domain.planValue
import com.youjin.app.domain.quadrantOf
import com.youjin.app.domain.valueFromDrag
import org.junit.Assert.assertEquals
import org.junit.Test

class PlanModelsTest {

    @Test
    fun quadrantOf_splitsAtSix() {
        assertEquals(PlanQuadrant.DO_NOW, quadrantOf(6, 6))
        assertEquals(PlanQuadrant.DO_NOW, quadrantOf(10, 9))
        assertEquals(PlanQuadrant.SCHEDULE, quadrantOf(6, 1))
        assertEquals(PlanQuadrant.DELEGATE, quadrantOf(1, 6))
        assertEquals(PlanQuadrant.DROP, quadrantOf(5, 5))
        assertEquals(PlanQuadrant.DROP, quadrantOf(1, 1))
    }

    @Test
    fun fraction_mapsOneToTenIntoZeroToOne() {
        assertEquals(0f, planFraction(1))
        assertEquals(1f, planFraction(10))
        assertEquals(1f, planFraction(99), 0f)
        assertEquals(0f, planFraction(-3))
    }

    @Test
    fun planValue_roundTripsAndClamps() {
        assertEquals(1, planValue(0f))
        assertEquals(10, planValue(1f))
        assertEquals(10, planValue(2f))
        assertEquals(1, planValue(-1f))
        assertEquals(5, planValue(planFraction(5)))
    }

    @Test
    fun valueFromDrag_movesAlongAxis() {
        assertEquals(1, valueFromDrag(5, -1000f, 200f))
        assertEquals(10, valueFromDrag(5, 1000f, 200f))
        assertEquals(5, valueFromDrag(5, 0f, 200f))
    }

    @Test
    fun valueFromDrag_withZeroAxisKeepsValue() {
        assertEquals(7, valueFromDrag(7, 30f, 0f))
    }
}
