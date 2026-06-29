package com.example.mobilnekt1;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.lifecycle.ViewModelProvider;

import com.example.mobilnekt1.friends.domain.FriendProfile;
import com.example.mobilnekt1.friends.domain.FriendRequest;
import com.example.mobilnekt1.friends.domain.MatchInvite;
import com.example.mobilnekt1.friends.presentation.FriendsState;
import com.example.mobilnekt1.friends.presentation.FriendsViewModel;
import com.example.mobilnekt1.friends.presentation.OpenFriendlyMatch;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanIntentResult;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.List;
import java.util.Locale;

public final class FriendsActivity extends BaseKt1Activity {
    private FriendsViewModel viewModel;
    private EditText searchInput;
    private ProgressBar progress;
    private LinearLayout searchResultContainer;
    private LinearLayout incomingRequestsContainer;
    private LinearLayout outgoingRequestsContainer;
    private LinearLayout incomingInvitesContainer;
    private LinearLayout outgoingInvitesContainer;
    private LinearLayout friendsContainer;
    private final ActivityResultLauncher<ScanOptions> qrScanner =
            registerForActivityResult(new ScanContract(), this::handleScanResult);

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);
        bindViews();
        viewModel = new ViewModelProvider(this).get(FriendsViewModel.class);
        viewModel.getState().observe(this, this::render);
        viewModel.getMessage().observe(this, event -> {
            String message = event.getIfNotHandled();
            if (message != null) Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
        viewModel.getOpenMatch().observe(this, event -> {
            OpenFriendlyMatch match = event.getIfNotHandled();
            if (match != null) openFriendlyMatch(match);
        });
        findViewById(R.id.button_search_friend).setOnClickListener(v ->
                viewModel.search(searchInput.getText().toString()));
        findViewById(R.id.button_scan_friend_qr).setOnClickListener(v -> startQrScanner());
        findViewById(R.id.button_friends_back).setOnClickListener(v -> finish());
    }

    private void bindViews() {
        searchInput = findViewById(R.id.input_friend_username);
        progress = findViewById(R.id.progress_friends);
        searchResultContainer = findViewById(R.id.container_friend_search_result);
        incomingRequestsContainer = findViewById(R.id.container_incoming_requests);
        outgoingRequestsContainer = findViewById(R.id.container_outgoing_requests);
        incomingInvitesContainer = findViewById(R.id.container_incoming_invites);
        outgoingInvitesContainer = findViewById(R.id.container_outgoing_invites);
        friendsContainer = findViewById(R.id.container_friends);
    }

    private void render(FriendsState state) {
        progress.setVisibility(state.loading ? View.VISIBLE : View.GONE);
        renderSearchResult(state.searchResult);
        renderRequests(incomingRequestsContainer, state.snapshot.incomingRequests, true);
        renderRequests(outgoingRequestsContainer, state.snapshot.outgoingRequests, false);
        renderInvites(incomingInvitesContainer, state.snapshot.incomingInvites, true);
        renderInvites(outgoingInvitesContainer, state.snapshot.outgoingInvites, false);
        renderFriends(state.snapshot.friends);
    }

    private void renderSearchResult(FriendProfile profile) {
        searchResultContainer.removeAllViews();
        if (profile == null) return;
        LinearLayout card = profileCard(profile);
        Button add = actionButton(R.string.add_friend);
        add.setOnClickListener(v -> viewModel.sendRequest(profile.uid));
        card.addView(add);
        searchResultContainer.addView(card, cardParams());
    }

    private void renderRequests(LinearLayout container, List<FriendRequest> requests,
                                boolean incoming) {
        container.removeAllViews();
        if (requests.isEmpty()) { addEmpty(container); return; }
        for (FriendRequest request : requests) {
            LinearLayout card = card();
            addTitle(card, incoming ? request.senderUsername : request.receiverUsername);
            addText(card, incoming ? getString(R.string.friend_request_received)
                    : getString(R.string.friend_request_sent));
            LinearLayout actions = actionRow();
            if (incoming) {
                Button accept = actionButton(R.string.accept);
                accept.setOnClickListener(v -> viewModel.acceptRequest(request.id));
                actions.addView(accept, actionParams());
                Button reject = actionButton(R.string.reject);
                reject.setOnClickListener(v -> viewModel.rejectRequest(request.id));
                actions.addView(reject, actionParams());
            } else {
                Button cancel = actionButton(R.string.cancel_request);
                cancel.setOnClickListener(v -> viewModel.cancelRequest(request.id));
                actions.addView(cancel, actionParams());
            }
            card.addView(actions);
            container.addView(card, cardParams());
        }
    }

    private void renderInvites(LinearLayout container, List<MatchInvite> invites,
                               boolean incoming) {
        container.removeAllViews();
        if (invites.isEmpty()) { addEmpty(container); return; }
        for (MatchInvite invite : invites) {
            LinearLayout card = card();
            addTitle(card, incoming ? invite.senderUsername : invite.receiverUsername);
            long seconds = invite.expiresAt == null ? 0 : Math.max(0,
                    (invite.expiresAt.toDate().getTime() - System.currentTimeMillis() + 999) / 1000);
            addText(card, getString(R.string.friendly_invite_expires, seconds));
            LinearLayout actions = actionRow();
            if (incoming) {
                Button accept = actionButton(R.string.accept);
                accept.setOnClickListener(v -> viewModel.acceptInvite(invite.id));
                actions.addView(accept, actionParams());
                Button reject = actionButton(R.string.reject);
                reject.setOnClickListener(v -> viewModel.rejectInvite(invite.id));
                actions.addView(reject, actionParams());
            } else {
                Button cancel = actionButton(R.string.cancel_invite);
                cancel.setOnClickListener(v -> viewModel.cancelInvite(invite.id));
                actions.addView(cancel, actionParams());
            }
            card.addView(actions);
            container.addView(card, cardParams());
        }
    }

    private void renderFriends(List<FriendProfile> friends) {
        friendsContainer.removeAllViews();
        if (friends.isEmpty()) { addEmpty(friendsContainer); return; }
        for (FriendProfile friend : friends) {
            LinearLayout card = profileCard(friend);
            Button invite = actionButton(R.string.invite_to_match);
            invite.setEnabled(friend.isOnline && !friend.inGame);
            invite.setOnClickListener(v -> viewModel.invite(friend.uid));
            card.addView(invite);
            friendsContainer.addView(card, cardParams());
        }
    }

    private LinearLayout profileCard(FriendProfile profile) {
        LinearLayout card = card();
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        ImageView avatar = new ImageView(this);
        avatar.setImageResource(avatarResource(profile.avatarId));
        avatar.setBackgroundResource(R.drawable.avatar_background);
        int size = dp(64);
        header.addView(avatar, new LinearLayout.LayoutParams(size, size));
        LinearLayout values = new LinearLayout(this);
        values.setOrientation(LinearLayout.VERTICAL);
        values.setPadding(dp(12), 0, 0, 0);
        addTitle(values, profile.username);
        String rank = profile.monthlyRank == 0 ? "-" : String.valueOf(profile.monthlyRank);
        addText(values, getString(R.string.friend_profile_summary,
                profile.stars, profile.league, rank, profile.monthlyStars));
        String status = profile.inGame ? getString(R.string.status_in_game)
                : profile.isOnline ? getString(R.string.status_online)
                : getString(R.string.status_offline);
        addText(values, status + " | " + getString(R.string.avatar_frame_value, profile.avatarFrame));
        header.addView(values, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(header);
        return card;
    }

    private void startQrScanner() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt(getString(R.string.scan_friend_qr_prompt));
        options.setBeepEnabled(false);
        options.setOrientationLocked(true);
        qrScanner.launch(options);
    }

    private void handleScanResult(ScanIntentResult result) {
        if (result.getContents() != null) viewModel.handleQr(result.getContents());
    }

    private void openFriendlyMatch(OpenFriendlyMatch match) {
        Intent intent = new Intent(this, MatchLobbyActivity.class);
        intent.putExtra(MatchLobbyActivity.EXTRA_MATCH_ID, match.matchId);
        intent.putExtra(MatchLobbyActivity.EXTRA_MATCH_INVITE_ID, match.inviteId);
        startActivity(intent);
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackgroundResource(R.drawable.card_background);
        return card;
    }

    private void addTitle(LinearLayout parent, String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(18);
        text.setTypeface(null, Typeface.BOLD);
        text.setTextColor(getResources().getColor(R.color.text_primary));
        parent.addView(text);
    }

    private void addText(LinearLayout parent, String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(14);
        text.setTextColor(getResources().getColor(R.color.text_secondary));
        parent.addView(text);
    }

    private Button actionButton(int textResource) {
        Button button = new Button(this);
        button.setText(textResource);
        button.setAllCaps(false);
        button.setBackgroundResource(R.drawable.button_secondary);
        button.setTextColor(getResources().getColor(android.R.color.white));
        return button;
    }

    private LinearLayout actionRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(8), 0, 0);
        return row;
    }

    private LinearLayout.LayoutParams actionParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(dp(3), 0, dp(3), 0);
        return params;
    }

    private LinearLayout.LayoutParams cardParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(6), 0, dp(6));
        return params;
    }

    private void addEmpty(LinearLayout container) {
        TextView empty = new TextView(this);
        empty.setText(R.string.no_items);
        empty.setTextColor(getResources().getColor(R.color.text_secondary));
        empty.setPadding(0, dp(6), 0, dp(6));
        container.addView(empty);
    }

    private int avatarResource(String id) {
        if ("M2".equals(id)) return R.drawable.avatar_2;
        if ("M3".equals(id)) return R.drawable.avatar_3;
        return R.drawable.avatar_1;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
