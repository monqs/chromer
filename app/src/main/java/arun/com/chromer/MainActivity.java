package arun.com.chromer;

import android.app.ActivityOptions;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsService;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.CardView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.color.ColorChooserDialog;
import com.flipboard.bottomsheet.BottomSheetLayout;
import com.mikepenz.community_material_typeface_library.CommunityMaterial;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import java.util.ArrayList;
import java.util.List;

import arun.com.chromer.about.AboutAppActivity;
import arun.com.chromer.about.changelog.ChangelogUtil;
import arun.com.chromer.activities.intro.ChromerIntro;
import arun.com.chromer.activities.intro.WebHeadsIntro;
import arun.com.chromer.blacklist.BlacklistManagerActivity;
import arun.com.chromer.customtabs.CustomTabBindingHelper;
import arun.com.chromer.customtabs.CustomTabDelegate;
import arun.com.chromer.customtabs.CustomTabHelper;
import arun.com.chromer.customtabs.prefetch.ScannerService;
import arun.com.chromer.customtabs.warmup.WarmupService;
import arun.com.chromer.model.App;
import arun.com.chromer.payments.DonateActivity;
import arun.com.chromer.preferences.BottomBarPreferenceFragment;
import arun.com.chromer.preferences.PersonalizationPreferenceFragment;
import arun.com.chromer.preferences.Preferences;
import arun.com.chromer.preferences.WebHeadPreferenceFragment;
import arun.com.chromer.services.util.ServicesUtil;
import arun.com.chromer.util.Constants;
import arun.com.chromer.util.Util;
import arun.com.chromer.views.IntentPickerSheetView;
import arun.com.chromer.views.MaterialSearchView;
import arun.com.chromer.views.adapter.AppRenderAdapter;
import arun.com.chromer.webheads.WebHeadService;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements ColorChooserDialog.ColorCallback {

    private static final int VOICE_REQUEST = 10001;

    private CustomTabBindingHelper mCustomTabBindingHelper;

    @BindView(R.id.warm_up_switch)
    public SwitchCompat mWarmUpSwitch;
    @BindView(R.id.pre_fetch_switch)
    public SwitchCompat mPrefetchSwitch;
    @BindView(R.id.merge_tabs_switch)
    public SwitchCompat mMergeSwitch;
    @BindView(R.id.only_wifi_switch)
    public AppCompatCheckBox mWifiCheckBox;
    @BindView(R.id.show_notification_checkbox)
    public AppCompatCheckBox mNotificationCheckBox;
    @BindView(R.id.secondary_browser_view)
    public ImageView mSecondaryBrowserIcon;
    @BindView(R.id.default_provider_view)
    public ImageView mDefaultProviderIcn;
    @BindView(R.id.fav_share_app_view)
    public ImageView mFavShareAppIcon;
    @BindView(R.id.set_default_image)
    public ImageView mSetDefaultIcon;
    @BindView(R.id.bottomsheet)
    public BottomSheetLayout mBottomSheet;
    @BindView(R.id.material_search_view)
    public MaterialSearchView mMaterialSearchView;
    @BindView(R.id.toolbar)
    public Toolbar mToolbar;
    @BindView(R.id.merge_tabs_apps_layout)
    public LinearLayout mMergeTabsLayout;
    @BindView(R.id.secondary_browser)
    public LinearLayout mSecondaryBrowser;
    @BindView(R.id.fav_share_app)
    public LinearLayout mFavShareLayout;
    @BindView(R.id.set_default_card)
    public CardView mSetDefaultCard;

    @Override
    protected void onStart() {
        super.onStart();
        if (shouldBind()) {
            mCustomTabBindingHelper.bindCustomTabsService(this);
        }
        // startService(new Intent(this, WebHeadService.class));
    }

    @Override
    protected void onStop() {
        super.onStop();
        mCustomTabBindingHelper.unbindCustomTabsService(this);
        // stopService(new Intent(this, WebHeadService.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDefaultBrowserCard();
        updatePrefetchIfPermissionGranted();
        setIconWithPackageName(mSecondaryBrowserIcon, Preferences.secondaryBrowserPackage(this));
        updateSubPreferences(Preferences.preFetch(this));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);

        if (Preferences.isFirstRun(this)) {
            startActivity(new Intent(this, ChromerIntro.class));
        }

        if (ChangelogUtil.shouldShowChangelog(this)) {
            ChangelogUtil.showChangelogDialog(this);
        }
        setupDefaultBrowser();

        setupMaterialSearch();

        setupDrawer();

        setupSwitches();

        setupCustomTab();

        setupDefaultProvider();

        setupSecondaryBrowser();

        setupFavShareApp();

        attachFragments();

        checkAndEducateUser();

        updateDefaultBrowserCard();

        ServicesUtil.takeCareOfServices(getApplicationContext());

        cleanOldDbs();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //noinspection ConstantConditions
            mMergeTabsLayout.setVisibility(View.VISIBLE);
        }
    }

    private void updateDefaultBrowserCard() {
        if (!Util.isDefaultBrowser(this)) {
            mSetDefaultCard.setVisibility(View.VISIBLE);
            if (Util.isLollipop()) {
                float elevation = Util.dpToPx(6);
                mSetDefaultCard
                        .animate()
                        .withLayer()
                        .z(elevation)
                        .translationZ(elevation)
                        .start();
            }
        } else
            mSetDefaultCard.setVisibility(View.GONE);
    }

    private void attachFragments() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.webhead_container, WebHeadPreferenceFragment.newInstance());
        ft.replace(R.id.preference_container, PersonalizationPreferenceFragment.newInstance());
        ft.replace(R.id.bottom_bar_container, BottomBarPreferenceFragment.newInstance());
        ft.commit();
    }

    private void setupMaterialSearch() {
        //noinspection ConstantConditions
        mMaterialSearchView.clearFocus();
        mMaterialSearchView.setOnKeyListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    launchCustomTab(mMaterialSearchView.getURL());
                    return true;
                }
                return false;
            }
        });
        mMaterialSearchView.setVoiceIconClickListener(new MaterialSearchView.VoiceIconClickListener() {
            @Override
            public void onClick() {
                if (Util.isVoiceRecognizerPresent(getApplicationContext())) {
                    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
                    intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.voice_prompt));
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
                    startActivityForResult(intent, VOICE_REQUEST);
                } else snack(getString(R.string.no_voice_rec_apps));

            }
        });
    }

    private void snack(@NonNull String textToSnack) {
        // Have to provide a view for view traversal, so providing the set default button.
        Snackbar.make(mMaterialSearchView, textToSnack, Snackbar.LENGTH_SHORT).show();
    }


    private void setupSecondaryBrowser() {
        setIconWithPackageName(mSecondaryBrowserIcon, Preferences.secondaryBrowserPackage(this));

        Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.GOOGLE_URL));
        final IntentPickerSheetView browserPicker = new IntentPickerSheetView(this,
                webIntent,
                getString(R.string.choose_secondary_browser),
                new IntentPickerSheetView.OnIntentPickedListener() {
                    @Override
                    public void onIntentPicked(IntentPickerSheetView.ActivityInfo activityInfo) {
                        mBottomSheet.dismissSheet();
                        String componentNameFlatten = activityInfo.componentName.flattenToString();
                        if (componentNameFlatten != null) {
                            Preferences.secondaryBrowserComponent(getApplicationContext(), componentNameFlatten);
                        }
                        setIconWithPackageName(mSecondaryBrowserIcon, activityInfo.componentName.getPackageName());
                        snack(String.format(getString(R.string.secondary_browser_success), activityInfo.label));
                    }
                });
        browserPicker.setFilter(new IntentPickerSheetView.Filter() {
            @Override
            public boolean include(IntentPickerSheetView.ActivityInfo info) {
                return !info.componentName.getPackageName().equalsIgnoreCase(getPackageName());
            }
        });
        //noinspection ConstantConditions
        mSecondaryBrowser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBottomSheet != null) mBottomSheet.showWithSheetView(browserPicker);
            }
        });
    }

    private void setupSwitches() {
        updatePrefetchIfPermissionGranted();
        setupCheckBoxes();

        final boolean preFetch = Preferences.preFetch(this);
        final boolean warmUpBrowser = Preferences.warmUp(this);
        final boolean mergeTabs = Preferences.mergeTabs(this);

        //noinspection ConstantConditions
        mWarmUpSwitch.setChecked(preFetch || warmUpBrowser);
        mWarmUpSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Preferences.warmUp(getApplicationContext(), isChecked);
                ServicesUtil.takeCareOfServices(getApplicationContext());
            }
        });

        //noinspection ConstantConditions
        mPrefetchSwitch.setChecked(preFetch);
        enableDisableWarmUpSwitch(preFetch);
        updateSubPreferences(preFetch);
        mPrefetchSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                boolean warmUp = !isChecked && Preferences.warmUp(getApplicationContext());

                if (!Util.isAccessibilityServiceEnabled(getApplicationContext())) {
                    mPrefetchSwitch.setChecked(false);
                    guideUserToAccessibilitySettings();
                } else {
                    mWarmUpSwitch.setChecked(!warmUp);
                    Preferences.warmUp(getApplicationContext(), warmUp);
                    enableDisableWarmUpSwitch(isChecked);
                }
                Preferences.preFetch(getApplicationContext(), isChecked);

                if (!isChecked) {
                    // Since pre fetch is not active, the  warm up preference should properly reflect what's on the
                    // UI, hence setting the preference to the checked value of the warm up switch.
                    Preferences.warmUp(getApplicationContext(), mWarmUpSwitch.isChecked());

                    // Ask user to revoke accessibility permission
                    Toast.makeText(getApplicationContext(), R.string.revoke_accessibility_permission, Toast.LENGTH_LONG).show();
                    startActivityForResult(new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS), 0);
                }

                ServicesUtil.takeCareOfServices(getApplicationContext());
                updateSubPreferences(isChecked);
            }
        });

        mMergeSwitch.setChecked(mergeTabs);
        mMergeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Preferences.mergeTabs(getApplicationContext(), isChecked);
            }
        });
    }

    private void setupCheckBoxes() {
        mWifiCheckBox.setChecked(Preferences.wifiOnlyPrefetch(this));
        mWifiCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Preferences.wifiOnlyPrefetch(getApplicationContext(), isChecked);
                ServicesUtil.takeCareOfServices(getApplicationContext());
            }
        });

        mNotificationCheckBox.setChecked(Preferences.preFetchNotification(this));
        mNotificationCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Preferences.preFetchNotification(getApplicationContext(), isChecked);
            }
        });
    }

    private void updateSubPreferences(boolean isChecked) {
        if (isChecked && Util.isAccessibilityServiceEnabled(this)) {
            mWifiCheckBox.setEnabled(true);
            mWifiCheckBox.setChecked(Preferences.wifiOnlyPrefetch(this));
            mNotificationCheckBox.setEnabled(true);
            mNotificationCheckBox.setChecked(Preferences.preFetchNotification(this));
        } else {
            mWifiCheckBox.setEnabled(false);
            mWifiCheckBox.setChecked(false);
            mNotificationCheckBox.setEnabled(false);
            mNotificationCheckBox.setChecked(false);
        }
    }

    private void guideUserToAccessibilitySettings() {
        new MaterialDialog.Builder(MainActivity.this)
                .title(R.string.accessibility_dialog_title)
                .content(R.string.accessibility_dialog_desc)
                .positiveText(R.string.open_settings)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                        startActivityForResult(new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS), 0);
                    }
                })
                .show();
    }

    private void updatePrefetchIfPermissionGranted() {
        if (Util.isAccessibilityServiceEnabled(this)) {
            Timber.d("Scanning permission granted");
            if (mPrefetchSwitch != null)
                mPrefetchSwitch.setChecked(Preferences.preFetch(getApplicationContext()));
        } else {
            // Turn off preference
            if (mPrefetchSwitch != null)
                mPrefetchSwitch.setChecked(false);
            Preferences.preFetch(getApplicationContext(), false);
        }
    }

    private void enableDisableWarmUpSwitch(boolean isChecked) {
        if (isChecked) {
            mWarmUpSwitch.setEnabled(false);
        } else {
            mWarmUpSwitch.setEnabled(true);
        }
    }

    private void setupDefaultProvider() {
        final String preferredApp = Preferences.customTabApp(MainActivity.this);

        if (preferredApp == null || preferredApp.length() == 0)
            // Setting an error icon
            mDefaultProviderIcn.setImageDrawable(new IconicsDrawable(this)
                    .icon(GoogleMaterial.Icon.gmd_error_outline)
                    .color(ContextCompat.getColor(this, R.color.error))
                    .sizeDp(24));
        else setIconWithPackageName(mDefaultProviderIcn, preferredApp);
    }

    private void setupDefaultBrowser() {
        mSetDefaultIcon.setImageDrawable(new IconicsDrawable(this)
                .icon(GoogleMaterial.Icon.gmd_new_releases)
                .color(ContextCompat.getColor(this, R.color.colorAccentText))
                .sizeDp(30));
    }


    private void setupDrawer() {
        Drawer drawer = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(mToolbar)
                .withAccountHeader(new AccountHeaderBuilder()
                        .withActivity(this)
                        .withHeaderBackground(R.drawable.chromer)
                        .withHeaderBackgroundScaleType(ImageView.ScaleType.CENTER_CROP)
                        .withDividerBelowHeader(true)
                        .build())
                .addDrawerItems(
                        new PrimaryDrawerItem().withName(getString(R.string.intro)).withIdentifier(4)
                                .withIcon(GoogleMaterial.Icon.gmd_assignment)
                                .withSelectable(false),
                        new PrimaryDrawerItem().withName(getString(R.string.feedback)).withIdentifier(2)
                                .withIcon(GoogleMaterial.Icon.gmd_feedback)
                                .withSelectable(false),
                        new PrimaryDrawerItem().withName(getString(R.string.rate_play_store)).withIdentifier(3)
                                .withIcon(GoogleMaterial.Icon.gmd_rate_review)
                                .withSelectable(false),
                        new PrimaryDrawerItem().withName(R.string.join_beta)
                                .withIdentifier(9)
                                .withIcon(CommunityMaterial.Icon.cmd_beta)
                                .withSelectable(false),
                        new DividerDrawerItem(),
                        new SecondaryDrawerItem().withName(getString(R.string.more_custom_tbs))
                                .withIcon(GoogleMaterial.Icon.gmd_open_in_new)
                                .withIdentifier(5)
                                .withSelectable(false),
                        new SecondaryDrawerItem().withName(getString(R.string.share))
                                .withIcon(GoogleMaterial.Icon.gmd_share)
                                .withDescription(getString(R.string.help_chromer_grow))
                                .withIdentifier(7)
                                .withSelectable(false),
                        new SecondaryDrawerItem().withName(getString(R.string.support_development))
                                .withDescription(R.string.consider_donation)
                                .withIcon(GoogleMaterial.Icon.gmd_favorite)
                                .withIdentifier(6)
                                .withSelectable(false),
                        new SecondaryDrawerItem().withName(getString(R.string.about))
                                .withIcon(GoogleMaterial.Icon.gmd_info_outline)
                                .withIdentifier(8)
                                .withSelectable(false)
                ).withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        if (drawerItem == null)
                            return false;
                        int i = (int) drawerItem.getIdentifier();
                        switch (i) {
                            case 2:
                                Intent emailIntent = new Intent(Intent.ACTION_SENDTO,
                                        Uri.fromParts("mailto", Constants.MAILID, null));
                                emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                                startActivity(Intent.createChooser(emailIntent, getString(R.string.send_email)));
                                break;
                            case 3:
                                Util.openPlayStore(MainActivity.this, getPackageName());
                                break;
                            case 4:
                                startActivity(new Intent(MainActivity.this, ChromerIntro.class));
                                break;
                            case 5:
                                launchCustomTab(Constants.CUSTOM_TAB_URL);
                                break;
                            case 6:
                                startActivity(new Intent(MainActivity.this, DonateActivity.class));
                                break;
                            case 7:
                                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                                shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text));
                                shareIntent.setType("text/plain");
                                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)));
                                break;
                            case 8:
                                Intent aboutActivityIntent = new Intent(MainActivity.this, AboutAppActivity.class);
                                startActivity(aboutActivityIntent,
                                        ActivityOptions.makeCustomAnimation(MainActivity.this,
                                                R.anim.slide_in_right_medium,
                                                R.anim.slide_out_left_medium).toBundle()
                                );
                                break;
                            case 9:
                                showJoinBetaDialog();
                                break;
                        }
                        return false;
                    }
                })
                .build();
        drawer.setSelection(-1);
    }

    private void showJoinBetaDialog() {
        new MaterialDialog.Builder(this)
                .title(R.string.join_beta)
                .content(R.string.join_beta_content)
                .btnStackedGravity(GravityEnum.END)
                .forceStacking(true)
                .positiveText(R.string.join_google_plus)
                .neutralText(R.string.become_a_tester)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        Intent googleIntent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://plus.google.com/communities/109754631011301174504"));
                        startActivity(googleIntent);
                    }
                })
                .onNeutral(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        launchCustomTab("https://play.google.com/apps/testing/arun.com.chromer");
                    }
                })
                .build()
                .show();
    }

    private void setupFavShareApp() {
        setIconWithPackageName(mFavShareAppIcon, Preferences.favSharePackage(this));

        final Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "");
        final IntentPickerSheetView picker = new IntentPickerSheetView(this,
                shareIntent,
                getString(R.string.choose_fav_share_app),
                new IntentPickerSheetView.OnIntentPickedListener() {
                    @Override
                    public void onIntentPicked(IntentPickerSheetView.ActivityInfo activityInfo) {
                        mBottomSheet.dismissSheet();
                        String componentNameFlatten = activityInfo.componentName.flattenToString();
                        if (componentNameFlatten != null) {
                            Preferences.favShareComponent(getApplicationContext(), componentNameFlatten);
                        }
                        setIconWithPackageName(mFavShareAppIcon,
                                activityInfo.componentName.getPackageName());
                        snack(String.format(getString(R.string.fav_share_success),
                                activityInfo.label));
                    }
                });
        picker.setFilter(new IntentPickerSheetView.Filter() {
            @Override
            public boolean include(IntentPickerSheetView.ActivityInfo info) {
                return !info.componentName.getPackageName().startsWith("com.android")
                        && !info.componentName.getPackageName().equalsIgnoreCase(getPackageName());
            }
        });
        //noinspection ConstantConditions
        mFavShareLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBottomSheet != null) mBottomSheet.showWithSheetView(picker);
            }
        });
    }

    private void launchCustomTab(String url) {
        if (url != null) {
            if (Preferences.webHeads(this)) {
                final Intent webHeadService = new Intent(this, WebHeadService.class);
                webHeadService.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                webHeadService.setData(Uri.parse(url));
                startService(webHeadService);
            } else {
                final CustomTabsIntent customTabsIntent = CustomTabDelegate.getCustomizedTabIntent(getApplicationContext(), url, false, Constants.NO_COLOR);
                CustomTabBindingHelper.openCustomTab(this, customTabsIntent, Uri.parse(url), CustomTabHelper.CUSTOM_TABS_FALLBACK);
            }
        }
    }

    private boolean shouldBind() {
        if (Preferences.warmUp(this)) return false;
        if (Preferences.preFetch(this) && Util.isAccessibilityServiceEnabled(this)) {
            return false;
        } else if (!Preferences.preFetch(this))
            return true;

        return true;
    }

    private void refreshCustomTabBindings() {
        // Unbind from currently bound service
        mCustomTabBindingHelper.unbindCustomTabsService(this);
        setupCustomTab();
        mCustomTabBindingHelper.bindCustomTabsService(this);

        // Restarting services will make them update their bindings.
        ServicesUtil.refreshCustomTabBindings(getApplicationContext());
    }

    private void setupCustomTab() {
        mCustomTabBindingHelper = new CustomTabBindingHelper();
        List<Bundle> possibleUrls = new ArrayList<>();
        Bundle bundle = new Bundle();
        bundle.putParcelable(CustomTabsService.KEY_URL, Uri.parse(Constants.CUSTOM_TAB_URL));
        possibleUrls.add(bundle);

        if (!shouldBind()) {
            try {
                boolean ok;
                if (ScannerService.getInstance() != null) {
                    ok = ScannerService.getInstance().mayLaunchUrl(Uri.parse(Constants.GOOGLE_URL), possibleUrls);
                    if (ok) return;
                }
                if (WarmupService.getInstance() != null) {
                    ok = WarmupService.getInstance().mayLaunchUrl(Uri.parse(Constants.GOOGLE_URL), possibleUrls);
                    if (ok) return;
                }
            } catch (Exception e) {
                // Ignored - best effort
                // If mayLaunch with a service failed, then we will bind a connection with this activity
                // and pre fetch the google url.
                e.printStackTrace();
            }
        }

        mCustomTabBindingHelper.setConnectionCallback(
                new CustomTabBindingHelper.ConnectionCallback() {
                    @Override
                    public void onCustomTabsConnected() {
                        Timber.d("Connect to custom tab in main activity");
                        try {
                            mCustomTabBindingHelper.mayLaunchUrl(Uri.parse(Constants.GOOGLE_URL), null, null);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onCustomTabsDisconnected() {
                    }
                });
    }

    @Override
    public void onColorSelection(@NonNull ColorChooserDialog dialog, @ColorInt int selectedColor) {
        switch (dialog.getTitle()) {
            case R.string.default_toolbar_color:
                Intent toolbarColorIntent = new Intent(Constants.ACTION_TOOLBAR_COLOR_SET);
                toolbarColorIntent.putExtra(Constants.EXTRA_KEY_TOOLBAR_COLOR, selectedColor);
                LocalBroadcastManager.getInstance(this).sendBroadcast(toolbarColorIntent);
                break;
            case R.string.web_heads_color:
                Intent webHeadColorIntent = new Intent(Constants.ACTION_WEBHEAD_COLOR_SET);
                webHeadColorIntent.putExtra(Constants.EXTRA_KEY_WEBHEAD_COLOR, selectedColor);
                LocalBroadcastManager.getInstance(this).sendBroadcast(webHeadColorIntent);
                break;
        }
    }

    private void checkAndEducateUser() {
        List packages = CustomTabHelper.getCustomTabSupportingPackages(this);
        if (packages.size() == 0) {
            new MaterialDialog.Builder(this)
                    .title(getString(R.string.custom_tab_provider_not_found))
                    .content(getString(R.string.custom_tab_provider_not_found_expln))
                    .positiveText(getString(R.string.install))
                    .negativeText(getString(android.R.string.no))
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            dialog.dismiss();
                            Util.openPlayStore(MainActivity.this, Constants.CHROME_PACKAGE);
                        }
                    }).show();
        }
    }

    private void setIconWithPackageName(@Nullable ImageView imageView, @Nullable String packageName) {
        if (imageView == null || packageName == null) return;

        try {
            // Calling getPackageManager directly from activity causes leak in UsageManager when accessibility is turned on.
            // Refer https://github.com/square/leakcanary/issues/62#issuecomment-101414452
            imageView.setImageDrawable(getApplicationContext().getPackageManager().getApplicationIcon(packageName));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void cleanOldDbs() {
        if (Preferences.shouldCleanDB(this)) {
            boolean ok = deleteDatabase(Constants.DATABASE_NAME);
            Timber.d("Deleted %s : %b", Constants.DATABASE_NAME, ok);
            ok = deleteDatabase(Constants.OLD_DATABASE_NAME);
            Timber.d("Deleted %s : %b", Constants.OLD_DATABASE_NAME, ok);
        }
    }

    @Override
    public void onBackPressed() {
        if (mMaterialSearchView.hasFocus()) {
            mMaterialSearchView.clearFocus();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VOICE_REQUEST) {
            switch (resultCode) {
                case RESULT_OK:
                    List<String> resultList = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (resultList != null && !resultList.isEmpty()) {
                        launchCustomTab(Util.processSearchText(resultList.get(0)));
                    }
                    break;
                default:
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @OnClick(R.id.set_default_card)
    public void setDefaultClick() {
        final String defaultBrowser = Util.getDefaultBrowserPackage(this);
        if (defaultBrowser.equalsIgnoreCase("android")
                || defaultBrowser.startsWith("org.cyanogenmod")) {
            // TODO Change this detection such that "if defaultBrowserPackage is not a compatible browser" condition is used
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.GOOGLE_URL)));
        } else {
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + defaultBrowser));
            Toast.makeText(getApplicationContext(),
                    Util.getAppNameWithPackage(getApplicationContext(), defaultBrowser)
                            + " "
                            + getString(R.string.default_clear_msg), Toast.LENGTH_LONG).show();
            startActivity(intent);
        }
    }

    @OnClick(R.id.web_heads_intro)
    public void webHeadIntro() {
        startActivity(new Intent(MainActivity.this, WebHeadsIntro.class));
    }


    @OnClick(R.id.blacklisted_target)
    public void blacklistClick() {
        Intent blackList = new Intent(MainActivity.this, BlacklistManagerActivity.class);
        startActivity(blackList,
                ActivityOptions.makeCustomAnimation(MainActivity.this,
                        R.anim.slide_in_right_medium,
                        R.anim.slide_out_left_medium).toBundle()
        );
    }

    @OnClick(R.id.fab)
    public void onFabClick() {
        if (mMaterialSearchView.hasFocus() && mMaterialSearchView.getText().length() > 0) {
            launchCustomTab(mMaterialSearchView.getURL());
        } else
            launchCustomTab(Constants.GOOGLE_URL);
    }

    @OnClick(R.id.default_provider)
    public void OnDefaultProviderClick() {
        final List<App> customTabApps = Util.getCustomTabApps(getApplicationContext());

        if (customTabApps.size() == 0) {
            checkAndEducateUser();
            return;
        }
        new MaterialDialog.Builder(MainActivity.this)
                .title(getString(R.string.choose_default_provider))
                .adapter(new AppRenderAdapter(getApplicationContext(), customTabApps),
                        new MaterialDialog.ListCallback() {
                            @Override
                            public void onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                                App app = customTabApps.get(which);
                                if (app != null) {
                                    String packageName = app.getPackageName();
                                    Preferences.customTabApp(getApplicationContext(), packageName);
                                    setIconWithPackageName(mDefaultProviderIcn, packageName);
                                    snack(String.format(getString(R.string.default_provider_success), app.getAppName()));

                                    // Refresh bindings so as to reflect changed custom tab package
                                    refreshCustomTabBindings();
                                }
                                if (dialog != null) dialog.dismiss();
                            }
                        })
                .show();
    }
}
