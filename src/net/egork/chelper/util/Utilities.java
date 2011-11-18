package net.egork.chelper.util;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerAdapter;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import net.egork.chelper.ProjectData;
import net.egork.chelper.task.StreamConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.TestType;

import javax.swing.JComponent;
import java.awt.Dimension;
import java.awt.Point;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class Utilities {
	private static Map<Project, ProjectData> eligibleProjects = new HashMap<Project, ProjectData>();
	private static Task defaultConfiguration = new Task(null, null, TestType.SINGLE, StreamConfiguration.STANDARD,
		StreamConfiguration.STANDARD, "256M", "64M", null);

	public static void addListeners() {
		ProjectManager.getInstance().addProjectManagerListener(new ProjectManagerAdapter() {
			@Override
			public void projectOpened(Project project) {
				ProjectData configuration = ProjectData.load(project);
				if (configuration != null)
					eligibleProjects.put(project, configuration);
			}

			@Override
			public void projectClosed(Project project) {
				eligibleProjects.remove(project);
			}
		});
	}

	public static boolean isEligible(DataContext dataContext) {
		return eligibleProjects.containsKey(getProject(dataContext));
	}

	public static Project getProject(DataContext dataContext) {
		return PlatformDataKeys.PROJECT.getData(dataContext);
	}

	public static void updateDefaultTask(Task task) {
		if (task != null)
			defaultConfiguration = task.setDirectory(null).setProject(null).setName("Task");
	}

	public static Task getDefaultTask() {
		return defaultConfiguration;
	}

	public static ProjectData getData(Project project) {
		return eligibleProjects.get(project);
	}

	public static void openElement(Project project, PsiElement element) {
		if (element instanceof PsiFile) {
			VirtualFile virtualFile = ((PsiFile) element).getVirtualFile();
			if (virtualFile == null)
				return;
			FileEditorManager.getInstance(project).openFile(virtualFile, true);
		} else if (element instanceof PsiClass) {
			FileEditorManager.getInstance(project).openFile(FileUtilities.getFile(project,
				getData(project).defaultDir + "/" + ((PsiClass) element).getName() + ".java"), true);
		}
	}

	public static Point getLocation(Project project, Dimension size) {
		JComponent component = WindowManager.getInstance().getIdeFrame(project).getComponent();
		Point center = component.getLocationOnScreen();
		center.x += component.getWidth() / 2;
		center.y += component.getHeight() / 2;
		center.x -= size.getWidth() / 2;
		center.y -= size.getHeight() / 2;
		return center;
	}
}
