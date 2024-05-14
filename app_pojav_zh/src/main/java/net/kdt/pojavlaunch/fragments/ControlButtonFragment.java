package net.kdt.pojavlaunch.fragments;

import static net.kdt.pojavlaunch.CustomControlsActivity.BUNDLE_CONTROL_PATH;
import static net.kdt.pojavlaunch.PojavZHTools.copyFileInBackground;
import static net.kdt.pojavlaunch.Tools.runOnUiThread;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.ipaulpro.afilechooser.FileIcon;
import com.kdt.pickafile.FileListView;
import com.kdt.pickafile.FileSelectedListener;

import net.kdt.pojavlaunch.CustomControlsActivity;
import net.kdt.pojavlaunch.PojavApplication;
import net.kdt.pojavlaunch.PojavZHTools;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.contracts.OpenDocumentWithExtension;
import net.kdt.pojavlaunch.dialog.CopyDialog;
import net.kdt.pojavlaunch.dialog.DeleteDialog;
import net.kdt.pojavlaunch.dialog.EditTextDialog;
import net.kdt.pojavlaunch.dialog.FilesDialog;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class ControlButtonFragment extends Fragment {
    public static final String TAG = "ControlButtonFragment";
    private ActivityResultLauncher<Object> openDocumentLauncher;
    private Button mReturnButton, mAddControlButton, mImportControlButton, mRefreshButton;
    private ImageButton mHelpButton;
    private FileListView mFileListView;
    private TextView mFilePathView;

    public ControlButtonFragment() {
        super(R.layout.fragment_files);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        openDocumentLauncher = registerForActivityResult(
                new OpenDocumentWithExtension("json"),
                result -> {
                    if (result != null) {
                        Toast.makeText(requireContext(), getString(R.string.tasks_ongoing), Toast.LENGTH_SHORT).show();

                        PojavApplication.sExecutorService.execute(() -> {
                            copyFileInBackground(requireContext(), result, mFileListView.getFullPath().getAbsolutePath());

                            runOnUiThread(() -> {
                                Toast.makeText(requireContext(), getString(R.string.zh_file_added), Toast.LENGTH_SHORT).show();
                                mFileListView.refreshPath();
                            });
                        });
                    }
                }
        );
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        bindViews(view);

        mFileListView.setShowFiles(true);
        mFileListView.setShowFolders(true);
        mFileListView.lockPathAt(controlPath());
        mFileListView.setDialogTitleListener((title) -> mFilePathView.setText(removeLockPath(title)));
        mFileListView.refreshPath();

        mFileListView.setFileSelectedListener(new FileSelectedListener() {
            @Override
            public void onFileSelected(File file, String path) {
                FilesDialog.FilesButton filesButton = new FilesDialog.FilesButton();
                filesButton.setButtonVisibility(true, true, true, true);

                filesButton.messageText = getString(R.string.zh_file_message) + "\n" + getString(R.string.zh_file_message_copy);
                filesButton.moreButtonText = getString(R.string.global_load);

                FilesDialog filesDialog = new FilesDialog(requireContext(), filesButton, () -> runOnUiThread(() -> mFileListView.refreshPath()), file);
                filesDialog.setMoreButtonClick(v -> {
                    Intent intent = new Intent(requireContext(), CustomControlsActivity.class);

                    Bundle bundle = new Bundle();
                    bundle.putString(BUNDLE_CONTROL_PATH, file.getAbsolutePath());
                    intent.putExtras(bundle);

                    startActivity(intent);
                    filesDialog.dismiss();
                }); //加载
                filesDialog.show();
            }

            @Override
            public void onItemLongClick(File file, String path) {
                if (file.isDirectory()) {
                    DeleteDialog deleteDialog = new DeleteDialog(requireContext(), () -> runOnUiThread(() -> mFileListView.refreshPath()), file);
                    deleteDialog.show();
                } else {
                    CopyDialog dialog = new CopyDialog(requireContext(), mFileListView, file);
                    dialog.show();
                }
            }
        });

        mReturnButton.setOnClickListener(v -> PojavZHTools.onBackPressed(requireActivity()));
        mImportControlButton.setOnClickListener(v -> {
            String suffix = ".json";
            Toast.makeText(requireActivity(), String.format(getString(R.string.zh_file_add_file_tip), suffix), Toast.LENGTH_SHORT).show();
            openDocumentLauncher.launch(suffix);
        }); //限制.json文件
        mAddControlButton.setOnClickListener(v -> {
            EditTextDialog editTextDialog = new EditTextDialog(requireContext(), getString(R.string.zh_controls_create_new_title), null, null, null);
            editTextDialog.setConfirm(view1 -> {
                File file = new File(mFileListView.getFullPath().getAbsolutePath(), editTextDialog.getEditBox().getText().toString().replace("/", "") + ".json");
                if (!file.exists()) {
                    boolean success;
                    try {
                        success = file.createNewFile();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    if (success) {
                        try (BufferedWriter optionFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
                            optionFileWriter.write("{\"version\":6}");
                        } catch (IOException e) {
                            Tools.showError(requireContext(), e);
                        }
                    }
                    mFileListView.refreshPath();
                } else {
                    Toast.makeText(requireContext(), getString(R.string.zh_file_create_file_invalid), Toast.LENGTH_SHORT).show();
                }

                editTextDialog.dismiss();
            });
            editTextDialog.show();
        });
        mRefreshButton.setOnClickListener(v -> mFileListView.refreshPath());
        mHelpButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());

            builder.setTitle(getString(R.string.zh_help_control_button_title));
            builder.setMessage(getString(R.string.zh_help_control_button_message));
            builder.setPositiveButton(getString(R.string.zh_help_ok), null);

            builder.show();
        });
    }

    private File controlPath() {
        File ctrlPath = new File(Tools.CTRLMAP_PATH);
        if (!ctrlPath.exists()) ctrlPath.mkdirs();
        return ctrlPath;
    }

    private String removeLockPath(String path) {
        return path.replace(Tools.CTRLMAP_PATH, ".");
    }

    private void bindViews(@NonNull View view) {
        mReturnButton = view.findViewById(R.id.zh_files_return_button);
        mImportControlButton = view.findViewById(R.id.zh_files_add_file_button);
        mAddControlButton = view.findViewById(R.id.zh_files_create_folder_button);
        mRefreshButton = view.findViewById(R.id.zh_files_refresh_button);
        mHelpButton = view.findViewById(R.id.zh_files_help_button);
        mFileListView = view.findViewById(R.id.zh_files);
        mFilePathView = view.findViewById(R.id.zh_files_current_path);

        mImportControlButton.setText(getString(R.string.zh_controls_import_control));
        mAddControlButton.setText(getString(R.string.zh_controls_create_new));

        view.findViewById(R.id.zh_files_icon).setVisibility(View.GONE);
        mFileListView.setFileIcon(FileIcon.CONTROL);
    }
}

