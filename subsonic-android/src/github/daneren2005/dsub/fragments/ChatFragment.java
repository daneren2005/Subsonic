package github.daneren2005.dsub.fragments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.ChatMessage;
import github.daneren2005.dsub.service.MusicService;
import github.daneren2005.dsub.service.MusicServiceFactory;
import github.daneren2005.dsub.util.BackgroundTask;
import github.daneren2005.dsub.util.TabBackgroundTask;
import github.daneren2005.dsub.util.Util;
import github.daneren2005.dsub.view.ChatAdapter;
import com.actionbarsherlock.view.Menu;

/**
 * @author Joshua Bahnsen
 */
public class ChatFragment extends SubsonicFragment {
	private ListView chatListView;
	private EditText messageEditText;
	private ImageButton sendButton;
	private Long lastChatMessageTime = (long) 0;
	private ArrayList<ChatMessage> messageList = new ArrayList<ChatMessage>();

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
		rootView = inflater.inflate(R.layout.chat, container, false);
		
		messageEditText = (EditText) rootView.findViewById(R.id.chat_edittext);
		sendButton = (ImageButton) rootView.findViewById(R.id.chat_send);

		sendButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				sendMessage();
			}
		});

		chatListView = (ListView) rootView.findViewById(R.id.chat_entries);

		messageEditText.setImeActionLabel("Send", KeyEvent.KEYCODE_ENTER);
		messageEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
			}

			@Override
			public void afterTextChanged(Editable editable) {
				sendButton.setEnabled(!Util.isNullOrWhiteSpace(editable.toString()));
			}
		});

		messageEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE || (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN)) {
					sendMessage();
					return true;
				}

				return false;
			}
		});

		invalidated = true;
		return rootView;
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, com.actionbarsherlock.view.MenuInflater menuInflater) {
		menuInflater.inflate(R.menu.empty, menu);
	}

	@Override
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
		if(super.onOptionsItemSelected(item)) {
			return true;
		}

		return false;
	}
	
	@Override
	protected void refresh(boolean refresh) {
		load();
	}
	
	private synchronized void load() {
		setTitle(R.string.button_bar_chat);
		BackgroundTask<List<ChatMessage>> task = new TabBackgroundTask<List<ChatMessage>>(this) {
			@Override
			protected List<ChatMessage> doInBackground() throws Throwable {
				MusicService musicService = MusicServiceFactory.getMusicService(context);
				return musicService.getChatMessages(lastChatMessageTime, context, this);
			}

			@Override
			protected void done(List<ChatMessage> result) {
				if (result != null && !result.isEmpty()) {
					// Reset lastChatMessageTime if we have a newer message
					for (ChatMessage message : result) {
						if (message.getTime() > lastChatMessageTime) {
							lastChatMessageTime = message.getTime();
						}
					}

					// Reverse results to show them on the bottom
					Collections.reverse(result);
					messageList.addAll(result);

					ChatAdapter chatAdapter = new ChatAdapter(context, messageList);
					chatListView.setAdapter(chatAdapter);
				}
			}
		};

		task.execute();
	}

	private void sendMessage() {
		final String message = messageEditText.getText().toString();

		if (!Util.isNullOrWhiteSpace(message)) {
			messageEditText.setText("");

			BackgroundTask<Void> task = new TabBackgroundTask<Void>(this) {
				@Override
				protected Void doInBackground() throws Throwable {
					MusicService musicService = MusicServiceFactory.getMusicService(context);
					musicService.addChatMessage(message, context, this);
					return null;
				}

				@Override
				protected void done(Void result) {
					load();
				}
			};

			task.execute();
		}
	}
}