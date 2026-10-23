package com.company.domain.repository

import com.company.domain.model.editor.ExportRequest
import com.company.domain.model.editor.ExportStatus
import kotlinx.coroutines.flow.Flow

/** Encodes a project to a video file and saves the file to the gallery. */
interface VideoExportRepository {
    /**
     * Encodes the request and saves the video to the gallery.
     *
     * The flow sends [ExportStatus.Progress] while it runs and [ExportStatus.Saved] at the end.
     * Cancel the collection to stop the export. The flow throws an exception when the export fails.
     *
     * @param request What to encode.
     */
    fun export(request: ExportRequest): Flow<ExportStatus>
}
