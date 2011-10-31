package com.wrapp.android.webimageexample;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import com.wrapp.android.webimage.WebImageView;

import java.net.MalformedURLException;
import java.net.URL;

public class WebImageListAdapter extends BaseAdapter {
  // Some random public gravatar images from my github feed
  private static String[] imageUrls = {
    "http://gravatar.com/avatar/08b4eab85774475a12eea640fbd5accd",
    "http://gravatar.com/avatar/18a730b4ed2e95a551bf75fb99d5ed7f",
    "http://gravatar.com/avatar/238e8b2100a5366b226234b946026462",
    "http://gravatar.com/avatar/340f1b7f106362a79a3ff96a1293db1c",
    "http://gravatar.com/avatar/54dfe6c69b839881780a4007113fbe4a",
    "http://gravatar.com/avatar/61eaf3cd71661eff93a827a0787e45c8",
    "http://gravatar.com/avatar/68602fa96bdda4c677ece48ab42b6eb2",
    "http://gravatar.com/avatar/7377347b05f27872d1d5c310f65d0f57",
    "http://gravatar.com/avatar/7ea0cc75793eb2b1ada4abc953a41592",
    "http://gravatar.com/avatar/8583cec0715c9bf8f067a456165e5d19",
    "http://gravatar.com/avatar/8c4cee1129bc11fbe9a0b9379dce2cb1",
    "http://gravatar.com/avatar/b4b5966a086e50d651ed2ae54206e278",
    "http://gravatar.com/avatar/b5f69060e8c08d8c6e44596086536ec0",
    "http://gravatar.com/avatar/c7cb70e24ae959ddd5b64fb3e494ad31",
    "http://gravatar.com/avatar/c85028af274331beade5f06525d8cb06",
    "http://gravatar.com/avatar/faf7332ded0a7868297531b60ddc3c33",
    "http://gravatar.com/avatar/fdc9d347464587dc5e963683a8bf1bb6",
  };

  public int getCount() {
    return imageUrls.length;
  }

  private URL getImageUrl(int i) {
    try {
      return new URL(imageUrls[i]);
    }
    catch(MalformedURLException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
    return null;
  }

  public Object getItem(int i) {
    return getImageUrl(i);
  }

  public long getItemId(int i) {
    return i;
  }

  public View getView(int i, View convertView, ViewGroup parentViewGroup) {
    LinearLayout webImageContainerView;
    if(convertView != null) {
      webImageContainerView = (LinearLayout)convertView;
    }
    else {
      final Context context = parentViewGroup.getContext();
      LayoutInflater layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      webImageContainerView = (LinearLayout)layoutInflater.inflate(R.layout.web_image_container_view, null);
    }

    WebImageView webImageView = (WebImageView)webImageContainerView.findViewById(R.id.WebImageView);
    webImageView.setImageUrl(getImageUrl(i));
    return webImageContainerView;
  }
}
