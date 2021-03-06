package mustafaozhan.github.com.androcat.main.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.View
import im.delight.android.webview.AdvancedWebView
import kotlinx.android.synthetic.main.fragment_main.mBottomNavigationView
import kotlinx.android.synthetic.main.fragment_main.mImgViewAndroCat
import kotlinx.android.synthetic.main.fragment_main.mSwipeRefreshLayout
import kotlinx.android.synthetic.main.fragment_main.webView
import me.piruin.quickaction.ActionItem
import me.piruin.quickaction.QuickAction
import mustafaozhan.github.com.androcat.R
import mustafaozhan.github.com.androcat.base.BaseMvvmFragment
import mustafaozhan.github.com.androcat.extensions.fadeIO
import mustafaozhan.github.com.androcat.extensions.remove
import mustafaozhan.github.com.androcat.extensions.runScript
import mustafaozhan.github.com.androcat.extensions.setInversion
import mustafaozhan.github.com.androcat.extensions.setState
import mustafaozhan.github.com.androcat.main.activity.MainActivity
import mustafaozhan.github.com.androcat.settings.SettingsFragment
import mustafaozhan.github.com.androcat.tools.JsScrip
import mustafaozhan.github.com.androcat.tools.State

/**
 * Created by Mustafa Ozhan on 2018-07-22.
 */
@Suppress("TooManyFunctions", "MagicNumber")
class MainFragment : BaseMvvmFragment<MainFragmentViewModel>(), AdvancedWebView.Listener {

    companion object {
        var TAG: String = MainFragment::class.java.simpleName
        private const val ARGS_OPEN_URL = "ARGS_OPEN_URL"
        lateinit var url: String
        fun newInstance(url: String): MainFragment {
            val args = Bundle()
            args.putString(ARGS_OPEN_URL, url)
            val fragment = MainFragment()
            fragment.arguments = args
            return fragment
        }

        fun newInstance() = MainFragment()
    }

    override fun getViewModelClass(): Class<MainFragmentViewModel> = MainFragmentViewModel::class.java

    override fun getLayoutResId(): Int = R.layout.fragment_main

    private var logoutCount = 0
    private var state: State = State.SUCCESS

    private lateinit var quickActionProfile: QuickAction
    private lateinit var quickActionExplorer: QuickAction

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
        setDash()
        initWebView()
        setListeners()
        setActionListeners()

        arguments?.getString(ARGS_OPEN_URL)?.let { url ->
            webView?.loadUrl(url)
            arguments?.clear()
        }
    }

    private fun init() {
        url = if (viewModel.isLoggedIn() == true) {
            getString(R.string.url_github)
        } else {
            getString(R.string.url_login)
        }

        webView?.loadUrl(url)

        invert(viewModel.getSettings().isInvert)

        mImgViewAndroCat?.apply {
            setArchSize(124f)
            setArchLength(240)
            setArchStroke(24f)
            setArchSpeed(12)
        }

        context?.let { ctx ->
            quickActionExplorer = QuickAction(ctx, QuickAction.VERTICAL)
            quickActionExplorer.apply {
                setColorRes(R.color.colorGitHubDash)
                setTextColorRes(R.color.white)
                setEnabledDivider(false)
                addActionItem(
                    ActionItem(1, getString(R.string.search), R.drawable.search_icon),
                    ActionItem(2, getString(R.string.market_place), R.drawable.ic_shopping_cart_black_24dp),
                    ActionItem(3, getString(R.string.trends), R.drawable.ic_trending_up_black_24dp),
                    ActionItem(4, getString(R.string.new_gist), R.drawable.ic_code_black_24dp),
                    ActionItem(5, getString(R.string.new_repository), R.drawable.new_repo),
                    ActionItem(6, getString(R.string.invert), R.drawable.invert)
                )
            }

            quickActionProfile = QuickAction(ctx, QuickAction.VERTICAL)
            quickActionProfile.apply {
                setColorRes(R.color.colorGitHubDash)
                setTextColorRes(R.color.white)
                setEnabledDivider(false)
                addActionItem(
                    ActionItem(1, getString(R.string.starts), R.drawable.ic_star_black_24dp),
                    ActionItem(2, getString(R.string.repositories), R.drawable.new_repo),
                    ActionItem(3, getString(R.string.gists), R.drawable.ic_code_black_24dp),
                    ActionItem(4, getString(R.string.notifications), R.drawable.notifications),
                    ActionItem(5, getString(R.string.app_settings), R.drawable.ic_settings_black_24dp),
                    ActionItem(6, getString(R.string.user_settings), R.drawable.user_settings),
                    ActionItem(7, getString(R.string.log_out), R.drawable.logout_icon),
                    ActionItem(8, getString(R.string.log_in), R.drawable.login_icon),
                    ActionItem(9, getString(R.string.profile), R.drawable.user)
                )
            }
        }
    }

    private fun setListeners() {
        webView?.setListener(getBaseActivity(), this)

        mSwipeRefreshLayout.setOnRefreshListener {
            webView?.loadUrl(webView?.url)
            mSwipeRefreshLayout.isRefreshing = false
        }

        mBottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_user -> quickActionProfile.show(mBottomNavigationView.getIconAt(4))
                R.id.navigation_find -> quickActionExplorer.show(mBottomNavigationView.getIconAt(3))
                R.id.navigation_feed -> {
                    webView?.loadUrl(
                        if (viewModel.isLoggedIn() == false) {
                            getString(R.string.url_login)
                        } else {
                            getString(R.string.url_github)
                        }
                    )
                }
                R.id.navigation_pull_request -> webView?.loadUrl(getString(R.string.url_pulls))
                R.id.navigation_Issues -> webView?.loadUrl(getString(R.string.url_issues))
            }
            true
        }
    }

    @Suppress("ComplexMethod")
    private fun setActionListeners() {
        quickActionProfile.setOnActionItemClickListener { item ->
            when (item.actionId) {
                1 -> webView?.loadUrl(getString(R.string.url_github) + viewModel.getUsername() + "?tab=stars")
                2 -> webView?.loadUrl(getString(R.string.url_github) + viewModel.getUsername() + "?tab=repositories")
                3 -> webView?.loadUrl(getString(R.string.url_gist) + viewModel.getUsername())
                4 -> webView?.loadUrl(getString(R.string.url_notifications))
                5 -> replaceFragment(SettingsFragment.newInstance(), true)
                6 -> webView?.loadUrl(getString(R.string.url_settings))
                7 -> {
                    webView?.loadUrl(getString(R.string.url_logout))
                    url = getString(R.string.url_login)
                }
                8 -> webView?.loadUrl(getString(R.string.url_login))
                9 -> webView?.loadUrl(getString(R.string.url_github) + viewModel.getUsername())
                else -> webView?.loadUrl(getString(R.string.url_github))
            }
        }
        quickActionExplorer.setOnActionItemClickListener { item ->
            when (item.actionId) {
                1 -> webView?.loadUrl(getString(R.string.url_search))
                2 -> webView?.loadUrl(getString(R.string.url_market_place))
                3 -> webView?.loadUrl(getString(R.string.url_trending))
                4 -> webView?.loadUrl(getString(R.string.url_gist))
                5 -> webView?.loadUrl(getString(R.string.url_new))
                6 -> invert(!viewModel.getSettings().isInvert, true)
                else -> webView?.loadUrl(getString(R.string.url_github))
            }
        }
    }

    private fun invert(invert: Boolean, changeSettings: Boolean = false) {
        mImgViewAndroCat?.setInversion(invert)
        webView?.runScript(JsScrip.getInversion(invert))

        if (changeSettings) {
            viewModel.updateInvertSettings(invert)
        }
    }

    private fun setDash() {
        mBottomNavigationView.apply {
            inflateMenu(R.menu.bnvm_dash)
            enableAnimation(false)
            enableItemShiftingMode(false)
            enableShiftingMode(false)
            enableAnimation(false)
            setTextSize(12.0f)
            setIconsMarginTop(10)
            setIconSize(30.0F, 30.0F)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        webView?.apply {
            setBackgroundColor(Color.parseColor("#FFFFFF"))

            settings.apply {
                setDesktopMode(true)
                useWideViewPort = true
                loadWithOverviewMode = true
                javaScriptEnabled = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        webView?.onResume()
        if (MainActivity.uri != null) {
            webView?.loadUrl(MainActivity.uri)
            MainActivity.uri = null
        }
    }

    override fun onPause() {
        webView?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        webView?.onDestroy()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        webView?.onActivityResult(requestCode, resultCode, intent)
    }

    override fun onDownloadRequested(
        url: String?,
        suggestedFilename: String?,
        mimeType: String?,
        contentLength: Long,
        contentDisposition: String?,
        userAgent: String?
    ) {
    }

    override fun onExternalPageRequest(url: String?) {
    }

    override fun onPageError(errorCode: Int, description: String?, failingUrl: String?) {
        webView?.loadUrl(webView?.context?.getString(R.string.url_blank))
    }

    override fun onPageStarted(url: String, favicon: Bitmap?) {
        mImgViewAndroCat?.fadeIO(true)

        if (url.contains(webView?.context?.getString(R.string.url_session).toString())) {
            webView?.runScript(JsScrip.GET_USERNAME) { str ->
                if (str != "null")
                    viewModel.updateUser(str.remove("\""), true)
            }
            logoutCount = 0
        }
    }

    override fun onPageFinished(url: String?) {
        mImgViewAndroCat?.fadeIO(false)
        webView?.apply {
            runScript(JsScrip.getInversion(viewModel.loadSettings().isInvert))
            when (url) {
                context.getString(R.string.url_blank) -> {
                    state = State.FAILED
                    logoutCount = 0
                }
                context.getString(R.string.url_logout) -> {
                    logoutCount++
                    if (logoutCount == 2) {
                        MainFragment.url = context.getString(R.string.url_login)
                        loadUrl(context.getString(R.string.url_login))
                        viewModel.updateUser(isLoggedIn = false)
                    }
                    state = State.SUCCESS
                }
                else -> {
                    logoutCount = 0
                    state = State.SUCCESS
                }
            }
        }
        mImgViewAndroCat?.setState(state, viewModel.loadSettings().isInvert)
    }
}