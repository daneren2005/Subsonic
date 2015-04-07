package github.daneren2005.dsub.adapter;

import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.activity.SubsonicActivity;
import github.daneren2005.dsub.domain.ChatMessage;
import github.daneren2005.dsub.util.ImageLoader;
import github.daneren2005.dsub.util.UserUtil;
import github.daneren2005.dsub.util.Util;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Pattern;

public class ChatAdapter extends ArrayAdapter<ChatMessage> {
	
	private final SubsonicActivity activity;
	private ArrayList<ChatMessage> messages;
	private final ImageLoader imageLoader;
	
    private static final String phoneRegex = "1?\\W*([2-9][0-8][0-9])\\W*([2-9][0-9]{2})\\W*([0-9]{4})"; //you can just place your support phone here
    private static final Pattern phoneMatcher = Pattern.compile(phoneRegex);

    public ChatAdapter(SubsonicActivity activity, ArrayList<ChatMessage> messages, ImageLoader imageLoader) {
        super(activity, R.layout.chat_item, messages);
        this.activity = activity;
        this.messages = messages;
		this.imageLoader = imageLoader;
    }
    
    @Override
	public int getCount() {
		return messages.size();
	}
    
    @Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ChatMessage message = this.getItem(position);

		ViewHolder holder;
        int layout;
		
        String messageUser = message.getUsername();
        Date messageTime = new java.util.Date(message.getTime());
        String messageText = message.getMessage();
        
        String me = UserUtil.getCurrentUsername(activity);
        
        if (messageUser.equals(me)) {
        	layout = R.layout.chat_item_reverse;
        } else {
        	layout = R.layout.chat_item;
        }
        
		if (convertView == null)
		{
			holder = new ViewHolder();
			
			convertView = LayoutInflater.from(activity).inflate(layout, parent, false);
			
	        TextView usernameView = (TextView) convertView.findViewById(R.id.chat_username);
	        TextView timeView = (TextView) convertView.findViewById(R.id.chat_time);
	        TextView messageView = (TextView) convertView.findViewById(R.id.chat_message);
	        
	        messageView.setMovementMethod(LinkMovementMethod.getInstance());
	        Linkify.addLinks(messageView, Linkify.EMAIL_ADDRESSES);
	        Linkify.addLinks(messageView, Linkify.WEB_URLS);
	        Linkify.addLinks(messageView, phoneMatcher, "tel:");

	        holder.message = messageView;
			holder.username = usernameView;
			holder.time = timeView;
			holder.avatar = (ImageView) convertView.findViewById(R.id.chat_avatar);
			
			convertView.setTag(holder);
		}
		else
		{
			holder = (ViewHolder) convertView.getTag();
		}

		DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(activity);
		String messageTimeFormatted = String.format("[%s]", timeFormat.format(messageTime));
		
      	holder.username.setText(messageUser);
        holder.message.setText(messageText);
    	holder.time.setText(messageTimeFormatted);

		imageLoader.loadAvatar(activity, holder.avatar, messageUser);

		return convertView;
	}
    
	private static class ViewHolder
	{
		TextView message;
		TextView username;
		TextView time;
		ImageView avatar;
	}
}
