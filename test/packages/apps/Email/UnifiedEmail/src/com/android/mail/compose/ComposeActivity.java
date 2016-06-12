/**
 * Copyright (c) 2011, Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.mail.compose;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.ActivityInfo;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;
import android.support.v4.app.RemoteInput;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.text.Editable;
import android.text.Html;
import android.text.SpanWatcher;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.util.Rfc822Token;
import android.text.util.Rfc822Tokenizer;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.common.Rfc822Validator;
import com.android.common.contacts.DataUsageStatUpdater;
import com.android.emailcommon.internet.MimeUtility;
import com.android.emailcommon.mail.Address;
import com.android.emailcommon.utility.DataCollectUtils;
import com.android.emailcommon.utility.Utility;
import com.android.mail.MailIntentService;
import com.android.mail.R;
import com.android.mail.analytics.Analytics;
import com.android.mail.browse.MessageHeaderView;
import com.android.mail.compose.AttachmentsView.AttachmentAddedOrDeletedListener;
import com.android.mail.compose.AttachmentsView.AttachmentFailureException;
import com.android.mail.compose.ComposeActivity.SendOrSaveMessage;
import com.android.mail.compose.FromAddressSpinner.OnAccountChangedListener;
import com.android.mail.compose.QuotedTextView.RespondInlineListener;
import com.android.mail.preferences.MailPrefs;
import com.android.mail.providers.Account;
import com.android.mail.providers.Attachment;
import com.android.mail.providers.Folder;
import com.android.mail.providers.MailAppProvider;
import com.android.mail.providers.Message;
import com.android.mail.providers.MessageModification;
import com.android.mail.providers.ReplyFromAccount;
import com.android.mail.providers.Settings;
import com.android.mail.providers.UIProvider;
import com.android.mail.providers.UIProvider.AccountCapabilities;
import com.android.mail.providers.UIProvider.AttachmentState;
import com.android.mail.providers.UIProvider.DraftType;
import com.android.mail.ui.AttachmentTile.AttachmentPreview;
import com.android.mail.ui.MailActivity;
import com.android.mail.ui.WaitFragment;
import com.android.mail.utils.AccountUtils;
import com.android.mail.utils.AttachmentUtils;
import com.android.mail.utils.ContentProviderTask;
import com.android.mail.utils.HtmlUtils;
import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;
import com.android.mail.utils.NotificationActionUtils;
import com.android.mail.utils.StorageLowState;
import com.android.mail.utils.Utils;
import com.android.mail.utils.ViewUtils;
import com.android.mtkex.chips.BaseRecipientAdapter;
import com.google.android.mail.common.html.parser.HtmlTree;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mediatek.emailcommon.EmailFeatureOptions;
import com.mediatek.mail.ui.utils.ChipsAddressTextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import com.android.email.Preferences;

//add DWYBL-743 zhaobeidou 20150409 (start)
import java.util.regex.Matcher;
import java.util.regex.Pattern;
//add DWYBL-743 zhaobeidou 20150409 (end)

/// M: Replace with ChipsAddressTextView
public class ComposeActivity extends ActionBarActivity
        implements OnClickListener, ActionBar.OnNavigationListener,
        RespondInlineListener, TextWatcher,
        AttachmentAddedOrDeletedListener, OnAccountChangedListener,
        LoaderManager.LoaderCallbacks<Cursor>, TextView.OnEditorActionListener,
        /*RecipientEditTextView.RecipientEntryItemClickedListener,*/ View.OnFocusChangeListener {
    /**
     * An {@link Intent} action that launches {@link ComposeActivity}, but is handled as if the
     * {@link Activity} were launched with no special action.
     */
    private static final String ACTION_LAUNCH_COMPOSE =
            "com.android.mail.intent.action.LAUNCH_COMPOSE";

    // Identifiers for which type of composition this is
    public static final int COMPOSE = -1;
    public static final int REPLY = 0;
    public static final int REPLY_ALL = 1;
    public static final int FORWARD = 2;
    public static final int EDIT_DRAFT = 3;

    // Integer extra holding one of the above compose action
    protected static final String EXTRA_ACTION = "action";

    private static final String EXTRA_SHOW_CC = "showCc";
    private static final String EXTRA_SHOW_BCC = "showBcc";
    private static final String EXTRA_RESPONDED_INLINE = "respondedInline";
    private static final String EXTRA_SAVE_ENABLED = "saveEnabled";
    ///M: String extra for save EDIT_DRAFT
    private static final String EXTRA_EDIT_DRAFT = "editDraft";

    private static final String UTF8_ENCODING_NAME = "UTF-8";

    private static final String MAIL_TO = "mailto";

    private static final String EXTRA_SUBJECT = "subject";

    private static final String EXTRA_BODY = "body";
    private static final String EXTRA_TEXT_CHANGED = "extraTextChanged";

    private static final String EXTRA_SKIP_PARSING_BODY = "extraSkipParsingBody";
    //A:ADD BUG ID :DWYSLM-131 liupeng 20160315 {
	private Preferences prefs;
	//A}
    /**
     * Expected to be html formatted text.
     */
    private static final String EXTRA_QUOTED_TEXT = "quotedText";

    protected static final String EXTRA_FROM_ACCOUNT_STRING = "fromAccountString";

    private static final String EXTRA_ATTACHMENT_PREVIEWS = "attachmentPreviews";

    // Extra that we can get passed from other activities
    @VisibleForTesting
    protected static final String EXTRA_TO = "to";
    private static final String EXTRA_CC = "cc";
    private static final String EXTRA_BCC = "bcc";

    public static final String ANALYTICS_CATEGORY_ERRORS = "compose_errors";

    /**
     * An optional extra containing a {@link ContentValues} of values to be added to
     * {@link SendOrSaveMessage#mValues}.
     */
    public static final String EXTRA_VALUES = "extra-values";

    // List of all the fields
    static final String[] ALL_EXTRAS = { EXTRA_SUBJECT, EXTRA_BODY, EXTRA_TO, EXTRA_CC, EXTRA_BCC,
            EXTRA_QUOTED_TEXT };

    private static final String LEGACY_WEAR_EXTRA = "com.google.android.wearable.extras";

    /**
     * Constant value for the threshold to use for auto-complete suggestions
     * for the to/cc/bcc fields.
     */
    private static final int COMPLETION_THRESHOLD = 1;

    private static SendOrSaveCallback sTestSendOrSaveCallback = null;
    // Map containing information about requests to create new messages, and the id of the
    // messages that were the result of those requests.
    //
    // This map is used when the activity that initiated the save a of a new message, is killed
    // before the save has completed (and when we know the id of the newly created message).  When
    // a save is completed, the service that is running in the background, will update the map
    //
    // When a new ComposeActivity instance is created, it will attempt to use the information in
    // the previously instantiated map.  If ComposeActivity.onCreate() is called, with a bundle
    // (restoring data from a previous instance), and the map hasn't been created, we will attempt
    // to populate the map with data stored in shared preferences.
    private static final ConcurrentHashMap<Integer, Long> sRequestMessageIdMap =
            new ConcurrentHashMap<Integer, Long>(10);
    private static final Random sRandom = new Random(System.currentTimeMillis());

    /**
     * Notifies the {@code Activity} that the caller is an Email
     * {@code Activity}, so that the back behavior may be modified accordingly.
     *
     * @see #onAppUpPressed
     */
    public static final String EXTRA_FROM_EMAIL_TASK = "fromemail";

    public static final String EXTRA_ATTACHMENTS = "attachments";

    /** If set, we will clear notifications for this folder. */
    public static final String EXTRA_NOTIFICATION_FOLDER = "extra-notification-folder";
    public static final String EXTRA_NOTIFICATION_CONVERSATION = "extra-notification-conversation";

    //  If this is a reply/forward then this extra will hold the original message
    private static final String EXTRA_IN_REFERENCE_TO_MESSAGE = "in-reference-to-message";
    // If this is a reply/forward then this extra will hold a uri we must query
    // to get the original message.
    protected static final String EXTRA_IN_REFERENCE_TO_MESSAGE_URI = "in-reference-to-message-uri";
    // If this is an action to edit an existing draft message, this extra will hold the
    // draft message
    private static final String ORIGINAL_DRAFT_MESSAGE = "original-draft-message";
    private static final String END_TOKEN = ", ";
    private static final String LOG_TAG = LogTag.getLogTag();
    // Request numbers for activities we start
    private static final int RESULT_PICK_ATTACHMENT = 1;
    private static final int RESULT_CREATE_ACCOUNT = 2;
    // TODO(mindyp) set mime-type for auto send?
    public static final String AUTO_SEND_ACTION = "com.android.mail.action.AUTO_SEND";

    private static final String EXTRA_SELECTED_REPLY_FROM_ACCOUNT = "replyFromAccount";
    private static final String EXTRA_REQUEST_ID = "requestId";
    private static final String EXTRA_FOCUS_SELECTION_START = "focusSelectionStart";
    private static final String EXTRA_FOCUS_SELECTION_END = "focusSelectionEnd";
    private static final String EXTRA_MESSAGE = "extraMessage";
    private static final int REFERENCE_MESSAGE_LOADER = 0;
    private static final int LOADER_ACCOUNT_CURSOR = 1;
    private static final int INIT_DRAFT_USING_REFERENCE_MESSAGE = 2;
    private static final String EXTRA_SELECTED_ACCOUNT = "selectedAccount";
    private static final String TAG_WAIT = "wait-fragment";
    private static final String MIME_TYPE_ALL = "*/*";
    private static final String MIME_TYPE_PHOTO = "image/*";
    private static final String MIME_TYPE_VIDEO = "video/*";

    private static final String KEY_INNER_SAVED_STATE = "compose_state";
    /// M: save attachments changed state avoid state lost after activity recreate
    private static final String KEY_ATTACHMENTS_CHANGED = "attachments_changed";

    /**M: add Bcc Myself @{
     * Flag the text changed caused by auto_bcc_myself,
     * in this case not mark the message as changed.
     ** @}*/
    private boolean mAddBccBySetting = false;
    /// M: Boolean indicating whether ComposeActivity was active exit.
    private boolean mBackPressed = false;
    ///M: Boolean indicating whether ComposeActivity was in edit draft mode.
    private boolean mEditDraft = false;
    /// M: Indicate the before character of edite view
    private String mBeforeCharSequence;
    /// M: Boolean indicating whether message was init by share content
    private boolean mHasShareContent = false;

    // A single thread for running tasks in the background.
    private static final Handler SEND_SAVE_TASK_HANDLER;
    @VisibleForTesting
    public static final AtomicInteger PENDING_SEND_OR_SAVE_TASKS_NUM = new AtomicInteger(0);

    /* Path of the data directory (used for attachment uri checking). */
    private static final String DATA_DIRECTORY_ROOT;

    // Static initializations
    static {
        HandlerThread handlerThread = new HandlerThread("Send Message Task Thread");
        handlerThread.start();
        SEND_SAVE_TASK_HANDLER = new Handler(handlerThread.getLooper());

        DATA_DIRECTORY_ROOT = Environment.getDataDirectory().toString();
    }

    private final Rect mRect = new Rect();

    private ScrollView mScrollView;
    private ChipsAddressTextView mTo;
    private ChipsAddressTextView mCc;
    private ChipsAddressTextView mBcc;
    private View mCcBccButton;
    private CcBccView mCcBccView;
    protected AttachmentsView mAttachmentsView;
    protected Account mAccount;
    protected ReplyFromAccount mReplyFromAccount;
    private Settings mCachedSettings;
    private Rfc822Validator mValidator;
    /// M: Change the view's type
    private EditText mSubject;

    private ComposeModeAdapter mComposeModeAdapter;
    protected int mComposeMode = -1;
    private boolean mForward;
    protected QuotedTextView mQuotedTextView;
    protected EditText mBodyView;
    private View mFromStatic;
    private TextView mFromStaticText;
    private View mFromSpinnerWrapper;
    @VisibleForTesting
    protected FromAddressSpinner mFromSpinner;
    protected boolean mAddingAttachment;
    protected boolean mAttachmentsChanged;
    private boolean mTextChanged;
    private boolean mReplyFromChanged;
    private MenuItem mSave;
    @VisibleForTesting
    protected Message mRefMessage;
    private long mDraftId = UIProvider.INVALID_MESSAGE_ID;
    private Message mDraft;
    private ReplyFromAccount mDraftAccount;
    private final Object mDraftLock = new Object();

    /**
     * Boolean indicating whether ComposeActivity was launched from a Gmail controlled view.
     */
    private boolean mLaunchedFromEmail = false;
    private RecipientTextWatcher mToListener;
    private RecipientTextWatcher mCcListener;
    private RecipientTextWatcher mBccListener;
    private Uri mRefMessageUri;
    private boolean mShowQuotedText = false;
    protected Bundle mInnerSavedState;
    private ContentValues mExtraValues = null;

    // This is used to track pending requests, refer to sRequestMessageIdMap
    private int mRequestId;
    private String mSignature;
    private Account[] mAccounts;
    private boolean mRespondedInline;
    private boolean mPerformedSendOrDiscard = false;

    // OnKeyListener solely used for intercepting CTRL+ENTER event for SEND.
    private final View.OnKeyListener mKeyListenerForSendShortcut = new View.OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (event.hasModifiers(KeyEvent.META_CTRL_ON) &&
                    keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
                doSend();
                return true;
            }
            return false;
        }
    };

    private final HtmlTree.ConverterFactory mSpanConverterFactory =
            new HtmlTree.ConverterFactory() {
            @Override
                public HtmlTree.Converter<Spanned> createInstance() {
                    return getSpanConverter();
                }
            };

    /**
     * Can be called from a non-UI thread.
     */
    public static void editDraft(Context launcher, Account account, Message message) {
        launch(launcher, account, message, EDIT_DRAFT, null, null, null, null,
                null /* extraValues */);
    }

    /**
     * Can be called from a non-UI thread.
     */
    public static void compose(Context launcher, Account account) {
        launch(launcher, account, null, COMPOSE, null, null, null, null, null /* extraValues */);
    }

    /**
     * Can be called from a non-UI thread.
     */
    public static void composeToAddress(Context launcher, Account account, String toAddress) {
        launch(launcher, account, null, COMPOSE, toAddress, null, null, null,
                null /* extraValues */);
    }

    /**
     * Can be called from a non-UI thread.
     */
    public static void composeWithExtraValues(Context launcher, Account account,
            String subject, final ContentValues extraValues) {
        launch(launcher, account, null, COMPOSE, null, null, null, subject, extraValues);
    }

    /**
     * Can be called from a non-UI thread.
     */
    public static Intent createReplyIntent(final Context launcher, final Account account,
            final Uri messageUri, final boolean isReplyAll) {
        return createActionIntent(launcher, account, messageUri, isReplyAll ? REPLY_ALL : REPLY);
    }

    /**
     * Can be called from a non-UI thread.
     */
    public static Intent createForwardIntent(final Context launcher, final Account account,
            final Uri messageUri) {
        return createActionIntent(launcher, account, messageUri, FORWARD);
    }

    private static Intent createActionIntent(final Context context, final Account account,
            final Uri messageUri, final int action) {
        final Intent intent = new Intent(ACTION_LAUNCH_COMPOSE);
        intent.setPackage(context.getPackageName());

        updateActionIntent(account, messageUri, action, intent);

        return intent;
    }

    @VisibleForTesting
    static Intent updateActionIntent(Account account, Uri messageUri, int action, Intent intent) {
        intent.putExtra(EXTRA_FROM_EMAIL_TASK, true);
        intent.putExtra(EXTRA_ACTION, action);
        intent.putExtra(Utils.EXTRA_ACCOUNT, account);
        intent.putExtra(EXTRA_IN_REFERENCE_TO_MESSAGE_URI, messageUri);

        return intent;
    }

    /**
     * Can be called from a non-UI thread.
     */
    public static void reply(Context launcher, Account account, Message message) {
        launch(launcher, account, message, REPLY, null, null, null, null, null /* extraValues */);
    }

    /**
     * Can be called from a non-UI thread.
     */
    public static void replyAll(Context launcher, Account account, Message message) {
        launch(launcher, account, message, REPLY_ALL, null, null, null, null,
                null /* extraValues */);
    }

    /**
     * Can be called from a non-UI thread.
     */
    public static void forward(Context launcher, Account account, Message message) {
        launch(launcher, account, message, FORWARD, null, null, null, null, null /* extraValues */);
    }

    public static void reportRenderingFeedback(Context launcher, Account account, Message message,
            String body) {
        launch(launcher, account, message, FORWARD,
                "android-gmail-readability@google.com", body, null, null, null /* extraValues */);
    }

    private static void launch(Context context, Account account, Message message, int action,
            String toAddress, String body, String quotedText, String subject,
            final ContentValues extraValues) {
        /// M: We Can't compose in low storage state @{
        if (StorageLowState.checkIfStorageLow(context)) {
            LogUtils.e(LOG_TAG, "Can't compose due to low storage");
            return;
        }
        /// @}

        /// M: [LBE] Large body enhancement, truncate body to avoid binder limitation.
        Message msg = getTruncatedMessage(context, action, message);

        /// M: For smart push, do not record an opening when back from ComposeActivity
         // to MailEmailActivity
        MailActivity.sRecordOpening = false;

        Intent intent = new Intent(ACTION_LAUNCH_COMPOSE);
        intent.setPackage(context.getPackageName());
        intent.putExtra(EXTRA_FROM_EMAIL_TASK, true);
        intent.putExtra(EXTRA_ACTION, action);
        intent.putExtra(Utils.EXTRA_ACCOUNT, account);
        if (action == EDIT_DRAFT) {
            intent.putExtra(ORIGINAL_DRAFT_MESSAGE, msg);
        } else {
            intent.putExtra(EXTRA_IN_REFERENCE_TO_MESSAGE, msg);
        }
        if (toAddress != null) {
            intent.putExtra(EXTRA_TO, toAddress);
        }
        if (body != null) {
            intent.putExtra(EXTRA_BODY, body);
        }
        if (quotedText != null) {
            intent.putExtra(EXTRA_QUOTED_TEXT, quotedText);
        }
        if (subject != null) {
            intent.putExtra(EXTRA_SUBJECT, subject);
        }
        if (extraValues != null) {
            LogUtils.d(LOG_TAG, "Launching with extraValues: %s", extraValues.toString());
            intent.putExtra(EXTRA_VALUES, extraValues);
        }
        if (action == COMPOSE) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        } else if (msg != null) {
            intent.setData(Utils.normalizeUri(msg.uri));
        }
        /// M: Catch the ActivityNotFoundException if there is no any account
        // which had been setup successfully @{
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            LogUtils.e(LOG_TAG, "The compose activity isn't enable, just ignore");
        }
        // @}
    }

    public static void composeMailto(Context context, Account account, Uri mailto) {
        final Intent intent = new Intent(Intent.ACTION_VIEW, mailto);
        intent.setPackage(context.getPackageName());
        intent.putExtra(EXTRA_FROM_EMAIL_TASK, true);
        intent.putExtra(Utils.EXTRA_ACCOUNT, account);
        if (mailto != null) {
            intent.setData(Utils.normalizeUri(mailto));
        }
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Change the title for accessibility so we announce "Compose" instead
        // of the app_name while still showing the app_name in recents.
        setTitle(R.string.compose_title);
        setContentView(R.layout.compose);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Hide the app icon.
            actionBar.setIcon(null);
            actionBar.setDisplayUseLogoEnabled(false);
        }

        mInnerSavedState = (savedInstanceState != null) ?
                savedInstanceState.getBundle(KEY_INNER_SAVED_STATE) : null;
        checkValidAccounts();
        /** M: When forwarding a message with attachments, do some modify and save the message.
         * Then change the orientation, the activity is recreate and the mAttachmentsChanged
         * will be set to true after initial the message. So we do restore the attachments
         * saved state if we had previous saved the state.
         *  @{ */
        if (savedInstanceState != null
                && savedInstanceState.containsKey(KEY_ATTACHMENTS_CHANGED)) {
            mAttachmentsChanged = savedInstanceState.getBoolean(KEY_ATTACHMENTS_CHANGED);
        }
		//  add bug DWLEBM-4 20160309 liupeng   (start)
        if (isDefSignature(getResources().getStringArray(R.array.all_default_signature), mSignature)) {
		// first,clear signature
			mBodyView.removeTextChangedListener(this);
			String bodyText = getBody().getText().toString();
			int signaturePos = getSignatureStartPositionAgain(bodyText);
			if (signaturePos > -1) {
				mBodyView.setText(bodyText.substring(0, signaturePos));
			}
			// second,add new signature
			String newSignature = getString(R.string.default_signature);
			boolean hasFocus = mBodyView.hasFocus();
			mSignature = newSignature;
			if (!TextUtils.isEmpty(mSignature)) {
				// Appending a signature does not count as changing text.
				mBodyView.removeTextChangedListener(this);
				mBodyView.append(convertToPrintableSignature(mSignature));
				mBodyView.addTextChangedListener(this);
			}
			if (hasFocus) {
				focusBody();
			}
			resetBodySelection();
       }
		//   add bug DWLEBM-4 20160309 liupeng (end)
        /** @} */
    }

    private void finishCreate() {
        final Bundle savedState = mInnerSavedState;
        findViews();
        Intent intent = getIntent();
        Message message;
        ArrayList<AttachmentPreview> previews;
        mShowQuotedText = false;
        CharSequence quotedText = null;
        int action;
        // Check for any of the possibly supplied accounts.;
        Account account = null;
        if (hadSavedInstanceStateMessage(savedState)) {
            action = savedState.getInt(EXTRA_ACTION, COMPOSE);
            ///M: restore the mEditDraft
            mEditDraft = savedState.getBoolean(EXTRA_EDIT_DRAFT, false);
            account = savedState.getParcelable(Utils.EXTRA_ACCOUNT);
            message = (Message) savedState.getParcelable(EXTRA_MESSAGE);

            previews = savedState.getParcelableArrayList(EXTRA_ATTACHMENT_PREVIEWS);
            mRefMessage = (Message) savedState.getParcelable(EXTRA_IN_REFERENCE_TO_MESSAGE);
            quotedText = savedState.getCharSequence(EXTRA_QUOTED_TEXT);

            mExtraValues = savedState.getParcelable(EXTRA_VALUES);

            // Get the draft id from the request id if there is one.
            if (savedState.containsKey(EXTRA_REQUEST_ID)) {
                final int requestId = savedState.getInt(EXTRA_REQUEST_ID);
                if (sRequestMessageIdMap.containsKey(requestId)) {
                    synchronized (mDraftLock) {
                        mDraftId = sRequestMessageIdMap.get(requestId);
                    }
                }
            }
        } else {
            account = obtainAccount(intent);
            action = intent.getIntExtra(EXTRA_ACTION, COMPOSE);
            // Initialize the message from the message in the intent
            message = (Message) intent.getParcelableExtra(ORIGINAL_DRAFT_MESSAGE);
            previews = intent.getParcelableArrayListExtra(EXTRA_ATTACHMENT_PREVIEWS);
            mRefMessage = (Message) intent.getParcelableExtra(EXTRA_IN_REFERENCE_TO_MESSAGE);
            mRefMessageUri = (Uri) intent.getParcelableExtra(EXTRA_IN_REFERENCE_TO_MESSAGE_URI);

            if (Analytics.isLoggable()) {
                if (intent.getBooleanExtra(Utils.EXTRA_FROM_NOTIFICATION, false)) {
                    Analytics.getInstance().sendEvent(
                            "notification_action", "compose", getActionString(action), 0);
                }
            }
        }
        mAttachmentsView.setAttachmentPreviews(previews);

        setAccount(account);
        if (mAccount == null) {
            return;
        }

        /*
         * M: if current account is not contained by mAccounts, just finish the
         * compose activity and back to the conversation list.
         */
        boolean bValidAccount = false;
        for (Account tempAccount : mAccounts) {
            if (tempAccount.uri.equals(mAccount.uri) && tempAccount.getDisplayName()
                    .equals(mAccount.getDisplayName())) {
                bValidAccount = true;
                break;
            }
        }
        if (!bValidAccount) {
            finish();
            return;
        }

        initRecipients();

        // Clear the notification and mark the conversation as seen, if necessary
        final Folder notificationFolder =
                intent.getParcelableExtra(EXTRA_NOTIFICATION_FOLDER);

        if (notificationFolder != null) {
            final Uri conversationUri = intent.getParcelableExtra(EXTRA_NOTIFICATION_CONVERSATION);
            Intent actionIntent;
            if (conversationUri != null) {
                actionIntent = new Intent(MailIntentService.ACTION_RESEND_NOTIFICATIONS_WEAR);
                actionIntent.putExtra(Utils.EXTRA_CONVERSATION, conversationUri);
            } else {
                actionIntent = new Intent(MailIntentService.ACTION_CLEAR_NEW_MAIL_NOTIFICATIONS);
                actionIntent.setData(Utils.appendVersionQueryParameter(this,
                        notificationFolder.folderUri.fullUri));
            }
            actionIntent.setPackage(getPackageName());
            actionIntent.putExtra(Utils.EXTRA_ACCOUNT, account);
            actionIntent.putExtra(Utils.EXTRA_FOLDER, notificationFolder);

            startService(actionIntent);
        }

        /// M: Except EDIT_DRAFT intent, the other action's content always need to be saved whatever
        /// there was any user's input, so we must listen every change from the beginning. @{
        if (action != EDIT_DRAFT) {
            initChangeListeners();
        }
        /// @}

        if (intent.getBooleanExtra(EXTRA_FROM_EMAIL_TASK, false)) {
            mLaunchedFromEmail = true;
        } else if (Intent.ACTION_SEND.equals(intent.getAction())) {
            final Uri dataUri = intent.getData();
            if (dataUri != null) {
                final String dataScheme = intent.getData().getScheme();
                final String accountScheme = mAccount.composeIntentUri.getScheme();
                mLaunchedFromEmail = TextUtils.equals(dataScheme, accountScheme);
            }
        }

        if (mRefMessageUri != null) {
            mShowQuotedText = true;
            mComposeMode = action;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
                String wearReply = null;
                if (remoteInput != null) {
                    LogUtils.d(LOG_TAG, "Got remote input from new api");
                    CharSequence input = remoteInput.getCharSequence(
                            NotificationActionUtils.WEAR_REPLY_INPUT);
                    if (input != null) {
                        wearReply = input.toString();
                    }
                } else {
                    // TODO: remove after legacy code has been removed.
                    LogUtils.d(LOG_TAG,
                            "No remote input from new api, falling back to compatibility mode");
                    ClipData clipData = intent.getClipData();
                    if (clipData != null
                            && LEGACY_WEAR_EXTRA.equals(clipData.getDescription().getLabel())) {
                        Bundle extras = clipData.getItemAt(0).getIntent().getExtras();
                        if (extras != null) {
                            wearReply = extras.getString(NotificationActionUtils.WEAR_REPLY_INPUT);
                        }
                    }
                }

                if (!TextUtils.isEmpty(wearReply)) {
                    createWearReplyTask(this, mRefMessageUri, UIProvider.MESSAGE_PROJECTION,
                            mComposeMode, wearReply).execute();
                    finish();
                    return;
                } else {
                    LogUtils.w(LOG_TAG, "remote input string is null");
                }
            }

            getLoaderManager().initLoader(INIT_DRAFT_USING_REFERENCE_MESSAGE, null, this);
            return;
        } else if (message != null && action != EDIT_DRAFT) {
            initFromDraftMessage(message);
            initQuotedTextFromRefMessage(mRefMessage, action);
            showCcBcc(savedState);
            mShowQuotedText = message.appendRefMessageContent;
            // if we should be showing quoted text but mRefMessage is null
            // and we have some quotedText, display that
            if (mShowQuotedText && mRefMessage == null) {
                if (quotedText != null) {
                    initQuotedText(quotedText, false /* shouldQuoteText */);
                } else if (mExtraValues != null) {
                    initExtraValues(mExtraValues);
                    return;
                }
            }
        } else if (action == EDIT_DRAFT) {
            if (message == null) {
                throw new IllegalStateException("Message must not be null to edit draft");
            }
            ///M: set the mEditDraft
            mEditDraft = true;
            initFromDraftMessage(message);
            boolean showBcc = !TextUtils.isEmpty(message.getBcc());
            boolean showCc = showBcc || !TextUtils.isEmpty(message.getCc());
            mCcBccView.show(false, showCc, showBcc);
            // Update the action to the draft type of the previous draft
            switch (message.draftType) {
                case UIProvider.DraftType.REPLY:
                    action = REPLY;
                    break;
                case UIProvider.DraftType.REPLY_ALL:
                    action = REPLY_ALL;
                    break;
                case UIProvider.DraftType.FORWARD:
                    action = FORWARD;
                    break;
                case UIProvider.DraftType.COMPOSE:
                default:
                    action = COMPOSE;
                    break;
            }
            LogUtils.d(LOG_TAG, "Previous draft had action type: %d", action);

            mShowQuotedText = message.appendRefMessageContent;
            if (message.refMessageUri != null) {
                // If we're editing an existing draft that was in reference to an existing message,
                // still need to load that original message since we might need to refer to the
                // original sender and recipients if user switches "reply <-> reply-all".
                mRefMessageUri = message.refMessageUri;
                mComposeMode = action;
                getLoaderManager().initLoader(REFERENCE_MESSAGE_LOADER, null, this);
                return;
            }
        } else if ((action == REPLY || action == REPLY_ALL || action == FORWARD)) {
            if (mRefMessage != null) {
                initFromRefMessage(action);
                mShowQuotedText = true;
            }
        } else {
            if (initFromExtras(intent)) {
                return;
            }
        }

        mComposeMode = action;
        finishSetup(action, intent, savedState);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private static AsyncTask<Void, Void, Message> createWearReplyTask(
            final ComposeActivity composeActivity,
            final Uri refMessageUri, final String[] projection, final int action,
            final String wearReply) {
        return new AsyncTask<Void, Void, Message>() {
            private Intent mEmptyServiceIntent = new Intent(composeActivity, EmptyService.class);

            @Override
            protected void onPreExecute() {
                // Start service so we won't be killed if this app is put in the background.
                composeActivity.startService(mEmptyServiceIntent);
            }

            @Override
            protected Message doInBackground(Void... params) {
                Cursor cursor = composeActivity.getContentResolver()
                        .query(refMessageUri, projection, null, null, null, null);
                if (cursor != null) {
                    try {
                        cursor.moveToFirst();
                        return new Message(cursor);
                    } finally {
                        cursor.close();
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Message message) {
                composeActivity.stopService(mEmptyServiceIntent);

                composeActivity.mRefMessage = message;
                composeActivity.initFromRefMessage(action);
                composeActivity.setBody(wearReply, false);
                composeActivity.finishSetup(action, composeActivity.getIntent(), null);
                composeActivity.sendOrSaveWithSanityChecks(false /* save */, true /* show  toast */,
                        false /* orientationChanged */, true /* autoSend */);
            }
        };
    }

    private void checkValidAccounts() {
        final Account[] allAccounts = AccountUtils.getAccounts(this);
        if (allAccounts == null || allAccounts.length == 0) {
            final Intent noAccountIntent = MailAppProvider.getNoAccountIntent(this);
            if (noAccountIntent != null) {
                mAccounts = null;
                startActivityForResult(noAccountIntent, RESULT_CREATE_ACCOUNT);
                /**
                 * M: if the oncreate is start by share, don't call finish(),
                 *   only the case: all account delete in setting APP, and re-launch
                 *   email from launcher or recent app, which mInnerSavedState != null
                 *   finish the activity, if there is no a valid account.
                 */
                if (mInnerSavedState != null) {
                    finish();
                }
            }
        } else {
            // If none of the accounts are syncing, setup a watcher.
            boolean anySyncing = false;
            for (Account a : allAccounts) {
                if (a.isAccountReady()) {
                    anySyncing = true;
                    break;
                }
            }
            if (!anySyncing) {
                // There are accounts, but none are sync'd, which is just like having no accounts.
                mAccounts = null;
                getLoaderManager().initLoader(LOADER_ACCOUNT_CURSOR, null, this);
                return;
            }
            Account[] accounts = AccountUtils.getSyncingAccounts(this);
            /// M: filter all the account which can't use to send message @{
            ArrayList<Account> listAccount = new ArrayList<Account>(accounts.length);
            for (Account acct : accounts) {
                if (!acct.supportsCapability(UIProvider.AccountCapabilities.VIRTUAL_ACCOUNT)) {
                    listAccount.add(acct);
                }
            }
            mAccounts = listAccount.toArray(new Account[listAccount.size()]);
            /// @}
            finishCreate();
        }
    }

    private Account obtainAccount(Intent intent) {
        Account account = null;
        Object accountExtra = null;
        if (intent != null && intent.getExtras() != null) {
            accountExtra = intent.getExtras().get(Utils.EXTRA_ACCOUNT);
            if (accountExtra instanceof Account) {
                account = (Account) accountExtra;
            } else if (accountExtra instanceof String) {
                // This is the Account attached to the widget compose intent.
                account = Account.newInstance((String) accountExtra);
            }
            /// M: if the account 's not an valid account(such as combined
            // account), need initialize the account from reference Message's
            // account, or use default value @{
            if (account != null && account
                    .supportsCapability(UIProvider
                            .AccountCapabilities.VIRTUAL_ACCOUNT)) {
                Message refMessage = (Message) intent
                        .getParcelableExtra(EXTRA_IN_REFERENCE_TO_MESSAGE);
                if (refMessage != null && refMessage.accountUri != null) {
                    for (Account acct:mAccounts) {
                        if (refMessage.accountUri.equals(acct.uri)) {
                            LogUtils.w(LogUtils.TAG,
                                    "origin account [%s] is sending unavailable, get suitable account" +
                                    " [%s] from refMessage", account.uri, acct.uri);
                            account = acct;
                            break;
                        }
                    }
                } else if (!Address.isAllValid(account.getEmailAddress())
                        && mAccounts.length > 0) {
                    LogUtils.w(LogUtils.TAG,
                            "origin account [%s] is sending unavailable, get first account" +
                            " [%s] from database", account.uri, mAccounts[0].uri);
                    account = mAccounts[0];
                }
            }
            if (account != null) {
                return account;
            }
            /// @}
            accountExtra = intent.hasExtra(Utils.EXTRA_ACCOUNT) ?
                    intent.getStringExtra(Utils.EXTRA_ACCOUNT) :
                        intent.getStringExtra(EXTRA_SELECTED_ACCOUNT);
        }

        MailAppProvider provider = MailAppProvider.getInstance();
        String lastAccountUri = provider.getLastSentFromAccount();
        if (TextUtils.isEmpty(lastAccountUri)) {
            lastAccountUri = provider.getLastViewedAccount();
        }
        if (!TextUtils.isEmpty(lastAccountUri)) {
            accountExtra = Uri.parse(lastAccountUri);
        }

        if (mAccounts != null && mAccounts.length > 0) {
            if (accountExtra instanceof String && !TextUtils.isEmpty((String) accountExtra)) {
                // For backwards compatibility, we need to check account
                // names.
                for (Account a : mAccounts) {
                    if (a.getEmailAddress().equals(accountExtra)) {
                        account = a;
                    }
                }
            } else if (accountExtra instanceof Uri) {
                // The uri of the last viewed account is what is stored in
                // the current code base.
                for (Account a : mAccounts) {
                    if (a.uri.equals(accountExtra)) {
                        account = a;
                    }
                }
            }
            if (account == null) {
                account = mAccounts[0];
            }
        }
        return account;
    }

    protected void finishSetup(int action, Intent intent, Bundle savedInstanceState) {
        setFocus(action);
        // Don't bother with the intent if we have procured a message from the
        // intent already.
        if (!hadSavedInstanceStateMessage(savedInstanceState)) {
            initAttachmentsFromIntent(intent);
        }
        initActionBar();
        initFromSpinner(savedInstanceState != null ? savedInstanceState : intent.getExtras(),
                action);

        // If this is a draft message, the draft account is whatever account was
        // used to open the draft message in Compose.
        if (mDraft != null) {
            mDraftAccount = mReplyFromAccount;
        }

        ///M: Auto Add Bcc Myself
        addBccMyself(mReplyFromAccount);
        initChangeListeners();
        updateHideOrShowCcBcc();
        updateHideOrShowQuotedText(mShowQuotedText);

        mRespondedInline = mInnerSavedState != null &&
                mInnerSavedState.getBoolean(EXTRA_RESPONDED_INLINE);
        if (mRespondedInline) {
            mQuotedTextView.setVisibility(View.GONE);
        }

        mTextChanged = (((savedInstanceState != null) ?
                savedInstanceState.getBoolean(EXTRA_TEXT_CHANGED) : false)
                /// M: if has compose is init form share way, treat as text change
                || mHasShareContent);
    }

    private static boolean hadSavedInstanceStateMessage(final Bundle savedInstanceState) {
        return savedInstanceState != null && savedInstanceState.containsKey(EXTRA_MESSAGE);
    }

    private void updateHideOrShowQuotedText(boolean showQuotedText) {
        mQuotedTextView.updateCheckedState(showQuotedText);
        mQuotedTextView.setUpperDividerVisible(mAttachmentsView.getAttachments().size() > 0);
    }

    private void setFocus(int action) {
        if (action == EDIT_DRAFT) {
            int type = mDraft.draftType;
            switch (type) {
                case UIProvider.DraftType.COMPOSE:
                case UIProvider.DraftType.FORWARD:
                    action = COMPOSE;
                    break;
                case UIProvider.DraftType.REPLY:
                case UIProvider.DraftType.REPLY_ALL:
                default:
                    action = REPLY;
                    break;
            }
        }
        switch (action) {
            case FORWARD:
            case COMPOSE:
                if (TextUtils.isEmpty(mTo.getText())) {
                    mTo.requestFocus();
                    break;
                }
                //$FALL-THROUGH$
            case REPLY:
            case REPLY_ALL:
            default:
                focusBody();
                break;
        }
    }

    /**
     * Focus the body of the message.
     */
    public void focusBody() {
        mBodyView.requestFocus();
        resetBodySelection();
    }

    private void resetBodySelection() {
        int length = mBodyView.getText().length();
        int signatureStartPos = getSignatureStartPosition(
                mSignature, mBodyView.getText().toString());
        if (signatureStartPos > -1) {
            // In case the user deleted the newlines...
            mBodyView.setSelection(signatureStartPos);
        } else if (length >= 0) {
            // Move cursor to the end.
            mBodyView.setSelection(length);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        Analytics.getInstance().activityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        Analytics.getInstance().activityStop(this);
    }
    
    //A:add bug DWLEBM-4 20160309 liupeng (start)
    private int getSignatureStartPositionAgain(String bodyText){
 	   int position = -1;
 	   String []allSignatures = getResources().getStringArray(R.array.all_default_signature);
 	   for(int i=0; i<allSignatures.length; i++){
 		   position = getSignatureStartPosition(allSignatures[i], bodyText);
 		   if(-1 != position){
 			   return position;
 		   }
 	   }
 	   return position;
    }

    
	private boolean isDefSignature(String[] signature, String temp) {
		for (String s : signature) {
			if (s.equals(temp)) {
				return true;
			}
		}
		return false;
	}
	//A:add bug DWLEBM-4 20160309 liupeng (end)

    @Override
    protected void onResume() {
        super.onResume();
        // Update the from spinner as other accounts
        // may now be available.
        if (mFromSpinner != null && mAccount != null) {
            mFromSpinner.initialize(mComposeMode, mAccount, mAccounts, mRefMessage);
        }

        /** M: Start a new duration recording when user compose/reply/forward an email.
          * At present, not take into account the case that user changing the sending
          * account @{ */
        if (mAccount != null) {
            DataCollectUtils.startRecord(this, Long.parseLong(
                    mAccount.uri.getLastPathSegment()), false);
        }
        /** @} */

        /** M: If user go to home screen by pressing "Home" key at MailboxSettings UI
        and then come back to Email(MailboxSettings::onResume called), we should
        record the current account being opened again when backing to EmailActivity. @{ */
        if (!MailActivity.sEmailActivityResumed) {
            MailActivity.sRecordOpening = true;
            DataCollectUtils.clearRecordedList();
        }
        /** @} */
    }

    @Override
    protected void onPause() {
        super.onPause();
        /** M: Stop the record @{ */
        if (mAccount != null) {
            DataCollectUtils.stopRecord(this);
        }
        MailActivity.sEmailActivityResumed = false;
        /** @} */

        // When the user exits the compose view, see if this draft needs saving.
        // Don't save unnecessary drafts if we are only changing the orientation.
        if (!isChangingConfigurations()) {
            saveIfNeeded();

            if (isFinishing() && !mPerformedSendOrDiscard && !isBlank()) {
                // log saving upon backing out of activity. (we avoid logging every sendOrSave()
                // because that method can be invoked many times in a single compose session.)
                logSendOrSave(true /* save */);
            }
        }
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        LogUtils.d(LogUtils.TAG, "ComposeActivity onActivityResult [%d] data: %s", result,
                (data != null) ? data.toString() : "NULL");
        if (request == RESULT_PICK_ATTACHMENT) {
            mAddingAttachment = false;
            if (result == RESULT_OK) {
                addAttachmentAndUpdateView(data);
            }
        } else if (request == RESULT_CREATE_ACCOUNT) {
            // We were waiting for the user to create an account
            if (result != RESULT_OK) {
                finish();
            } else {
                /**
                 * M: If the variable mAccount is not null, it indicates that
                 * the account information had been got ready, and there is no
                 * need to re-query the account info. For example, before it
                 * backed to the compose activity, the system language had been
                 * changed, the compose activity would re-create first, and then
                 * call the onActivityResult. In this case, it would resulted in
                 * to load attachments twice, but the user only send one.
                 *
                 * @{
                 */
                if (mAccount != null) {
                    return;
                }
                /** @} */
                // Watch for accounts to show up!
                // restart the loader to get the updated list of accounts
                getLoaderManager().initLoader(LOADER_ACCOUNT_CURSOR, null, this);
                showWaitFragment(null);
            }
        }
    }

    @Override
    protected final void onRestoreInstanceState(Bundle savedInstanceState) {
        final boolean hasAccounts = mAccounts != null && mAccounts.length > 0;
        if (hasAccounts) {
            clearChangeListeners();
        }
        super.onRestoreInstanceState(savedInstanceState);
        if (mInnerSavedState != null) {
            if (mInnerSavedState.containsKey(EXTRA_FOCUS_SELECTION_START)) {
                int selectionStart = mInnerSavedState.getInt(EXTRA_FOCUS_SELECTION_START);
                int selectionEnd = mInnerSavedState.getInt(EXTRA_FOCUS_SELECTION_END);
                // There should be a focus and it should be an EditText since we
                // only save these extras if these conditions are true.
                EditText focusEditText = (EditText) getCurrentFocus();
                final int length = focusEditText.getText().length();
                if (selectionStart < length && selectionEnd < length) {
                    focusEditText.setSelection(selectionStart, selectionEnd);
                }
            }
        }
        if (hasAccounts) {
            initChangeListeners();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        final Bundle inner = new Bundle();
        saveState(inner);
        state.putBundle(KEY_INNER_SAVED_STATE, inner);
        /// M: save attachments changed state.
        state.putBoolean(KEY_ATTACHMENTS_CHANGED, mAttachmentsChanged);
    }

    private void saveState(Bundle state) {
        // We have no accounts so there is nothing to compose, and therefore, nothing to save.
        if (mAccounts == null || mAccounts.length == 0) {
            return;
        }
        // The framework is happy to save and restore the selection but only if it also saves and
        // restores the contents of the edit text. That's a lot of text to put in a bundle so we do
        // this manually.
        View focus = getCurrentFocus();
        if (focus != null && focus instanceof EditText) {
            EditText focusEditText = (EditText) focus;
            state.putInt(EXTRA_FOCUS_SELECTION_START, focusEditText.getSelectionStart());
            state.putInt(EXTRA_FOCUS_SELECTION_END, focusEditText.getSelectionEnd());
        }

        final List<ReplyFromAccount> replyFromAccounts = mFromSpinner.getReplyFromAccounts();
        final int selectedPos = mFromSpinner.getSelectedItemPosition();
        final ReplyFromAccount selectedReplyFromAccount = (replyFromAccounts != null
                && replyFromAccounts.size() > 0 && replyFromAccounts.size() > selectedPos) ?
                        replyFromAccounts.get(selectedPos) : null;
        if (selectedReplyFromAccount != null) {
            state.putString(EXTRA_SELECTED_REPLY_FROM_ACCOUNT, selectedReplyFromAccount.serialize()
                    .toString());
            state.putParcelable(Utils.EXTRA_ACCOUNT, selectedReplyFromAccount.account);
        } else {
            state.putParcelable(Utils.EXTRA_ACCOUNT, mAccount);
        }

        if (mDraftId == UIProvider.INVALID_MESSAGE_ID && mRequestId != 0) {
            // We don't have a draft id, and we have a request id,
            // save the request id.
            state.putInt(EXTRA_REQUEST_ID, mRequestId);
        }

        // We want to restore the current mode after a pause
        // or rotation.
        int mode = getMode();
        state.putInt(EXTRA_ACTION, mode);
        ///M: save the mEditDraft for resume
        state.putBoolean(EXTRA_EDIT_DRAFT, mEditDraft);

        final Message message = createMessage(selectedReplyFromAccount, mRefMessage, mode,
                removeComposingSpans(mBodyView.getText()));
        if (mDraft != null) {
            message.id = mDraft.id;
            message.serverId = mDraft.serverId;
            message.uri = mDraft.uri;
            LogUtils.w(LogUtils.TAG, "ComposeMail, save draft uri [%s], id [%s]",
                    mDraft.uri, mDraft.id);
        }
        state.putParcelable(EXTRA_MESSAGE, message);

        if (mRefMessage != null) {
            state.putParcelable(EXTRA_IN_REFERENCE_TO_MESSAGE, mRefMessage);
        } else if (message.appendRefMessageContent) {
            // If we have no ref message but should be appending
            // ref message content, we have orphaned quoted text. Save it.
            state.putCharSequence(EXTRA_QUOTED_TEXT, mQuotedTextView.getQuotedTextIfIncluded());
            LogUtils.d(LOG_TAG, "saveState mQuotedTextView size : %d",
                    mQuotedTextView.getQuotedTextIfIncluded() == null ?
                            0 : mQuotedTextView.getQuotedTextIfIncluded().length());
        }
        state.putBoolean(EXTRA_SHOW_CC, mCcBccView.isCcVisible());
        state.putBoolean(EXTRA_SHOW_BCC, mCcBccView.isBccVisible());
        state.putBoolean(EXTRA_RESPONDED_INLINE, mRespondedInline);
        /// M: If mSave is null, it indicate onCreateOptionsMenu hadn't been
        // invoked, the state of save may be a incorrect value, so we shouldn't
        // save this state, just let the default logic to judge the status @{
        if (mSave != null) {
            state.putBoolean(EXTRA_SAVE_ENABLED, mSave.isEnabled());
        } else {
            LogUtils.d(LOG_TAG, "mSave is %s", mSave);
        }
        // @}
        /// M: saving bitmap by parcel will cost large memory size, we prefer not do it.
        //state.putParcelableArrayList(
        //        EXTRA_ATTACHMENT_PREVIEWS, mAttachmentsView.getAttachmentPreviews());
        state.putParcelable(EXTRA_VALUES, mExtraValues);

        state.putBoolean(EXTRA_TEXT_CHANGED, mTextChanged);
        // On configuration changes, we don't actually need to parse the body html ourselves because
        // the framework can correctly restore the body EditText to its exact original state.
        state.putBoolean(EXTRA_SKIP_PARSING_BODY, isChangingConfigurations());
    }

    private int getMode() {
        int mode = ComposeActivity.COMPOSE;
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null
                && actionBar.getNavigationMode() == ActionBar.NAVIGATION_MODE_LIST) {
            mode = actionBar.getSelectedNavigationIndex();
        }
        return mode;
    }

    /**
     * This function might be called from a background thread, so be sure to move everything that
     * can potentially modify the UI to the main thread (e.g. removeComposingSpans for body).
     */
    private Message createMessage(ReplyFromAccount selectedReplyFromAccount, Message refMessage,
            int mode, Spanned body) {
        Message message = new Message();
        message.id = UIProvider.INVALID_MESSAGE_ID;
        message.serverId = null;
        message.uri = null;
        message.conversationUri = null;
        message.subject = mSubject.getText().toString();
        message.snippet = null;
        message.setTo(formatSenders(mTo.getText().toString()));
        message.setCc(formatSenders(mCc.getText().toString()));
        message.setBcc(formatSenders(mBcc.getText().toString()));
        message.setReplyTo(null);
        message.dateReceivedMs = 0;
        message.bodyHtml = spannedBodyToHtml(body, true);
        message.bodyText = body.toString();
        // Fallback to use the text version if html conversion fails for whatever the reason.
        final String htmlInPlainText = Utils.convertHtmlToPlainText(message.bodyHtml);
        if (message.bodyText != null && message.bodyText.trim().length() > 0 &&
                TextUtils.isEmpty(htmlInPlainText)) {
            LogUtils.w(LOG_TAG, "FAILED HTML CONVERSION: from %d to %d", message.bodyText.length(),
                    htmlInPlainText.length());
            Analytics.getInstance().sendEvent(ANALYTICS_CATEGORY_ERRORS,
                    "failed_html_conversion", null, 0);
            message.bodyHtml = "<p>" + message.bodyText + "</p>";
        }
        message.embedsExternalResources = false;
        message.refMessageUri = mRefMessage != null ? mRefMessage.uri : null;
        message.appendRefMessageContent = mQuotedTextView.getQuotedTextIfIncluded() != null;
        ArrayList<Attachment> attachments = mAttachmentsView.getAttachments();
        message.hasAttachments = attachments != null && attachments.size() > 0;
        message.attachmentListUri = null;
        message.messageFlags = 0;
        message.alwaysShowImages = false;
        message.attachmentsJson = Attachment.toJSONArray(attachments);
        CharSequence quotedText = mQuotedTextView.getQuotedText();
        message.quotedTextOffset = -1; // Just a default value.
        if (refMessage != null && !TextUtils.isEmpty(quotedText)) {
            if (!TextUtils.isEmpty(refMessage.bodyHtml)) {
                // We want the index to point to just the quoted text and not the
                // "On December 25, 2014..." part of it.
                message.quotedTextOffset =
                        QuotedTextView.getQuotedTextOffset(quotedText.toString());
            } else if (!TextUtils.isEmpty(refMessage.bodyText)) {
                // We want to point to the entire quoted text.
                message.quotedTextOffset = QuotedTextView.findQuotedTextIndex(quotedText);
            }
        }
        message.accountUri = null;
        message.setFrom(computeFromForAccount(selectedReplyFromAccount));
        message.draftType = getDraftType(mode);
        ///M: Init the extend flags
        message.extendFlags = 0;
        return message;
    }

    protected String computeFromForAccount(ReplyFromAccount selectedReplyFromAccount) {
        final String email = selectedReplyFromAccount != null ? selectedReplyFromAccount.address
                : mAccount != null ? mAccount.getEmailAddress() : null;
        final String senderName = selectedReplyFromAccount != null ? selectedReplyFromAccount.name
                : mAccount != null ? mAccount.getSenderName() : null;
        final Address address = new Address(email, senderName);
        //return address.toHeader();
        ///M: For no-english charactors,toHeader method would encode it with base64,So for friendly
        /// display,use toString instead,it just impact display,no impact for sending,receiving msg.
        return address.toString();
    }

    private static String formatSenders(final String string) {
        if (!TextUtils.isEmpty(string) && string.charAt(string.length() - 1) == ',') {
            return string.substring(0, string.length() - 1);
        }
        return string;
    }

    @VisibleForTesting
    protected void setAccount(Account account) {
        if (account == null) {
            return;
        }
        if (!account.equals(mAccount)) {
            mAccount = account;
            mCachedSettings = mAccount.settings;
            appendSignature();
        }
        if (mAccount != null) {
            MailActivity.setNfcMessage(mAccount.getEmailAddress());
        }
    }

    private void initFromSpinner(Bundle bundle, int action) {
        if (action == EDIT_DRAFT && mDraft.draftType == UIProvider.DraftType.COMPOSE) {
            action = COMPOSE;
        }
        mFromSpinner.initialize(action, mAccount, mAccounts, mRefMessage);

        if (bundle != null) {
            if (bundle.containsKey(EXTRA_SELECTED_REPLY_FROM_ACCOUNT)) {
                mReplyFromAccount = ReplyFromAccount.deserialize(mAccount,
                        bundle.getString(EXTRA_SELECTED_REPLY_FROM_ACCOUNT));
            } else if (bundle.containsKey(EXTRA_FROM_ACCOUNT_STRING)) {
                final String accountString = bundle.getString(EXTRA_FROM_ACCOUNT_STRING);
                mReplyFromAccount = mFromSpinner.getMatchingReplyFromAccount(accountString);
            }
        }
        if (mReplyFromAccount == null) {
            if (mDraft != null) {
                mReplyFromAccount = getReplyFromAccountFromDraft(mDraft);
            } else if (mRefMessage != null) {
                mReplyFromAccount = getReplyFromAccountForReply(mAccount, mRefMessage);
            }
        }
        if (mReplyFromAccount == null) {
            mReplyFromAccount = getDefaultReplyFromAccount(mAccount);
        }

        mFromSpinner.setCurrentAccount(mReplyFromAccount);

        refreshSenderUI();
    }

    /// M: Refresh the Sender UI
    private void refreshSenderUI() {
        if (mFromSpinner.getCount() > 1) {
            // If there is only 1 account, just show that account.
            // Otherwise, give the user the ability to choose which account to
            // send mail from / save drafts to.
            mFromStatic.setVisibility(View.GONE);
            mFromStaticText.setText(mReplyFromAccount.address);
            mFromSpinnerWrapper.setVisibility(View.VISIBLE);
        } else {
            mFromStatic.setVisibility(View.VISIBLE);
            mFromStaticText.setText(mReplyFromAccount.address);
            mFromSpinnerWrapper.setVisibility(View.GONE);
        }
    }

    private ReplyFromAccount getReplyFromAccountForReply(Account account, Message refMessage) {
        if (refMessage.accountUri != null) {
            // This must be from combined inbox.
            List<ReplyFromAccount> replyFromAccounts = mFromSpinner.getReplyFromAccounts();
            for (ReplyFromAccount from : replyFromAccounts) {
                if (from.account.uri.equals(refMessage.accountUri)) {
                    return from;
                }
            }
            return null;
        } else {
            return getReplyFromAccount(account, refMessage);
        }
    }

    /**
     * Given an account and the message we're replying to,
     * return who the message should be sent from.
     * @param account Account in which the message arrived.
     * @param refMessage Message to analyze for account selection
     * @return the address from which to reply.
     */
    public ReplyFromAccount getReplyFromAccount(Account account, Message refMessage) {
        // First see if we are supposed to use the default address or
        // the address it was sentTo.
        if (mCachedSettings.forceReplyFromDefault) {
            return getDefaultReplyFromAccount(account);
        } else {
            // If we aren't explicitly told which account to look for, look at
            // all the message recipients and find one that matches
            // a custom from or account.
            List<String> allRecipients = new ArrayList<String>();
            allRecipients.addAll(Arrays.asList(refMessage.getToAddressesUnescaped()));
            allRecipients.addAll(Arrays.asList(refMessage.getCcAddressesUnescaped()));
            return getMatchingRecipient(account, allRecipients);
        }
    }

    /**
     * Compare all the recipients of an email to the current account and all
     * custom addresses associated with that account. Return the match if there
     * is one, or the default account if there isn't.
     */
    protected ReplyFromAccount getMatchingRecipient(Account account, List<String> sentTo) {
        // Tokenize the list and place in a hashmap.
        ReplyFromAccount matchingReplyFrom = null;
        Rfc822Token[] tokens;
        HashSet<String> recipientsMap = new HashSet<String>();
        for (String address : sentTo) {
            tokens = Rfc822Tokenizer.tokenize(address);
            for (final Rfc822Token token : tokens) {
                recipientsMap.add(token.getAddress());
            }
        }

        int matchingAddressCount = 0;
        List<ReplyFromAccount> customFroms;
        customFroms = account.getReplyFroms();
        if (customFroms != null) {
            for (ReplyFromAccount entry : customFroms) {
                if (recipientsMap.contains(entry.address)) {
                    matchingReplyFrom = entry;
                    matchingAddressCount++;
                }
            }
        }
        if (matchingAddressCount > 1) {
            matchingReplyFrom = getDefaultReplyFromAccount(account);
        }
        return matchingReplyFrom;
    }

    private static ReplyFromAccount getDefaultReplyFromAccount(final Account account) {
        for (final ReplyFromAccount from : account.getReplyFroms()) {
            if (from.isDefault) {
                return from;
            }
        }
        return new ReplyFromAccount(account, account.uri, account.getEmailAddress(),
                account.getSenderName(), account.getEmailAddress(), true, false);
    }

    private ReplyFromAccount getReplyFromAccountFromDraft(final Message msg) {
        final Address[] draftFroms = Address.parse(msg.getFrom());
        final String sender = draftFroms.length > 0 ? draftFroms[0].getAddress() : "";
        ReplyFromAccount replyFromAccount = null;
        // Do not try to check against the "default" account because the default might be an alias.
        for (ReplyFromAccount fromAccount : mFromSpinner.getReplyFromAccounts()) {
            if (TextUtils.equals(fromAccount.address, sender)) {
                replyFromAccount = fromAccount;
                break;
            }
        }
        return replyFromAccount;
    }

    private void findViews() {
        mScrollView = (ScrollView) findViewById(R.id.compose);
        mScrollView.setVisibility(View.VISIBLE);
        mCcBccButton = findViewById(R.id.add_cc_bcc);
        if (mCcBccButton != null) {
            mCcBccButton.setOnClickListener(this);
        }
        mCcBccView = (CcBccView) findViewById(R.id.cc_bcc_wrapper);
        mAttachmentsView = (AttachmentsView) findViewById(R.id.attachments);
        mTo = (ChipsAddressTextView) findViewById(R.id.to);
        mTo.setOnKeyListener(mKeyListenerForSendShortcut);
        initializeRecipientEditTextView(mTo);
        /// M: Replace with ChipsAddressTextView
//        mTo.setAlternatePopupAnchor(findViewById(R.id.compose_to_dropdown_anchor));
        mCc = (ChipsAddressTextView) findViewById(R.id.cc);
        mCc.setOnKeyListener(mKeyListenerForSendShortcut);
        initializeRecipientEditTextView(mCc);
        mBcc = (ChipsAddressTextView) findViewById(R.id.bcc);
        mBcc.setOnKeyListener(mKeyListenerForSendShortcut);
        initializeRecipientEditTextView(mBcc);
        // TODO: add special chips text change watchers before adding
        // this as a text changed watcher to the to, cc, bcc fields.
        /// M: Change the view's type
        mSubject = (EditText) findViewById(R.id.subject);
        mSubject.setOnKeyListener(mKeyListenerForSendShortcut);
        mSubject.setOnEditorActionListener(this);
        mSubject.setOnFocusChangeListener(this);
        mQuotedTextView = (QuotedTextView) findViewById(R.id.quoted_text_view);
        mQuotedTextView.setRespondInlineListener(this);
        mBodyView = (EditText) findViewById(R.id.body);
        mBodyView.setOnKeyListener(mKeyListenerForSendShortcut);
        mBodyView.setOnFocusChangeListener(this);
        mFromStatic = findViewById(R.id.static_from_content);
        mFromStaticText = (TextView) findViewById(R.id.from_account_name);
        mFromSpinnerWrapper = findViewById(R.id.spinner_from_content);
        mFromSpinner = (FromAddressSpinner) findViewById(R.id.from_picker);
        /// M: Init view's length filter
        initLenghtFilter();

        // Bottom placeholder to forward click events to the body
        findViewById(R.id.composearea_tap_trap_bottom).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mBodyView.requestFocus();
                mBodyView.setSelection(mBodyView.getText().length());
            }
        });
    }

    /// M: Replace with ChipsAddressTextView
    private void initializeRecipientEditTextView(ChipsAddressTextView view) {
        view.setTokenizer(new Rfc822Tokenizer());
        view.setThreshold(COMPLETION_THRESHOLD);
    }

    @Override
    public boolean onEditorAction(TextView view, int action, KeyEvent keyEvent) {
        if (action == EditorInfo.IME_ACTION_DONE) {
            focusBody();
            return true;
        }
        return false;
    }

    /**
     * Convert the body text (in {@link Spanned} form) to ready-to-send HTML format as a plain
     * String.
     *
     * @param body the body text including fancy style spans
     * @param removedComposing whether the function already removed composingSpans. Necessary
     *   because we cannot call removeComposingSpans from a background thread.
     * @return HTML formatted body that's suitable for sending or saving
     */
    private String spannedBodyToHtml(Spanned body, boolean removedComposing) {
        if (!removedComposing) {
            body = removeComposingSpans(body);
        }
        final HtmlifyBeginResult r = onHtmlifyBegin(body);
        return onHtmlifyEnd(Html.toHtml(r.result), r.extras);
    }

    /**
     * A hook for subclasses to convert custom spans in the body text prior to system HTML
     * conversion. That HTML conversion is lossy, so anything above and beyond its capability
     * has to be handled here.
     *
     * @param body
     * @return a copy of the body text with custom spans replaced with HTML
     */
    protected HtmlifyBeginResult onHtmlifyBegin(Spanned body) {
        return new HtmlifyBeginResult(body, null /* extras */);
    }

    protected String onHtmlifyEnd(String html, Object extras) {
        return html;
    }

    protected TextView getBody() {
        return mBodyView;
    }

    @VisibleForTesting
    public String getBodyHtml() {
        return spannedBodyToHtml(mBodyView.getText(), false);
    }

    @VisibleForTesting
    public Account getFromAccount() {
        return mReplyFromAccount != null && mReplyFromAccount.account != null ?
                mReplyFromAccount.account : mAccount;
    }

    private void clearChangeListeners() {
        mSubject.removeTextChangedListener(this);
        mBodyView.removeTextChangedListener(this);
        mTo.removeTextChangedListener(mToListener);
        mCc.removeTextChangedListener(mCcListener);
        mBcc.removeTextChangedListener(mBccListener);
        mFromSpinner.setOnAccountChangedListener(null);
        mAttachmentsView.setAttachmentChangesListener(null);
    }

    // Now that the message has been initialized from any existing draft or
    // ref message data, set up listeners for any changes that occur to the
    // message.
    private void initChangeListeners() {
        // Make sure we only add text changed listeners once!
        clearChangeListeners();
        mSubject.addTextChangedListener(this);
        mBodyView.addTextChangedListener(this);
        if (mToListener == null) {
            mToListener = new RecipientTextWatcher(mTo, this);
        }
        mTo.addTextChangedListener(mToListener);
        if (mCcListener == null) {
            mCcListener = new RecipientTextWatcher(mCc, this);
        }
        mCc.addTextChangedListener(mCcListener);
        if (mBccListener == null) {
            mBccListener = new RecipientTextWatcher(mBcc, this);
        }
        mBcc.addTextChangedListener(mBccListener);
        mFromSpinner.setOnAccountChangedListener(this);
        mAttachmentsView.setAttachmentChangesListener(this);
    }

    private void initActionBar() {
        LogUtils.d(LOG_TAG, "initializing action bar in ComposeActivity");
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) {
            return;
        }
        ///M: add mEditDraft for edit draft just show compose mode.
        if (mComposeMode == ComposeActivity.COMPOSE || mEditDraft) {
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
            actionBar.setTitle(R.string.compose_title);
        } else {
            actionBar.setTitle(null);
            if (mComposeModeAdapter == null) {
                mComposeModeAdapter = new ComposeModeAdapter(actionBar.getThemedContext());
            }
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            actionBar.setListNavigationCallbacks(mComposeModeAdapter, this);
            switch (mComposeMode) {
                case ComposeActivity.REPLY:
                    actionBar.setSelectedNavigationItem(0);
                    break;
                case ComposeActivity.REPLY_ALL:
                    actionBar.setSelectedNavigationItem(1);
                    break;
                case ComposeActivity.FORWARD:
                    actionBar.setSelectedNavigationItem(2);
                    break;
            }
        }
        actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP,
                ActionBar.DISPLAY_HOME_AS_UP);
        actionBar.setHomeButtonEnabled(true);
    }

    /**M:Add Bcc Myself. @{*/
    private boolean isAutoBcc() {
        return MailPrefs.get(this).getIsAutoBccMyselfEnabled();
    }

    private boolean isBccEmpty() {
        ///M: remove affect from Auto Bcc feature.
        boolean AutoBcc = isAutoBcc();
        /// M: Use a common method to check the BCC view is empty? @{
        if (isRecipientEmpty(mBcc)) {
            return true;
        } else if (AutoBcc) {
        /// @}
            String[] addrStr = getAddressesFromList(mBcc);
            Address[] addr = Address.parse(mBcc.getText().toString());
            if (addrStr.length == 1 && addr.length == 1
                    && addr[0].getAddress().equals(mAccount.getEmailAddress())) {
                return true;
            }
        }
        return false;
    }

    private void addBccMyself(ReplyFromAccount account) {
        boolean bccMySelf = isAutoBcc();
        if (account == null) {
            LogUtils.e(LOG_TAG, "Current account is null, can't do bcc myself opertation.");
            return;
        }
        if (bccMySelf) {
            String addr = account.address;
            if (Address.parse(addr).length <= 0) {
                return;
            }
            Address[] bcc = Address.parse(mBcc.getText().toString().trim());
            if (!mBcc.getText().toString().contains(addr.trim())) {
                mAddBccBySetting = true;
                Address a = new Address(addr, account.name);
                if (bcc.length == 1 && bcc[0].getAddress().equals(mAccount.getEmailAddress())) {
                    mBcc.setText("");
                }
                /// M: Invoking addBccAddresses which uses 'appendList' instead of 'append'
                /// to ensure correct chips is generated .
                addBccAddresses(Arrays.asList(TextUtils.split(a.toString(), ",")));
                /// @}
                LogUtils.d(LOG_TAG, "Add bcc myself [%s] ", a.toString());
            }
            boolean showBcc = mCcBccView.isBccVisible();
            if (!showBcc) {
                mCcBccView.show(false, mCcBccView.isCcVisible(), true);
            }
        }
    }
    /**@} */

    private void initFromRefMessage(int action) {
        /**
         * M: Set the original attachment of the message. When
         * mAttachmentChange is true, after delete the original attachment from UI, it
         * will not be displayed in UI when change mode between replay and
         * forward @{
         */
        if (mRefMessage.getAttachments().size() > 0) {
            mAttachmentsView.setOriginalAttachments(mRefMessage.getAttachments());
        }
        /** @} */
        setFieldsFromRefMessage(action);

        // Check if To: address and email body needs to be prefilled based on extras.
        // This is used for reporting rendering feedback.
        if (MessageHeaderView.ENABLE_REPORT_RENDERING_PROBLEM) {
            Intent intent = getIntent();
            if (intent.getExtras() != null) {
                String toAddresses = intent.getStringExtra(EXTRA_TO);
                if (toAddresses != null) {
                    addToAddresses(Arrays.asList(TextUtils.split(toAddresses, ",")));
                }
                String body = intent.getStringExtra(EXTRA_BODY);
                if (body != null) {
                    setBody(body, false /* withSignature */);
                }
            }
        }

        if (mRefMessage != null) {
            // CC field only gets populated when doing REPLY_ALL.
            // BCC never gets auto-populated, unless the user is editing
            // a draft with one.
            if (!TextUtils.isEmpty(mRefMessage.getCc())
                    && action == REPLY_ALL) {
                mCcBccView.show(false, true, false);
            }
        }

        updateHideOrShowCcBcc();
    }

    private void setFieldsFromRefMessage(int action) {
        setSubject(mRefMessage, action);
        // Setup recipients
        if (action == FORWARD) {
            mForward = true;
        }
        initRecipientsFromRefMessage(mRefMessage, action);
        initQuotedTextFromRefMessage(mRefMessage, action);
        if (action == ComposeActivity.FORWARD) {
            initAttachments(mRefMessage);
        }
    }

    protected HtmlTree.Converter<Spanned> getSpanConverter() {
        return new HtmlUtils.SpannedConverter();
    }

    private void initFromDraftMessage(Message message) {
        LogUtils.d(LOG_TAG, "Initializing draft from previous draft message: %s", message);

        synchronized (mDraftLock) {
            // Draft id might already be set by the request to id map, if so we don't need to set it
            if (mDraftId == UIProvider.INVALID_MESSAGE_ID) {
                mDraftId = message.id;
            } else {
                message.id = mDraftId;
            }
            mDraft = message;
        }
        mSubject.setText(message.subject);
        mForward = message.draftType == UIProvider.DraftType.FORWARD;

        final List<String> toAddresses = Arrays.asList(message.getToAddressesUnescaped());
        addToAddresses(toAddresses);
        addCcAddresses(Arrays.asList(message.getCcAddressesUnescaped()), toAddresses);
        addBccAddresses(Arrays.asList(message.getBccAddressesUnescaped()));
        if (message.hasAttachments) {
            List<Attachment> attachments = message.getAttachments();
            for (Attachment a : attachments) {
                addAttachmentAndUpdateView(a);
            }
        }

        // If we don't need to re-populate the body, and the quoted text will be restored from
        // ref message. So we can skip rest of this code.
        if (mInnerSavedState != null && mInnerSavedState.getBoolean(EXTRA_SKIP_PARSING_BODY)) {
            LogUtils.i(LOG_TAG, "Skipping manually populating body and quoted text from draft.");
            return;
        }

        int quotedTextIndex = message.appendRefMessageContent ? message.quotedTextOffset : -1;
        // Set the body
        CharSequence quotedText = null;
        if (!TextUtils.isEmpty(message.bodyHtml)) {
            String body = message.bodyHtml;
            if (quotedTextIndex > -1) {
                // Find the offset in the html text of the actual quoted text and strip it out.
                // Note that the actual quotedTextOffset in the message has not changed as
                // this different offset is used only for display purposes. They point to different
                // parts of the original message.  Please see the comments in QuoteTextView
                // to see the differences.
                quotedTextIndex = QuotedTextView.findQuotedTextIndex(message.bodyHtml);
                if (quotedTextIndex > -1) {
                    body = message.bodyHtml.substring(0, quotedTextIndex);
                    quotedText = message.bodyHtml.subSequence(quotedTextIndex,
                            message.bodyHtml.length());
                }
            }
            new HtmlToSpannedTask().execute(body);
        } else {
            final String body = message.bodyText;
            final CharSequence bodyText;
            if (TextUtils.isEmpty(body)) {
                bodyText = "";
                quotedText = null;
            } else {
                if (quotedTextIndex > body.length()) {
                    // Sanity check to guarantee that we will not over index the String.
                    // If this happens there is a bigger problem. This should never happen hence
                    // the wtf logging.
                    quotedTextIndex = -1;
                    LogUtils.wtf(LOG_TAG, "quotedTextIndex (%d) > body.length() (%d)",
                            quotedTextIndex, body.length());
                }
                bodyText = quotedTextIndex > -1 ? body.substring(0, quotedTextIndex) : body;
                if (quotedTextIndex > -1) {
                    quotedText = body.substring(quotedTextIndex);
                }
            }
            setBody(bodyText, false);
        }
        if (quotedTextIndex > -1 && quotedText != null) {
            mQuotedTextView.setQuotedTextFromDraft(quotedText, mForward);
        }
    }

    /**
     * Fill all the widgets with the content found in the Intent Extra, if any.
     * Also apply the same style to all widgets. Note: if initFromExtras is
     * called as a result of switching between reply, reply all, and forward per
     * the latest revision of Gmail, and the user has already made changes to
     * attachments on a previous incarnation of the message (as a reply, reply
     * all, or forward), the original attachments from the message will not be
     * re-instantiated. The user's changes will be respected. This follows the
     * web gmail interaction.
     * @return {@code true} if the activity should not call {@link #finishSetup}.
     */
    public boolean initFromExtras(Intent intent) {
        // If we were invoked with a SENDTO intent, the value
        // should take precedence
        final Uri dataUri = intent.getData();
        if (dataUri != null) {
            if (MAIL_TO.equals(dataUri.getScheme())) {
                initFromMailTo(dataUri.toString());
            } else {
                if (!mAccount.composeIntentUri.equals(dataUri)) {
                    String toText = dataUri.getSchemeSpecificPart();
                    if (toText != null) {
                        /// M: check address validation to intercept some mess data. @{
                        if (Address.isAllValid(toText)) {
                            mTo.setText("");
                            addToAddresses(Arrays.asList(TextUtils.split(
                                    toText, ",")));
                        } else {
                            LogUtils.i(LOG_TAG, "Invaid email address %s, not add to UI", toText);
                        }
                        /// @}
                    }
                }
            }
        }

        String[] extraStrings = intent.getStringArrayExtra(Intent.EXTRA_EMAIL);
        if (extraStrings != null) {
            addToAddresses(Arrays.asList(extraStrings));
        }
        extraStrings = intent.getStringArrayExtra(Intent.EXTRA_CC);
        if (extraStrings != null) {
            addCcAddresses(Arrays.asList(extraStrings), null);
        }
        extraStrings = intent.getStringArrayExtra(Intent.EXTRA_BCC);
        if (extraStrings != null) {
            addBccAddresses(Arrays.asList(extraStrings));
        }

        String extraString = intent.getStringExtra(Intent.EXTRA_SUBJECT);
        if (extraString != null) {
            mSubject.setText(extraString);
        }

        for (String extra : ALL_EXTRAS) {
            if (intent.hasExtra(extra)) {
                String value = intent.getStringExtra(extra);
                if (EXTRA_TO.equals(extra)) {
                    addToAddresses(Arrays.asList(TextUtils.split(value, ",")));
                } else if (EXTRA_CC.equals(extra)) {
                    addCcAddresses(Arrays.asList(TextUtils.split(value, ",")), null);
                } else if (EXTRA_BCC.equals(extra)) {
                    addBccAddresses(Arrays.asList(TextUtils.split(value, ",")));
                } else if (EXTRA_SUBJECT.equals(extra)) {
                    mSubject.setText(value);
                } else if (EXTRA_BODY.equals(extra)) {
                    setBody(value, true /* with signature */);
                } else if (EXTRA_QUOTED_TEXT.equals(extra)) {
                    initQuotedText(value, true /* shouldQuoteText */);
                }
            }
        }

        Bundle extras = intent.getExtras();
        if (extras != null) {
            CharSequence text = extras.getCharSequence(Intent.EXTRA_TEXT);
            /// M: align with KK, not append body if EXTRA_TEXT is null,
            /// It is unreasonable for this case and some original body will lost. @{
            if (text != null) {
                setBody(text, true /* with signature */);
                /// M: the intent has contains data,
                // we treat it as text changed @{
                mHasShareContent = true;
                /// @}
            }
            /// @}

            // TODO - support EXTRA_HTML_TEXT
        }

        mExtraValues = intent.getParcelableExtra(EXTRA_VALUES);
        if (mExtraValues != null) {
            LogUtils.d(LOG_TAG, "Launched with extra values: %s", mExtraValues.toString());
            initExtraValues(mExtraValues);
            return true;
        }

        return false;
    }

    protected void initExtraValues(ContentValues extraValues) {
        // DO NOTHING - Gmail will override
    }


    @VisibleForTesting
    protected String decodeEmailInUri(String s) throws UnsupportedEncodingException {
        // TODO: handle the case where there are spaces in the display name as
        // well as the email such as "Guy with spaces <guy+with+spaces@gmail.com>"
        // as they could be encoded ambiguously.
        // Since URLDecode.decode changes + into ' ', and + is a valid
        // email character, we need to find/ replace these ourselves before
        // decoding.
        try {
            return URLDecoder.decode(replacePlus(s), UTF8_ENCODING_NAME);
        } catch (IllegalArgumentException e) {
            if (LogUtils.isLoggable(LOG_TAG, LogUtils.VERBOSE)) {
                LogUtils.e(LOG_TAG, "%s while decoding '%s'", e.getMessage(), s);
            } else {
                LogUtils.e(LOG_TAG, e, "Exception  while decoding mailto address");
            }
            return null;
        }
    }

    /**
     * Replaces all occurrences of '+' with "%2B", to prevent URLDecode.decode from
     * changing '+' into ' '
     *
     * @param toReplace Input string
     * @return The string with all "+" characters replaced with "%2B"
     */
    private static String replacePlus(String toReplace) {
        return toReplace.replace("+", "%2B");
    }

    /**
     * Replaces all occurrences of '%' with "%25", to prevent URLDecode.decode from
     * crashing on decoded '%' symbols
     *
     * @param toReplace Input string
     * @return The string with all "%" characters replaced with "%25"
     */
    private static String replacePercent(String toReplace) {
        return toReplace.replace("%", "%25");
    }

    /**
     * Helper function to encapsulate encoding/decoding string from Uri.getQueryParameters
     * @param content Input string
     * @return The string that's properly escaped to be shown in mail subject/content
     */
    private static String decodeContentFromQueryParam(String content) {
        try {
            return URLDecoder.decode(replacePlus(replacePercent(content)), UTF8_ENCODING_NAME);
        } catch (UnsupportedEncodingException e) {
            LogUtils.e(LOG_TAG, "%s while decoding '%s'", e.getMessage(), content);
            return "";  // Default to empty string so setText/setBody has same behavior as before.
        }
    }

    /**
     * Initialize the compose view from a String representing a mailTo uri.
     * @param mailToString The uri as a string.
     */
    public void initFromMailTo(String mailToString) {
        // We need to disguise this string as a URI in order to parse it
        // TODO:  Remove this hack when http://b/issue?id=1445295 gets fixed
        Uri uri = Uri.parse("foo://" + mailToString);
        int index = mailToString.indexOf("?");
        int length = "mailto".length() + 1;
        String to;
        try {
            // Extract the recipient after mailto:
            if (index == -1) {
                to = decodeEmailInUri(mailToString.substring(length));
            } else {
                to = decodeEmailInUri(mailToString.substring(length, index));
            }
            if (!TextUtils.isEmpty(to)) {
                addToAddresses(Arrays.asList(TextUtils.split(to, ",")));
            }
        } catch (UnsupportedEncodingException e) {
            if (LogUtils.isLoggable(LOG_TAG, LogUtils.VERBOSE)) {
                LogUtils.e(LOG_TAG, "%s while decoding '%s'", e.getMessage(), mailToString);
            } else {
                LogUtils.e(LOG_TAG, e, "Exception  while decoding mailto address");
            }
        }

        List<String> cc = uri.getQueryParameters("cc");
        addCcAddresses(Arrays.asList(cc.toArray(new String[cc.size()])), null);

        List<String> otherTo = uri.getQueryParameters("to");
        addToAddresses(Arrays.asList(otherTo.toArray(new String[otherTo.size()])));

        List<String> bcc = uri.getQueryParameters("bcc");
        addBccAddresses(Arrays.asList(bcc.toArray(new String[bcc.size()])));

        // NOTE: Uri.getQueryParameters already decodes % encoded characters
        List<String> subject = uri.getQueryParameters("subject");
        if (subject.size() > 0) {
            mSubject.setText(decodeContentFromQueryParam(subject.get(0)));
        }

        List<String> body = uri.getQueryParameters("body");
        if (body.size() > 0) {
            try {
                setBody(decodeContentFromQueryParam(body.get(0)), true /* with signature */);
           /** M: If the url was invalid, URLDecoder.decode will throw IllegalArgumentException.
            *  Then we use the original data as body. @{ */
            } catch (IllegalArgumentException e) {
                LogUtils.e(LOG_TAG, e, "Exception occurred while decoding body '%s'", body);
                setBody(body.get(0), true);
            }
           /** @} */
        }
    }

    @VisibleForTesting
    protected void initAttachments(Message refMessage) {
        addAttachments(refMessage.getAttachments(), false);
    }

    /**
     * M: Add attachments, allowint duplication
     * @param attachments
     * @return
     */
    public long addAttachments(List<Attachment> attachments) {
        return addAttachments(attachments, true);
    }

    /**
     * M: Add attchments
     * Allow duplication if we are initializing from a ref-message, otherwise, don't allow duplication
     * @param attachments
     * @param allowDup Allow duplicate attachments if true
     * @return
     */
    public long addAttachments(List<Attachment> attachments, boolean allowDup) {
        long size = 0;
        AttachmentFailureException error = null;
        /**
         * M: if we don't allow duplication, do some filter operations @{
         */
        ArrayList<Attachment> currentAttachments = mAttachmentsView.getAttachments();
        for (Attachment attachment : attachments) {
            try {
                if (allowDup || !currentAttachments.contains(attachment)) {
                    LogUtils.d(LOG_TAG, "Add attachment %s with allowDup: " + allowDup, attachment);
                    size += mAttachmentsView.addAttachment(mAccount, attachment);
                }
            } catch (AttachmentFailureException e) {
                error = e;
            }
        }
        /** @} */
        if (error != null) {
            LogUtils.e(LOG_TAG, error, "Error adding attachment");
            showAttachmentTooBigToast(error.getErrorRes());
        }
        return size;
    }

    /**
     * When an attachment is too large to be added to a message, show a toast.
     * This method also updates the position of the toast so that it is shown
     * clearly above they keyboard if it happens to be open.
     */
    private void showAttachmentTooBigToast(int errorRes) {
        String maxSize = AttachmentUtils.convertToHumanReadableSize(
                getApplicationContext(), mAccount.settings.getMaxAttachmentSize());
        showErrorToast(getString(errorRes, maxSize));
    }

    protected void showErrorToast(String message) {
        Toast t = Toast.makeText(this, message, Toast.LENGTH_LONG);
        t.setText(message);
        t.setGravity(Gravity.CENTER_HORIZONTAL, 0,
                getResources().getDimensionPixelSize(R.dimen.attachment_toast_yoffset));
        t.show();
    }

    protected void initAttachmentsFromIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras == null) {
            extras = Bundle.EMPTY;
        }
        final String action = intent.getAction();
        if (!mAttachmentsChanged) {
            long totalSize = 0;
            if (extras.containsKey(EXTRA_ATTACHMENTS)) {
                final String[] uris = (String[]) extras.getSerializable(EXTRA_ATTACHMENTS);
                final ArrayList<Uri> parsedUris = Lists.newArrayListWithCapacity(uris.length);
                for (String uri : uris) {
                    parsedUris.add(Uri.parse(uri));
                }
                totalSize += handleAttachmentUrisFromIntent(parsedUris);
            }
            if (extras.containsKey(Intent.EXTRA_STREAM)) {
                if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
                    final ArrayList<Uri> uris = extras
                            .getParcelableArrayList(Intent.EXTRA_STREAM);
                    totalSize += handleAttachmentUrisFromIntent(uris);
                } else {
                    final Uri uri = extras.getParcelable(Intent.EXTRA_STREAM);
                    final ArrayList<Uri> uris = Lists.newArrayList(uri);
                    totalSize += handleAttachmentUrisFromIntent(uris);
                }
            }

            if (totalSize > 0) {
                mAttachmentsChanged = true;
                updateSaveUi();

                Analytics.getInstance().sendEvent("send_intent_with_attachments",
                        Integer.toString(getAttachments().size()), null, totalSize);
            }
        }
    }

    /**
     * @return the authority of EmailProvider for this app. should be overridden in concrete
     * app implementations. can't be known here because this project doesn't know about that sort
     * of thing.
     */
    protected String getEmailProviderAuthority() {
        throw new UnsupportedOperationException("unimplemented, EmailProvider unknown");
    }

    /**
     * Helper function to handle a list of uris to attach.
     * @return the total size of all successfully attached files.
     */
    private long handleAttachmentUrisFromIntent(List<Uri> uris) {
        ArrayList<Attachment> attachments = Lists.newArrayList();
        for (Uri uri : uris) {
            try {
                if (uri != null) {
                    if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
                        // We must not allow files from /data, even from our process.
                        final File f = new File(uri.getPath());
                        final String filePath = f.getCanonicalPath();
                        if (filePath.startsWith(DATA_DIRECTORY_ROOT)) {
                                showErrorToast(getString(R.string.attachment_permission_denied));
                          Analytics.getInstance().sendEvent(ANALYTICS_CATEGORY_ERRORS,
                                  "send_intent_attachment", "data_dir", 0);
                                continue;
                        }
                    } else if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
                        // disallow attachments from our own EmailProvider (b/27308057)
                        if (getEmailProviderAuthority().equals(uri.getAuthority())) {
                            showErrorToast(getString(R.string.attachment_permission_denied));
                            Analytics.getInstance().sendEvent(ANALYTICS_CATEGORY_ERRORS,
                                    "send_intent_attachment", "email_provider", 0);
                            continue;
                        }
                    }

                    if (!handleSpecialAttachmentUri(uri)) {
                        final Attachment a = mAttachmentsView.generateLocalAttachment(uri,
                                getApplicationContext());
                        attachments.add(a);

                        Analytics.getInstance().sendEvent("send_intent_attachment",
                                Utils.normalizeMimeType(a.getContentType()), null, a.size);
                    }
                }
            } catch (AttachmentFailureException e) {
                LogUtils.e(LOG_TAG, e, "Error adding attachment");
                showAttachmentTooBigToast(e.getErrorRes());
            } catch (IOException | SecurityException e) {
                LogUtils.e(LOG_TAG, e, "Error adding attachment");
                showErrorToast(getString(R.string.attachment_permission_denied));
            }
        }
        return addAttachments(attachments);
    }

    protected void initQuotedText(CharSequence quotedText, boolean shouldQuoteText) {
        mQuotedTextView.setQuotedTextFromHtml(quotedText, shouldQuoteText);
        mShowQuotedText = true;
    }

    private void initQuotedTextFromRefMessage(Message refMessage, int action) {
        if (mRefMessage != null
                && (action == REPLY || action == REPLY_ALL || action == FORWARD)
                /** M: not show quoted text again since user have added it. @{ */
                && !mRespondedInline
                /** @} */) {
            mQuotedTextView.setQuotedText(action, refMessage, action != FORWARD);
            LogUtils.d(LOG_TAG, "initQuotedTextFromRefMessage show quoted text view");
        }
    }

    private void updateHideOrShowCcBcc() {
        // Its possible there is a menu item OR a button.
        boolean ccVisible = mCcBccView.isCcVisible();
        boolean bccVisible = mCcBccView.isBccVisible();
        if (mCcBccButton != null) {
            if (!ccVisible || !bccVisible) {
                mCcBccButton.setVisibility(View.VISIBLE);
            } else {
                mCcBccButton.setVisibility(View.GONE);
            }
        }
    }

    private void showCcBcc(Bundle state) {
        if (state != null && state.containsKey(EXTRA_SHOW_CC)) {
            boolean showCc = state.getBoolean(EXTRA_SHOW_CC);
            boolean showBcc = state.getBoolean(EXTRA_SHOW_BCC);
            if (showCc || showBcc) {
                mCcBccView.show(false, showCc, showBcc);
            }
        }
    }

    /**
     * Add attachment and update the compose area appropriately.
     */
    protected void addAttachmentAndUpdateView(Intent data) {
        if (data == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            final ClipData clipData = data.getClipData();
            if (clipData != null) {
                for (int i = 0, size = clipData.getItemCount(); i < size; i++) {
                    addAttachmentAndUpdateView(clipData.getItemAt(i).getUri());
                }
                return;
            }
        }

        addAttachmentAndUpdateView(data.getData());
    }

    protected void addAttachmentAndUpdateView(Uri contentUri) {
        if (contentUri == null) {
            return;
        }
        try {

            if (handleSpecialAttachmentUri(contentUri)) {
                return;
            }

            addAttachmentAndUpdateView(mAttachmentsView.generateLocalAttachment(contentUri,
                    getApplicationContext()));
        } catch (AttachmentFailureException e) {
            LogUtils.e(LOG_TAG, e, "Error adding attachment");
            showErrorToast(getResources().getString(
                    e.getErrorRes(),
                    AttachmentUtils.convertToHumanReadableSize(
                            getApplicationContext(), mAccount.settings.getMaxAttachmentSize())));
        }
    }

    /**
     * Allow subclasses to implement custom handling of attachments.
     *
     * @param contentUri a passed-in URI from a pick intent
     * @return true iff handled
     */
    protected boolean handleSpecialAttachmentUri(final Uri contentUri) {
        return false;
    }

    private void addAttachmentAndUpdateView(Attachment attachment) {
        try {
            long size = mAttachmentsView.addAttachment(mAccount, attachment);
            if (size > 0) {
                /// M: Do not set the mAttachmentsChanged to true when load a draft message.@{
                Intent intent = getIntent();
                if (null != intent && intent.getIntExtra(EXTRA_ACTION, COMPOSE) != EDIT_DRAFT) {
                    mAttachmentsChanged = true;
                }
                /// @}
                updateSaveUi();
            }
        } catch (AttachmentFailureException e) {
            LogUtils.e(LOG_TAG, e, "Error adding attachment");
            showAttachmentTooBigToast(e.getErrorRes());
        }
    }

    void initRecipientsFromRefMessage(Message refMessage, int action) {
        // Don't populate the address if this is a forward.
        if (action == ComposeActivity.FORWARD) {
            return;
        }
        initReplyRecipients(refMessage, action);
    }

    // TODO: This should be private.  This method shouldn't be used by ComposeActivityTests, as
    // it doesn't setup the state of the activity correctly
    @VisibleForTesting
    void initReplyRecipients(final Message refMessage, final int action) {
        String[] sentToAddresses = refMessage.getToAddressesUnescaped();
        final Collection<String> toAddresses;
        final String[] fromAddresses = refMessage.getFromAddressesUnescaped();
        final String fromAddress = fromAddresses.length > 0 ? fromAddresses[0] : null;
        final String[] replyToAddresses = getReplyToAddresses(
                refMessage.getReplyToAddressesUnescaped(), fromAddress);

        // If this is a reply, the Cc list is empty. If this is a reply-all, the
        // Cc list is the union of the To and Cc recipients of the original
        // message, excluding the current user's email address and any addresses
        // already on the To list.
        if (action == ComposeActivity.REPLY) {
            ///M: just reply no need sentToAddresses@{
            toAddresses = initToRecipients(fromAddress, replyToAddresses, null);
            /// @}
            addToAddresses(toAddresses);
        } else if (action == ComposeActivity.REPLY_ALL) {
            final Set<String> ccAddresses = Sets.newHashSet();
            toAddresses = initToRecipients(fromAddress, replyToAddresses, sentToAddresses);
            addToAddresses(toAddresses);
            addRecipients(ccAddresses, refMessage.getCcAddressesUnescaped());
            addCcAddresses(ccAddresses, toAddresses);
        }
    }

    // If there is no reply to address, the reply to address is the sender.
    private static String[] getReplyToAddresses(String[] replyTo, String from) {
        boolean hasReplyTo = false;
        for (final String replyToAddress : replyTo) {
            if (!TextUtils.isEmpty(replyToAddress)) {
                hasReplyTo = true;
            }
        }
        return hasReplyTo ? replyTo : new String[] {from};
    }

    private void addToAddresses(Collection<String> addresses) {
        addAddressesToList(addresses, mTo);
    }

    private void addCcAddresses(Collection<String> addresses, Collection<String> toAddresses) {
        addCcAddressesToList(tokenizeAddressList(addresses),
                toAddresses != null ? tokenizeAddressList(toAddresses) : null, mCc);
    }

    private void addBccAddresses(Collection<String> addresses) {
        addAddressesToList(addresses, mBcc);
    }

    @VisibleForTesting
    protected void addCcAddressesToList(List<Rfc822Token[]> addresses,
            List<Rfc822Token[]> compareToList, MultiAutoCompleteTextView list) {
        String address;

        if (compareToList == null) {
            for (final Rfc822Token[] tokens : addresses) {
                for (final Rfc822Token token : tokens) {
                    address = token.toString();
                    list.append(address + END_TOKEN);
                }
            }
        } else {
            HashSet<String> compareTo = convertToHashSet(compareToList);
            for (final Rfc822Token[] tokens : addresses) {
                for (final Rfc822Token token : tokens) {
                    address = token.toString();
                    // Check if this is a duplicate:
                    if (!compareTo.contains(token.getAddress())) {
                        // Get the address here
                        list.append(address + END_TOKEN);
                    }
                }
            }
        }
    }

    protected static HashSet<String> convertToHashSet(final List<Rfc822Token[]> list) {
        final HashSet<String> hash = new HashSet<String>();
        for (final Rfc822Token[] tokens : list) {
            for (final Rfc822Token token : tokens) {
                hash.add(token.getAddress());
            }
        }
        return hash;
    }

    protected List<Rfc822Token[]> tokenizeAddressList(Collection<String> addresses) {
        @VisibleForTesting
        List<Rfc822Token[]> tokenized = new ArrayList<Rfc822Token[]>();

        for (String address: addresses) {
            tokenized.add(Rfc822Tokenizer.tokenize(address));
        }
        return tokenized;
    }

    @VisibleForTesting
    protected void addAddressesToList(Collection<String> addresses, MultiAutoCompleteTextView list) {
        for (String address : addresses) {
            addAddressToList(address, list);
        }
    }

    private void addAddressToList(final String address, final MultiAutoCompleteTextView list) {
        if (address == null || list == null)
            return;

        final Rfc822Token[] tokens = Rfc822Tokenizer.tokenize(address);

        for (final Rfc822Token token : tokens) {
            list.append(token + END_TOKEN);
        }
    }

    @VisibleForTesting
    protected Collection<String> initToRecipients(final String fullSenderAddress,
            final String[] replyToAddresses, final String[] inToAddresses) {
        // The To recipient is the reply-to address specified in the original
        // message, unless it is:
        // the current user OR a custom from of the current user, in which case
        // it's the To recipient list of the original message.
        // OR missing, in which case use the sender of the original message
        Set<String> toAddresses = Sets.newHashSet();
        ///M: add all the addresses to toaddresses, but exclude the current account
        //    (unless current account is the sender or replyto address) and repeat account. @{
        if (!TextUtils.isEmpty(fullSenderAddress)
                && !TextUtils.isEmpty(fullSenderAddress.trim())) {
            toAddresses.add(fullSenderAddress);
        }
        for (final String replyToAddress : replyToAddresses) {
            if (!TextUtils.isEmpty(replyToAddress)
                    && !TextUtils.isEmpty(replyToAddress.trim())
                    && !toAddresses.contains(replyToAddress)
                    && !recipientMatchesThisAccount(replyToAddress)) {
                toAddresses.add(replyToAddress);
            }
        }
        if (inToAddresses != null) {
            // In this case, the user is replying to a message in which their
            // current account or some of their custom from addresses are the only
            // recipients and they sent the original message.
            if (inToAddresses.length == 1 && recipientMatchesThisAccount(fullSenderAddress)
                    && recipientMatchesThisAccount(inToAddresses[0])) {
                toAddresses.add(inToAddresses[0]);
                return toAddresses;
            }
            // This happens if the user replies to a message they originally
            // wrote. In this case, "reply" really means "re-send," so we
            // target the original recipients. This works as expected even
            // if the user sent the original message to themselves.
            for (String address : inToAddresses) {
                if (!recipientMatchesThisAccount(address) && !toAddresses.contains(address)) {
                    toAddresses.add(address);
                }
            }
        }
        /// @}
        return toAddresses;
    }

    private void addRecipients(final Set<String> recipients, final String[] addresses) {
        for (final String email : addresses) {
            // Do not add this account, or any of its custom from addresses, to
            // the list of recipients.
            final String recipientAddress = Address.getEmailAddress(email).getAddress();
            if (!recipientMatchesThisAccount(recipientAddress)) {
                recipients.add(email.replace("\"\"", ""));
            }
        }
    }

    /**
     * A recipient matches this account if it has the same address as the
     * currently selected account OR one of the custom from addresses associated
     * with the currently selected account.
     * @param recipientAddress address we are comparing with the currently selected account
     */
    protected boolean recipientMatchesThisAccount(String recipientAddress) {
        return ReplyFromAccount.matchesAccountOrCustomFrom(mAccount, recipientAddress,
                        mAccount.getReplyFroms());
    }

    /**
     * Returns a formatted subject string with the appropriate prefix for the action type.
     * E.g., "FWD: " is prepended if action is {@link ComposeActivity#FORWARD}.
     */
    public static String buildFormattedSubject(Resources res, String subject, int action) {
        final String prefix;
        final String correctedSubject;
        if (action == ComposeActivity.COMPOSE) {
            prefix = "";
        } else if (action == ComposeActivity.FORWARD) {
            prefix = res.getString(R.string.forward_subject_label);
        } else {
            prefix = res.getString(R.string.reply_subject_label);
        }

        if (TextUtils.isEmpty(subject)) {
            correctedSubject = prefix;
        } else {
            // Don't duplicate the prefix
            if (subject.toLowerCase().startsWith(prefix.toLowerCase())) {
                correctedSubject = subject;
            } else {
                correctedSubject = String.format(
                        res.getString(R.string.formatted_subject), prefix, subject);
            }
        }

        return correctedSubject;
    }

    private void setSubject(Message refMessage, int action) {
        mSubject.setText(buildFormattedSubject(getResources(), refMessage.subject, action));
    }

    private void initRecipients() {
        setupRecipients(mTo);
        setupRecipients(mCc);
        setupRecipients(mBcc);
    }

    //M: Change from private to protected for override in subclass
    protected void setupRecipients(ChipsAddressTextView view) {
//        final DropdownChipLayouter layouter = getDropdownChipLayouter();
//        if (layouter != null) {
//            view.setDropdownChipLayouter(layouter);
//        }
        view.setAdapter(getRecipientAdapter());
//        view.setRecipientEntryItemClickedListener(this);
        //M: if the sender account changed, update the mValidator
        if (mValidator == null || mReplyFromChanged) {
            final String accountName = mAccount.getEmailAddress();
            int offset = accountName.indexOf("@") + 1;
            String account = accountName;
            if (offset > 0) {
                account = account.substring(offset);
            }
            mValidator = new Rfc822Validator(account);
        }
        view.setValidator(mValidator);
    }

    /**
     * Derived classes should override if they wish to provide their own autocomplete behavior.
     */
    public BaseRecipientAdapter getRecipientAdapter() {
        return new RecipientAdapter(this, mAccount);
    }

    /// M: Comments out, replaced with ChipsAddressTextView
//    /**
//     * Derived classes should override this to provide their own dropdown behavior.
//     * If the result is null, the default {@link com.android.ex.chips.DropdownChipLayouter}
//     * is used.
//     */
//    public DropdownChipLayouter getDropdownChipLayouter() {
//        return null;
//    }

    @Override
    public void onClick(View v) {
        final int id = v.getId();
        if (id == R.id.add_cc_bcc) {
            // Verify that cc/ bcc aren't showing.
            // Animate in cc/bcc.
            showCcBccViews();
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        final int id = v.getId();
        if (hasFocus && (id == R.id.subject || id == R.id.body)) {
            /// M: Collapse cc/bcc if it is empty @{
            final boolean showCcFields = !TextUtils.isEmpty(mCc.getText());
            final boolean showBccFields = !TextUtils.isEmpty(mBcc.getText());
            mCcBccView.show(false /* animate */, showCcFields, showBccFields);
            mCcBccButton.setVisibility((showCcFields && showBccFields) ? View.GONE : View.VISIBLE);

            // On phones autoscroll down so that Cc/Bcc aligns to the top if we are showing cc/bcc.
            if (getResources().getBoolean(R.bool.auto_scroll_cc)
                    && (showCcFields || showBccFields)) {
                final int[] coords = new int[2];
                if (showCcFields) {
                    mCc.getLocationOnScreen(coords);
                } else {
                    mBcc.getLocationOnScreen(coords);
                }
                /// @}

                // Subtract status bar and action bar height from y-coord.
                getWindow().getDecorView().getWindowVisibleDisplayFrame(mRect);
                final int deltaY = coords[1] - getSupportActionBar().getHeight() - mRect.top;

                // Only scroll down
                if (deltaY > 0) {
                    mScrollView.smoothScrollBy(0, deltaY);
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final boolean superCreated = super.onCreateOptionsMenu(menu);
        // Don't render any menu items when there are no accounts.
        if (mAccounts == null || mAccounts.length == 0) {
            return superCreated;
        }
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.compose_menu, menu);

        /*
         * Start save in the correct enabled state.
         * 1) If a user launches compose from within gmail, save is disabled
         * until they add something, at which point, save is enabled, auto save
         * on exit; if the user empties everything, save is disabled, exiting does not
         * auto-save
         * 2) if a user replies/ reply all/ forwards from within gmail, save is
         * disabled until they change something, at which point, save is
         * enabled, auto save on exit; if the user empties everything, save is
         * disabled, exiting does not auto-save.
         * 3) If a user launches compose from another application and something
         * gets populated (attachments, recipients, body, subject, etc), save is
         * enabled, auto save on exit; if the user empties everything, save is
         * disabled, exiting does not auto-save
         */
        mSave = menu.findItem(R.id.save);
        String action = getIntent() != null ? getIntent().getAction() : null;
        /// M: Restore the save menu state only if it contains the key.
        enableSave(mInnerSavedState != null && mInnerSavedState.containsKey(EXTRA_SAVE_ENABLED) ?
                mInnerSavedState.getBoolean(EXTRA_SAVE_ENABLED)
                /// M: If compose ui is blank although it is launched from another application,
                /// save is disabled.
                    : (((Intent.ACTION_SEND.equals(action)
                            || Intent.ACTION_SEND_MULTIPLE.equals(action)
                            || Intent.ACTION_SENDTO.equals(action)) && !isBlank())
                            || isDraftDirty()));
        /// M: Restore save menu state only at first time, in case wrong state is set to the
        /// save menu after invalidateOptionsMenu is called which cause the menu is recreated.
        if (mInnerSavedState != null && mInnerSavedState.containsKey(EXTRA_SAVE_ENABLED)) {
            mInnerSavedState.remove(EXTRA_SAVE_ENABLED);
        }

        final MenuItem helpItem = menu.findItem(R.id.help_info_menu_item);
        final MenuItem sendFeedbackItem = menu.findItem(R.id.feedback_menu_item);
        final MenuItem attachFromServiceItem = menu.findItem(R.id.attach_from_service_stub1);
        if (helpItem != null) {
            helpItem.setVisible(mAccount != null
                    && mAccount.supportsCapability(AccountCapabilities.HELP_CONTENT));
        }
        if (sendFeedbackItem != null) {
            sendFeedbackItem.setVisible(mAccount != null
                    && mAccount.supportsCapability(AccountCapabilities.SEND_FEEDBACK));
        }
        if (attachFromServiceItem != null) {
            attachFromServiceItem.setVisible(shouldEnableAttachFromServiceMenu(mAccount));
        }

        // Show attach picture on pre-K devices.
        menu.findItem(R.id.add_photo_attachment).setVisible(!Utils.isRunningKitkatOrLater());

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();

        Analytics.getInstance().sendMenuItemEvent(Analytics.EVENT_CATEGORY_MENU_ITEM, id,
                "compose", 0);

        boolean handled = true;
        if (id == R.id.add_file_attachment) {
            doAttach(MIME_TYPE_ALL);
        } else if (id == R.id.add_photo_attachment) {
            doAttach(MIME_TYPE_PHOTO);
        } else if (id == R.id.save) {
            doSave(true);
        } else if (id == R.id.send) {
            doSend();
        } else if (id == R.id.discard) {
            doDiscard();
        } else if (id == R.id.settings) {
            Utils.showSettings(this, mAccount);
        } else if (id == android.R.id.home) {
            onAppUpPressed();
        } else if (id == R.id.help_info_menu_item) {
            Utils.showHelp(this, mAccount, getString(R.string.compose_help_context));
        } else {
            handled = false;
        }
        return handled || super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        /// M: Indicating back is pressed, exit compose.
        mBackPressed = true;
        // If we are showing the wait fragment, just exit.
        if (getWaitFragment() != null) {
            finish();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Carries out the "up" action in the action bar.
     */
    private void onAppUpPressed() {
        if (mLaunchedFromEmail) {
            // If this was started from Gmail, simply treat app up as the system back button, so
            // that the last view is restored.
            onBackPressed();
            return;
        }

        /// M: Indicating back is pressed, exit compose.
        mBackPressed = true;
        // Fire the main activity to ensure it launches the "top" screen of mail.
        // Since the main Activity is singleTask, it should revive that task if it was already
        // started.
        final Intent mailIntent = Utils.createViewInboxIntent(mAccount);
        mailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_TASK_ON_HOME);
        startActivity(mailIntent);
        finish();
    }

    private void doSend() {
        /// M: We Can't send in low storage state @{
        if (StorageLowState.checkIfStorageLow(this)) {
            LogUtils.logFeature(LogTag.SENDMAIL_TAG, "Can't send message due to low storage");
            return;
        }
        /// @}
        LogUtils.logFeature(LogTag.SENDMAIL_TAG, "ComposeActivity doSend message subject[%s]",
                mSubject != null ? mSubject.getText() : "");
        sendOrSaveWithSanityChecks(false, true, false, false);
        logSendOrSave(false /* save */);
        mPerformedSendOrDiscard = true;
    }

    private void doSave(boolean showToast) {
        sendOrSaveWithSanityChecks(true, showToast, false, false);
    }

    /// M: Replace with ChipsAddressTextView
//    @Override
//    public void onRecipientEntryItemClicked(int charactersTyped, int position) {
//        // Send analytics of characters typed and position in dropdown selected.
//        Analytics.getInstance().sendEvent(
//                "suggest_click", Integer.toString(charactersTyped), Integer.toString(position), 0);
//    }

    @VisibleForTesting
    public interface SendOrSaveCallback {
        void initializeSendOrSave();
        void notifyMessageIdAllocated(SendOrSaveMessage sendOrSaveMessage, Message message);
        long getMessageId();
        void sendOrSaveFinished(SendOrSaveMessage message, boolean success);
        /**M: actually, we need update attachment fields when it has changed, such as saved to db and the uri and
         *  contentUri should have value.
         * @param sendOrSaveMessage
         * @param attachments
         */
        void notifyAttachmentsIdAllocated(SendOrSaveMessage sendOrSaveMessage,
                List<Attachment> attachments);
    }

    private void runSendOrSaveProviderCalls(SendOrSaveMessage sendOrSaveMessage,
            SendOrSaveCallback callback, ReplyFromAccount currReplyFromAccount,
            ReplyFromAccount originalReplyFromAccount) {
        long messageId = callback.getMessageId();
        // If a previous draft has been saved, in an account that is different
        // than what the user wants to send from, remove the old draft, and treat this
        // as a new message
        if (originalReplyFromAccount != null
                && !currReplyFromAccount.account.uri.equals(originalReplyFromAccount.account.uri)) {
            if (messageId != UIProvider.INVALID_MESSAGE_ID) {
                ContentResolver resolver = getContentResolver();
                ContentValues values = new ContentValues();
                values.put(BaseColumns._ID, messageId);
                if (originalReplyFromAccount.account.expungeMessageUri != null) {
                    new ContentProviderTask.UpdateTask()
                            .run(resolver, originalReplyFromAccount.account.expungeMessageUri,
                                    values, null, null);
                } else {
                    // TODO(mindyp) delete the conversation.
                    LogUtils.d(LOG_TAG, "switch account, will reset message id to -1");
                }
                // reset messageId to 0, so a new message will be created
                messageId = UIProvider.INVALID_MESSAGE_ID;
            }
        }

        final long messageIdToSave = messageId;
        sendOrSaveMessage(callback, messageIdToSave, sendOrSaveMessage, currReplyFromAccount);

        if (!sendOrSaveMessage.mSave) {
            incrementRecipientsTimesContacted(
                    (String) sendOrSaveMessage.mValues.get(UIProvider.MessageColumns.TO),
                    (String) sendOrSaveMessage.mValues.get(UIProvider.MessageColumns.CC),
                    (String) sendOrSaveMessage.mValues.get(UIProvider.MessageColumns.BCC));
        }
        callback.sendOrSaveFinished(sendOrSaveMessage, true);
    }

    private void incrementRecipientsTimesContacted(
            final String toAddresses, final String ccAddresses, final String bccAddresses) {
        final List<String> recipients = Lists.newArrayList();
        addAddressesToRecipientList(recipients, toAddresses);
        addAddressesToRecipientList(recipients, ccAddresses);
        addAddressesToRecipientList(recipients, bccAddresses);
        incrementRecipientsTimesContacted(recipients);
    }

    private void addAddressesToRecipientList(
            final List<String> recipients, final String addressString) {
        if (recipients == null) {
            throw new IllegalArgumentException("recipientList cannot be null");
        }
        if (TextUtils.isEmpty(addressString)) {
            return;
        }
        final Rfc822Token[] tokens = Rfc822Tokenizer.tokenize(addressString);
        for (final Rfc822Token token : tokens) {
            recipients.add(token.getAddress());
        }
    }

    /**
     * Send or Save a message.
     */
    private void sendOrSaveMessage(SendOrSaveCallback callback, final long messageIdToSave,
            final SendOrSaveMessage sendOrSaveMessage, final ReplyFromAccount selectedAccount) {
        final ContentResolver resolver = getContentResolver();
        final boolean updateExistingMessage = messageIdToSave != UIProvider.INVALID_MESSAGE_ID;

        final String accountMethod = sendOrSaveMessage.mSave ?
                UIProvider.AccountCallMethods.SAVE_MESSAGE :
                UIProvider.AccountCallMethods.SEND_MESSAGE;

        try {
            Bundle result = null;
            if (updateExistingMessage) {
                sendOrSaveMessage.mValues.put(BaseColumns._ID, messageIdToSave);
            }

            result = callAccountSendSaveMethod(resolver,
                    selectedAccount.account, accountMethod, sendOrSaveMessage);
            Uri messageUri = null;
            if (result != null) {
                // If a non-null value was returned, then the provider handled the call
                // method
                messageUri = result.getParcelable(UIProvider.MessageColumns.URI);
            }
            /** M: Save the message and update messageId if needed. @{ */
            if (null != messageUri) {
                long id = Long.valueOf(messageUri.getLastPathSegment());
                if (id != mDraftId) {
                    LogUtils.d(LOG_TAG, "MessageId changed oldId %d, newId %d", mDraftId, id);
                    if (sendOrSaveMessage.mSave && messageUri != null) {
                        final Cursor messageCursor = resolver.query(messageUri,
                                UIProvider.MESSAGE_PROJECTION, null, null, null);
                        if (messageCursor != null) {
                            try {
                                if (messageCursor.moveToFirst()) {
                                    // Broadcast notification that a new message has
                                    // been allocated
                                    callback.notifyMessageIdAllocated(sendOrSaveMessage,
                                            new Message(messageCursor));
                                }
                            } finally {
                                messageCursor.close();
                            }
                        }
                    }
                }
            }
            /** @} */

            /// M: after saving message, the attachment's uri and contentUri could been changed, so update it. @{
            if (result != null) {
                List<Attachment> resultAttachments = Attachment.fromJSONArray(result
                        .getString(UIProvider.MessageColumns.ATTACHMENTS));
                if (resultAttachments.size() > 0) {
                    callback.notifyAttachmentsIdAllocated(sendOrSaveMessage, resultAttachments);
                }
            }
            /// @}
        } finally {
            // Close any opened file descriptors
            closeOpenedAttachmentFds(sendOrSaveMessage);
        }
    }

    private static void closeOpenedAttachmentFds(final SendOrSaveMessage sendOrSaveMessage) {
        final Bundle openedFds = sendOrSaveMessage.attachmentFds();
        if (openedFds != null) {
            final Set<String> keys = openedFds.keySet();
            for (final String key : keys) {
                final Object fd = openedFds.getParcelable(key);
                if (fd != null) {
                    try {
                        if (fd instanceof AssetFileDescriptor) {
                            /// M: for vcf attachment, the fd type is AssetFileDescriptor
                            ((AssetFileDescriptor) fd).close();
                            LogUtils.d(LOG_TAG, "close opened vcf attachment fds");
                        } else if (fd instanceof ParcelFileDescriptor) {
                            ((ParcelFileDescriptor) fd).close();
                        }
                    } catch (IOException e) {
                        // Do nothing
                    }
                }
            }
        }
    }

    /**
     * Use the {@link ContentResolver#call} method to send or save the message.
     *
     * If this was successful, this method will return an non-null Bundle instance
     */
    private static Bundle callAccountSendSaveMethod(final ContentResolver resolver,
            final Account account, final String method,
            final SendOrSaveMessage sendOrSaveMessage) {
        // Copy all of the values from the content values to the bundle
        final Bundle methodExtras = new Bundle(sendOrSaveMessage.mValues.size());
        final Set<Entry<String, Object>> valueSet = sendOrSaveMessage.mValues.valueSet();

        for (Entry<String, Object> entry : valueSet) {
            final Object entryValue = entry.getValue();
            final String key = entry.getKey();
            if (entryValue instanceof String) {
                methodExtras.putString(key, (String) entryValue);
            } else if (entryValue instanceof Boolean) {
                methodExtras.putBoolean(key, (Boolean) entryValue);
            } else if (entryValue instanceof Integer) {
                methodExtras.putInt(key, (Integer) entryValue);
            } else if (entryValue instanceof Long) {
                methodExtras.putLong(key, (Long) entryValue);
            } else {
                LogUtils.wtf(LOG_TAG, "Unexpected object type: %s",
                        entryValue.getClass().getName());
            }
        }

        // If the SendOrSaveMessage has some opened fds, add them to the bundle
        final Bundle fdMap = sendOrSaveMessage.attachmentFds();
        if (fdMap != null) {
            methodExtras.putParcelable(
                    UIProvider.SendOrSaveMethodParamKeys.OPENED_FD_MAP, fdMap);
        }

        return resolver.call(account.uri, method, account.uri.toString(), methodExtras);
    }

    /**
     * Reports recipients that have been contacted in order to improve auto-complete
     * suggestions. Default behavior updates usage statistics in ContactsProvider.
     * @param recipients addresses
     */
    protected void incrementRecipientsTimesContacted(List<String> recipients) {
        final DataUsageStatUpdater statsUpdater = new DataUsageStatUpdater(this);
        statsUpdater.updateWithAddress(recipients);
    }

    @VisibleForTesting
    public static class SendOrSaveMessage {
        final int mRequestId;
        final ContentValues mValues;
        final String mRefMessageId;
        @VisibleForTesting
        public final boolean mSave;
        /// M: Do not initialize attachment file descriptor at Main thread to avoid ANR @{
        private Bundle mAttachmentFds;
        private final Context mContext;
        private List<Attachment> mAttachments;
        /// M: @}

        public SendOrSaveMessage(Context context, int requestId, ContentValues values,
                String refMessageId, List<Attachment> attachments, Bundle optionalAttachmentFds,
                boolean save) {
            mContext = context; /// M: initialize context
            mRequestId = requestId;
            mValues = values;
            mRefMessageId = refMessageId;
            mSave = save;
            /// M: Do not initialize attachment file descriptor at Main thread to avoid ANR @{
            mAttachments = attachments;
            /// M: @}
            // If the attachments are already open for us (pre-JB), then don't open them again
            if (optionalAttachmentFds != null) {
                mAttachmentFds = optionalAttachmentFds;
            } else {
                mAttachmentFds = null;
            }
        }

        Bundle attachmentFds() {
            /// M: Do initialize attachment file descriptor when using it at work thread @{
            if (mAttachmentFds == null && mAttachments != null) {
                mAttachmentFds = initializeAttachmentFds(mContext, mAttachments);
            }
            /// M: @}
            return mAttachmentFds;
        }
    }

    /**
     * M:
     * Opens {@link ParcelFileDescriptor} for each of the attachments.  This method must be
     * called before the ComposeActivity finishes.
     * Note: The caller is responsible for closing these file descriptors.
     */
    private static Bundle initializeAttachmentFds(final Context context,
            final List<Attachment> attachments) {
        if (attachments == null || attachments.size() == 0) {
            return null;
        }

        final Bundle result = new Bundle(attachments.size());
        final ContentResolver resolver = context.getContentResolver();

        for (Attachment attachment : attachments) {
            if (attachment == null || Utils.isEmpty(attachment.contentUri)) {
                continue;
            }
            /// M: Don't open file descriptor for attachments locates in our db,
            // cause we would use its content uri rather than its fd to send, and
            // we may synchronize the content uri between EmailProvider and
            // AutoClearAttachments@{
            if (attachment.uri != null
                    && UIProvider.EMAIL_PROVIDER_AUTHORITY.equals(attachment.uri.getAuthority())) {
                continue;
            }
            /// @}

            ParcelFileDescriptor fileDescriptor = null;
            /// M: for vcf attachment, must using AssetFileDescriptor.
            AssetFileDescriptor vcfFileDescriptor = null;
            try {
                /// M: Don't re-open same contentUri @{
                if (result.getParcelable(attachment.contentUri.toString()) != null) {
                    continue;
                }
                /// @}
                if (attachment.contentUri.toString().startsWith(
                        UIProvider.ATTACHMENT_CONTACT_URI_PREFIX)) {
                    /// M: for vcf attachment, must open it as AssetFileDescriptor.
                    vcfFileDescriptor = resolver
                            .openAssetFileDescriptor(attachment.contentUri, "r");
                } else {
                    fileDescriptor = resolver.openFileDescriptor(attachment.contentUri, "r");
                }
            } catch (FileNotFoundException e) {
                LogUtils.e(LOG_TAG, e, "Exception attempting to open attachment");
                fileDescriptor = null;
            } catch (SecurityException e) {
                // We have encountered a security exception when attempting to open the file
                // specified by the content uri.  If the attachment has been cached, this
                // isn't a problem, as even through the original permission may have been
                // revoked, we have cached the file.  This will happen when saving/sending
                // a previously saved draft.
                // TODO(markwei): Expose whether the attachment has been cached through the
                // attachment object.  This would allow us to limit when the log is made, as
                // if the attachment has been cached, this really isn't an error
                LogUtils.e(LOG_TAG, e, "Security Exception attempting to open attachment");
                // Just set the file descriptor to null, as the underlying provider needs
                // to handle the file descriptor not being set.
                fileDescriptor = null;

                /// M: Catch IllegalArgumentException for contacts sharing from
                // Google account. There are some encoding error in those
                // contacts and sharing those contacts would get a exception,
                // this is a known Google issue, but not fixed, so need
                // application catch this exception by themselves. @{
            } catch (IllegalArgumentException e) {
                LogUtils.e(LOG_TAG, e, "Exception attempting to open attachment");
                fileDescriptor = null;
                /// @}
            }

            if (fileDescriptor != null) {
                result.putParcelable(attachment.contentUri.toString(), fileDescriptor);
            } else if (vcfFileDescriptor != null) {
                /// M: for vcf attachment, must using AssetFileDescriptor.
                result.putParcelable(attachment.contentUri.toString(), vcfFileDescriptor);
            }
        }

        return result;
    }

    /**
     * Get the to recipients.
     */
    public String[] getToAddresses() {
        return getAddressesFromList(mTo);
    }

    /**
     * Get the cc recipients.
     */
    public String[] getCcAddresses() {
        return getAddressesFromList(mCc);
    }

    /**
     * Get the bcc recipients.
     */
    public String[] getBccAddresses() {
        return getAddressesFromList(mBcc);
    }

    public String[] getAddressesFromList(MultiAutoCompleteTextView list) {
        if (list == null) {
            return new String[0];
        }
        Rfc822Token[] tokens = Rfc822Tokenizer.tokenize(list.getText());
        int count = tokens.length;
        String[] result = new String[count];
        for (int i = 0; i < count; i++) {
            result[i] = tokens[i].toString();
        }
        return result;
    }

    /**
     * Check for invalid email addresses.
     * @param to String array of email addresses to check.
     * @param wrongEmailsOut Emails addresses that were invalid.
     */
    public void checkInvalidEmails(final String[] to, final List<String> wrongEmailsOut) {
        if (mValidator == null) {
            return;
        }
        for (final String email : to) {
            if (!mValidator.isValid(email)) {
                wrongEmailsOut.add(email);
            }
        }
    }

    public static class RecipientErrorDialogFragment extends DialogFragment {
        // Public no-args constructor needed for fragment re-instantiation
        public RecipientErrorDialogFragment() {}

        public static RecipientErrorDialogFragment newInstance(final String message) {
            final RecipientErrorDialogFragment frag = new RecipientErrorDialogFragment();
            final Bundle args = new Bundle(1);
            args.putString("message", message);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final String message = getArguments().getString("message");
            return new AlertDialog.Builder(getActivity())
                    .setMessage(message)
                    .setPositiveButton(
                            R.string.ok, new Dialog.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ((ComposeActivity) getActivity()).finishRecipientErrorDialog();
                        }
                    }).create();
        }
    }

    private void finishRecipientErrorDialog() {
        // after the user dismisses the recipient error
        // dialog we want to make sure to refocus the
        // recipient to field so they can fix the issue
        // easily
        if (mTo != null) {
            mTo.requestFocus();
        }
    }

    /**
     * Show an error because the user has entered an invalid recipient.
     * @param message
     */
    private void showRecipientErrorDialog(final String message) {
        final DialogFragment frag = RecipientErrorDialogFragment.newInstance(message);
        frag.show(getFragmentManager(), "recipient error");
    }

    /**
     * Update the state of the UI based on whether or not the current draft
     * needs to be saved and the message is not empty.
     */
    public void updateSaveUi() {
        if (mSave != null) {
            mSave.setEnabled((isDraftDirty() && !isBlank()));
        }
    }

    /**
     * Returns true if the current draft is modified from the version we previously saved.
     */
    private boolean isDraftDirty() {
        synchronized (mDraftLock) {
            // The message should only be saved if:
            // It hasn't been sent AND
            // Some text has been added to the message OR
            // an attachment has been added or removed
            // AND there is actually something in the draft to save.
            return (mTextChanged || mAttachmentsChanged || mReplyFromChanged)
                    && !isBlank();
        }
    }

    /**
     * Returns whether the "Attach from Drive" menu item should be visible.
     */
    protected boolean shouldEnableAttachFromServiceMenu(Account mAccount) {
        return false;
    }

    /**
     * M: Return true if we need to discard the draft which has be edited as null.
     */
    private boolean shouldDisCard() {
        synchronized (mDraftLock) {
            return (mTextChanged || mAttachmentsChanged || mReplyFromChanged)
                    && isBlank();
        }
    }

    /**
     * M: Check if the recipient view has valid recipients
     * @param recipientView the recipient text view
     * @return true if the view has no valid recipient, else false
     */
    private boolean isRecipientEmpty(TextView recipientView) {
        CharSequence addressList = recipientView.getText();
        Address[] addresses = Address.parse(addressList != null ? addressList.toString().trim() : "");
        String address = Address.pack(addresses);
        if (TextUtils.isEmpty(address)) {
            return true;
        }
        return false;
    }

    /**
     * Check if all fields are blank.
     * @return boolean
     */
    public boolean isBlank() {
        // Need to check for null since isBlank() can be called from onPause()
        // before findViews() is called
        if (mSubject == null || mBodyView == null || mTo == null || mCc == null ||
                mAttachmentsView == null) {
            LogUtils.w(LOG_TAG, "null views in isBlank check");
            return true;
        }
        ///M: add Bcc Myself
        return mSubject.getText().length() == 0
                && (mBodyView.getText().length() == 0 || getSignatureStartPosition(mSignature,
                        mBodyView.getText().toString()) == 0)
                /// M: Use a common method to check the BCC view @{
                && isRecipientEmpty(mTo)
                && isRecipientEmpty(mCc) && isBccEmpty()
                /// @}
                && mAttachmentsView.getAttachments().size() == 0;
    }

    @VisibleForTesting
    protected int getSignatureStartPosition(String signature, String bodyText) {
        int startPos = -1;

        if (TextUtils.isEmpty(signature) || TextUtils.isEmpty(bodyText)) {
            return startPos;
        }

        int bodyLength = bodyText.length();
        int signatureLength = signature.length();
        String printableVersion = convertToPrintableSignature(signature);
        int printableLength = printableVersion.length();

        if (bodyLength >= printableLength
                && bodyText.substring(bodyLength - printableLength)
                .equals(printableVersion)) {
            startPos = bodyLength - printableLength;
        } else if (bodyLength >= signatureLength
                && bodyText.substring(bodyLength - signatureLength)
                .equals(signature)) {
            startPos = bodyLength - signatureLength;
        }
        return startPos;
    }

    /**
     * Allows any changes made by the user to be ignored. Called when the user
     * decides to discard a draft.
     */
    private void discardChanges() {
        mTextChanged = false;
        mAttachmentsChanged = false;
        mReplyFromChanged = false;
    }

    /**
     * @param save True to save, false to send
     * @param showToast True to show a toast once the message is sent/saved
     */
    protected void sendOrSaveWithSanityChecks(final boolean save, final boolean showToast,
            final boolean orientationChanged, final boolean autoSend) {
        if (mAccounts == null || mAccount == null) {
            Toast.makeText(this, R.string.send_failed, Toast.LENGTH_SHORT).show();
            if (autoSend) {
                finish();
            }
            return;
        }

        final String[] to, cc, bcc;
        if (orientationChanged) {
            to = cc = bcc = new String[0];
        } else {
            to = getToAddresses();
            cc = getCcAddresses();
            bcc = getBccAddresses();
        }

        final ArrayList<String> recipients = buildEmailAddressList(to);
        recipients.addAll(buildEmailAddressList(cc));
        recipients.addAll(buildEmailAddressList(bcc));

        // Don't let the user send to nobody (but it's okay to save a message
        // with no recipients)
        if (!save && (to.length == 0 && cc.length == 0 && bcc.length == 0)) {
            showRecipientErrorDialog(getString(R.string.recipient_needed));
            return;
        }

        List<String> wrongEmails = new ArrayList<String>();
        if (!save) {
            checkInvalidEmails(to, wrongEmails);
            checkInvalidEmails(cc, wrongEmails);
            checkInvalidEmails(bcc, wrongEmails);
        }

        // Don't let the user send an email with invalid recipients
        if (wrongEmails.size() > 0) {
            String errorText = String.format(getString(R.string.invalid_recipient),
                    wrongEmails.get(0));
            showRecipientErrorDialog(errorText);
            return;
        }

        if (!save) {
            if (autoSend) {
                // Skip all further checks during autosend. This flow is used by Android Wear
                // and Google Now.
                sendOrSave(save, showToast);
                return;
            }

            // Show a warning before sending only if there are no attachments, body, or subject.
            if (mAttachmentsView.getAttachments().isEmpty() && showEmptyTextWarnings()) {
                boolean warnAboutEmptySubject = isSubjectEmpty();
                boolean emptyBody = TextUtils.getTrimmedLength(mBodyView.getEditableText()) == 0;

                // A warning about an empty body may not be warranted when
                // forwarding mails, since a common use case is to forward
                // quoted text and not append any more text.
                boolean warnAboutEmptyBody = emptyBody && (!mForward || isBodyEmpty());

                // When we bring up a dialog warning the user about a send,
                // assume that they accept sending the message. If they do not,
                // the dialog listener is required to enable sending again.
                if (warnAboutEmptySubject) {
                    showSendConfirmDialog(R.string.confirm_send_message_with_no_subject,
                            showToast, recipients);
                    return;
                }

                if (warnAboutEmptyBody) {
                    showSendConfirmDialog(R.string.confirm_send_message_with_no_body,
                            showToast, recipients);
                    return;
                }
            }
            // Ask for confirmation to send.
            if (showSendConfirmation()) {
                showSendConfirmDialog(R.string.confirm_send_message, showToast, recipients);
                return;
            }
        }

        performAdditionalSendOrSaveSanityChecks(save, showToast, recipients);
    }

    /**
     * Returns a boolean indicating whether warnings should be shown for empty
     * subject and body fields
     *
     * @return True if a warning should be shown for empty text fields
     */
    protected boolean showEmptyTextWarnings() {
        return mAttachmentsView.getAttachments().size() == 0;
    }

    /**
     * Returns a boolean indicating whether the user should confirm each send
     *
     * @return True if a warning should be on each send
     */
    protected boolean showSendConfirmation() {
        return mCachedSettings != null && mCachedSettings.confirmSend;
    }

    public static class SendConfirmDialogFragment extends DialogFragment
            implements DialogInterface.OnClickListener {

        private static final String MESSAGE_ID = "messageId";
        private static final String SHOW_TOAST = "showToast";
        private static final String RECIPIENTS = "recipients";

        private boolean mShowToast;

        private ArrayList<String> mRecipients;

        // Public no-args constructor needed for fragment re-instantiation
        public SendConfirmDialogFragment() {}

        public static SendConfirmDialogFragment newInstance(final int messageId,
                final boolean showToast, final ArrayList<String> recipients) {
            final SendConfirmDialogFragment frag = new SendConfirmDialogFragment();
            final Bundle args = new Bundle(3);
            args.putInt(MESSAGE_ID, messageId);
            args.putBoolean(SHOW_TOAST, showToast);
            args.putStringArrayList(RECIPIENTS, recipients);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final int messageId = getArguments().getInt(MESSAGE_ID);
            mShowToast = getArguments().getBoolean(SHOW_TOAST);
            mRecipients = getArguments().getStringArrayList(RECIPIENTS);

            final int confirmTextId = (messageId == R.string.confirm_send_message) ?
                    R.string.ok : R.string.send;

            return new AlertDialog.Builder(getActivity())
                    .setMessage(messageId)
                    .setPositiveButton(confirmTextId, this)
                    .setNegativeButton(R.string.cancel, null)
                    .create();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                ((ComposeActivity) getActivity()).finishSendConfirmDialog(mShowToast, mRecipients);
            }
        }
    }

    private void finishSendConfirmDialog(
            final boolean showToast, final ArrayList<String> recipients) {
        performAdditionalSendOrSaveSanityChecks(false /* save */, showToast, recipients);
    }

    // The list of recipients are used by the additional sendOrSave checks.
    // However, the send confirm dialog may be shown before performing
    // the additional checks. As a result, we need to plumb the recipient
    // list through the send confirm dialog so that
    // performAdditionalSendOrSaveChecks can be performed properly.
    private void showSendConfirmDialog(final int messageId,
            final boolean showToast, final ArrayList<String> recipients) {
        final DialogFragment frag = SendConfirmDialogFragment.newInstance(
                messageId, showToast, recipients);
        frag.show(getFragmentManager(), "send confirm");
    }

    /**
     * Returns whether the ComposeArea believes there is any text in the body of
     * the composition. TODO: When ComposeArea controls the Body as well, add
     * that here.
     */
    public boolean isBodyEmpty() {
        return !mQuotedTextView.isTextIncluded();
    }

    /**
     * Test to see if the subject is empty.
     *
     * @return boolean.
     */
    // TODO: this will likely go away when composeArea.focus() is implemented
    // after all the widget control is moved over.
    public boolean isSubjectEmpty() {
        return TextUtils.getTrimmedLength(mSubject.getText()) == 0;
    }

    @VisibleForTesting
    public String getSubject() {
        return mSubject.getText().toString();
    }

    private void sendOrSaveInternal(Context context, int requestId,
            ReplyFromAccount currReplyFromAccount, ReplyFromAccount originalReplyFromAccount,
            Message message, Message refMessage, CharSequence quotedText,
            SendOrSaveCallback callback, boolean save, int composeMode, ContentValues extraValues,
            Bundle optionalAttachmentFds) {
        final ContentValues values = new ContentValues();

        final String refMessageId = refMessage != null ? refMessage.uri.toString() : "";

        MessageModification.putToAddresses(values, message.getToAddresses());
        MessageModification.putCcAddresses(values, message.getCcAddresses());
        MessageModification.putBccAddresses(values, message.getBccAddresses());
        MessageModification.putCustomFromAddress(values, message.getFrom());

        MessageModification.putSubject(values, message.subject);

        // bodyHtml already have the composing spans removed.
        final String htmlBody = message.bodyHtml;
        final String textBody = message.bodyText;
        // fullbodyhtml/fullbodytext will contain the actual body plus the quoted text.
        String fullBodyHtml = htmlBody;
        String fullBodyText = textBody;
        String quotedString = null;
        final boolean hasQuotedText = !TextUtils.isEmpty(quotedText);
        if (hasQuotedText) {
            // The quoted text is HTML at this point.
            quotedString = quotedText.toString();
            fullBodyHtml = htmlBody + quotedString;
            fullBodyText = textBody + Utils.convertHtmlToPlainText(quotedString);
            MessageModification.putForward(values, composeMode == ComposeActivity.FORWARD);
            MessageModification.putAppendRefMessageContent(values, true /* include quoted */);
        }

        // Only take refMessage into account if either one of its html/text is not empty.
        int quotedTextPos = -1;
        if (refMessage != null && !(TextUtils.isEmpty(refMessage.bodyHtml) &&
                TextUtils.isEmpty(refMessage.bodyText))) {
            // The code below might need to be revisited. The quoted text position is different
            // between text/html and text/plain parts and they should be stored seperately and
            // the right version should be used in the UI. text/html should have preference
            // if both exist.  Issues like this made me file b/14256940 to make sure that we
            // properly handle the existing of both text/html and text/plain parts and to verify
            // that we are not making some assumptions that break if there is no text/html part.
            if (!TextUtils.isEmpty(refMessage.bodyHtml)) {
                MessageModification.putBodyHtml(values, fullBodyHtml);
                if (hasQuotedText) {
                    quotedTextPos = htmlBody.length() +
                            QuotedTextView.getQuotedTextOffset(quotedString);
                }
            }
            if (!TextUtils.isEmpty(refMessage.bodyText)) {
                MessageModification.putBody(values, fullBodyText);
                if (hasQuotedText && (quotedTextPos == -1)) {
                    quotedTextPos = textBody.length();
                }
            }
            if (quotedTextPos != -1) {
                // The quoted text pos is the text/html version first and the text/plan version
                // if there is no text/html part. The reason for this is because preference
                // is given to text/html in the compose window if it exists. In the future, we
                // should calculate the index for both since the user could choose to compose
                // explicitly in text/plain.
                MessageModification.putQuoteStartPos(values, quotedTextPos);
            }
        } else {
            MessageModification.putBodyHtml(values, fullBodyHtml);
            MessageModification.putBody(values, fullBodyText);
        }
        int draftType = getDraftType(composeMode);
        MessageModification.putDraftType(values, draftType);
        MessageModification.putAttachments(values, message.getAttachments());
        if (!TextUtils.isEmpty(refMessageId)) {
            MessageModification.putRefMessageId(values, refMessageId);
        }
        if (extraValues != null) {
            values.putAll(extraValues);
        }

        SendOrSaveMessage sendOrSaveMessage = new SendOrSaveMessage(context, requestId,
                values, refMessageId, message.getAttachments(), optionalAttachmentFds, save);

        runSendOrSaveProviderCalls(sendOrSaveMessage, callback,
                currReplyFromAccount, originalReplyFromAccount);

        LogUtils.i(LOG_TAG, "[compose] SendOrSaveMessage [%s] posted (isSave: %s) - " +
                "bodyHtml length: %d, bodyText length: %d, quoted text pos: %d, attach count: %d",
                requestId, save, message.bodyHtml.length(), message.bodyText.length(),
                quotedTextPos, message.getAttachmentCount(true));
    }

    /**
     * Removes any composing spans from the specified string.  This will create a new
     * SpannableString instance, as to not modify the behavior of the EditText view.
     */
    private static SpannableString removeComposingSpans(Spanned body) {
        final SpannableString messageBody = new SpannableString(body);
        BaseInputConnection.removeComposingSpans(messageBody);

        // Remove watcher spans while we're at it, so any off-UI thread manipulation of these
        // spans doesn't trigger unexpected side-effects. This copy is essentially 100% detached
        // from the EditText.
        //
        // (must remove SpanWatchers first to avoid triggering them as we remove other spans)
        removeSpansOfType(messageBody, SpanWatcher.class);
        removeSpansOfType(messageBody, TextWatcher.class);

        return messageBody;
    }

    private static void removeSpansOfType(SpannableString str, Class<?> cls) {
        for (Object span : str.getSpans(0, str.length(), cls)) {
            str.removeSpan(span);
        }
    }

    private static int getDraftType(int mode) {
        int draftType = -1;
        switch (mode) {
            case ComposeActivity.COMPOSE:
                draftType = DraftType.COMPOSE;
                break;
            case ComposeActivity.REPLY:
                draftType = DraftType.REPLY;
                break;
            case ComposeActivity.REPLY_ALL:
                draftType = DraftType.REPLY_ALL;
                break;
            case ComposeActivity.FORWARD:
                draftType = DraftType.FORWARD;
                break;
        }
        return draftType;
    }

    /**
     * Derived classes should override this step to perform additional checks before
     * send or save. The default implementation simply calls {@link #sendOrSave(boolean, boolean)}.
     */
    protected void performAdditionalSendOrSaveSanityChecks(
            final boolean save, final boolean showToast, ArrayList<String> recipients) {
        sendOrSave(save, showToast);
    }

    protected void sendOrSave(final boolean save, final boolean showToast) {
        // Check if user is a monkey. Monkeys can compose and hit send
        // button but are not allowed to send anything off the device.
        /* M: disable it for mtbf
        if (ActivityManager.isUserAMonkey()) {
            LogUtils.logFeature(LogTag.SENDMAIL_TAG, "send mail abort, for running Monkey");
            return;
        } */
    	// A:ADD BUG ID :DWYSLM-131 liupeng 20160315 {
    	prefs = Preferences.getPreferences(this);
    	Spanned mbody = null;
    	LogUtils.d("qinzhaojin", "prefs.getConfirmDeleteSignature() ==== " + prefs.getConfirmDeleteSignature(this));
		if(prefs.getConfirmDeleteSignature(this)){
			String mConstantSignature = this.getResources().getString(R.string.constant_default_signature);
			StringBuilder builder = new StringBuilder(mBodyView.getEditableText().toString());
			builder.append("\r\n");
			builder.append("\r\n");
			builder.append(mConstantSignature);
			EditText editText = new EditText(this);
			editText.setText(builder.toString());
			mbody = editText.getEditableText();
		}
		//A}
        final SendOrSaveCallback callback = new SendOrSaveCallback() {
            @Override
            public void initializeSendOrSave() {
                final Intent i = new Intent(ComposeActivity.this, EmptyService.class);

                // API 16+ allows for setClipData. For pre-16 we are going to open the fds
                // on the main thread.
                if (Utils.isRunningJellybeanOrLater()) {
                    // Grant the READ permission for the attachments to the service so that
                    // as long as the service stays alive we won't hit PermissionExceptions.
                    final ClipDescription desc = new ClipDescription("attachment_uris",
                            new String[]{ClipDescription.MIMETYPE_TEXT_URILIST});
                    ClipData clipData = null;
                    for (Attachment a : mAttachmentsView.getAttachments()) {
                        if (a != null && !Utils.isEmpty(a.contentUri)) {
                            final ClipData.Item uriItem = new ClipData.Item(a.contentUri);
                            if (clipData == null) {
                                clipData = new ClipData(desc, uriItem);
                            } else {
                                clipData.addItem(uriItem);
                            }
                        }
                    }
                    i.setClipData(clipData);
                    i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }

                synchronized (PENDING_SEND_OR_SAVE_TASKS_NUM) {
                    if (PENDING_SEND_OR_SAVE_TASKS_NUM.getAndAdd(1) == 0) {
                        // Start service so we won't be killed if this app is
                        // put in the background.
                        startService(i);
                    }
                }
                if (sTestSendOrSaveCallback != null) {
                    sTestSendOrSaveCallback.initializeSendOrSave();
                }
            }

            /*
             * M: see com.android.mail.compose.ComposeActivity
             * .SendOrSaveCallback.notifyAttachmentsIdAllocated
             */
            @Override
            public void notifyAttachmentsIdAllocated(final SendOrSaveMessage sendOrSaveMessage,
                    final List<Attachment> updatedAtts) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (null == mAttachmentsView || isDestroyed()) {
                            return;
                        }

                        /**
                         * M: Just update the attachments who are to save for the first time,
                         * do nothing about attachments that previous saved.
                         */
                        boolean needUpdateAttachmentsView = false;
                        List<Attachment> uiAtts = new ArrayList<Attachment>();
                        uiAtts.addAll(mAttachmentsView.getAttachments());
                        List<Attachment> needUpdatedAtts = new ArrayList<Attachment>();

                        // If message id changed, remove previous message's attachments from UI
                        long messageId = UIProvider.INVALID_MESSAGE_ID;
                        if (null != sendOrSaveMessage.mValues
                                && sendOrSaveMessage.mValues.containsKey(BaseColumns._ID)) {
                            messageId = sendOrSaveMessage.mValues.getAsLong(BaseColumns._ID);
                            if (mDraftId != UIProvider.INVALID_MESSAGE_ID
                                    && messageId != mDraftId) {
                                int cnt = 0;
                                for (Attachment att : sendOrSaveMessage.mAttachments) {
                                    if (!(null == att.uri
                                            || TextUtils.isEmpty(att.uri.toString()))) {
                                        uiAtts.remove(att);
                                        cnt++;
                                    }
                                }
                                needUpdateAttachmentsView = cnt > 0;
                                LogUtils.d(LOG_TAG, "Remove %d previous msg's attachment", cnt);
                            }
                        }

                        // Find the attachments need update and exclude previous saved attachments
                        for (Attachment att : sendOrSaveMessage.mAttachments) {
                            if (null == att.uri || TextUtils.isEmpty(att.uri.toString())) {
                                needUpdatedAtts.add(att);
                            } else {
                                updatedAtts.remove(att);
                            }
                        }
                        LogUtils.d(LOG_TAG, "needUpdatedAtts %d, Updated %d",
                                needUpdatedAtts.size(), updatedAtts.size());

                        // Update the attachments
                        if (needUpdatedAtts.size() == updatedAtts.size() && updatedAtts.size() > 0){
                            int index = -1;
                            int cnt = 0;
                            for (int i = 0; i < needUpdatedAtts.size(); i++) {
                                index = uiAtts.indexOf(needUpdatedAtts.get(i));
                                if (index >= 0) {// Attachment exists
                                    uiAtts.remove(index);
                                    uiAtts.add(index, updatedAtts.get(i));
                                    cnt++;
                                }
                            }
                            needUpdateAttachmentsView |= cnt > 0;
                            LogUtils.d(LOG_TAG, "Update Attachment count " + cnt);
                        }

                        // if attachments updated, we refresh UI.
                        LogUtils.d(LOG_TAG, "UpdateAttachmentsView " + needUpdateAttachmentsView);
                        if (needUpdateAttachmentsView) {
                            mAttachmentsView.deleteAllAttachments();
                            for (Attachment att : uiAtts) {
                                mAttachmentsView.addAttachment(att);
                            }
                        }
                    }
                });
            }

            @Override
            public void notifyMessageIdAllocated(SendOrSaveMessage sendOrSaveMessage,
                    Message message) {
                synchronized (mDraftLock) {
                    mDraftId = message.id;
                    mDraft = message;
                    LogUtils.d(LOG_TAG,
                            "notifyMessageIdAllocated : new Message Uri [%s]",
                            mDraft != null ? mDraft.uri : "null");
                    if (sRequestMessageIdMap != null) {
                        sRequestMessageIdMap.put(sendOrSaveMessage.mRequestId, mDraftId);
                    }
                    // Cache request message map, in case the process is killed
                    saveRequestMap();
                }
                if (sTestSendOrSaveCallback != null) {
                    sTestSendOrSaveCallback.notifyMessageIdAllocated(sendOrSaveMessage, message);
                }
            }

            @Override
            public long getMessageId() {
                synchronized (mDraftLock) {
                    return mDraftId;
                }
            }

            @Override
            public void sendOrSaveFinished(SendOrSaveMessage message, boolean success) {
                // Update the last sent from account.
                if (mAccount != null) {
                    MailAppProvider.getInstance().setLastSentFromAccount(mAccount.uri.toString());
                }
                if (success) {
                    // Successfully sent or saved so reset change markers
                    /// M: We had discard changes early, see comment when we start to save message.
//                    discardChanges();
                    LogUtils.d(LOG_TAG, "Message save successfully without doing discard.");
                } else {
                    // A failure happened with saving/sending the draft
                    // TODO(pwestbro): add a better string that should be used
                    // when failing to send or save
                    Toast.makeText(ComposeActivity.this, R.string.send_failed, Toast.LENGTH_SHORT)
                            .show();
                }

                synchronized (PENDING_SEND_OR_SAVE_TASKS_NUM) {
                    if (PENDING_SEND_OR_SAVE_TASKS_NUM.addAndGet(-1) == 0) {
                        // Stop service so we can be killed.
                        stopService(new Intent(ComposeActivity.this, EmptyService.class));
                    }
                }
                if (sTestSendOrSaveCallback != null) {
                    sTestSendOrSaveCallback.sendOrSaveFinished(message, success);
                }
            }
        };
        setAccount(mReplyFromAccount.account);
		//M:ADD BUG ID :DWYSLM-131 liupeng 20160315{
        final Spanned body = prefs.getConfirmDeleteSignature(this) ? mbody : removeComposingSpans(mBodyView.getText());
        //M}
	    callback.initializeSendOrSave();

        // For pre-JB we need to open the fds on the main thread
        final Bundle attachmentFds;
        if (!Utils.isRunningJellybeanOrLater()) {
            attachmentFds = initializeAttachmentFds(this, mAttachmentsView.getAttachments());
        } else {
            attachmentFds = null;
        }

        // Generate a unique message id for this request
        mRequestId = sRandom.nextInt();

        /// M: save the content of quoted textview to a final variable to avoid
        // NPE. @{
        final CharSequence quotedText = mQuotedTextView.getQuotedTextIfIncluded();
        SEND_SAVE_TASK_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                final Message msg = createMessage(mReplyFromAccount, mRefMessage, getMode(), body);
                sendOrSaveInternal(ComposeActivity.this, mRequestId, mReplyFromAccount,
                        mDraftAccount, msg, mRefMessage, quotedText,
                        callback, save, mComposeMode, mExtraValues, attachmentFds);
            }
        });
        /// @}

        /// M: Why we decide to discard the changes early is because: When we get here,
        /// there will be only two results for saving message: success or fail
        /// 1.if the message save successfully, then it sames to be the same for us to
        /// discard the changes early or late. but the benift is that we can keep the
        /// changes made during saving the message.
        /// 2.if the message save fail which means the program had gone wrong or crash.
        /// Then the changes is meaningless to us.
        discardChanges();

        // Don't display the toast if the user is just changing the orientation,
        // but we still need to save the draft to the cursor because this is how we restore
        // the attachments when the configuration change completes.
        if (showToast && (getChangingConfigurations() & ActivityInfo.CONFIG_ORIENTATION) == 0) {
            Toast.makeText(this, save ? R.string.message_saved : R.string.sending_message,
                    Toast.LENGTH_LONG).show();
        }

        // Need to update variables here because the send or save completes
        // asynchronously even though the toast shows right away.
        discardChanges();
        updateSaveUi();

        // If we are sending, finish the activity
        if (!save) {
            finish();
        }
    }

    /**
     * Save the state of the request messageid map. This allows for the Gmail
     * process to be killed, but and still allow for ComposeActivity instances
     * to be recreated correctly.
     */
    private void saveRequestMap() {
        // TODO: store the request map in user preferences.
    }

    @SuppressLint("NewApi")
    private void doAttach(String type) {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        i.setType(type);
        mAddingAttachment = true;
        startActivityForResult(Intent.createChooser(i, getText(R.string.select_attachment_type)),
                RESULT_PICK_ATTACHMENT);
    }

    private void showCcBccViews() {
        mCcBccView.show(true, true, true);
        if (mCcBccButton != null) {
            mCcBccButton.setVisibility(View.GONE);
        }
    }

    private static String getActionString(int action) {
        final String msgType;
        switch (action) {
            case COMPOSE:
                msgType = "new_message";
                break;
            case REPLY:
                msgType = "reply";
                break;
            case REPLY_ALL:
                msgType = "reply_all";
                break;
            case FORWARD:
                msgType = "forward";
                break;
            default:
                msgType = "unknown";
                break;
        }
        return msgType;
    }

    private void logSendOrSave(boolean save) {
        if (!Analytics.isLoggable() || mAttachmentsView == null) {
            return;
        }

        final String category = (save) ? "message_save" : "message_send";
        final int attachmentCount = getAttachments().size();
        final String msgType = getActionString(mComposeMode);
        final String label;
        final long value;
        if (mComposeMode == COMPOSE) {
            label = Integer.toString(attachmentCount);
            value = attachmentCount;
        } else {
            label = null;
            value = 0;
        }
        Analytics.getInstance().sendEvent(category, msgType, label, value);
    }

    @Override
    public boolean onNavigationItemSelected(int position, long itemId) {
        int initialComposeMode = mComposeMode;
        if (position == ComposeActivity.REPLY) {
            mComposeMode = ComposeActivity.REPLY;
        } else if (position == ComposeActivity.REPLY_ALL) {
            mComposeMode = ComposeActivity.REPLY_ALL;
        } else if (position == ComposeActivity.FORWARD) {
            mComposeMode = ComposeActivity.FORWARD;
        }
        clearChangeListeners();
        if (initialComposeMode != mComposeMode) {
            resetMessageForModeChange();
            if (mRefMessage != null) {
                setFieldsFromRefMessage(mComposeMode);
            }
            boolean showCc = false;
            boolean showBcc = false;
            if (mDraft != null) {
                // Following desktop behavior, if the user has added a BCC
                // field to a draft, we show it regardless of compose mode.
                showBcc = !TextUtils.isEmpty(mDraft.getBcc());
                // Use the draft to determine what to populate.
                // If the Bcc field is showing, show the Cc field whether it is populated or not.
                showCc = showBcc
                        || (!TextUtils.isEmpty(mDraft.getCc()) && mComposeMode == REPLY_ALL);
            }
            if (mRefMessage != null) {
                showCc = !TextUtils.isEmpty(mCc.getText());
                showBcc = !TextUtils.isEmpty(mBcc.getText());
            }
            mCcBccView.show(false /* animate */, showCc, showBcc);
            /// M: Auto Add Bcc Myself
            addBccMyself(mReplyFromAccount);
        }
        updateHideOrShowCcBcc();
        initChangeListeners();
        return true;
    }

    @VisibleForTesting
    protected void resetMessageForModeChange() {
        // When switching between reply, reply all, forward,
        // follow the behavior of webview.
        // The contents of the following fields are cleared
        // so that they can be populated directly from the
        // ref message:
        // 1) Any recipient fields
        // 2) The subject
        mTo.setText("");
        mCc.setText("");
        mBcc.setText("");
        // Any edits to the subject are replaced with the original subject.
        mSubject.setText("");

        // M: follow L default design.
        // Any changes to the contents of the following fields are kept:
        // 1) Body
        // 2) Attachments
        // If the user made changes to attachments, keep their changes.
        if (!mAttachmentsChanged) {
            mAttachmentsView.deleteAllAttachments();
        }
    }

    private class ComposeModeAdapter extends ArrayAdapter<String> {

        private Context mContext;
        private LayoutInflater mInflater;

        public ComposeModeAdapter(Context context) {
            super(context, R.layout.compose_mode_item, R.id.mode, getResources()
                    .getStringArray(R.array.compose_modes));
            mContext = context;
        }

        private LayoutInflater getInflater() {
            if (mInflater == null) {
                mInflater = LayoutInflater.from(mContext);
            }
            return mInflater;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getInflater().inflate(R.layout.compose_mode_display_item, null);
            }
            ((TextView) convertView.findViewById(R.id.mode)).setText(getItem(position));
            return super.getView(position, convertView, parent);
        }
    }

    @Override
    public void onRespondInline(String text) {
        appendToBody(text, false);
        mQuotedTextView.setUpperDividerVisible(false);
        mRespondedInline = true;
        if (!mBodyView.hasFocus()) {
            mBodyView.requestFocus();
        }
    }

    /**
     * Append text to the body of the message. If there is no existing body
     * text, just sets the body to text.
     *
     * @param text Text to append
     * @param withSignature True to append a signature.
     */
    public void appendToBody(CharSequence text, boolean withSignature) {
        Editable bodyText = mBodyView.getEditableText();
        if (bodyText != null && bodyText.length() > 0) {
            bodyText.append(text);
        } else {
            setBody(text, withSignature);
        }
    }

    /**
     * Set the body of the message.
     * Please try to exclusively use this method instead of calling mBodyView.setText(..) directly.
     *
     * @param text text to set
     * @param withSignature True to append a signature.
     */
    public void setBody(CharSequence text, boolean withSignature) {
        LogUtils.i(LOG_TAG, "Body populated, len: %d, sig: %b", text.length(), withSignature);
        mBodyView.setText(text);
        if (withSignature) {
            appendSignature();
        }
    }

    private void appendSignature() {
        final String newSignature = mCachedSettings != null ? mCachedSettings.signature : null;
        final int signaturePos = getSignatureStartPosition(mSignature, mBodyView.getText().toString());
        if (!TextUtils.equals(newSignature, mSignature) || signaturePos < 0) {
            mSignature = newSignature;
            if (!TextUtils.isEmpty(mSignature)) {
                // Appending a signature does not count as changing text.
                mBodyView.removeTextChangedListener(this);
                mBodyView.append(convertToPrintableSignature(mSignature));
                mBodyView.addTextChangedListener(this);
            }
            resetBodySelection();
        }
    }

    private String convertToPrintableSignature(String signature) {
        String signatureResource = getResources().getString(R.string.signature);
        if (signature == null) {
            signature = "";
        }
        return String.format(signatureResource, signature);
    }

    @Override
    public void onAccountChanged() {
        mReplyFromAccount = mFromSpinner.getCurrentAccount();
        if (!mAccount.equals(mReplyFromAccount.account)) {
            // Clear a signature, if there was one.
            mBodyView.removeTextChangedListener(this);
            String oldSignature = mSignature;
            String bodyText = getBody().getText().toString();
            if (!TextUtils.isEmpty(oldSignature)) {
                int pos = getSignatureStartPosition(oldSignature, bodyText);
                if (pos > -1) {
                    setBody(bodyText.substring(0, pos), false);
                }
            }
            ///M: Add Bcc Myself
            addBccMyself(mReplyFromAccount);
            setAccount(mReplyFromAccount.account);
            mBodyView.addTextChangedListener(this);
            // TODO: handle discarding attachments when switching accounts.
            // Only enable save for this draft if there is any other content
            // in the message.
            if (!isBlank()) {
                enableSave(true);
            }
            mReplyFromChanged = true;
            initRecipients();

            invalidateOptionsMenu();

            /// M: Refresh the Sender UI
            refreshSenderUI();
        }
    }

    public void enableSave(boolean enabled) {
        if (mSave != null) {
            mSave.setEnabled(enabled);
        }
    }

    public static class DiscardConfirmDialogFragment extends DialogFragment {
        // Public no-args constructor needed for fragment re-instantiation
        public DiscardConfirmDialogFragment() {}

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.confirm_discard_text)
                    .setPositiveButton(R.string.discard,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ((ComposeActivity) getActivity()).doDiscardWithoutConfirmation();
                                }
                            })
                    .setNegativeButton(R.string.cancel, null)
                    .create();
        }
    }

    private void doDiscard() {
        // Only need to ask for confirmation if the draft is in a dirty state.
        if (isDraftDirty()) {
            final DialogFragment frag = new DiscardConfirmDialogFragment();
            frag.show(getFragmentManager(), "discard confirm");
        } else {
            doDiscardWithoutConfirmation();
        }
    }

    /**
     * Effectively discard the current message.
     *
     * This method is either invoked from the menu or from the dialog
     * once the user has confirmed that they want to discard the message.
     */
    private void doDiscardWithoutConfirmation() {
        synchronized (mDraftLock) {
            if (mDraftId != UIProvider.INVALID_MESSAGE_ID) {
                ContentValues values = new ContentValues();
                values.put(BaseColumns._ID, mDraftId);
                if (!mAccount.expungeMessageUri.equals(Uri.EMPTY)) {
                    getContentResolver().update(mAccount.expungeMessageUri, values, null, null);
                } else {
                    /**
                     * M: Any case of draft without uri, but has a valid id ?
                     * Add some log for this case. @{
                     */
                    if (!Uri.EMPTY.equals(mDraft.uri) && mDraft.uri != null) {
                        getContentResolver().delete(mDraft.uri, null, null);
                    } else {
                        LogUtils.w(LOG_TAG,
                                "doDiscardWithoutConfirmation failed, message [%s] without uri",
                                mDraftId);
                    }
                    /** @} */
                }
                // This is not strictly necessary (since we should not try to
                // save the draft after calling this) but it ensures that if we
                // do save again for some reason we make a new draft rather than
                // trying to resave an expunged draft.
                mDraftId = UIProvider.INVALID_MESSAGE_ID;
            }
        }

        // Display a toast to let the user know
        Toast.makeText(this, R.string.message_discarded, Toast.LENGTH_SHORT).show();

        // This prevents the draft from being saved in onPause().
        discardChanges();
        mPerformedSendOrDiscard = true;
        finish();
    }

    private void saveIfNeeded() {
        if (mAccount == null) {
            // We have not chosen an account yet so there's no way that we can save. This is ok,
            // though, since we are saving our state before AccountsActivity is activated. Thus, the
            // user has not interacted with us yet and there is no real state to save.
            return;
        }

        /** M: Discard the draft when edited it as null. @{ */
        if (mBackPressed && shouldDisCard()) {
            doDiscardWithoutConfirmation();
            return;
        }
        /** @} */
        if (isDraftDirty()) {
            doSave(!mAddingAttachment /* show toast */);
        }
    }

    @Override
    public void onAttachmentDeleted() {
        mAttachmentsChanged = true;
        // If we are showing any attachments, make sure we have an upper
        // divider.
        mQuotedTextView.setUpperDividerVisible(mAttachmentsView.getAttachments().size() > 0);
        updateSaveUi();
    }

    @Override
    public void onAttachmentAdded() {
        mQuotedTextView.setUpperDividerVisible(mAttachmentsView.getAttachments().size() > 0);
        mAttachmentsView.focusLastAttachment();
    }

    /**
     * This is called any time one of our text fields changes.
     */
    @Override
    public void afterTextChanged(Editable s) {
        /** M: Adjust the text is really changed or not.@{ */
        if (!TextUtils.equals(mBeforeCharSequence, s.toString())) {
            LogUtils.d(LOG_TAG, "CharSequence has changed really.");
            mTextChanged = true;
        }
        /** @} */
        updateSaveUi();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        /// M: Record the CharSequence before text changed.
        mBeforeCharSequence = s.toString();
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // Do nothing.
    }


    // There is a big difference between the text associated with an address changing
    // to add the display name or to format properly and a recipient being added or deleted.
    // Make sure we only notify of changes when a recipient has been added or deleted.
    private class RecipientTextWatcher implements TextWatcher {
        private HashMap<String, Integer> mContent = new HashMap<String, Integer>();

        private MultiAutoCompleteTextView mView;

        private TextWatcher mListener;

        public RecipientTextWatcher(MultiAutoCompleteTextView view, TextWatcher listener) {
            mView = view;
            mListener = listener;
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (hasChanged()) {
                mListener.afterTextChanged(s);
            }
        }

        private boolean hasChanged() {
            /**M:Add Bcc Myself @{*/
            if (mAddBccBySetting && mView.equals(mBcc)) {
                mAddBccBySetting = false;
                return false;
            }
            /**@} */
            final ArrayList<String> currRecips = buildEmailAddressList(getAddressesFromList(mView));
            int totalCount = currRecips.size();
            int totalPrevCount = 0;
            for (Entry<String, Integer> entry : mContent.entrySet()) {
                totalPrevCount += entry.getValue();
            }
            if (totalCount != totalPrevCount) {
                return true;
            }

            for (String recip : currRecips) {
                if (!mContent.containsKey(recip)) {
                    return true;
                } else {
                    int count = mContent.get(recip) - 1;
                    if (count < 0) {
                        return true;
                    } else {
                        mContent.put(recip, count);
                    }
                }
            }
            return false;
        }

        private String[] tokenizeRecips(String[] recips) {
            // Tokenize them all and put them in the list.
            String[] recipAddresses = new String[recips.length];
            for (int i = 0; i < recips.length; i++) {
                recipAddresses[i] = Rfc822Tokenizer.tokenize(recips[i])[0].getAddress();
            }
            return recipAddresses;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            final ArrayList<String> recips = buildEmailAddressList(getAddressesFromList(mView));
            for (String recip : recips) {
                if (!mContent.containsKey(recip)) {
                    mContent.put(recip, 1);
                } else {
                    mContent.put(recip, (mContent.get(recip)) + 1);
                }
            }
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // Do nothing.
        }
    }

    /**
     * Returns a list of email addresses from the recipients. List only contains
     * email addresses strips additional info like the recipient's name.
     */
    private static ArrayList<String> buildEmailAddressList(String[] recips) {
        // Tokenize them all and put them in the list.
        final ArrayList<String> recipAddresses = Lists.newArrayListWithCapacity(recips.length);
        for (int i = 0; i < recips.length; i++) {
            recipAddresses.add(Rfc822Tokenizer.tokenize(recips[i])[0].getAddress());
        }
        return recipAddresses;
    }

    public static void registerTestSendOrSaveCallback(SendOrSaveCallback testCallback) {
        if (sTestSendOrSaveCallback != null && testCallback != null) {
            throw new IllegalStateException("Attempting to register more than one test callback");
        }
        sTestSendOrSaveCallback = testCallback;
    }

    @VisibleForTesting
    protected ArrayList<Attachment> getAttachments() {
        return mAttachmentsView.getAttachments();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case INIT_DRAFT_USING_REFERENCE_MESSAGE:
                return new CursorLoader(this, mRefMessageUri, UIProvider.MESSAGE_PROJECTION, null,
                        null, null);
            case REFERENCE_MESSAGE_LOADER:
                return new CursorLoader(this, mRefMessageUri, UIProvider.MESSAGE_PROJECTION, null,
                        null, null);
            case LOADER_ACCOUNT_CURSOR:
                return new CursorLoader(this, MailAppProvider.getAccountsUri(),
                        UIProvider.ACCOUNTS_PROJECTION, null, null, null);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        int id = loader.getId();
        switch (id) {
            case INIT_DRAFT_USING_REFERENCE_MESSAGE:
                if (data != null && data.moveToFirst()) {
                    mRefMessage = new Message(data);
                    Intent intent = getIntent();
                    initFromRefMessage(mComposeMode);
                    finishSetup(mComposeMode, intent, null);
                    if (mComposeMode != FORWARD) {
                        String to = intent.getStringExtra(EXTRA_TO);
                        if (!TextUtils.isEmpty(to)) {
                            mRefMessage.setTo(null);
                            mRefMessage.setFrom(null);
                            clearChangeListeners();
                            mTo.append(to);
                            initChangeListeners();
                        }
                    }
                } else {
                    finish();
                }
                break;
            case REFERENCE_MESSAGE_LOADER:
                // Only populate mRefMessage and leave other fields untouched.
                if (data != null && data.moveToFirst()) {
                    mRefMessage = new Message(data);
                }
                finishSetup(mComposeMode, getIntent(), mInnerSavedState);
                break;
            case LOADER_ACCOUNT_CURSOR:
                if (data != null && data.moveToFirst()) {
                    // there are accounts now!
                    Account account;
                    final ArrayList<Account> accounts = new ArrayList<Account>();
                    final ArrayList<Account> initializedAccounts = new ArrayList<Account>();
                    do {
                        account = Account.builder().buildFrom(data);
                        /// M: only account with send function should be included
                        // in ComposeActivity's accounts
                        if (account.isAccountReady() && !account
                                .supportsCapability(UIProvider
                                        .AccountCapabilities.VIRTUAL_ACCOUNT)) {
                            initializedAccounts.add(account);
                        }
                        accounts.add(account);
                    } while (data.moveToNext());
                    if (initializedAccounts.size() > 0) {
                        findViewById(R.id.wait).setVisibility(View.GONE);
                        getLoaderManager().destroyLoader(LOADER_ACCOUNT_CURSOR);
                        findViewById(R.id.compose).setVisibility(View.VISIBLE);
                        mAccounts = initializedAccounts.toArray(
                                new Account[initializedAccounts.size()]);

                        finishCreate();
                        invalidateOptionsMenu();
                    } else {
                        // Show "waiting"
                        account = accounts.size() > 0 ? accounts.get(0) : null;
                        showWaitFragment(account);
                    }
                }
                break;
        }
    }

    private void showWaitFragment(Account account) {
        WaitFragment fragment = getWaitFragment();
        if (fragment != null) {
            fragment.updateAccount(account);
        } else {
            findViewById(R.id.wait).setVisibility(View.VISIBLE);
            replaceFragment(WaitFragment.newInstance(account, false /* expectingMessages */),
                    FragmentTransaction.TRANSIT_FRAGMENT_OPEN, TAG_WAIT);
        }
    }

    private WaitFragment getWaitFragment() {
        return (WaitFragment) getFragmentManager().findFragmentByTag(TAG_WAIT);
    }

    private int replaceFragment(Fragment fragment, int transition, String tag) {
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.setTransition(transition);
        fragmentTransaction.replace(R.id.wait, fragment, tag);
        final int transactionId = fragmentTransaction.commitAllowingStateLoss();
        return transactionId;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        // Do nothing.
    }

    /**
     * Background task to convert the message's html to Spanned.
     */
    private class HtmlToSpannedTask extends AsyncTask<String, Void, Spanned> {

        @Override
        protected Spanned doInBackground(String... input) {
            return HtmlUtils.htmlToSpan(input[0], mSpanConverterFactory);
        }

        @Override
        protected void onPostExecute(Spanned spanned) {
            mBodyView.removeTextChangedListener(ComposeActivity.this);
            setBody(spanned, false);
            mTextChanged = false;
            mBodyView.addTextChangedListener(ComposeActivity.this);
        }
    }

    @Override
    public void onSupportActionModeStarted(ActionMode mode) {
        super.onSupportActionModeStarted(mode);
        ViewUtils.setStatusBarColor(this, R.color.action_mode_statusbar_color);
    }

    @Override
    public void onSupportActionModeFinished(ActionMode mode) {
        super.onSupportActionModeFinished(mode);
        ViewUtils.setStatusBarColor(this, R.color.primary_dark_color);
    }

    /**
     * M: support for Truncatable quoted text view.
     * default return ture, no need truncate quoted text.
     * @param text
     */
    @Override
    public boolean onRespondInlineSanityCheck(String text) {
        return true;
    }

    /**
     * M: Get subject view
     * @return
     */
    public EditText getSubjectEditText() {
        return mSubject;
    }

    /**
     * M: Get body view
     * @return
     */
    public EditText getBodyEditText() {
        return mBodyView;
    }

    /**
     * M: Init editView's length filter
     */
    protected void initLenghtFilter() {
    }

    /**
     * M: Truncate Large Body, when forward or reply a mail, avoid binder size limitation.
     * If source message need be truncated, clone a new message and truncate the body.
     * Else return the source massage.
     * Make sure not change the original message.
     */
    private static Message getTruncatedMessage(Context context, int action, Message sourceMessage) {
        // Just return source message if not support or null.
        if (!EmailFeatureOptions.LAEGE_BODY_ENHANCEMENT || sourceMessage == null) {
            return sourceMessage;
        }

        // Clone a new message, before start clone keep original.
        Message msg = sourceMessage;
        try {
            // we'd like clone a object instead if changed source message.
            msg = (sourceMessage != null ? (Message) sourceMessage.clone() : null);
        } catch (CloneNotSupportedException ce) {
            // keep source meeage.
            return sourceMessage;
        }

        // Set body truncate length as 250k, half of
        // @MimeUtility.NEED_COMPRESS_BODY_SIZE.
        int limitationSize = MimeUtility.NEED_COMPRESS_BODY_SIZE / 2;
        int textlength = 0;
        int htmLength = 0;
        if (msg.bodyText != null) {
            textlength = msg.bodyText.length();
        }
        if (sourceMessage.bodyHtml != null) {
            htmLength = msg.bodyHtml.length();
        }
        if ((htmLength + textlength) < limitationSize) {
            return sourceMessage;
        }
        if (action == REPLY || action == REPLY_ALL || action == FORWARD) {
            if (!TextUtils.isEmpty(sourceMessage.bodyHtml)) {
                // ingore txt body, since UI not use it.
                msg.bodyText = "";
                if (htmLength > limitationSize) {
                    msg.bodyHtml = msg.bodyHtml.substring(0, limitationSize);
                    LogUtils.w(LOG_TAG, "[LBE] Html body will be truncated to size [%d]",
                            limitationSize);
                    Utility.showToast(context, R.string.body_too_large_truncated);
                }
                return msg;
            } else {
                if (textlength > limitationSize) {
                    msg.bodyText = msg.bodyText.substring(0, limitationSize);
                    LogUtils.w(LOG_TAG, "[LBE] Text body will be truncated to size [%d]",
                            limitationSize);
                    Utility.showToast(context, R.string.body_too_large_truncated);
                    return msg;
                }
            }
        }
        LogUtils.w(LOG_TAG, "[LBE] getTruncatedMessage, return source message");
        return sourceMessage;
    }
}