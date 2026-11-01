package com.company.data.editor

/**
 * Stores the text files of the works on the device (F15). The device backup does not keep them (D51).
 * The calls block, so call them on a background thread.
 */
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

    /** Names of all the files in the store, also the files that a write did not finish. */
    fun list(): List<String>
}

/**
 * Copies the picked photos and videos into the app storage, so that a saved work can open them after an app restart
 * and after the user removed the original (D50). The device backup does not keep the copies (D51).
 * The calls block, so call them on a background thread.
 */
internal interface MediaPersister {
    /**
     * Copies a source file into the app storage. A copy can take a long time for a large video.
     *
     * @param uri Location of the source file.
     * @return The location of the copy. When the file needs no copy or the copy fails, [uri].
     */
    fun persist(uri: String): String

    /**
     * Gives the current location of a copy. The app folder can move when the app updates.
     *
     * @param uri Location that [persist] gave.
     * @return The location to open now.
     */
    fun restore(uri: String): String

    /**
     * Tells if a location is a copy that [persist] made. A work from an older app version can still point to a
     * source file.
     *
     * @param uri Location to check.
     */
    fun isCopy(uri: String): Boolean

    /**
     * Removes a copy. Call it when no work uses the copy. Nothing happens for a file that is not a copy.
     *
     * @param uri Location that [persist] gave.
     */
    fun release(uri: String)

    /**
     * Tells the size of a file.
     *
     * @param uri Location of a source file or of a copy.
     * @return The size in bytes, or 0 when it is not known.
     */
    fun sizeOf(uri: String): Long

    /** Tells the free bytes for new copies, or [Long.MAX_VALUE] when the device does not tell them. */
    fun freeBytes(): Long

    /**
     * Removes all the copies that are not in [keep], also the copies that did not finish.
     *
     * @param keep Locations of the copies that the works use.
     */
    fun cleanUp(keep: Set<String>)
}
