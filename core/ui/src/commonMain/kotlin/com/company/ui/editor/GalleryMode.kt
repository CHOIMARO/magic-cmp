package com.company.ui.editor

/** Purpose of the video picker. */
enum class GalleryMode {
    /** Picked videos start a new project with automatic decoration. */
    MAGIC,

    /** Picked videos start a new project without decoration. */
    MANUAL,

    /** Picked videos go to the end of the current project. */
    APPEND,
}
