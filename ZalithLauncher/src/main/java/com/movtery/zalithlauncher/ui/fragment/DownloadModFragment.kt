package com.movtery.zalithlauncher.ui.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.movtery.zalithlauncher.event.value.DownloadRecyclerEnableEvent
import com.movtery.zalithlauncher.feature.download.InfoViewModel
import com.movtery.zalithlauncher.feature.download.ScreenshotAdapter
import com.movtery.zalithlauncher.feature.download.VersionAdapter
import com.movtery.zalithlauncher.feature.download.item.InfoItem
import com.movtery.zalithlauncher.feature.download.item.ScreenshotItem
import com.movtery.zalithlauncher.feature.download.item.VersionItem
import com.movtery.zalithlauncher.feature.download.platform.AbstractPlatformHelper
import com.movtery.zalithlauncher.feature.log.Logging
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.task.Task
import com.movtery.zalithlauncher.task.TaskExecutors
import com.movtery.zalithlauncher.ui.subassembly.modlist.ModListAdapter
import com.movtery.zalithlauncher.ui.subassembly.modlist.ModListFragment
import com.movtery.zalithlauncher.ui.subassembly.modlist.ModListItemBean
import com.movtery.zalithlauncher.utils.MCVersionRegex.Companion.RELEASE_REGEX
import net.kdt.pojavlaunch.Tools
import org.greenrobot.eventbus.EventBus
import org.jackhuang.hmcl.util.versioning.VersionNumber
import java.io.File
import java.util.Collections
import java.util.concurrent.Future
import java.util.function.Consumer

class DownloadModFragment : ModListFragment() {
    companion object {
        const val TAG: String = "DownloadModFragment"
    }

    private lateinit var platformHelper: AbstractPlatformHelper
    private lateinit var mInfoItem: InfoItem
    private var mPath: File? = null
    private var linkGetSubmit: Future<*>? = null

    override fun init() {
        parseViewModel()
        linkGetSubmit = TaskExecutors.getDefault().submit {
            runCatching {
                val webUrl = platformHelper.getWebUrl(mInfoItem)
                fragmentActivity?.runOnUiThread { setLink(webUrl) }
            }.getOrElse { e ->
                Logging.e("DownloadModFragment", "Failed to retrieve the website link, ${Tools.printToString(e)}")
            }
        }
        super.init()
    }

    override fun initRefresh(): Future<*> {
        return refresh(false)
    }

    override fun refresh(): Future<*> {
        return refresh(true)
    }

    override fun onDestroy() {
        EventBus.getDefault().post(DownloadRecyclerEnableEvent(true))
        linkGetSubmit?.apply {
            if (!isCancelled && !isDone) cancel(true)
        }
        super.onDestroy()
    }

    private fun refresh(force: Boolean): Future<*> {
        return TaskExecutors.getDefault().submit {
            runCatching {
                TaskExecutors.runInUIThread {
                    cancelFailedToLoad()
                    componentProcessing(true)
                }
                val versions = platformHelper.getVersions(mInfoItem, force)
                processModDetails(versions)
            }.getOrElse { e ->
                TaskExecutors.runInUIThread {
                    componentProcessing(false)
                    setFailedToLoad(e.toString())
                }
                Logging.e("DownloadModFragment", Tools.printToString(e))
            }
        }
    }

    private fun processModDetails(versions: List<VersionItem>?) {
        val pattern = RELEASE_REGEX

        val releaseCheckBoxChecked = releaseCheckBox.isChecked
        val mModVersionsByMinecraftVersion: MutableMap<String, MutableList<VersionItem>> = HashMap()

        versions?.forEach(Consumer { versionItem ->
            currentTask?.apply { if (isCancelled) return@Consumer }

            for (mcVersion in versionItem.mcVersions) {
                currentTask?.apply { if (isCancelled) return@Consumer }

                if (releaseCheckBoxChecked) {
                    val matcher = pattern.matcher(mcVersion)
                    if (!matcher.matches()) {
                        //如果不是正式版本，将继续检测下一项
                        continue
                    }
                }

                mModVersionsByMinecraftVersion.computeIfAbsent(mcVersion) { Collections.synchronizedList(ArrayList()) }
                    .add(versionItem) //将版本数据加入到相应的版本号分组中
            }
        })

        currentTask?.apply { if (isCancelled) return }

        val mData: MutableList<ModListItemBean> = ArrayList()
        mModVersionsByMinecraftVersion.entries
            .sortedWith { entry1, entry2 -> -VersionNumber.compare(entry1.key, entry2.key) }
            .forEach { entry: Map.Entry<String, List<VersionItem>> ->
                currentTask?.apply { if (isCancelled) return }

                mData.add(
                    ModListItemBean("Minecraft " + entry.key,
                    VersionAdapter(this, mInfoItem, platformHelper, entry.value, mPath))
                )
            }

        currentTask?.apply { if (isCancelled) return }

        Task.runTask(TaskExecutors.getAndroidUI()) {
            val modVersionView = recyclerView
            runCatching {
                var mModAdapter = modVersionView.adapter as ModListAdapter?
                mModAdapter ?: run {
                    mModAdapter = ModListAdapter(this, mData)
                    modVersionView.layoutManager = LinearLayoutManager(fragmentActivity!!)
                    modVersionView.adapter = mModAdapter
                    return@runCatching
                }
                mModAdapter?.updateData(mData)
            }.getOrElse { e ->
                Logging.e("Set Adapter", Tools.printToString(e))
            }

            componentProcessing(false)
            modVersionView.scheduleLayoutAnimation()
        }.execute()
    }

    @SuppressLint("CheckResult")
    private fun parseViewModel() {
        val viewModel = ViewModelProvider(fragmentActivity!!)[InfoViewModel::class.java]
        platformHelper = viewModel.platformHelper
        mInfoItem = viewModel.infoItem
        mPath = viewModel.targetPath

        mInfoItem.apply {
            setNameText(title)
            loadScreenshots()

            iconUrl?.apply {
                Glide.with(fragmentActivity!!).load(this).apply {
                    if (!AllSettings.resourceImageCache) diskCacheStrategy(DiskCacheStrategy.NONE)
                }.into(getIconView())
            }
        }
    }

    private fun loadScreenshots() {
        val progressBar = createProgressView(fragmentActivity!!)
        addMoreView(progressBar)

        Task.runTask {
            platformHelper.getScreenshots(mInfoItem.projectId)
        }.ended(TaskExecutors.getAndroidUI()) { screenshotItems ->
            screenshotItems?.let { setScreenshotView(it) }
            removeMoreView(progressBar)
        }.onThrowable { e ->
            Logging.e(
                "DownloadModFragment",
                "Unable to load screenshots, ${Tools.printToString(e)}"
            )
        }.execute()
    }

    @SuppressLint("CheckResult")
    private fun setScreenshotView(screenshotItems: List<ScreenshotItem>) {
        fragmentActivity?.let { activity ->
            val recyclerView = RecyclerView(activity).apply {
                layoutParams = RecyclerView.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                layoutManager = LinearLayoutManager(activity)
                adapter = ScreenshotAdapter(screenshotItems)
            }

            addMoreView(recyclerView)
        }
    }

    private fun createProgressView(context: Context): ProgressBar {
        return ProgressBar(context).apply {
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
        }
    }
}
