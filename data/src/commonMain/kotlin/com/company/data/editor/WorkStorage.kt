package com.company.data.editor

/** Stores the text files of the works on the device (F15). The calls block, so call them on a background thread. */
internal interface WorkFileStore {
    /**
     * Reads a file.
     *
     * @param fileName Name of the file.
     * @return The text, or null when the file does not exist.
     */
    fun read(fileName: String): String?

    /**
     * Writes a file. A file with the same name is replaced. A half-written file never replaces the old file.
     *
     * @param fileName Name of the file.
     * @param text Content of the file.
     * @throws Exception When the file cannot be written, for example when the storage is full.
     */
    fun write(fileName: String, text: String)

    /**
     * Removes a file. Nothing happens when the file does not exist.
     *
     * @param fileName Name of the file.
     */
    fun delete(fileName: String)
}

/**
 * Keeps the access to the picked photos and videos after the app restarts, so that a saved work can open them again.
 * The calls block, so call them on a background thread.
 */
internal interface MediaPersister {
    /**
     * Makes the access to a source file last.
     *
     * @param uri Location of the source file.
     * @return The location to save in the work. It can differ from [uri], for example after a copy.
     */
    fun persist(uri: String): String

    /**
     * Gives the current location of a saved source file. The app folder can move when the app updates.
     *
     * @param uri Location that [persist] gave.
     * @return The location to open now.
     */
    fun restore(uri: String): String

    /**
     * Gives up the lasting access to a source file. Call it when no work uses the file.
     *
     * @param uri Location that [persist] gave.
     */
    fun release(uri: String)
}
