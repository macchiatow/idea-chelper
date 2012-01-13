package net.egork.chelper;

import com.intellij.execution.RunManagerAdapter;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerAdapter;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import net.egork.chelper.configurations.TaskConfiguration;
import net.egork.chelper.configurations.TopCoderConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.TopCoderTask;
import org.jetbrains.annotations.NotNull;

/**
 * @author Egor Kulikov (egor@egork.net)
 */
public class AutoSwitcher implements ProjectComponent {
	private final Project project;
	private boolean busy;

	public AutoSwitcher(Project project) {
		this.project = project;
	}

	public void initComponent() {
		// TODO: insert component initialization logic here
	}

	public void disposeComponent() {
		// TODO: insert component disposal logic here
	}

	@NotNull
	public String getComponentName() {
		return "AutoSwitcher";
	}

	public void projectOpened() {
		RunManagerImpl.getInstanceImpl(project).addRunManagerListener(new RunManagerAdapter() {
			@Override
			public void runConfigurationSelected() {
				RunnerAndConfigurationSettings selectedConfiguration =
					RunManagerImpl.getInstanceImpl(project).getSelectedConfiguration();
				if (selectedConfiguration == null)
					return;
				RunConfiguration configuration = selectedConfiguration.getConfiguration();
				if (busy ||
					!(configuration instanceof TopCoderConfiguration || configuration instanceof TaskConfiguration))
				{
					return;
				}
				busy = true;
				VirtualFile toOpen = null;
				if (configuration instanceof TopCoderConfiguration)
					toOpen = ((TopCoderConfiguration) configuration).getConfiguration().getFile();
				else if (configuration instanceof TaskConfiguration)
					toOpen = ((TaskConfiguration) configuration).getConfiguration().getFile();
				if (toOpen != null)
					FileEditorManager.getInstance(project).openFile(toOpen, true);
				busy = false;
			}
		});
		FileEditorManager.getInstance(project).addFileEditorManagerListener(new FileEditorManagerAdapter() {
			@Override
			public void fileOpened(FileEditorManager source, VirtualFile file) {
				selectTask(file);
			}

			private void selectTask(VirtualFile file) {
				if (busy || file == null)
					return;
				RunManagerImpl runManager = RunManagerImpl.getInstanceImpl(project);
				for (RunConfiguration configuration : runManager.getAllConfigurations()) {
					if (configuration instanceof TopCoderConfiguration) {
						TopCoderTask task = ((TopCoderConfiguration) configuration).getConfiguration();
						if (file.equals(task.getFile())) {
							busy = true;
							runManager.setActiveConfiguration(new RunnerAndConfigurationSettingsImpl(runManager,
								configuration, false));
							busy = false;
							return;
						}
					} else if (configuration instanceof TaskConfiguration) {
						Task task = ((TaskConfiguration) configuration).getConfiguration();
						if (file.equals(task.getFile()) || file.equals(task.getCheckerFile())) {
							busy = true;
							runManager.setActiveConfiguration(new RunnerAndConfigurationSettingsImpl(runManager,
								configuration, false));
							busy = false;
							return;
						}
					}
				}
			}

			@Override
			public void selectionChanged(FileEditorManagerEvent event) {
				selectTask(event.getNewFile());
			}
		});
	}

	public void projectClosed() {
		// called when project is being closed
	}
}