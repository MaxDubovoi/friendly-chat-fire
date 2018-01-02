package kotlin.chat_service

import android.support.v4.app.FragmentActivity
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.GoogleApiClient

/**
 * Created by maxim on 12/20/2017.
 */
class ChatService(private val activityContext: FragmentActivity) : IChatService {
    // Configure Google Sign In
    override fun signInWithGoogle(idToken: String, connectionFailedListener: GoogleApiClient.OnConnectionFailedListener): GoogleApiClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(idToken)
                .requestEmail()
                .build()
        return GoogleApiClient.Builder(activityContext)
                .enableAutoManage(activityContext/* FragmentActivity */, connectionFailedListener /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build()
    }

    override fun signOut() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun sendMessage() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun uploadImage() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun changeSettings() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun inviteFriend() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}