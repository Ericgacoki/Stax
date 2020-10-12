package com.hover.stax.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.amplitude.api.Amplitude;
import com.hover.stax.utils.bubbleshowcase.BubbleShowCase;
import com.hover.stax.utils.bubbleshowcase.BubbleShowCaseListener;
import com.hover.sdk.transactions.TransactionContract;
import com.hover.stax.ApplicationInstance;
import com.hover.stax.R;
import com.hover.stax.channels.Channel;
import com.hover.stax.requests.Request;
import com.hover.stax.schedules.Schedule;
import com.hover.stax.transactions.TransactionHistoryAdapter;
import com.hover.stax.transactions.TransactionHistoryViewModel;
import com.hover.stax.utils.DateUtils;
import com.hover.stax.utils.UIHelper;
import com.hover.stax.utils.Utils;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HomeFragment extends Fragment implements TransactionHistoryAdapter.SelectListener, ScheduledAdapter.SelectListener, RequestsAdapter.SelectListener {
	final public static String TAG = "HomeFragment";

	private BalancesViewModel balancesViewModel;
	private FutureViewModel futureViewModel;
	private TransactionHistoryViewModel transactionsViewModel;
	private ImageView showcasingStaxLogo;
	private BalanceAdapter balanceAdapter;
	private TextView homeTimeAgo;
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Amplitude.getInstance().logEvent(getString(R.string.visit_screen, getString(R.string.visit_home)));
		balancesViewModel = new ViewModelProvider(requireActivity()).get(BalancesViewModel.class);
		futureViewModel = new ViewModelProvider(requireActivity()).get(FutureViewModel.class);
		transactionsViewModel = new ViewModelProvider(requireActivity()).get(TransactionHistoryViewModel.class);
		return inflater.inflate(R.layout.fragment_home, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		setHomeLogoViewToMainActivity(view);
		setUpBalances(view);
		setUpFuture(view);
		setUpHistory(view);
		startShowcasing();

	}

	private void setHomeLogoViewToMainActivity(View view) {
		showcasingStaxLogo = view.findViewById(R.id.home_stax_logo);
	}

	private void setUpBalances(View view) {
		RecyclerView recyclerView = view.findViewById(R.id.balances_recyclerView);
		recyclerView.setLayoutManager(UIHelper.setMainLinearManagers(getContext()));
		recyclerView.setHasFixedSize(true);

		balancesViewModel.getSelectedChannels().observe(getViewLifecycleOwner(), channels -> {
			balanceAdapter = new BalanceAdapter(channels, (MainActivity) getActivity());
			recyclerView.setAdapter(balanceAdapter);
			setMeta(view, channels);
		});
	}

	private void setUpFuture(View root) {
		RecyclerView recyclerView = root.findViewById(R.id.scheduled_recyclerView);
		recyclerView.setLayoutManager(UIHelper.setMainLinearManagers(getContext()));
		futureViewModel.getScheduled().observe(getViewLifecycleOwner(), schedules -> {
			recyclerView.setAdapter(new ScheduledAdapter(schedules, this));
			setFutureVisible(root, schedules, futureViewModel.getRequests().getValue());
		});

		RecyclerView rv = root.findViewById(R.id.requests_recyclerView);
		rv.setLayoutManager(UIHelper.setMainLinearManagers(getContext()));
		futureViewModel.getRequests().observe(getViewLifecycleOwner(), requests -> {
			Log.e(TAG, "found requests: " + requests.size());
			recyclerView.setAdapter(new RequestsAdapter(requests, this, getContext()));
			setFutureVisible(root, futureViewModel.getScheduled().getValue(), requests);
		});
	}

	private void setFutureVisible(View root, List<Schedule> schedules, List<Request> requests) {
		boolean visible = (schedules != null && schedules.size() > 0) || (requests != null && requests.size() > 0);
		root.findViewById(R.id.scheduled_card).setVisibility(visible ? View.VISIBLE : View.GONE);
	}

	private void setUpHistory(View view) {
		RecyclerView rv = view.findViewById(R.id.transaction_history_recyclerView);
		rv.setLayoutManager(UIHelper.setMainLinearManagers(getContext()));

		transactionsViewModel.getStaxTransactions().observe(getViewLifecycleOwner(), staxTransactions -> {
			rv.setAdapter(new TransactionHistoryAdapter(staxTransactions, HomeFragment.this));
			view.findViewById(R.id.no_history).setVisibility(staxTransactions.size() > 0 ? View.GONE : View.VISIBLE);
		});
	}

	private void setMeta(View view, List<Channel> channels) {
		homeTimeAgo = view.findViewById(R.id.homeTimeAgo);
		long mostRecentTimestamp = 0;
		for (Channel c : channels) {
			if (c.latestBalanceTimestamp != null && c.latestBalanceTimestamp > mostRecentTimestamp)
				mostRecentTimestamp = c.latestBalanceTimestamp;
		}
		homeTimeAgo.setText(mostRecentTimestamp > 0 ? DateUtils.timeAgo(ApplicationInstance.getContext(), mostRecentTimestamp) : "Refresh");
		view.findViewById(R.id.homeTimeAgo).setVisibility(channels.size() > 0 ? View.VISIBLE : View.GONE);
	}

	@Override
	public void viewTransactionDetail(String uuid) {
		Bundle bundle = new Bundle();
		bundle.putString(TransactionContract.COLUMN_UUID, uuid);
		NavHostFragment.findNavController(this).navigate(R.id.transactionDetailsFragment, bundle);
	}

	@Override
	public void viewScheduledDetail(int id) {
		Bundle bundle = new Bundle();
		bundle.putInt("id", id);
		NavHostFragment.findNavController(this).navigate(R.id.scheduleDetailsFragment, bundle);
	}

	@Override
	public void viewRequestDetail(int id) {
		Bundle bundle = new Bundle();
		bundle.putInt("id", id);
		NavHostFragment.findNavController(this).navigate(R.id.requestDetailsFragment, bundle);
	}
	private void showcaseSecondStage() {
		if(balanceAdapter !=null) balanceAdapter.balanceShowcase(stageTwoBubbleListener, getActivity());
	}



	public void startShowcasing() {
		UIHelper.showCase(
				Utils.getStaxString(R.string.world_class_security),
				Utils.getStaxString(R.string.world_class_security_description),
				BubbleShowCase.ArrowPosition.TOP,
				stageOneBubbleListener,
				showcasingStaxLogo,
				getActivity());
	}

	private void showcaseThirdStage() {
		UIHelper.showCase(
				Utils.getStaxString(R.string.refresh_stax),
				Utils.getStaxString(R.string.refresh_stax_desc),
				BubbleShowCase.ArrowPosition.TOP,
				stageThreeBubbleListener,
				homeTimeAgo,
				getActivity());
	}

	BubbleShowCaseListener stageOneBubbleListener = new BubbleShowCaseListener() {
		@Override
		public void onTargetClick(@NotNull BubbleShowCase bubbleShowCase) {
			bubbleShowCase.dismiss();
			showcaseSecondStage();
		}

		@Override
		public void onCloseActionImageClick(@NotNull BubbleShowCase bubbleShowCase) {
			bubbleShowCase.dismiss();
			showcaseSecondStage();
		}

		@Override
		public void onBackgroundDimClick(@NotNull BubbleShowCase bubbleShowCase) {
		}

		@Override
		public void onBubbleClick(@NotNull BubbleShowCase bubbleShowCase) {
			bubbleShowCase.dismiss();
			showcaseSecondStage();
		}
	};

	BubbleShowCaseListener stageTwoBubbleListener = new BubbleShowCaseListener() {
		@Override
		public void onTargetClick(@NotNull BubbleShowCase bubbleShowCase) {
			bubbleShowCase.dismiss();
			showcaseThirdStage();
		}

		@Override
		public void onCloseActionImageClick(@NotNull BubbleShowCase bubbleShowCase) {
			bubbleShowCase.dismiss();
			showcaseThirdStage();
		}

		@Override
		public void onBackgroundDimClick(@NotNull BubbleShowCase bubbleShowCase) {
		}

		@Override
		public void onBubbleClick(@NotNull BubbleShowCase bubbleShowCase) {
			bubbleShowCase.dismiss();
			showcaseThirdStage();
		}
	};

	BubbleShowCaseListener stageThreeBubbleListener = new BubbleShowCaseListener() {
		@Override
		public void onTargetClick(@NotNull BubbleShowCase bubbleShowCase) {
			bubbleShowCase.dismiss();
		}

		@Override
		public void onCloseActionImageClick(@NotNull BubbleShowCase bubbleShowCase) {
			bubbleShowCase.dismiss();
		}

		@Override
		public void onBackgroundDimClick(@NotNull BubbleShowCase bubbleShowCase) {
		}

		@Override
		public void onBubbleClick(@NotNull BubbleShowCase bubbleShowCase) {
			bubbleShowCase.dismiss();
		}
	};
}
