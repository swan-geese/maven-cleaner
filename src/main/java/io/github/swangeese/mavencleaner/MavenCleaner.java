package io.github.swangeese.mavencleaner;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * @auther swan-geese
 * @verison
 * @date
 * @Description 通过 Pulgins Devkit 创建的 action 继承了 Ananction
 * 此方式是基于点击 Tools 菜单中选择 MavenCleaner 插件实现
 */
public class MavenCleaner extends AnAction {

    /**
     * maven 存储路径
     */
    private static final String MAVEN_REPO_PATH_KEY = "MAVEN_REPO_PATH";

    /**
     * 是否总是使用保存的路径
     */
    private static final String ALWAYS_USE_SAVED_PATH_KEY = "ALWAYS_USE_SAVED_PATH";

    @Override
    public void actionPerformed(AnActionEvent e) {
        // 获取用户选择的 Maven 仓库路径
        String mavenRepoPath = getMavenRepoPath();

        // 执行清理操作
        boolean success = cleanMavenRepository(mavenRepoPath);

        // 显示清理结果
        if (success) {
            Messages.showInfoMessage("Maven Repository Cleaned Successfully!", "Success");
//        } else {
//            Messages.showErrorDialog("Failed To Clean Maven Repository.", "Error");
        }
    }

    // 获取用户选择的 Maven 仓库路径
    public String getMavenRepoPath() {
        PropertiesComponent propertiesComponent = PropertiesComponent.getInstance();

        // 尝试从缓存中获取
        String cachedPath = propertiesComponent.getValue(MAVEN_REPO_PATH_KEY);
        boolean alwaysUseSavedPath = propertiesComponent.getBoolean(ALWAYS_USE_SAVED_PATH_KEY, false);

        if (alwaysUseSavedPath && !StringUtils.isEmpty(cachedPath)) {
            return cachedPath;
        } else {
            // 显示设置面板
            MavenCleanerSettingsDialog dialog = new MavenCleanerSettingsDialog();
            boolean showDialog = dialog.showAndGet();
            if (!showDialog) {
                // 用户取消了设置，返回空字符串
                return "";
            }

            String mavenRepoPath = dialog.getMavenRepoPath();
            propertiesComponent.setValue(MAVEN_REPO_PATH_KEY, mavenRepoPath);
            propertiesComponent.setValue(ALWAYS_USE_SAVED_PATH_KEY, dialog.isAlwaysUseSavedPath());
            return mavenRepoPath;
        }
    }

    /**
     * 清理 Maven 仓库
     * @param mavenRepoPath
     * @return
     */
    public boolean cleanMavenRepository(String mavenRepoPath) {
        if (StringUtils.isEmpty(mavenRepoPath)) {
            Messages.showErrorDialog("Maven Repository Path Is Empty.", "Error");
            return false;
        }

        Path mavenRepoDir = Paths.get(mavenRepoPath);

        if (!Files.exists(mavenRepoDir) || !Files.isDirectory(mavenRepoDir)) {
            Messages.showErrorDialog("Invalid Maven Repository Path.", "Error");
            return false;
        }

        try {
            Files.walkFileTree(mavenRepoDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    //method 1: 检查是否存在对应.jar文件,存在则跳过
                    // 检查文件是否是 JAR 文件并且下载失败（根据需要实现）
//                    if (downloadFailed(file)) {
//                        // 检查是否存在对应的 .jar 文件
//                        Path jarFile = getCorrespondingJarFile(file);
//                        if (Files.exists(jarFile)) {
//                            // 如果存在对应的 .jar 文件，跳过删除操作
//                            System.out.println("Skipping: " + file);
//                        } else {
//                            Files.delete(file);
//                            System.out.println("Deleted: " + file);
//                        }
//                    }
                    //method2: 暴力手段，只要有满足 .lastUpdated 的文件，直接强行删除
                    if (downloadFailed(file)) {
                        Files.delete(file);
                        System.out.println("Deleted: " + file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            Messages.showErrorDialog("Failed To Clean Maven Repository.", "Error");
            e.printStackTrace();
            return false;
        }

        return true; // 返回清理操作是否成功
    }

    private boolean isJarFile(Path file) {
        return Files.isRegularFile(file) && file.toString().toLowerCase().endsWith(".jar");
    }

    private Path getCorrespondingJarFile(Path lastUpdatedFile) {
        String lastUpdatedFileName = lastUpdatedFile.getFileName().toString();
        String jarFileName = lastUpdatedFileName.replace(".jar.lastUpdated", ".jar");
        return lastUpdatedFile.resolveSibling(jarFileName);
    }

    private boolean downloadFailed(Path file) {
        // 根据需要实现判断 JAR 文件是否下载失败
        if (file.getFileName().toString().endsWith(".lastUpdated")) {
            return true;
        }
        return false;
    }

    private static class MavenCleanerSettingsDialog extends DialogWrapper {

        private JCheckBox alwaysUseSavedPathCheckBox;
        private JTextField mavenRepoPathField;

        protected MavenCleanerSettingsDialog() {
            super(true);
            init();
            // 设计窗体大小
            setTitle("Maven Cleaner Settings");
        }

        @Nullable
        @Override
        protected JComponent createCenterPanel() {
            JPanel panel = new JPanel(new GridBagLayout());

            alwaysUseSavedPathCheckBox = new JCheckBox("Always Use Saved Path");
            mavenRepoPathField = new JTextField();
            JLabel mavenLabel = new JLabel("Maven Path:");

            // 从缓存中获取上次保存的路径，并在界面上显示
            String cachedPath = PropertiesComponent.getInstance().getValue(MAVEN_REPO_PATH_KEY);
            mavenRepoPathField.setText(cachedPath);

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 1;
            gbc.gridy = 0;

            panel.add(alwaysUseSavedPathCheckBox, gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            panel.add(mavenLabel, gbc);

            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(5, 30, 5, 30);
            panel.add(mavenRepoPathField, gbc);

            return panel;
        }

        public boolean isAlwaysUseSavedPath() {
            return alwaysUseSavedPathCheckBox.isSelected();
        }

        public String getMavenRepoPath() {
            return mavenRepoPathField.getText();
        }
    }
}
