package com.news.kimo.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationBarView;
import com.news.kimo.R;
import com.news.kimo.databinding.ActivityMainBinding;
import com.news.kimo.ui.fragments.ExploreFragment;
import com.news.kimo.ui.fragments.HomeFragment;
import com.news.kimo.ui.fragments.MessagesFragment;
import com.news.kimo.ui.fragments.NotificationsFragment;
import com.news.kimo.ui.fragments.ProfileFragment;
import com.news.kimo.utils.Constants;

/**
 * Main container activity hosting the bottom navigation bar,
 * a create-post FAB, and the primary fragments.
 * <p>
 * Bottom nav items: Home, Explore, Create (placeholder), Notifications, Messages.
 * The profile page is accessible via the 5th item or a separate action.
 * Handles notification deep links from {@code onNewIntent}.
 */
public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivity";
    private static final int NAV_HOME = R.id.nav_home;
    private static final int NAV_EXPLORE = R.id.nav_explore;
    private static final int NAV_NOTIFICATIONS = R.id.nav_notifications;
    private static final int NAV_MESSAGES = R.id.nav_messages;
    private static final int NAV_PROFILE = R.id.nav_profile;

    private ActivityMainBinding binding;
    private Fragment currentFragment;
    private boolean doubleBackToExitPressedOnce = false;
    private androidx.os.Handler exitHandler = new androidx.os.Handler(android.os.Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initViews();
        initBottomNav();
        initFab();
        initToolbar();

        // Load home fragment by default
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), NAV_HOME);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null && intent.getExtras() != null) {
            handleNotificationDeepLink(intent.getExtras());
        }
    }

    // ==================================================================
    // Initialisation
    // ==================================================================

    private void initViews() {
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setTitle(R.string.app_name);
    }

    /**
     * Sets up the {@link BottomNavigationView} with an
     * {@link NavigationBarView.OnItemSelectedListener} that loads
     * the corresponding fragment and manages the FAB visibility.
     */
    private void initBottomNav() {
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == NAV_HOME) {
                loadFragment(new HomeFragment(), id);
                showFab();
            } else if (id == NAV_EXPLORE) {
                loadFragment(new ExploreFragment(), id);
                showFab();
            } else if (id == NAV_NOTIFICATIONS) {
                loadFragment(new NotificationsFragment(), id);
                hideFab();
            } else if (id == NAV_MESSAGES) {
                loadFragment(new MessagesFragment(), id);
                hideFab();
            } else if (id == NAV_PROFILE) {
                loadFragment(new ProfileFragment(), id);
                hideFab();
            }
            return true;
        });

        // Start with home selected
        binding.bottomNav.getMenu().findItem(NAV_HOME).setChecked(true);

        // Setup notification badge placeholder
        BadgeDrawable badge = binding.bottomNav.getOrCreateBadge(NAV_NOTIFICATIONS);
        badge.setVisible(false); // Will be updated from a listener
    }

    /**
     * Sets up the FAB for creating a new post.
     */
    private void initFab() {
        binding.fabCreatePost.setOnClickListener(v -> {
            // Navigate to create-post activity
            // TODO: Replace with actual CreatePostActivity when available
            showMessage(getString(R.string.create_post_coming_soon));
        });
    }

    /**
     * Sets up the toolbar with search icon, notification badge,
     * and dark-mode toggle.
     */
    private void initToolbar() {
        // Dark mode toggle in toolbar menu is handled via onOptionsItemSelected
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_search) {
            // TODO: Open search activity/fragment
            showMessage(getString(R.string.search_coming_soon));
            return true;
        } else if (id == R.id.action_dark_mode) {
            toggleDarkMode();
            return true;
        } else if (id == R.id.action_notifications_toolbar) {
            binding.bottomNav.getMenu().findItem(NAV_NOTIFICATIONS).setChecked(true);
            loadFragment(new NotificationsFragment(), NAV_NOTIFICATIONS);
            hideFab();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ==================================================================
    // Fragment Management
    // ==================================================================

    /**
     * Loads (or replaces) the current fragment in the main container.
     *
     * @param fragment    the fragment to load
     * @param selectedNav the selected navigation menu item id
     */
    private void loadFragment(Fragment fragment, int selectedNav) {
        if (fragment == null) return;
        // Avoid reloading the same fragment
        if (currentFragment != null && currentFragment.getClass().equals(fragment.getClass())) {
            return;
        }

        currentFragment = fragment;

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        transaction.addToBackStack(null);
        transaction.commitAllowingStateLoss();
    }

    /**
     * Returns to the Home fragment if the current fragment is not home.
     */
    private void navigateToHome() {
        binding.bottomNav.getMenu().findItem(NAV_HOME).setChecked(true);
        loadFragment(new HomeFragment(), NAV_HOME);
        showFab();

        // Clear the back stack up to home
        getSupportFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    // ==================================================================
    // FAB Visibility
    // ==================================================================

    private void showFab() {
        if (binding.fabCreatePost.getVisibility() != View.VISIBLE) {
            binding.fabCreatePost.show();
        }
    }

    private void hideFab() {
        if (binding.fabCreatePost.getVisibility() != View.GONE) {
            binding.fabCreatePost.hide();
        }
    }

    // ==================================================================
    // Notification Deep Links
    // ==================================================================

    /**
     * Handles incoming notification deep links by navigating to the
     * appropriate screen (post details, user profile, etc.).
     *
     * @param extras the intent extras containing deep link data
     */
    private void handleNotificationDeepLink(Bundle extras) {
        String postId = extras.getString(Constants.EXTRA_POST_ID);
        String userId = extras.getString(Constants.EXTRA_USER_ID);
        String commentId = extras.getString(Constants.EXTRA_COMMENT_ID);
        String notificationType = extras.getString("notification_type");

        if (postId != null && !postId.isEmpty()) {
            // Navigate to post details
            Bundle postBundle = new Bundle();
            postBundle.putString(Constants.EXTRA_POST_ID, postId);
            openActivity(PostDetailsActivity.class, postBundle);
        } else if (userId != null && !userId.isEmpty()) {
            // Navigate to user profile
            Bundle userBundle = new Bundle();
            userBundle.putString(Constants.EXTRA_USER_ID, userId);
            openActivity(ProfileDetailsActivity.class, userBundle);
        } else if ("message".equals(notificationType)) {
            // Navigate to messages
            binding.bottomNav.getMenu().findItem(NAV_MESSAGES).setChecked(true);
            loadFragment(new MessagesFragment(), NAV_MESSAGES);
            hideFab();
        } else {
            // Default: navigate to notifications
            binding.bottomNav.getMenu().findItem(NAV_NOTIFICATIONS).setChecked(true);
            loadFragment(new NotificationsFragment(), NAV_NOTIFICATIONS);
            hideFab();
        }
    }

    // ==================================================================
    // Dark Mode Toggle
    // ==================================================================

    /**
     * Toggles between dark and light (or system) mode.
     */
    private void toggleDarkMode() {
        int currentMode = AppCompatDelegate.getDefaultNightMode();
        int newMode;
        if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
            newMode = AppCompatDelegate.MODE_NIGHT_NO;
            com.news.kimo.utils.SessionManager.getInstance(this)
                    .saveSetting(Constants.KEY_THEME, Constants.THEME_LIGHT);
        } else {
            newMode = AppCompatDelegate.MODE_NIGHT_YES;
            com.news.kimo.utils.SessionManager.getInstance(this)
                    .saveSetting(Constants.KEY_THEME, Constants.THEME_DARK);
        }
        AppCompatDelegate.setDefaultNightMode(newMode);
    }

    // ==================================================================
    // Back Press
    // ==================================================================

    @Override
    public void onBackPressed() {
        // If not on home fragment, go to home first
        if (currentFragment != null && !(currentFragment instanceof HomeFragment)) {
            navigateToHome();
            return;
        }

        // Double-tap to exit
        if (doubleBackToExitPressedOnce) {
            exitHandler.removeCallbacksAndMessages(null);
            super.onBackPressed();
            return;
        }

        doubleBackToExitPressedOnce = true;
        showMessage(getString(R.string.press_back_again_to_exit));

        exitHandler.postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
    }

    @Override
    protected void onDestroy() {
        exitHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
