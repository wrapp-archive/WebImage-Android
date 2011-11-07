package com.wrapp.android.webimagelist;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.wrapp.android.webimage.WebImageView;

import java.net.MalformedURLException;
import java.net.URL;

/** Small container class for a WebImageView and a corresponding TextView */
@SuppressWarnings({"UnusedDeclaration"})
public class WebImageContainerView extends RelativeLayout {
  private WebImageView webImageView;
  private TextView imageText;
  private static Drawable errorImage;

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
    if(errorImage == null) {
      errorImage = context.getResources().getDrawable(R.drawable.person_placeholder_error);
    }

    LayoutInflater layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    layoutInflater.inflate(R.layout.web_image_container_view, this, true);
    webImageView = (WebImageView)findViewById(R.id.WebImageView);
    imageText = (TextView)findViewById(R.id.WebImageViewText);
  }

  public void setImageUrl(String imageUrlString, WebImageView.Listener listener) {
    try {
      URL imageUrl = new URL(imageUrlString);
      webImageView.setImageResource(R.drawable.person_placeholder);
      webImageView.setListener(listener);
      webImageView.setImageUrl(imageUrl, true, errorImage);
    }
    catch(MalformedURLException e) {
      e.printStackTrace();
    }
  }

  public void setImageText(String text) {
    imageText.setText(text);
  }
}
