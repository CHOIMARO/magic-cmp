package com.company.feature.account.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.company.ui.navigation.onlyWhenResumed
import kotlinx.serialization.Serializable

/** Route of the account management screen (S15). */
@Serializable
object AccountRoute

/** Route of the withdrawal screen (S16). */
@Serializable
object WithdrawRoute

/**
 * Adds the account management screen to the graph.
 *
 * @param onClose Leaves the screen.
 * @param onOpenWithdraw Opens the withdrawal screen.
 */
fun NavGraphBuilder.accountScreen(onClose: () -> Unit, onOpenWithdraw: () -> Unit) {
    composable<AccountRoute> { entry ->
        com.company.feature.account.manage.AccountRoute(
            onClose = entry.onlyWhenResumed(onClose),
            onOpenWithdraw = entry.onlyWhenResumed(onOpenWithdraw),
        )
    }
}

/**
 * Adds the withdrawal screen to the graph.
 *
 * @param onClose Goes back to the account management screen.
 * @param onWithdrawn Goes to the home tab after the withdrawal.
 */
fun NavGraphBuilder.withdrawScreen(onClose: () -> Unit, onWithdrawn: () -> Unit) {
    composable<WithdrawRoute> { entry ->
        com.company.feature.account.withdraw.WithdrawRoute(
            onClose = entry.onlyWhenResumed(onClose),
            // 탈퇴 뒤의 이동은 사용자 입력이 아니므로 화면 상태와 관계없이 한다.
            onWithdrawn = onWithdrawn,
        )
    }
}
