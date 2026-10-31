package com.company.feature.editor.editor

import com.company.core.base.Intent
import com.company.core.base.SideEffect
import com.company.core.base.State
import com.company.domain.model.editor.AudioTrack
import com.company.domain.model.editor.CaptionSegment
import com.company.domain.model.editor.CaptionStyleType
import com.company.domain.model.editor.Clip
import com.company.domain.model.editor.DEFAULT_SIZE_STEP
import com.company.domain.model.editor.DecorationRange
import com.company.domain.model.editor.Dubbing
import com.company.domain.model.editor.DubbingSpeed
import com.company.domain.model.editor.DubbingVoice
import com.company.domain.model.editor.EditorProject
import com.company.domain.model.editor.ElementClipboard
import com.company.domain.model.editor.LayoutType
import com.company.domain.model.editor.MusicRange
import com.company.domain.model.editor.MusicTrack
import com.company.domain.model.editor.MyVoiceStatus
import com.company.domain.model.editor.PasteTarget
import com.company.domain.model.editor.PushDirection
import com.company.domain.model.editor.SoundEffect
import com.company.domain.model.editor.SoundEffectCategory
import com.company.domain.model.editor.SoundEffectClip
import com.company.domain.model.editor.TextEntrance
import com.company.domain.model.editor.TextExit
import com.company.domain.model.editor.TextFontType
import com.company.domain.model.editor.TextOverlay
import com.company.domain.model.editor.TextStyleType
import com.company.domain.model.editor.TransitionSlot
import com.company.domain.model.editor.TransitionType
import com.company.domain.model.editor.placedCaptions
import com.company.ui.text.UiText

// =====================================================================
// Sheets and drags
// =====================================================================

/** Bottom sheet of the editor. */
enum class EditorSheet {
    /** LAYOUT : Screen layouts. */
    LAYOUT,

    /** TEXT : Text input, text styles, fonts, and decorations. It also changes one caption. */
    TEXT,

    /** MUSIC : Background music, the part of the track, the volume, and the fade. */
    MUSIC,

    /** SPEED : Play speed of the selected clip. */
    SPEED,

    /** CAPTIONS : Captions from speech or from a script, and caption styles. */
    CAPTIONS,

    /** ANIMATION : Entrance and exit animations of the selected text. */
    ANIMATION,

    /** PASTE : Targets of a paste of the copied element. */
    PASTE,

    /** SOUND_EFFECT : Sound effects by category. */
    SOUND_EFFECT,

    /** DUBBING : Text, voice, and speed of a dubbing. */
    DUBBING,

    /** TRANSITION : Transition at one clip boundary or at the video start. */
    TRANSITION,

    /** VOLUME : Volume of a clip, a dubbing, or a sound effect. */
    VOLUME,
}

/** Edge of a clip, a text overlay, or a caption that the user drags. */
enum class TrimEdge {
    /** START : Start of the item. */
    START,

    /** END : End of the item. */
    END,
}

/** Part of the timeline that the user drags. */
sealed interface TimelineDragTarget {
    /** ClipEdge(clipId, edge) : Trims one edge of a clip. The stage shows the frame at the edge. */
    data class ClipEdge(val clipId: String, val edge: TrimEdge) : TimelineDragTarget

    /** TextEdge(textId, edge) : Changes when a text overlay starts or ends. */
    data class TextEdge(val textId: String, val edge: TrimEdge) : TimelineDragTarget

    /** TextMove(textId) : Moves a text overlay in time and to another row. Its length stays. */
    data class TextMove(val textId: String) : TimelineDragTarget

    /** CaptionEdge(captionId, edge) : Changes when a caption starts or ends. The caption gets a fixed time. */
    data class CaptionEdge(val captionId: String, val edge: TrimEdge) : TimelineDragTarget

    /** AudioMove(track, id) : Moves a dubbing or a sound effect in time. Its length stays. */
    data class AudioMove(val track: AudioTrack, val id: String) : TimelineDragTarget
}

/** Selected bar of the caption, dubbing, or sound effect track. */
sealed interface TrackSelection {
    /** ID of the selected item. */
    val id: String

    /** Caption(id) : A caption. */
    data class Caption(override val id: String) : TrackSelection

    /** Dubbing(id) : A dubbing. */
    data class Dubbing(override val id: String) : TrackSelection

    /** SoundEffect(id) : A sound effect. */
    data class SoundEffect(override val id: String) : TrackSelection
}

// =====================================================================
// Sheet drafts
// =====================================================================

/** Tab of the text sheet. */
enum class TextSheetTab {
    /** TEMPLATE : Text styles. */
    TEMPLATE,

    /** FONT : Fonts. */
    FONT,

    /** DECORATION : Size, color, bold, outline, shadow, and background. */
    DECORATION,
}

/**
 * Text that the user types in the text sheet.
 *
 * @property id ID of the overlay to change, or null for a new overlay.
 * @property text Current input.
 * @property style Selected style.
 * @property font Selected font, or null for the font of [style].
 * @property sizeStep Size step from 1 to 5.
 * @property decorations Decorations of character ranges in [text].
 * @property selectionStart Start of the selected characters in the text field.
 * @property selectionEnd End of the selected characters. It is equal to [selectionStart] when nothing is selected.
 * @property captionId ID of the caption to change, or null. A caption draft has only the decoration tab.
 * @property tab Open tab.
 */
data class TextDraft(
    val id: String?,
    val text: String,
    val style: TextStyleType,
    val font: TextFontType? = null,
    val sizeStep: Int = DEFAULT_SIZE_STEP,
    val decorations: List<DecorationRange> = emptyList(),
    val selectionStart: Int = text.length,
    val selectionEnd: Int = text.length,
    val captionId: String? = null,
    val tab: TextSheetTab = if (captionId != null) TextSheetTab.DECORATION else TextSheetTab.TEMPLATE,
) {
    /** True when the draft changes a caption. */
    val isCaption: Boolean
        get() = captionId != null

    /** True when some characters are selected. */
    val hasSelection: Boolean
        get() = selectionEnd > selectionStart

    /** Range that a decoration changes: the selected characters, or all characters. */
    val decorationRange: IntRange
        get() = if (hasSelection) selectionStart until selectionEnd else text.indices
}

/** Change of one decoration property. */
sealed interface DecorationChange {
    /** Color(rgb) : Sets the text color as 0xRRGGBB, or removes it when null. */
    data class Color(val rgb: Int?) : DecorationChange

    /** Bold(on) : Turns bold on or off. */
    data class Bold(val on: Boolean) : DecorationChange

    /** Outline(on) : Turns the black outline on or off. */
    data class Outline(val on: Boolean) : DecorationChange

    /** Shadow(on) : Turns the soft shadow on or off. */
    data class Shadow(val on: Boolean) : DecorationChange

    /** Background(rgb) : Sets the background color as 0xRRGGBB, or removes it when null. */
    data class Background(val rgb: Int?) : DecorationChange
}

/**
 * Music that the user tries in the music sheet before the change.
 *
 * @property musicId Selected track, or null for no music.
 * @property range Part of the track to play.
 * @property fade True to fade the music in and out.
 * @property volume Volume from 0 to 100.
 * @property mood Mood filter of the list, or null for all moods.
 * @property previewing True while the selected part plays as a preview.
 * @property previewStart Counter that starts the preview again from the start of the part. It changes when the part moves.
 */
data class MusicDraft(
    val musicId: String?,
    val range: MusicRange?,
    val fade: Boolean,
    val volume: Int,
    val mood: String? = null,
    val previewing: Boolean = false,
    val previewStart: Int = 0,
)

/**
 * Dubbing that the user prepares in the dubbing sheet.
 *
 * @property text Text that the voice reads.
 * @property voiceId Selected voice.
 * @property speed Selected reading speed.
 * @property captionId Caption that the text came from, or null.
 * @property editId Dubbing to make again, or null for a new dubbing.
 * @property busy True while the voice makes the audio.
 */
data class DubbingDraft(
    val text: String,
    val voiceId: String,
    val speed: DubbingSpeed,
    val captionId: String?,
    val editId: String?,
    val busy: Boolean = false,
)

/** Tab of the animation sheet. */
enum class AnimationTab {
    /** ENTRANCE : Entrance animations ("등장"). */
    ENTRANCE,

    /** EXIT : Exit animations ("퇴장"). */
    EXIT,
}

/**
 * Item whose volume the volume sheet changes.
 *
 * @property kind Kind of the item.
 * @property id ID of the item.
 */
data class VolumeTarget(val kind: VolumeKind, val id: String)

/** Kind of an item with a volume. */
enum class VolumeKind {
    /** CLIP : The original sound of a video clip. */
    CLIP,

    /** DUBBING : A dubbing. */
    DUBBING,

    /** SOUND_EFFECT : A sound effect. */
    SOUND_EFFECT,
}

/**
 * Popup that asks the user before a change.
 *
 * @property title Title of the popup.
 * @property message Text under the title.
 * @property confirmLabel Label of the confirm button.
 * @property action Change to make when the user confirms.
 */
data class EditorDialog(val title: UiText, val message: UiText, val confirmLabel: UiText, val action: DialogAction)

/** Change that a popup asks about. */
sealed interface DialogAction {
    /** ReplaceWithSpeechCaptions : Removes the captions and makes new captions from the speech. */
    data object ReplaceWithSpeechCaptions : DialogAction

    /** InsertScript(checkExisting) : Puts the script lines into the clips. [checkExisting] asks first when captions exist. */
    data class InsertScript(val checkExisting: Boolean) : DialogAction

    /** PasteReplacingCaption(target) : Pastes the copied caption and removes the captions of the target clip. */
    data class PasteReplacingCaption(val target: PasteTarget) : DialogAction
}

// =====================================================================
// State
// =====================================================================

/**
 * State of the editor screen.
 *
 * @property project Project that the user edits.
 * @property canUndo True when the undo button can restore an earlier state.
 * @property time Position of the playhead in seconds.
 * @property playing True while the preview plays.
 * @property scrubbing True while the user drags the timeline. The time then shows tenths of a second.
 * @property selectedClipId ID of the selected clip, or null. Only one item is selected at a time.
 * @property selectedTextId ID of the text overlay selected on the timeline or on the stage, or null.
 * @property selectedTrackItem Selected caption, dubbing, or sound effect, or null.
 * @property activeDrag Part of the timeline that the user drags now, or null.
 * @property dragShift Timeline seconds that the timeline moves to the right during a timeline drag.
 *   The playhead cannot go before 0, so this shift moves the timeline for the rest. It is 0 at other times.
 * @property dragBlocked True while the moved text overlaps another text in its row. The text cannot stay there,
 *   so it goes back when the drag ends.
 * @property sheet Open bottom sheet, or null.
 * @property textDraft Input of the text sheet, or null.
 * @property fontDownloading Font that downloads now, or null.
 * @property recentColors Colors that the user picked with the color picker, newest first.
 * @property captionProgress Progress of the caption generation from 0 to 100, or null when it does not run.
 * @property captionScript Script of the caption sheet, or null when the sheet does not show the script field.
 * @property musicTracks Music tracks that the user can pick.
 * @property musicDraft Music that the user tries in the music sheet, or null.
 * @property voices Voices for dubbings. The user's own voice is not in the list.
 * @property myVoice State of the user's own voice.
 * @property dubbingDraft Dubbing that the user prepares, or null.
 * @property soundEffects Sound effects of the catalog.
 * @property soundEffectCategory Category of the effect list, or null for the recent effects.
 * @property recentSoundEffects IDs of the effects that the user added, newest first.
 * @property volumeTarget Item of the volume sheet, or null.
 * @property transitionSlot Place of the transition sheet, or null.
 * @property animationTab Open tab of the animation sheet.
 * @property animationReplay Counter that plays the animation of the selected text once on the stage. It changes
 *   when the user picks an animation.
 * @property clipboard Copied text or caption, or null.
 * @property dialog Open popup, or null.
 * @property leaveDialog True while the popup that asks before the editor closes shows.
 * @property watermark True to show the "magic" watermark on the stage.
 */
data class EditorState(
    val project: EditorProject = EditorProject(name = ""),
    val canUndo: Boolean = false,
    val time: Double = 0.0,
    val playing: Boolean = false,
    val scrubbing: Boolean = false,
    val selectedClipId: String? = null,
    val selectedTextId: String? = null,
    val selectedTrackItem: TrackSelection? = null,
    val activeDrag: TimelineDragTarget? = null,
    val dragShift: Double = 0.0,
    val dragBlocked: Boolean = false,
    val sheet: EditorSheet? = null,
    val textDraft: TextDraft? = null,
    val fontDownloading: TextFontType? = null,
    val recentColors: List<Int> = emptyList(),
    val captionProgress: Int? = null,
    val captionScript: String? = null,
    val musicTracks: List<MusicTrack> = emptyList(),
    val musicDraft: MusicDraft? = null,
    val voices: List<DubbingVoice> = emptyList(),
    val myVoice: MyVoiceStatus = MyVoiceStatus.None,
    val dubbingDraft: DubbingDraft? = null,
    val soundEffects: List<SoundEffect> = emptyList(),
    val soundEffectCategory: SoundEffectCategory? = SoundEffectCategory.HIGHLIGHT,
    val recentSoundEffects: List<String> = emptyList(),
    val volumeTarget: VolumeTarget? = null,
    val transitionSlot: TransitionSlot? = null,
    val animationTab: AnimationTab = AnimationTab.ENTRANCE,
    val animationReplay: Int = 0,
    val clipboard: ElementClipboard? = null,
    val dialog: EditorDialog? = null,
    val leaveDialog: Boolean = false,
    val watermark: Boolean = false,
) : State() {
    /** Length of the project in seconds. */
    val totalDuration: Double
        get() = project.totalDuration

    /** Selected clip, or null. */
    val selectedClip: Clip?
        get() = project.clips.find { it.id == selectedClipId }

    /** Selected text overlay, or null. */
    val selectedText: TextOverlay?
        get() = project.texts.find { it.id == selectedTextId }

    /** Selected caption with its time on the timeline, or null. */
    val selectedCaption: CaptionSegment?
        get() = (selectedTrackItem as? TrackSelection.Caption)?.let { selection -> project.placedCaptions.find { it.id == selection.id } }

    /** Selected dubbing, or null. */
    val selectedDubbing: Dubbing?
        get() = (selectedTrackItem as? TrackSelection.Dubbing)?.let { selection -> project.dubbings.find { it.id == selection.id } }

    /** Selected sound effect, or null. */
    val selectedSoundEffect: SoundEffectClip?
        get() = (selectedTrackItem as? TrackSelection.SoundEffect)?.let { selection -> project.soundEffects.find { it.id == selection.id } }
}

// =====================================================================
// Intents
// =====================================================================

/** User actions on the editor screen. */
sealed class EditorIntent : Intent() {
    // ----- Playback -----

    /** TogglePlay : Starts or pauses the preview. */
    data object TogglePlay : EditorIntent()

    /** Pause : Pauses the preview. */
    data object Pause : EditorIntent()

    /** Scrub(time) : Moves the playhead. Ignored while the preview plays or while a timeline drag runs. */
    data class Scrub(val time: Double) : EditorIntent()

    /** ScrubEnded : The user stopped to drag the timeline. The time shows whole seconds again. */
    data object ScrubEnded : EditorIntent()

    /** PlaybackProgress(time) : Moves the playhead to the position of the playing preview. Ignored when the preview does not play. */
    data class PlaybackProgress(val time: Double) : EditorIntent()

    /** PlaybackEnded : Stops the preview at the end of the project. */
    data object PlaybackEnded : EditorIntent()

    // ----- Selection and timeline drags -----

    /** SelectClip(clipId) : Selects the clip, or clears the selection if the clip is selected. */
    data class SelectClip(val clipId: String) : EditorIntent()

    /** SelectTrackItem(item) : Selects a caption, dubbing, or sound effect bar, or clears the selection if it is selected. */
    data class SelectTrackItem(val item: TrackSelection) : EditorIntent()

    /** ClearSelection : Clears the selection and the clipboard. It is the "완료" button. */
    data object ClearSelection : EditorIntent()

    /**
     * StartTimelineDrag(target, snapSeconds) : Starts a drag on the timeline, and keeps an undo step. A text drag also
     * selects the text. Dragged edges snap to the playhead and to other items within [snapSeconds].
     */
    data class StartTimelineDrag(val target: TimelineDragTarget, val snapSeconds: Double) : EditorIntent()

    /**
     * MoveTimelineDrag(delta, rowDelta) : The finger moved [delta] timeline seconds and [rowDelta] text rows from the
     * drag start. A positive [rowDelta] is downward. Only a text move uses [rowDelta]. The dragged part follows the
     * finger. During a clip trim, the other edge of the clip stays in place on the screen.
     */
    data class MoveTimelineDrag(val delta: Double, val rowDelta: Int = 0) : EditorIntent()

    /**
     * ScrollTimelineDrag(seconds) : Scrolls the timeline by [seconds] during a timeline drag, because the finger is at
     * the edge of the timeline. The dragged part moves with the timeline. A negative value scrolls toward the project start.
     */
    data class ScrollTimelineDrag(val seconds: Double) : EditorIntent()

    /**
     * EndTimelineDrag : Ends the timeline drag. The timeline moves back from [EditorState.dragShift].
     * A moved text that overlaps another text goes back to its place before the drag.
     */
    data object EndTimelineDrag : EditorIntent()

    // ----- Clips -----

    /** SplitClip : Splits the clip at the playhead. */
    data object SplitClip : EditorIntent()

    /** DuplicateClip : Puts a copy of the selected clip after it. */
    data object DuplicateClip : EditorIntent()

    /** RemoveClip : Removes the selected clip. */
    data object RemoveClip : EditorIntent()

    /** AddClips : Opens the video picker to add clips. */
    data object AddClips : EditorIntent()

    // ----- Texts on the stage and on the timeline -----

    /** OpenTextEditor(textId) : Opens the text sheet for an overlay, or for a new overlay if null. */
    data class OpenTextEditor(val textId: String?) : EditorIntent()

    /** SelectText(textId) : Selects the text overlay, or clears the selection if it is selected. */
    data class SelectText(val textId: String) : EditorIntent()

    /** DuplicateText : Puts a copy of the selected text overlay right after it in time. */
    data object DuplicateText : EditorIntent()

    /** RemoveText : Removes the selected text overlay. */
    data object RemoveText : EditorIntent()

    /** StartTextDrag : Keeps an undo step before the first text move. */
    data object StartTextDrag : EditorIntent()

    /** MoveText(textId, x, y) : Moves a text overlay to a percent position. */
    data class MoveText(val textId: String, val x: Double, val y: Double) : EditorIntent()

    /** StartTextTransform : Pauses the preview and keeps an undo step before the first change of a size and angle gesture. */
    data object StartTextTransform : EditorIntent()

    /**
     * TransformText(textId, scale, rotation) : Changes the size factor and the angle in degrees of a text overlay.
     * The whole text box changes. A positive angle turns it clockwise.
     */
    data class TransformText(val textId: String, val scale: Double, val rotation: Double) : EditorIntent()

    // ----- Text sheet -----

    /** ChangeDraftText(text, selectionStart, selectionEnd) : Changes the input and the selected characters. Decorations move with the text. */
    data class ChangeDraftText(val text: String, val selectionStart: Int, val selectionEnd: Int) : EditorIntent()

    /** SelectTextTab(tab) : Opens a tab of the text sheet. */
    data class SelectTextTab(val tab: TextSheetTab) : EditorIntent()

    /** ChangeDraftStyle(style) : Changes the style in the text sheet. */
    data class ChangeDraftStyle(val style: TextStyleType) : EditorIntent()

    /** ChangeDraftFont(font) : Changes the font. A font that is not on the device downloads first. */
    data class ChangeDraftFont(val font: TextFontType) : EditorIntent()

    /** ChangeDraftSize(step) : Changes the size step from 1 to 5. */
    data class ChangeDraftSize(val step: Int) : EditorIntent()

    /** DecorateDraft(change) : Changes one decoration of the selected characters, or of all characters. */
    data class DecorateDraft(val change: DecorationChange) : EditorIntent()

    /** PickCustomColor(rgb) : Sets a color from the color picker and keeps it in the recent colors. */
    data class PickCustomColor(val rgb: Int) : EditorIntent()

    /** ResetDraftDecorations : Removes all decorations of the draft. */
    data object ResetDraftDecorations : EditorIntent()

    /** SaveText : Adds or changes the overlay or the caption from the text sheet. */
    data object SaveText : EditorIntent()

    /** DeleteText : Removes the overlay or the caption of the text sheet. */
    data object DeleteText : EditorIntent()

    // ----- Animations -----

    /** OpenAnimationSheet : Opens the animation sheet of the selected text. */
    data object OpenAnimationSheet : EditorIntent()

    /** SelectAnimationTab(tab) : Opens the entrance or the exit tab. */
    data class SelectAnimationTab(val tab: AnimationTab) : EditorIntent()

    /** PickEntrance(entrance) : Sets the entrance of the selected text and plays it once on the stage. */
    data class PickEntrance(val entrance: TextEntrance) : EditorIntent()

    /** PickExit(exit) : Sets the exit of the selected text and plays it once on the stage. */
    data class PickExit(val exit: TextExit) : EditorIntent()

    /** ChangeAnimationLength(millis) : Sets the length of the animation of the open tab. A quick series of changes is one undo step. */
    data class ChangeAnimationLength(val millis: Int) : EditorIntent()

    /** ResetAnimation : The selected text uses the animations of its style again. */
    data object ResetAnimation : EditorIntent()

    // ----- Sheets -----

    /** OpenSheet(sheet) : Opens a tool sheet and pauses the preview. */
    data class OpenSheet(val sheet: EditorSheet) : EditorIntent()

    /** CloseSheet : Closes the sheet and stops the caption generation. */
    data object CloseSheet : EditorIntent()

    /** PickLayout(layout) : Applies the layout. */
    data class PickLayout(val layout: LayoutType) : EditorIntent()

    /** PickSpeed(speed) : Sets the play speed of the selected clip. */
    data class PickSpeed(val speed: Double) : EditorIntent()

    // ----- Captions -----

    /** PickCaptionStyle(style) : Applies the caption style. */
    data class PickCaptionStyle(val style: CaptionStyleType) : EditorIntent()

    /** GenerateCaptions : Makes captions from the speech. When captions exist, a popup asks first. */
    data object GenerateCaptions : EditorIntent()

    /** RemoveCaptions : Removes all captions. */
    data object RemoveCaptions : EditorIntent()

    /** OpenCaptionScript : Shows the script field of the caption sheet. */
    data object OpenCaptionScript : EditorIntent()

    /** CloseCaptionScript : Goes back from the script field to the caption sheet. */
    data object CloseCaptionScript : EditorIntent()

    /** ChangeCaptionScript(script) : Changes the script. Each line is one caption. */
    data class ChangeCaptionScript(val script: String) : EditorIntent()

    /** InsertCaptionScript : Puts the script lines into the clips. A popup asks first when lines are left over or captions exist. */
    data object InsertCaptionScript : EditorIntent()

    /** AddCaptionAtPlayhead : Adds a caption at the playhead and opens it in the text sheet. */
    data object AddCaptionAtPlayhead : EditorIntent()

    /** OpenCaptionEditor(captionId) : Opens the text sheet for one caption. */
    data class OpenCaptionEditor(val captionId: String) : EditorIntent()

    /** SplitCaption : Splits the selected caption at the playhead. */
    data object SplitCaption : EditorIntent()

    // ----- Music -----

    /** PickMusicMood(mood) : Filters the music list by mood. Null shows all moods. */
    data class PickMusicMood(val mood: String?) : EditorIntent()

    /** TapMusicTrack(musicId) : Selects a track and plays its preview. A tap on the selected track stops or starts the preview. */
    data class TapMusicTrack(val musicId: String?) : EditorIntent()

    /** MoveMusicRange(origin, delta, moveStart, moveEnd) : Moves the edges of the music part from [origin] by [delta] track seconds. */
    data class MoveMusicRange(val origin: MusicRange, val delta: Double, val moveStart: Boolean, val moveEnd: Boolean) : EditorIntent()

    /** ChangeMusicVolume(volume) : Changes the volume in the music sheet. */
    data class ChangeMusicVolume(val volume: Int) : EditorIntent()

    /** ToggleMusicFade : Turns the fade in and out of the music on or off. */
    data object ToggleMusicFade : EditorIntent()

    /** ApplyMusic : Applies the music of the sheet, or removes the music. */
    data object ApplyMusic : EditorIntent()

    // ----- Dubbing -----

    /** OpenDubbing(captionId, editId) : Opens the dubbing sheet. [captionId] fills the text of that caption. [editId] makes that dubbing again. */
    data class OpenDubbing(val captionId: String? = null, val editId: String? = null) : EditorIntent()

    /** ChangeDubbingText(text) : Changes the text of the dubbing, 200 characters at most. */
    data class ChangeDubbingText(val text: String) : EditorIntent()

    /** PickDubbingVoice(voiceId) : Selects a voice. */
    data class PickDubbingVoice(val voiceId: String) : EditorIntent()

    /** PickDubbingSpeed(speed) : Selects a reading speed. */
    data class PickDubbingSpeed(val speed: DubbingSpeed) : EditorIntent()

    /** InsertDubbing : Makes the audio and adds or replaces the dubbing. */
    data object InsertDubbing : EditorIntent()

    /** FitDubbing : Plays the selected dubbing faster so that it ends with its caption. */
    data object FitDubbing : EditorIntent()

    /** OpenMyVoice : Opens the screen of the user's own voice. */
    data object OpenMyVoice : EditorIntent()

    // ----- Sound effects and volume -----

    /** PickSoundEffectCategory(category) : Shows the effects of a category. Null shows the recent effects. */
    data class PickSoundEffectCategory(val category: SoundEffectCategory?) : EditorIntent()

    /** InsertSoundEffect(effectId) : Adds the effect at the playhead and closes the sheet. */
    data class InsertSoundEffect(val effectId: String) : EditorIntent()

    /** OpenVolume(target) : Opens the volume sheet of an item. */
    data class OpenVolume(val target: VolumeTarget) : EditorIntent()

    /** ChangeVolume(volume) : Changes the volume of the item of the volume sheet. A quick series of changes is one undo step. */
    data class ChangeVolume(val volume: Int) : EditorIntent()

    /** DuplicateTrackItem : Puts a copy of the selected dubbing or sound effect after it. */
    data object DuplicateTrackItem : EditorIntent()

    /** RemoveTrackItem : Removes the selected caption, dubbing, or sound effect. */
    data object RemoveTrackItem : EditorIntent()

    // ----- Transitions -----

    /** OpenTransition(slot) : Opens the transition sheet of a slot. The playhead moves to the boundary. */
    data class OpenTransition(val slot: TransitionSlot) : EditorIntent()

    /** OpenNearestTransition : Opens the transition sheet of the boundary nearest to the playhead. */
    data object OpenNearestTransition : EditorIntent()

    /** PickTransition(type) : Sets the transition of the open slot. Null removes it. */
    data class PickTransition(val type: TransitionType?) : EditorIntent()

    /** PickPushDirection(direction) : Sets the side of the push transition of the open slot. */
    data class PickPushDirection(val direction: PushDirection) : EditorIntent()

    /** ApplyTransitionToAll : Sets the transition of the open slot at all clip boundaries. */
    data object ApplyTransitionToAll : EditorIntent()

    // ----- Copy and paste -----

    /** CopySelected : Copies the selected text or caption. */
    data object CopySelected : EditorIntent()

    /** OpenPaste : Opens the paste sheet. */
    data object OpenPaste : EditorIntent()

    /** Paste(target) : Pastes the copied element into the target clips. */
    data class Paste(val target: PasteTarget) : EditorIntent()

    // ----- Popups -----

    /** ConfirmDialog : Makes the change of the open popup. */
    data object ConfirmDialog : EditorIntent()

    /** DismissDialog : Closes the popup without a change. */
    data object DismissDialog : EditorIntent()

    // ----- Project -----

    /** Undo : Restores the last undo step. */
    data object Undo : EditorIntent()

    /** Export : Opens the export screen. The draft of a new project gets the length and the first clip. */
    data object Export : EditorIntent()

    /** Back : Asks the user before the editor closes. The back button and the system back action send it. */
    data object Back : EditorIntent()

    /** ConfirmLeave : Closes the editor. The user confirmed the leave popup. The draft of a new project gets the length and the first clip. */
    data object ConfirmLeave : EditorIntent()

    /** DismissLeave : Closes the leave popup. The user stays in the editor. */
    data object DismissLeave : EditorIntent()
}

/** One-time events of the editor screen. */
sealed class EditorSideEffect : SideEffect() {
    /** ShowToast(message) : Shows a short message. */
    data class ShowToast(val message: UiText) : EditorSideEffect()

    /** OpenGalleryToAppend : Navigates to the video picker in append mode. */
    data object OpenGalleryToAppend : EditorSideEffect()

    /** OpenExport : Navigates to the export screen. */
    data object OpenExport : EditorSideEffect()

    /** OpenMyVoice : Navigates to the screen of the user's own voice. */
    data object OpenMyVoice : EditorSideEffect()

    /** Close : Leaves the editor. */
    data object Close : EditorSideEffect()

    /** Snapped : The dragged part snapped to the playhead or to another item. The screen gives a short vibration. */
    data object Snapped : EditorSideEffect()
}
