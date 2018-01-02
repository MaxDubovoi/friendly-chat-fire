/**
 * Copyright Google Inc. All Rights Reserved.
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kotlin.ui.main

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.*
import com.bumptech.glide.Glide
import com.crashlytics.android.Crashlytics
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.SnapshotParser
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.appinvite.AppInviteInvitation
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.appindexing.Action
import com.google.firebase.appindexing.FirebaseAppIndex
import com.google.firebase.appindexing.FirebaseUserActions
import com.google.firebase.appindexing.Indexable
import com.google.firebase.appindexing.builders.Indexables
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.codelab.friendlychat.R
import com.google.firebase.crash.FirebaseCrash
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import de.hdodenhof.circleimageview.CircleImageView
import io.fabric.sdk.android.Fabric
import java.util.*
import kotlin.firebase_repository.CodelabPreferences
import kotlin.firebase_repository.FriendlyMessage
import kotlin.ui.signin.SignInActivity

class MainActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener {
    private var mAdView: AdView? = null
    // Firebase instance variables
    private val mFirebaseAuth = FirebaseAuth.getInstance()
    private val mFirebaseUser = mFirebaseAuth.currentUser
    // Firebase instance variables
    private var mFirebaseDatabaseReference: DatabaseReference? = null
    private var mFirebaseAdapter: FirebaseRecyclerAdapter<FriendlyMessage, MessageViewHolder>? = null
    // Firebase instance variables
    private var mFirebaseRemoteConfig: FirebaseRemoteConfig? = null
    private var mFirebaseAnalytics: FirebaseAnalytics? = null
    private var mUsername: String? = null
    private var mPhotoUrl: String? = null
    private var mSharedPreferences: SharedPreferences? = null
    private var mGoogleApiClient: GoogleApiClient? = null
    private var mSendButton: Button? = null
    private var mMessageRecyclerView: RecyclerView? = null
    private var mLinearLayoutManager: LinearLayoutManager? = null
    private var mProgressBar: ProgressBar? = null
    private var mMessageEditText: EditText? = null
    private var mAddMessageImageView: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Fabric.with(this, Crashlytics())
        mAdView = findViewById<View>(R.id.adView) as AdView
        val adRequest = AdRequest.Builder().build()
        mAdView!!.loadAd(adRequest)
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        // Set default username is anonymous.
        mUsername = ANONYMOUS

        mGoogleApiClient = GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .build()

        // Initialize ProgressBar and RecyclerView.
        mProgressBar = findViewById<View>(R.id.progressBar) as ProgressBar
        mMessageRecyclerView = findViewById<View>(R.id.messageRecyclerView) as RecyclerView
        mLinearLayoutManager = LinearLayoutManager(this)
        mLinearLayoutManager!!.stackFromEnd = true
        mMessageRecyclerView!!.layoutManager = mLinearLayoutManager

        // New child entries
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().reference
        val parser = SnapshotParser<FriendlyMessage> { dataSnapshot ->
            val friendlyMessage = dataSnapshot.getValue(FriendlyMessage::class.java) as FriendlyMessage?
            if (friendlyMessage != null) {
                friendlyMessage!!.id = dataSnapshot.key
            }
            friendlyMessage
        }

        val messagesRef = mFirebaseDatabaseReference!!.child(MESSAGES_CHILD)
        val options = FirebaseRecyclerOptions.Builder<FriendlyMessage>()
                .setQuery(messagesRef, parser)
                .build()
        mFirebaseAdapter = object : FirebaseRecyclerAdapter<FriendlyMessage, MessageViewHolder>(options) {
            override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): MessageViewHolder {
                val inflater = LayoutInflater.from(viewGroup.context)
                return MessageViewHolder(inflater.inflate(R.layout.item_message, viewGroup, false))
            }

            override fun onBindViewHolder(viewHolder: MessageViewHolder,
                                          position: Int,
                                          friendlyMessage: FriendlyMessage) {
                mProgressBar!!.visibility = ProgressBar.INVISIBLE
                if (friendlyMessage.text != null) {
                    viewHolder.messageTextView.text = friendlyMessage.text
                    viewHolder.messageTextView.visibility = TextView.VISIBLE
                    viewHolder.messageImageView.visibility = ImageView.GONE
                } else {
                    val imageUrl = friendlyMessage.imageUrl
                    if (imageUrl.startsWith("gs://")) {
                        val storageReference = FirebaseStorage.getInstance()
                                .getReferenceFromUrl(imageUrl)
                        storageReference.downloadUrl.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val downloadUrl = task.result.toString()
                                Glide.with(viewHolder.messageImageView.context)
                                        .load(downloadUrl)
                                        .into(viewHolder.messageImageView)
                            } else {
                                Log.w(TAG, "Getting download url was not successful.",
                                        task.exception)
                            }
                        }
                    } else {
                        Glide.with(viewHolder.messageImageView.context)
                                .load(friendlyMessage.imageUrl)
                                .into(viewHolder.messageImageView)
                    }
                    viewHolder.messageImageView.visibility = ImageView.VISIBLE
                    viewHolder.messageTextView.visibility = TextView.GONE
                }


                viewHolder.messengerTextView.text = friendlyMessage.name
                if (friendlyMessage.photoUrl == null) {
                    viewHolder.messengerImageView.setImageDrawable(ContextCompat.getDrawable(this@MainActivity,
                            R.drawable.ic_account_circle_black_36dp))
                } else {
                    Glide.with(this@MainActivity)
                            .load(friendlyMessage.photoUrl)
                            .into(viewHolder.messengerImageView)
                }
                if (friendlyMessage.text != null) {
                    // write this message to the on-device index
                    FirebaseAppIndex.getInstance()
                            .update(getMessageIndexable(friendlyMessage))
                }
                if (friendlyMessage.text != null) {
                    // write this message to the on-device index
                    FirebaseAppIndex.getInstance()
                            .update(getMessageIndexable(friendlyMessage))
                }

                // log a view action on it
                FirebaseUserActions.getInstance().end(getMessageViewAction(friendlyMessage))
            }
        }

        mFirebaseAdapter!!.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                val friendlyMessageCount = mFirebaseAdapter!!.itemCount
                val lastVisiblePosition = mLinearLayoutManager!!.findLastCompletelyVisibleItemPosition()
                // If the recycler view is initially being loaded or the
                // user is at the bottom of the list, scroll to the bottom
                // of the list to show the newly added message.
                if ((lastVisiblePosition == -1 || ((positionStart >= (friendlyMessageCount - 1) && lastVisiblePosition == (positionStart - 1))))) {
                    mMessageRecyclerView!!.scrollToPosition(positionStart)
                }
            }
        })

        mMessageRecyclerView!!.adapter = mFirebaseAdapter

        mMessageEditText = findViewById<View>(R.id.messageEditText) as EditText
        mMessageEditText!!.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(mSharedPreferences!!
                .getInt(CodelabPreferences.FRIENDLY_MSG_LENGTH, DEFAULT_MSG_LENGTH_LIMIT)))
        mMessageEditText!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                mSendButton!!.isEnabled = charSequence.toString().trim { it <= ' ' }.isNotEmpty()
            }

            override fun afterTextChanged(editable: Editable) {}
        })

        mSendButton = findViewById<View>(R.id.sendButton) as Button
        mSendButton!!.setOnClickListener {
            val friendlyMessage = FriendlyMessage(mMessageEditText!!.text.toString(),
                    mUsername,
                    mPhotoUrl, null/* no image */)
            mFirebaseDatabaseReference!!.child(MESSAGES_CHILD)
                    .push().setValue(friendlyMessage)
            mMessageEditText!!.setText("")
        }

        mAddMessageImageView = findViewById<View>(R.id.addMessageImageView) as ImageView
        mAddMessageImageView!!.setOnClickListener {
            var intent: Intent? = null
            intent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                Intent(Intent.ACTION_OPEN_DOCUMENT)
            } else
                Intent(Intent.ACTION_GET_CONTENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "*/*"
            startActivityForResult(intent, REQUEST_IMAGE)
        }
        if (mFirebaseUser == null) {
            // Not signed in, launch the Sign In activity
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        } else {
            mUsername = mFirebaseUser.displayName
            if (mFirebaseUser.photoUrl != null) {
                mPhotoUrl = mFirebaseUser.photoUrl!!.toString()
            }
            // Initialize Firebase Remote Config.
            mFirebaseRemoteConfig = getInstance()

            // Define Firebase Remote Config Settings.
            val firebaseRemoteConfigSettings = FirebaseRemoteConfigSettings.Builder()
                    .setDeveloperModeEnabled(true)
                    .build()

            // Define default config values. Defaults are used when fetched config values are not
            // available. Eg: if an error occurred fetching values from the server.
            val defaultConfigMap = HashMap<String, Any>()
            defaultConfigMap.put("friendly_msg_length", 10L)

            // Apply config settings and default values.
            mFirebaseRemoteConfig!!.setConfigSettings(firebaseRemoteConfigSettings)
            mFirebaseRemoteConfig!!.setDefaults(defaultConfigMap)

            // Fetch remote config.
            fetchConfig()
        }
    }

    // Firebase instance variables

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in.
        // TODO: Add code to check if user is signed in.
    }

    public override fun onPause() {
        if (mAdView != null) {
            mAdView!!.pause()
        }
        mFirebaseAdapter!!.stopListening()
        super.onPause()
    }

    /**
     * Called when returning to the activity
     */
    public override fun onResume() {
        super.onResume()
        mFirebaseAdapter!!.startListening()
        if (mAdView != null) {
            mAdView!!.resume()
        }
    }

    /** Called before the activity is destroyed  */
    public override fun onDestroy() {
        if (mAdView != null) {
            mAdView!!.destroy()
        }
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View,
                                     menuInfo: ContextMenu.ContextMenuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        Log.d(TAG, "onConnectionFailed:" + connectionResult)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.crash_menu -> {
                FirebaseCrash.logcat(Log.ERROR, TAG, "crash caused")
                causeCrash()
                return true
            }
            R.id.invite_menu -> {
                sendInvitation()
                return true
            }
            R.id.fresh_config_menu -> {
                fetchConfig()
                return true
            }
            R.id.sign_out_menu -> {
                mFirebaseAuth.signOut()
                mUsername = ANONYMOUS
                startActivity(Intent(this, SignInActivity::class.java))
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult: requestCode=$requestCode, resultCode=$resultCode")

        if (requestCode == REQUEST_IMAGE) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    val uri = data!!.data
                    Log.d(TAG, "Uri: " + uri!!.toString())

                    val tempMessage = FriendlyMessage(null, mUsername, mPhotoUrl,
                            LOADING_IMAGE_URL)
                    mFirebaseDatabaseReference!!.child(MESSAGES_CHILD).push()
                            .setValue(tempMessage, object : DatabaseReference.CompletionListener {
                                override fun onComplete(databaseError: DatabaseError?,
                                                        databaseReference: DatabaseReference) {
                                    if (databaseError == null) {
                                        val key = databaseReference.key
                                        val storageReference = FirebaseStorage.getInstance()
                                                .getReference(mFirebaseUser!!.uid)
                                                .child(key)
                                                .child(uri!!.lastPathSegment)

                                        putImageInStorage(storageReference, uri, key)
                                    } else {
                                        Log.w(TAG, "Unable to write message to database.",
                                                databaseError!!.toException())
                                    }
                                }
                            })
                }
            }
        } else if (requestCode == REQUEST_INVITE) {
            if (resultCode == Activity.RESULT_OK) {
                // Check how many invitations were sent and log.
                val ids = AppInviteInvitation.getInvitationIds(resultCode, data!!)
                Log.d(TAG, "Invitations sent: " + ids.size)
            } else {
                // Sending failed or it was canceled, show failure message to the user
                Log.d(TAG, "Failed to send invitation.")
            }
        } else if (requestCode == REQUEST_INVITE) {
            if (resultCode == Activity.RESULT_OK) {
                val payload = Bundle()
                payload.putString(FirebaseAnalytics.Param.VALUE, "sent")
                mFirebaseAnalytics!!.logEvent(FirebaseAnalytics.Event.SHARE,
                        payload)
                // Check how many invitations were sent and log.
                val ids = AppInviteInvitation.getInvitationIds(resultCode,
                        data!!)
                Log.d(TAG, "Invitations sent: " + ids.size)
            } else {
                val payload = Bundle()
                payload.putString(FirebaseAnalytics.Param.VALUE, "not sent")
                mFirebaseAnalytics!!.logEvent(FirebaseAnalytics.Event.SHARE,
                        payload)
                // Sending failed or it was canceled, show failure message to
                // the user
                Log.d(TAG, "Failed to send invitation.")
            }
        }
    }

    private fun putImageInStorage(storageReference: StorageReference, uri: Uri?, key: String) {
        storageReference.putFile(uri!!).addOnCompleteListener(this@MainActivity,
                object : OnCompleteListener<UploadTask.TaskSnapshot> {
                    override fun onComplete(task: Task<UploadTask.TaskSnapshot>) {
                        if (task.isSuccessful) {
                            val friendlyMessage = FriendlyMessage(null, mUsername, mPhotoUrl,
                                    task.result.metadata!!.downloadUrl!!
                                            .toString())
                            mFirebaseDatabaseReference!!.child(MESSAGES_CHILD).child(key)
                                    .setValue(friendlyMessage)
                        } else {
                            Log.w(TAG, "Image upload task was not successful.",
                                    task.exception)
                        }
                    }
                })
    }

    private fun getMessageIndexable(friendlyMessage: FriendlyMessage): Indexable {
        val sender = Indexables.personBuilder()
                .setIsSelf(mUsername == friendlyMessage.name)
                .setName(friendlyMessage.name)
                .setUrl(MESSAGE_URL + (friendlyMessage.id + "/sender"))

        val recipient = Indexables.personBuilder()
                .setName(mUsername!!)
                .setUrl(MESSAGE_URL + (friendlyMessage.id + "/recipient"))

        return Indexables.messageBuilder()
                .setName(friendlyMessage.text)
                .setUrl(MESSAGE_URL + friendlyMessage.id)
                .setSender(sender)
                .setRecipient(recipient)
                .build()
    }

    private fun getMessageViewAction(friendlyMessage: FriendlyMessage): Action {
        return Action.Builder(Action.Builder.VIEW_ACTION)
                .setObject(friendlyMessage.name, MESSAGE_URL + friendlyMessage.id)
                .setMetadata(Action.Metadata.Builder().setUpload(false))
                .build()
    }

    // Fetch the config to determine the allowed length of messages.
    fun fetchConfig() {
        var cacheExpiration: Long = 3600 // 1 hour in seconds
        // If developer mode is enabled reduce cacheExpiration to 0 so that
        // each fetch goes to the server. This should not be used in release
        // builds.
        if (mFirebaseRemoteConfig!!.info.configSettings
                .isDeveloperModeEnabled) {
            cacheExpiration = 0
        }
        mFirebaseRemoteConfig!!.fetch(cacheExpiration)
                .addOnSuccessListener {
                    // Make the fetched config available via
                    // FirebaseRemoteConfig get<type> calls.
                    mFirebaseRemoteConfig!!.activateFetched()
                    applyRetrievedLengthLimit()
                }
                .addOnFailureListener { e ->
                    // There has been an error fetching the config
                    Log.w(TAG, ("Error fetching config: " + e.message))
                    applyRetrievedLengthLimit()
                }
    }

    /**
     * Apply retrieved length limit to edit text field.
     * This result may be fresh from the server or it may be from cached
     * values.
     */
    private fun applyRetrievedLengthLimit() {
        val friendly_msg_length = mFirebaseRemoteConfig!!.getLong("friendly_msg_length")
        mMessageEditText!!.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(friendly_msg_length.toInt()))
        Log.d(TAG, "FML is: " + friendly_msg_length)
    }

    private fun sendInvitation() {
        val intent = AppInviteInvitation.IntentBuilder(getString(R.string.invitation_title))
                .setMessage(getString(R.string.invitation_message))
                .setCallToActionText(getString(R.string.invitation_cta))
                .build()
        startActivityForResult(intent, REQUEST_INVITE)
    }

    private fun causeCrash() {
        throw NullPointerException("Fake null pointer exception")
    }

    class MessageViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        internal var messageTextView: TextView
        internal var messageImageView: ImageView
        internal var messengerTextView: TextView
        internal var messengerImageView: CircleImageView

        init {
            messageTextView = itemView.findViewById<View>(R.id.messageTextView) as TextView
            messageImageView = itemView.findViewById<View>(R.id.messageImageView) as ImageView
            messengerTextView = itemView.findViewById<View>(R.id.messengerTextView) as TextView
            messengerImageView = itemView.findViewById<View>(R.id.messengerImageView) as CircleImageView
        }
    }

    companion object {
        val MESSAGES_CHILD = "messages"
        val DEFAULT_MSG_LENGTH_LIMIT = 10
        val ANONYMOUS = "anonymous"
        // Initialize Firebase Auth
        private val TAG = "MainActivity"
        private val REQUEST_INVITE = 1
        private val REQUEST_IMAGE = 2
        private val LOADING_IMAGE_URL = "https://www.google.com/images/spin-32.gif"
        private val MESSAGE_SENT_EVENT = "message_sent"
        private val MESSAGE_URL = "http://friendlychat.firebase.google.com/message/"
    }
}
