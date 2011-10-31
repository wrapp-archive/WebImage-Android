package com.wrapp.android.webimageexample;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.wrapp.android.webimage.WebImageView;

import java.net.MalformedURLException;
import java.net.URL;

@SuppressWarnings({"UnusedDeclaration"})
public class WebImageContainerView extends RelativeLayout {
  private WebImageView webImageView;
  private TextView imageText;

  public WebImageContainerView(Context context) {
    super(context);
    initialize(context);
  }

  public WebImageContainerView(Context context, AttributeSet attrs) {
    super(context, attrs);
    initialize(context);
  }

  public WebImageContainerView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    initialize(context);
  }

  private void initialize(Context context) {
    LayoutInflater layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    layoutInflater.inflate(R.layout.web_image_container_view, this, true);
    webImageView = (WebImageView)findViewById(R.id.WebImageView);
    imageText = (TextView)findViewById(R.id.WebImageViewText);
  }

  public void setImageUrl(String imageUrlString) {
    try {
      URL imageUrl = new URL(imageUrlString);
      webImageView.setImageResource(R.drawable.person_placeholder);
      webImageView.setImageUrl(imageUrl, true);
    }
    catch(MalformedURLException e) {
      e.printStackTrace();
    }
  }

  public void setImageText(String text) {
    imageText.setText(text);
  }
}
