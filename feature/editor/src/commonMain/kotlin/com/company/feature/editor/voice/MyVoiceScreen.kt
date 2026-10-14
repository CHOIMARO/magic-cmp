package com.company.feature.editor.voice

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.component.MagicButton
import com.company.core.designsystem.component.MagicButtonDefaults
import com.company.core.designsystem.component.MagicButtonStyle
import com.company.core.designsystem.component.MagicDialog
import com.company.core.designsystem.component.MagicIconButton
import com.company.core.designsystem.component.MagicSpinner
import com.company.core.designsystem.component.pressClickable
import com.company.core.designsystem.theme.MagicColors
import com.company.core.designsystem.theme.MagicPreview
import com.company.core.designsystem.theme.PHONE_PREVIEW_HEIGHT
import com.company.core.designsystem.theme.PHONE_PREVIEW_WIDTH
import com.company.core.designsystem.theme.oklch
import com.company.core.domain.model.editor.MyVoiceStatus
import com.company.ui.editor.formatTime
import kotlin.math.abs
import kotlin.math.sin

/** Consent sentence that the user reads. A vendor sentence replaces it when the vendor needs one. */
private const val CONSENT_LINE = "나는 이 목소리의 주인이며 magic이 이 목소리로 음성 합성 모델을 만드는 것에 동의합니다."

/** Sample sentence that the user reads. */
private const val SAMPLE_LINE = "오늘은 바닷가에서 하루를 보냈어요. 바람이 시원하고 하늘이 정말 맑았어요. 다음에는 친구들과 함께 오고 싶어요."

/** Red of the record button and the recording waveform. */
private val RecordRed = oklch(0.68f, 0.2f, 25f)

/** Green of a passed check item. */
private val CheckGreen = oklch(0.85f, 0.1f, 160f)

/** Warning color of a short recording. */
private val WarningColor = oklch(0.84f, 0.12f, 70f)

/** Number of bars of the recording waveform. */
private const val WAVE_BARS = 28

/**
 * Screen of the user's own voice (F06). It draws [state] and sends user actions to [onIntent].
 *
 * @param state Screen state.
 * @param onIntent Called with each user action.
 */
@Composable
internal fun MyVoiceScreen(state: MyVoiceState, onIntent: (MyVoiceIntent) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(MagicColors.Background).navigationBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MagicIconButton(icon = Icons.Rounded.Close, contentDescription = "닫기", onClick = { onIntent(MyVoiceIntent.Close) }, iconSize = 26.dp)
            Text(text = "내 목소리", modifier = Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            val stepLabel = when (state.step) {
                MyVoiceStep.CONSENT -> "1 / 2"
                MyVoiceStep.SAMPLE -> "2 / 2"
                else -> ""
            }
            Text(text = stepLabel, modifier = Modifier.padding(end = 10.dp), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MagicColors.OnSurface50)
        }
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            when (state.step) {
                MyVoiceStep.INTRO -> IntroStep(state, onIntent)
                MyVoiceStep.CONSENT, MyVoiceStep.SAMPLE -> RecordStep(state, onIntent)
                MyVoiceStep.CHECK -> CheckStep(state)
                MyVoiceStep.MAKING -> MakingStep(state)
                MyVoiceStep.DONE -> DoneStep()
                MyVoiceStep.MANAGE -> ManageStep(state, onIntent)
            }
        }
        BottomButtons(state, onIntent)
    }
    if (state.confirmDelete) {
        MagicDialog(
            title = "내 목소리를 지울까요?",
            message = "만든 목소리와 녹음 파일을 함께 지워요. 이미 넣은 더빙은 그대로 남아요",
            confirmLabel = "지우기",
            onConfirm = { onIntent(MyVoiceIntent.ConfirmDelete) },
            cancelLabel = "취소",
            onCancel = { onIntent(MyVoiceIntent.DismissDelete) },
            onDismissRequest = { onIntent(MyVoiceIntent.DismissDelete) },
        )
    }
}

@Composable
private fun ColumnScope.IntroStep(state: MyVoiceState, onIntent: (MyVoiceIntent) -> Unit) {
    Text(text = "내 목소리로\n더빙해요", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 34.sp)
    Text(
        text = "문장 두 개를 소리 내어 읽으면 내 목소리를 만들어요. 그다음부터는 직접 녹음하지 않아도 내 목소리로 더빙할 수 있어요.",
        fontSize = 14.sp,
        lineHeight = 22.sp,
        color = MagicColors.OnSurface.copy(alpha = 0.65f),
    )
    InfoCard(
        items = listOf(
            Icons.Outlined.Mic to "동의 문장 하나와 짧은 문장 하나를 소리 내어 읽어요",
            Icons.Outlined.VerifiedUser to "본인 목소리만 등록할 수 있어요. 다른 사람 목소리는 등록하면 안 돼요",
            Icons.Outlined.Lock to "녹음은 목소리를 만드는 데만 쓰고, 목소리 업체에 안전하게 보내요",
            Icons.Outlined.Delete to "마이 > 내 목소리에서 언제든 지울 수 있고, 지우면 녹음도 함께 지워요",
        ),
    )
    Row(
        modifier = Modifier.fillMaxWidth().pressClickable(pressedScale = 0.99f) { onIntent(MyVoiceIntent.ToggleAgree) }.padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(if (state.agreed) MagicColors.Accent else Color.Transparent)
                .border(2.dp, if (state.agreed) MagicColors.Accent else MagicColors.OnSurface40, RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (state.agreed) Icon(Icons.Rounded.Check, contentDescription = null, tint = MagicColors.OnAccent, modifier = Modifier.size(18.dp))
        }
        Text(text = "위 내용을 확인했고, 내 목소리로 녹음하는 것에 동의해요", fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, lineHeight = 21.sp)
    }
}

@Composable
private fun ColumnScope.RecordStep(state: MyVoiceState, onIntent: (MyVoiceIntent) -> Unit) {
    val sample = state.step == MyVoiceStep.SAMPLE
    Text(text = if (sample) "이 문장을 읽어 주세요" else "동의 문장을 그대로 읽어 주세요", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 30.sp)
    Text(
        text = if (sample) "평소 말투로 5초 이상 읽어 주세요. 조용한 곳이면 더 좋아요" else "내 목소리를 등록한다는 동의를 녹음해요",
        modifier = Modifier.padding(top = 0.dp),
        fontSize = 14.sp,
        lineHeight = 21.sp,
        color = MagicColors.OnSurface60,
    )
    Text(
        text = if (sample) SAMPLE_LINE else CONSENT_LINE,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(MagicColors.Surface).padding(horizontal = 18.dp, vertical = 20.dp),
        fontSize = 19.sp,
        lineHeight = 31.sp,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(modifier = Modifier.height(8.dp))
    RecordingWave(recording = state.recording, recorded = state.recordedSeconds > 0.0)
    val seconds = state.recordedSeconds
    Text(
        text = when {
            state.recording -> "녹음 중 ${formatTime(seconds)}"
            seconds > 0.0 && seconds >= state.requiredSeconds -> "${formatTime(seconds)} 녹음했어요"
            seconds > 0.0 -> "${state.requiredSeconds.toInt()}초 이상 읽어 주세요"
            else -> "버튼을 누르고 읽기 시작해요"
        },
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = when {
            state.recording -> oklch(0.8f, 0.14f, 25f)
            seconds > 0.0 && seconds < state.requiredSeconds -> WarningColor
            else -> MagicColors.OnSurface60
        },
    )
    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .pressClickable { onIntent(MyVoiceIntent.ToggleRecording) }
                .border(4.dp, MagicColors.OnSurface.copy(alpha = 0.9f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (state.recording) {
                Box(modifier = Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(RecordRed))
            } else {
                Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(RecordRed), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Mic, contentDescription = "녹음 시작", tint = Color.White, modifier = Modifier.size(30.dp))
                }
            }
        }
    }
}

/** Bars that move while the microphone records. */
@Composable
private fun RecordingWave(recording: Boolean, recorded: Boolean) {
    val clock = remember { mutableLongStateOf(0L) }
    LaunchedEffect(recording) {
        if (!recording) return@LaunchedEffect
        while (true) withFrameMillis { clock.longValue = it }
    }
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val time = clock.longValue.toFloat()
        repeat(WAVE_BARS) { index ->
            val height = when {
                recording -> 8f + 44f * abs(sin(time / 130f + index * 0.7f) * sin(time / 330f + index * 1.3f))
                recorded -> 6f + 10f * abs(sin(index * 1.7f))
                else -> 4f
            }
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(height.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        when {
                            recording -> oklch(0.75f, 0.17f, 25f)
                            recorded -> MagicColors.OnSurface50
                            else -> MagicColors.OnSurface.copy(alpha = 0.2f)
                        },
                    ),
            )
        }
    }
}

@Composable
private fun CheckStep(state: MyVoiceState) {
    val checking = state.checkedItems < CHECK_ITEMS
    Text(text = if (checking) "녹음을 확인하고 있어요" else "녹음이 잘 됐어요", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MagicColors.Surface).padding(horizontal = 16.dp, vertical = 4.dp)) {
        listOf("잡음이 적어요", "길이가 충분해요", "동의한 사람과 같은 목소리예요").forEachIndexed { index, text ->
            if (index > 0) Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MagicColors.Divider))
            Row(modifier = Modifier.padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (index < state.checkedItems) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = CheckGreen, modifier = Modifier.size(22.dp))
                } else {
                    MagicSpinner(color = MagicColors.OnSurface50, trackColor = MagicColors.SurfaceHighest, size = 20.dp)
                }
                Text(text = text, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun MakingStep(state: MyVoiceState) {
    val percent = when (val voice = state.voice) {
        is MyVoiceStatus.Making -> voice.percent
        MyVoiceStatus.Ready -> 100
        MyVoiceStatus.None -> 0
    }
    Text(text = "내 목소리를\n만들고 있어요", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 30.sp)
    Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = "$percent%", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
        Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(MagicColors.SurfaceHigh)) {
            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(percent / 100f).clip(RoundedCornerShape(4.dp)).background(MagicColors.Accent))
        }
        Text(text = "다른 화면으로 나가도 계속 만들어요. 다 되면 알려 드릴게요.", fontSize = 14.sp, lineHeight = 21.sp, color = MagicColors.OnSurface60)
    }
}

@Composable
private fun DoneStep() {
    Box(modifier = Modifier.padding(top = 12.dp).size(72.dp).clip(CircleShape).background(MagicColors.Accent), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.RecordVoiceOver, contentDescription = null, tint = MagicColors.OnAccent, modifier = Modifier.size(36.dp))
    }
    Text(text = "내 목소리가 준비됐어요", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
    Text(
        text = "더빙 목소리 목록 맨 앞에 '내 목소리'가 생겼어요. 미리 듣기로 확인해 보세요.",
        fontSize = 14.sp,
        lineHeight = 22.sp,
        color = MagicColors.OnSurface60,
    )
}

@Composable
private fun ManageStep(state: MyVoiceState, onIntent: (MyVoiceIntent) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(MagicColors.Surface).padding(horizontal = 16.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(52.dp).clip(CircleShape).background(MagicColors.Accent), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Person, contentDescription = null, tint = MagicColors.OnAccent, modifier = Modifier.size(28.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "내 목소리", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(text = "등록됨 · 계정마다 하나만 만들 수 있어요", modifier = Modifier.padding(top = 2.dp), fontSize = 12.5.sp, color = MagicColors.OnSurface55)
        }
        Box(
            modifier = Modifier.size(44.dp).pressClickable { onIntent(MyVoiceIntent.PlaySample) }.clip(CircleShape).background(MagicColors.SurfaceHighest),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (state.samplePlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                contentDescription = "미리 듣기",
                tint = if (state.samplePlaying) MagicColors.Accent else MagicColors.OnSurface,
                modifier = Modifier.size(26.dp),
            )
        }
    }
    Text(
        text = "지우면 만든 목소리와 녹음 파일도 함께 지워요. 이미 영상에 넣은 더빙은 그대로 남아요.",
        fontSize = 13.sp,
        lineHeight = 20.sp,
        color = MagicColors.OnSurface55,
    )
}

/** Card with an icon and a sentence on each row. */
@Composable
private fun InfoCard(items: List<Pair<ImageVector, String>>) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MagicColors.Surface).padding(horizontal = 16.dp, vertical = 4.dp)) {
        items.forEachIndexed { index, (icon, text) ->
            if (index > 0) Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MagicColors.Divider))
            Row(modifier = Modifier.padding(vertical = 13.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(icon, contentDescription = null, tint = MagicColors.AccentSoft, modifier = Modifier.size(22.dp))
                Text(text = text, fontSize = 14.sp, lineHeight = 21.sp, color = MagicColors.OnSurface.copy(alpha = 0.85f))
            }
        }
    }
}

/** Second button and main button at the bottom of the screen. */
@Composable
private fun BottomButtons(state: MyVoiceState, onIntent: (MyVoiceIntent) -> Unit) {
    val step = state.step
    val (label, enabled) = when (step) {
        MyVoiceStep.INTRO -> "시작하기" to state.agreed
        MyVoiceStep.CONSENT, MyVoiceStep.SAMPLE -> "다음" to state.recordedEnough
        MyVoiceStep.CHECK -> "내 목소리 만들기" to (state.checkedItems >= CHECK_ITEMS)
        MyVoiceStep.MAKING -> "닫기" to true
        MyVoiceStep.DONE -> "확인" to true
        MyVoiceStep.MANAGE -> "다시 만들기" to true
    }
    val secondary = when (step) {
        MyVoiceStep.MANAGE -> "삭제"
        MyVoiceStep.DONE -> if (state.samplePlaying) "정지" else "미리 듣기"
        MyVoiceStep.CONSENT, MyVoiceStep.SAMPLE -> "다시 녹음".takeIf { state.recordedSeconds > 0.0 && !state.recording }
        else -> null
    }
    Row(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (secondary != null) {
            MagicButton(
                text = secondary,
                onClick = { onIntent(MyVoiceIntent.Secondary) },
                style = MagicButtonStyle.SECONDARY,
                colors = if (step == MyVoiceStep.MANAGE) {
                    MagicButtonDefaults.colors(MagicButtonStyle.SECONDARY).copy(content = MagicColors.Danger, pressedContent = MagicColors.Danger)
                } else {
                    MagicButtonDefaults.colors(MagicButtonStyle.SECONDARY)
                },
                height = MagicButtonStyle.PRIMARY.height,
            )
        }
        MagicButton(text = label, onClick = { onIntent(MyVoiceIntent.Primary) }, modifier = Modifier.weight(1f), enabled = enabled)
    }
}

@Preview(name = "내 목소리 - 안내", widthDp = PHONE_PREVIEW_WIDTH, heightDp = PHONE_PREVIEW_HEIGHT)
@Composable
private fun MyVoiceIntroPreview() {
    MagicPreview { MyVoiceScreen(state = MyVoiceState(), onIntent = {}) }
}

@Preview(name = "내 목소리 - 녹음", widthDp = PHONE_PREVIEW_WIDTH, heightDp = PHONE_PREVIEW_HEIGHT)
@Composable
private fun MyVoiceRecordPreview() {
    MagicPreview { MyVoiceScreen(state = MyVoiceState(step = MyVoiceStep.SAMPLE, recordedSeconds = 3.2), onIntent = {}) }
}

@Preview(name = "내 목소리 - 관리", widthDp = PHONE_PREVIEW_WIDTH, heightDp = PHONE_PREVIEW_HEIGHT)
@Composable
private fun MyVoiceManagePreview() {
    MagicPreview { MyVoiceScreen(state = MyVoiceState(step = MyVoiceStep.MANAGE, voice = MyVoiceStatus.Ready), onIntent = {}) }
}
