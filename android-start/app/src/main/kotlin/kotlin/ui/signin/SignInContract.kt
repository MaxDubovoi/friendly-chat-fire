package kotlin.ui.signin

import android.content.Intent
import com.arellomobile.mvp.MvpView
import com.google.android.gms.common.api.GoogleApiClient

/**
 * Created by maxim on 12/19/2017.
 */
interface SignInContract {
    interface View : MvpView {
        fun activityResult(intent: Intent)

    }

    interface Presenter {
        fun signIn(idToken: String, connectionFailedListener: GoogleApiClient.OnConnectionFailedListener)
    }
}