package com.example;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

import java.net.Authenticator;

public class MainActivity extends Activity
{
    public static final String TAG = MainActivity.class.getSimpleName();

    private Button mLoginButton;
    private Button mLogoutButton;
    private Button mTweetButton;
    private Twitter mTwitter;
    private RequestToken mRequestToken;
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mLoginButton = (Button)findViewById(R.id.login);
        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ConfigurationBuilder confbuilder = new ConfigurationBuilder();
                Configuration conf = confbuilder
                    .setOAuthConsumerKey(Const.CONSUMER_KEY)
                    .setOAuthConsumerSecret(Const.CONSUMER_SECRET)
                    .build();
                mTwitter = new TwitterFactory(conf).getInstance();
                mTwitter.setOAuthAccessToken(null);
                try {
                    mRequestToken = mTwitter.getOAuthRequestToken(Const.CALLBACK_URL);
                    Intent intent = new Intent(MainActivity.this, TwitterLogin.class);
                    intent.putExtra(Const.IEXTRA_AUTH_URL, mRequestToken.getAuthorizationURL());
                    startActivityForResult(intent, 0);
                } catch (TwitterException e) {
                    e.printStackTrace();
                }
            }
        });
        
        mLogoutButton = (Button)findViewById(R.id.logout);
        mLogoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences pref = getSharedPreferences(Const.PREF_NAME, MODE_PRIVATE);
                SharedPreferences.Editor editor = pref.edit();
                editor.remove(Const.PREF_KEY_ACCESS_TOKEN);
                editor.remove(Const.PREF_KEY_ACCESS_TOKEN_SECRET);
                editor.commit();
                
                if (mTwitter != null) {
                    mTwitter.shutdown();
                }
                
                Toast.makeText(MainActivity.this, "unauthorized", Toast.LENGTH_SHORT).show();
            }
        });
        
        mTweetButton = (Button)findViewById(R.id.tweet);
        mTweetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (mTwitter == null) {
                        ConfigurationBuilder confbuilder = new ConfigurationBuilder();
                        Configuration conf = confbuilder
                            .setOAuthConsumerKey(Const.CONSUMER_KEY)
                            .setOAuthConsumerSecret(Const.CONSUMER_SECRET)
                            .build();
                        mTwitter = new TwitterFactory(conf).getInstance();
                    }

                    SharedPreferences pref = getSharedPreferences(Const.PREF_NAME, MODE_PRIVATE);
                    String accessToken = pref.getString(Const.PREF_KEY_ACCESS_TOKEN, null);
                    String accessTokenSecret = pref.getString(Const.PREF_KEY_ACCESS_TOKEN_SECRET, null);
                    if (accessToken == null || accessTokenSecret == null) {
                        Toast.makeText(MainActivity.this, "not authorize yet", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    mTwitter.setOAuthAccessToken(new AccessToken(accessToken, accessTokenSecret));

                    EditText statusText = (EditText)findViewById(R.id.status);
                    String status = statusText.getText().toString();
                    mTwitter.updateStatus(status);

                    Toast.makeText(MainActivity.this, "tweeted", Toast.LENGTH_SHORT).show();
                    statusText.setText(null);
                } catch (TwitterException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                AccessToken accessToken = null;
                try {
                    String oauthVerifier = intent.getExtras().getString(Const.IEXTRA_OAUTH_VERIFIER);
                    accessToken = mTwitter.getOAuthAccessToken(mRequestToken, oauthVerifier);
                    SharedPreferences pref = getSharedPreferences(Const.PREF_NAME, MODE_PRIVATE);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString(Const.PREF_KEY_ACCESS_TOKEN, accessToken.getToken());
                    editor.putString(Const.PREF_KEY_ACCESS_TOKEN_SECRET, accessToken.getTokenSecret());
                    editor.commit();

                    Toast.makeText(this, "authorized", Toast.LENGTH_SHORT).show();
                } catch(TwitterException e) {
                    e.printStackTrace();
                }
            } else if (resultCode == RESULT_CANCELED) {
                Log.w(TAG, "Twitter auth canceled.");
            }
        }
    }
}
