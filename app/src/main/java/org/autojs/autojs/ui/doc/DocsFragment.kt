package org.autojs.autojs.ui.doc

import android.app.Activity
import android.content.Context
import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import org.androidannotations.annotations.AfterViews
import org.androidannotations.annotations.EFragment
import org.androidannotations.annotations.ViewById
import org.autojs.autojs.event.BackPressedHandler
import org.autojs.autojs.ui.main.MainActivity
import org.autojs.autojs.ui.main.QueryEvent
import org.autojs.autojs.ui.main.ViewPagerFragment
import org.autojs.autojs.ui.main.ViewStatesManageable
import org.autojs.autojs.ui.widget.EWebView
import org.autojs.autojs.ui.widget.NestedWebView
import org.autojs.autojs.util.DocsUtils.getUrl
import org.autojs.autojs.util.WebViewUtils.Companion.adaptDarkMode
import org.autojs.autojs6.R
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

/**
 * Created by Stardust on 2017/8/22.
 * Modified by SuperMonster003 as of Mar 26, 2022.
 * Transformed by SuperMonster003 on Mar 31, 2023.
 */
@EFragment(R.layout.fragment_online_docs)
open class DocsFragment : ViewPagerFragment(ROTATION_GONE), BackPressedHandler, ViewStatesManageable {

    @JvmField
    @ViewById(R.id.eweb_view)
    var mEWebView: EWebView? = null

    private var mIndexUrl: String? = null
    private var mPreviousQuery: String? = null
    private var mIsCurrentPageDocs = false

    private lateinit var mContext: Context

    lateinit var webView: NestedWebView

    init {
        arguments = Bundle()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
        mContext = requireContext()
    }

    @AfterViews
    fun setUpViews() {
        mEWebView!!.webView.also {
            adaptDarkMode(mContext, it)
            webView = it
        }
        restoreViewStates()
        (activity as? MainActivity)?.apply {
            val tabLayout: TabLayout = this.findViewById(R.id.tab)
            val docsTab = tabLayout.getTabAt(this.docsItemIndex)
            docsTab?.view?.let { setTabViewClickListeners(it) }
        }
    }

    private fun loadMainPage() {
        arguments?.let { mIndexUrl = it.getString(ARGUMENT_URL, getUrl("index.html")) }
        mIndexUrl?.let { webView.loadUrl(it) }
    }

    override fun onPause() {
        super.onPause()
        saveViewStates()
    }

    override fun onBackPressed(activity: Activity): Boolean {
        webView.let {
            if (it.canGoBack()) {
                it.goBack()
                return true
            }
        }
        return false
    }

    override fun onFabClick(fab: FloatingActionButton) {}

    @Subscribe
    fun onQuerySummit(event: QueryEvent) = when {
        !isShown -> {}
        event === QueryEvent.CLEAR -> {
            webView.clearMatches()
            mPreviousQuery = null
        }
        event.isFindForward -> webView.findNext(false)
        event.query == mPreviousQuery -> webView.findNext(true)
        else -> {
            webView.findAllAsync(event.query)
            mPreviousQuery = event.query
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    override fun onPageShow() {
        super.onPageShow()
        mIsCurrentPageDocs = true
    }

    override fun onPageHide() {
        super.onPageHide()
        mIsCurrentPageDocs = false
    }

    override fun saveViewStates() {
        Bundle().let {
            webView.saveState(it)
            arguments?.putBundle("savedWebViewState", it)
        }
    }

    override fun restoreViewStates() {
        arguments?.getBundle("savedWebViewState")?.let { webView.restoreState(it) } ?: loadMainPage()
    }

    private fun setTabViewClickListeners(tabView: TabLayout.TabView) {
        tabView.setOnClickListener { if (mIsCurrentPageDocs) webView.scrollTo(0, 0) }
        tabView.setOnLongClickListener { if (mIsCurrentPageDocs) true.also { loadMainPage() } else false }
    }

    companion object {

        const val ARGUMENT_URL = "url"

    }

}