package com.hover.stax.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.hover.stax.utils.bubbleshowcase.BubbleShowCase;
import com.hover.stax.utils.bubbleshowcase.BubbleShowCaseBuilder;
import com.hover.stax.utils.bubbleshowcase.BubbleShowCaseListener;
import com.google.android.material.snackbar.Snackbar;
import com.hover.stax.ApplicationInstance;
import com.hover.stax.R;

public class UIHelper {

	private static final int INITIAL_ITEMS_FETCH = 30;

	public static void flashMessage(Context context, @Nullable View view, String message) {
		if (view == null) flashMessage(context, message);
		else Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
	}

	public static void flashMessage(Context context, String message) {
		if (context == null) context = ApplicationInstance.getContext();
		Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
	}

	public static LinearLayoutManager setMainLinearManagers(Context context) {
		LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
		linearLayoutManager.setInitialPrefetchItemCount(INITIAL_ITEMS_FETCH);
		linearLayoutManager.setSmoothScrollbarEnabled(true);
		return linearLayoutManager;
	}

	static public void setColoredDrawable(ImageButton imageButton, int drawable, int color) {
		Drawable unwrappedDrawable = AppCompatResources.getDrawable(ApplicationInstance.getContext(), drawable);
		assert unwrappedDrawable != null;
		Drawable wrappedDrawable = DrawableCompat.wrap(unwrappedDrawable);
		DrawableCompat.setTint(wrappedDrawable, color);
		imageButton.setImageDrawable(wrappedDrawable);
	}

	public static void setTextUnderline(TextView textView, String cs) {
		SpannableString content = new SpannableString(cs);
		content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
		content.setSpan(android.graphics.Typeface.BOLD, 0, content.length(), 0);
		try {textView.setText(content); }
		catch (Exception ignored) { }
	}

	public static void fixListViewHeight(ListView listView) {
		ListAdapter listAdapter = listView.getAdapter();
		if (listAdapter == null) { return; }

		int totalHeight = 0;
		int desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.AT_MOST);
		for (int i = 0; i < listAdapter.getCount(); i++) {
			View listItem = listAdapter.getView(i, null, listView);
			listItem.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
			totalHeight += listItem.getMeasuredHeight();
		}

		ViewGroup.LayoutParams params = listView.getLayoutParams();
		params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
		listView.setLayoutParams(params);
		listView.requestLayout();
	}


	public static void showCase(String title, String desc, BubbleShowCase.ArrowPosition arrowPosition, BubbleShowCaseListener listener, View v, Activity activity) {
		new BubbleShowCaseBuilder(activity) //Activity instance
				.title(title) //Any title for the bubble view
				.description(desc) //More detailed description
				.arrowPosition(arrowPosition) //You can force the position of the arrow to change the location of the bubble.
				.backgroundColor(ApplicationInstance.getContext().getResources().getColor(R.color.colorAccent)) //Bubble background color
				.textColor(ApplicationInstance.getContext().getResources().getColor(R.color.colorPrimary)) //Bubble Text color
				.titleTextSize(20) //Title text size in SP (default value 16sp)
				.descriptionTextSize(20) //Subtitle text size in SP (default value 14sp)
				//.image(imageDrawable) //Bubble main image
				//.closeActionImage(CloseImageDrawable) //Custom close action image
				//.showOnce(title)  //Id to show only once the BubbleShowCase
				.listener(listener)
				.targetView(v).show();
	}
}
