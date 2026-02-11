package com.markolukarami.copilotclone.domain.repositories

import com.markolukarami.copilotclone.domain.entities.patch.PatchPlan

interface PatchApplierRepository {
    fun apply(patch: PatchPlan)
}