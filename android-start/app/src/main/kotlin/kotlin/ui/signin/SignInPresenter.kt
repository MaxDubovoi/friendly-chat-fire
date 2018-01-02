package kotlin.ui.signin

import android.util.Log
import com.arellomobile.mvp.InjectViewState
import com.arellomobile.mvp.MvpPresenter
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.common.api.GoogleApiClient
import kotlin.chat_service.IChatService

/**
 * Created by maxim on 12/19/2017.
 */
@InjectViewState
class SignInPresenter(private val chatService: IChatService) : SignInContract.Presenter, MvpPresenter<SignInContract.View>() {
    override fun signIn(idToken: String, connectionFailedListener: GoogleApiClient.OnConnectionFailedListener) {
        val googleApiClient = chatService.signInWithGoogle(idToken, connectionFailedListener)
        Log.e("Presenter", "SIGNIN")
        val signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient)
        viewState.activityResult(signInIntent)
    }


}