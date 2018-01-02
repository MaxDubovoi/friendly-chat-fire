/**
 * Copyright Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kotlin.ui.signin

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.arellomobile.mvp.MvpAppCompatActivity
import com.arellomobile.mvp.presenter.InjectPresenter
import com.arellomobile.mvp.presenter.PresenterType
import com.arellomobile.mvp.presenter.ProvidePresenter
import com.crashlytics.android.Crashlytics
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.codelab.friendlychat.R
import io.fabric.sdk.android.Fabric
import kotlin.chat_service.ChatService
import kotlin.chat_service.IChatService
import kotlin.firebase_repository.FirebaseRepo
import kotlin.firebase_repository.IFirebaseRepo
import kotlin.ui.main.MainActivity


class SignInActivity : MvpAppCompatActivity(), GoogleApiClient.OnConnectionFailedListener, View.OnClickListener, SignInContract.View, OnCompleteListener<AuthResult> {
    override fun onComplete(task: Task<AuthResult>) {
        Log.d(ContentValues.TAG, "signInWithCredential:onComplete:" + task.isSuccessful)

        // If sign in fails, display a message to the user. If sign in succeeds
        // the auth state listener will be notified and logic to handle the
        // signed in user can be handled in the listener.
        if (!task.isSuccessful) {
            Log.w(ContentValues.TAG, "signInWithCredential", task.exception)
            Toast.makeText(this, "Authentication failed.",
                    Toast.LENGTH_SHORT).show()
        } else {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    companion object {

        private val TAG = "SignInActivity"
        private val RC_SIGN_IN = 9001
    }

    private lateinit var mSignInButton: SignInButton
    private val mChatService: IChatService = ChatService(this)
    private val mFirebaseRepo: IFirebaseRepo = FirebaseRepo(this)
    // Firebase instance variables
    private var mFirebaseAuth: FirebaseAuth? = null
    @InjectPresenter(type = PresenterType.GLOBAL)
    lateinit var presenter: SignInPresenter

    @ProvidePresenter(type = PresenterType.GLOBAL)
    fun providePresenterWithConstructor(): SignInPresenter {
        return SignInPresenter(mChatService)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)
        Fabric.with(this, Crashlytics())


        // Initialize FirebaseAuth

        // Assign fields
        mSignInButton = findViewById(R.id.sign_in_button)

        // Set click listeners
        mSignInButton.setOnClickListener(this)

        getString(R.string.default_web_client_id)

        // Initialize FirebaseAuth
    }

    override fun onClick(v: View) {
        Log.i(TAG, "CLICK")
        when (v.id) {
            R.id.sign_in_button -> presenter.signIn(getString(R.string.default_web_client_id), this)
        }
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d(TAG, "onConnectionFailed:" + connectionResult)
        Toast.makeText(this, "Google Play Services error.", Toast.LENGTH_SHORT).show()
    }

    override fun activityResult(intent: Intent) {

        startActivityForResult(intent, RC_SIGN_IN)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if (result.isSuccess) {
                // Google Sign-In was successful, authenticate with Firebase
                val account = result.signInAccount
                mFirebaseRepo.firebaseAuthWithGoogle(account, this)
                startActivity(Intent(this@SignInActivity, MainActivity::class.java))
            } else {
                // Google Sign-In failed
                Log.e(TAG, "Google Sign-In failed.")
            }
        }
    }
}
