package com.locksite.app.presentation.common

import com.locksite.app.R
import com.locksite.app.domain.model.ProtectionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ProtectionStatusTextTest {

    @Test
    fun `every status has a non-zero label and no two labels collide`() {
        val labels = ProtectionStatus.entries.map { ProtectionStatusText.label(it) }

        labels.forEach { assertNotEquals(0, it) }
        assertEquals(ProtectionStatus.entries.size, labels.toSet().size)
    }

    @Test
    fun `every status has a non-zero note and no two notes collide`() {
        val notes = ProtectionStatus.entries.map { ProtectionStatusText.note(it) }

        notes.forEach { assertNotEquals(0, it) }
        assertEquals(ProtectionStatus.entries.size, notes.toSet().size)
    }

    @Test
    fun `maps the six states the product promises`() {
        assertEquals(R.string.status_not_configured, ProtectionStatusText.label(ProtectionStatus.NOT_CONFIGURED))
        assertEquals(R.string.status_permission_pending, ProtectionStatusText.label(ProtectionStatus.PERMISSION_PENDING))
        assertEquals(R.string.status_active, ProtectionStatusText.label(ProtectionStatus.ACTIVE))
        assertEquals(R.string.status_error, ProtectionStatusText.label(ProtectionStatus.ERROR))
        assertEquals(R.string.status_stopped, ProtectionStatusText.label(ProtectionStatus.STOPPED))
        assertEquals(R.string.status_conflict, ProtectionStatusText.label(ProtectionStatus.CONFLICT_WITH_OTHER_VPN))
    }
}
