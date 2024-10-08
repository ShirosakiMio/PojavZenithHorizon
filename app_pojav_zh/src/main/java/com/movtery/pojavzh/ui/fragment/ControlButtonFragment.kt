package com.movtery.pojavzh.ui.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import com.getkeepsafe.taptargetview.TapTargetSequence
import com.movtery.anim.AnimPlayer
import com.movtery.anim.animations.Animations
import com.movtery.pojavzh.setting.Settings
import com.movtery.pojavzh.ui.dialog.EditControlInfoDialog
import com.movtery.pojavzh.ui.dialog.FilesDialog
import com.movtery.pojavzh.ui.dialog.FilesDialog.FilesButton
import com.movtery.pojavzh.ui.dialog.TipDialog
import com.movtery.pojavzh.ui.subassembly.customcontrols.ControlInfoData
import com.movtery.pojavzh.ui.subassembly.customcontrols.ControlSelectedListener
import com.movtery.pojavzh.ui.subassembly.customcontrols.ControlsListViewCreator
import com.movtery.pojavzh.ui.subassembly.customcontrols.EditControlData.Companion.createNewControlFile
import com.movtery.pojavzh.ui.subassembly.view.SearchViewWrapper
import com.movtery.pojavzh.utils.NewbieGuideUtils
import com.movtery.pojavzh.utils.PathAndUrlManager
import com.movtery.pojavzh.utils.ZHTools
import com.movtery.pojavzh.utils.anim.AnimUtils.Companion.setVisibilityAnim
import com.movtery.pojavzh.utils.file.FileTools.Companion.copyFileInBackground
import com.movtery.pojavzh.utils.file.PasteFile
import net.kdt.pojavlaunch.CustomControlsActivity
import net.kdt.pojavlaunch.PojavApplication
import net.kdt.pojavlaunch.R
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.contracts.OpenDocumentWithExtension
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import java.io.File

class ControlButtonFragment : FragmentWithAnim(R.layout.fragment_control_manager) {
    companion object {
        const val TAG: String = "ControlButtonFragment"
        const val BUNDLE_SELECT_CONTROL: String = "bundle_select_control"
    }

    private var openDocumentLauncher: ActivityResultLauncher<Any>? = null
    private var mControlLayout: View? = null
    private var mOperateLayout: View? = null
    private var mOperateView: View? = null
    private var mReturnButton: ImageButton? = null
    private var mAddControlButton: ImageButton? = null
    private var mImportControlButton: ImageButton? = null
    private var mPasteButton: ImageButton? = null
    private var mSearchSummonButton: ImageButton? = null
    private var mRefreshButton: ImageButton? = null
    private var mNothingTip: TextView? = null
    private var mSearchViewWrapper: SearchViewWrapper? = null
    private var controlsListViewCreator: ControlsListViewCreator? = null
    private var mSelectControl = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openDocumentLauncher = registerForActivityResult(OpenDocumentWithExtension("json")) { result: Uri? ->
            result?.let {
                Toast.makeText(requireContext(), getString(R.string.tasks_ongoing), Toast.LENGTH_SHORT).show()

                PojavApplication.sExecutorService.execute {
                    copyFileInBackground(requireContext(), result, File(PathAndUrlManager.DIR_CTRLMAP_PATH!!).absolutePath)
                    Tools.runOnUiThread {
                        Toast.makeText(requireContext(), getString(R.string.zh_file_added), Toast.LENGTH_SHORT).show()
                        controlsListViewCreator?.refresh()
                    }
                }
            }
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindViews(view)
        parseBundle()

        controlsListViewCreator?.setSelectedListener(object : ControlSelectedListener() {
            override fun onItemSelected(file: File) {
                if (mSelectControl) {
                    ExtraCore.setValue(ExtraConstants.FILE_SELECTOR, removeLockPath(file.absolutePath))
                    Tools.removeCurrentFragment(requireActivity())
                } else {
                    if (file.isFile) showDialog(file)
                }
            }

            override fun onItemLongClick(file: File) {
                TipDialog.Builder(requireContext())
                    .setTitle(R.string.default_control)
                    .setMessage(R.string.zh_controls_set_default_message)
                    .setConfirmClickListener {
                        val absolutePath = file.absolutePath
                        Settings.Manager.put("defaultCtrl", absolutePath).save()
                    }.buildDialog()
            }
        })

        controlsListViewCreator?.setRefreshListener {
            val itemCount = controlsListViewCreator!!.itemCount
            val show = itemCount == 0
            setVisibilityAnim(mNothingTip!!, show)
        }

        mReturnButton?.setOnClickListener { ZHTools.onBackPressed(requireActivity()) }
        mPasteButton?.setOnClickListener {
            PasteFile.getInstance().pasteFiles(requireActivity(), File(PathAndUrlManager.DIR_CTRLMAP_PATH!!), null) {
                Tools.runOnUiThread {
                    mPasteButton?.visibility = View.GONE
                    controlsListViewCreator?.refresh()
                }
            }
        }
        mImportControlButton?.setOnClickListener {
            val suffix = ".json"
            Toast.makeText(requireActivity(), String.format(getString(R.string.zh_file_add_file_tip), suffix), Toast.LENGTH_SHORT).show()
            openDocumentLauncher?.launch(suffix)
        } //限制.json文件
        mAddControlButton?.setOnClickListener {
            val editControlInfoDialog = EditControlInfoDialog(requireContext(), true, null, ControlInfoData())
            editControlInfoDialog.setTitle(getString(R.string.zh_controls_create_new))
            editControlInfoDialog.setOnConfirmClickListener { fileName: String, controlInfoData: ControlInfoData? ->
                val file = File(File(PathAndUrlManager.DIR_CTRLMAP_PATH!!).absolutePath, "$fileName.json")
                if (file.exists()) { //检查文件是否已经存在
                    editControlInfoDialog.fileNameEditBox.error =
                        getString(R.string.zh_file_rename_exitis)
                    return@setOnConfirmClickListener
                }

                //创建布局文件
                createNewControlFile(requireContext(), file, controlInfoData)

                controlsListViewCreator?.refresh()
                editControlInfoDialog.dismiss()
            }
            editControlInfoDialog.show()
        }
        mSearchSummonButton?.setOnClickListener { mSearchViewWrapper?.setVisibility() }
        mRefreshButton?.setOnClickListener { controlsListViewCreator?.refresh() }

        controlsListViewCreator?.listAtPath()

        startNewbieGuide()
    }

    private fun removeLockPath(path: String?): String {
        return path!!.replace(PathAndUrlManager.DIR_CTRLMAP_PATH!!, ".")
    }

    private fun showDialog(file: File) {
        val filesButton = FilesButton()
        filesButton.setButtonVisibility(true, true, true, true, true, true)
        filesButton.setMessageText(getString(R.string.zh_file_message))
        filesButton.setMoreButtonText(getString(R.string.global_load))

        val filesDialog = FilesDialog(requireContext(), filesButton,
            { Tools.runOnUiThread { controlsListViewCreator?.refresh() } },
            controlsListViewCreator!!.fullPath, file
        )

        filesDialog.setCopyButtonClick { mPasteButton?.visibility = View.VISIBLE }

        filesDialog.setMoreButtonClick {
            val intent = Intent(requireContext(), CustomControlsActivity::class.java)
            val bundle = Bundle()
            bundle.putString(CustomControlsActivity.BUNDLE_CONTROL_PATH, file.absolutePath)
            intent.putExtras(bundle)

            startActivity(intent)
            filesDialog.dismiss()
        } //加载
        filesDialog.show()
    }

    private fun parseBundle() {
        val bundle = arguments ?: return
        mSelectControl = bundle.getBoolean(BUNDLE_SELECT_CONTROL, mSelectControl)
    }

    private fun bindViews(view: View) {
        mControlLayout = view.findViewById(R.id.control_layout)
        mOperateLayout = view.findViewById(R.id.operate_layout)

        mOperateView = view.findViewById(R.id.operate_view)

        mReturnButton = view.findViewById(R.id.zh_return_button)
        mImportControlButton = view.findViewById(R.id.zh_add_file_button)
        mAddControlButton = view.findViewById(R.id.zh_create_folder_button)
        mPasteButton = view.findViewById(R.id.zh_paste_button)
        mRefreshButton = view.findViewById(R.id.zh_refresh_button)
        mSearchSummonButton = view.findViewById(R.id.zh_search_button)
        mNothingTip = view.findViewById(R.id.zh_controls_nothing)

        mImportControlButton?.setContentDescription(getString(R.string.zh_controls_import_control))
        mAddControlButton?.setContentDescription(getString(R.string.zh_controls_create_new))

        controlsListViewCreator =
            ControlsListViewCreator(requireContext(), view.findViewById(R.id.zh_controls_list))

        mSearchViewWrapper = SearchViewWrapper(view, view.findViewById(R.id.zh_search_view))
        mSearchViewWrapper?.setAsynchronousUpdatesListener(object : SearchViewWrapper.SearchAsynchronousUpdatesListener {
            override fun onSearch(searchCount: TextView?, string: String?, caseSensitive: Boolean) {
                controlsListViewCreator?.searchControls(searchCount, string, caseSensitive)
            }
        })
        mSearchViewWrapper?.setShowSearchResultsListener(object : SearchViewWrapper.ShowSearchResultsListener {
            override fun onSearch(show: Boolean) {
                controlsListViewCreator?.setShowSearchResultsOnly(show)
            }
        })

        mPasteButton?.setVisibility(if (PasteFile.getInstance().pasteType != null) View.VISIBLE else View.GONE)

        ZHTools.setTooltipText(mReturnButton, mReturnButton?.contentDescription)
        ZHTools.setTooltipText(mImportControlButton, mImportControlButton?.contentDescription)
        ZHTools.setTooltipText(mAddControlButton, mAddControlButton?.contentDescription)
        ZHTools.setTooltipText(mPasteButton, mPasteButton?.contentDescription)
        ZHTools.setTooltipText(mSearchSummonButton, mSearchSummonButton?.contentDescription)
        ZHTools.setTooltipText(mRefreshButton, mRefreshButton?.contentDescription)
    }

    private fun startNewbieGuide() {
        if (NewbieGuideUtils.showOnlyOne(TAG)) return
        val fragmentActivity = requireActivity()
        TapTargetSequence(fragmentActivity)
            .targets(
                NewbieGuideUtils.getSimpleTarget(fragmentActivity, mRefreshButton, getString(R.string.zh_refresh), getString(R.string.zh_newbie_guide_general_refresh)),
                NewbieGuideUtils.getSimpleTarget(fragmentActivity, mSearchSummonButton, getString(R.string.zh_search), getString(R.string.zh_newbie_guide_control_search)),
                NewbieGuideUtils.getSimpleTarget(fragmentActivity, mImportControlButton, getString(R.string.zh_controls_import_control), getString(R.string.zh_newbie_guide_control_import)),
                NewbieGuideUtils.getSimpleTarget(fragmentActivity, mAddControlButton, getString(R.string.zh_controls_create_new), getString(R.string.zh_newbie_guide_control_create)),
                NewbieGuideUtils.getSimpleTarget(fragmentActivity, mReturnButton, getString(R.string.zh_return), getString(R.string.zh_newbie_guide_general_close)))
            .start()
    }

    override fun slideIn(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(mControlLayout!!, Animations.BounceInDown))
            .apply(AnimPlayer.Entry(mOperateLayout!!, Animations.BounceInLeft))
            .apply(AnimPlayer.Entry(mOperateView!!, Animations.FadeInLeft))
    }

    override fun slideOut(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(mControlLayout!!, Animations.FadeOutUp))
            .apply(AnimPlayer.Entry(mOperateLayout!!, Animations.FadeOutRight))
    }
}

