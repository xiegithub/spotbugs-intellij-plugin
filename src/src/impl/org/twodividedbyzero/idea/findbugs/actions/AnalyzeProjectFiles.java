/*
 * Copyright 2008-2016 Andre Pfeiler
 *
 * This file is part of FindBugs-IDEA.
 *
 * FindBugs-IDEA is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FindBugs-IDEA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FindBugs-IDEA.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.twodividedbyzero.idea.findbugs.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.util.Consumer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jetbrains.annotations.NotNull;
import org.twodividedbyzero.idea.findbugs.collectors.RecurseFileCollector;
import org.twodividedbyzero.idea.findbugs.common.util.New;
import org.twodividedbyzero.idea.findbugs.core.FindBugsProject;
import org.twodividedbyzero.idea.findbugs.core.FindBugsProjects;
import org.twodividedbyzero.idea.findbugs.core.FindBugsStarter;
import org.twodividedbyzero.idea.findbugs.core.FindBugsState;
import org.twodividedbyzero.idea.findbugs.resources.ResourcesLoader;

import java.io.File;
import java.util.Map;

public final class AnalyzeProjectFiles extends AbstractAnalyzeAction {

	@Override
	void updateImpl(
			@NotNull final AnActionEvent e,
			@NotNull final Project project,
			@NotNull final ToolWindow toolWindow,
			@NotNull final FindBugsState state
	) {

		final boolean enable = state.isIdle();

		e.getPresentation().setEnabled(enable);
		e.getPresentation().setVisible(true);
	}

	@SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
	@Override
	void analyze(
			@NotNull final AnActionEvent e,
			@NotNull final Project project,
			@NotNull final ToolWindow toolWindow,
			@NotNull final FindBugsState state
	) {

		new FindBugsStarter(project, "Running FindBugs analysis for project '" + project.getName() + "'...") {
			@Override
			protected void createCompileScope(@NotNull final CompilerManager compilerManager, @NotNull final Consumer<CompileScope> consumer) {
				consumer.consume(compilerManager.createProjectCompileScope(project));
			}

			@Override
			protected boolean configure(@NotNull final ProgressIndicator indicator, @NotNull final FindBugsProjects projects) {
				final Module[] modules = ModuleManager.getInstance(project).getModules();
				final Map<Module, VirtualFile> compilerOutputPaths = New.map();
				for (final Module module : modules) {
					final CompilerModuleExtension extension = CompilerModuleExtension.getInstance(module);
					if (extension == null) {
						throw new IllegalStateException("No compiler extension for module " + module.getName());
					}
					final VirtualFile compilerOutputPath = extension.getCompilerOutputPath();
					if (compilerOutputPath == null) {
						showWarning(ResourcesLoader.getString("analysis.moduleNotCompiled", module.getName()));
						return false;
					}
					compilerOutputPaths.put(module, compilerOutputPath);
				}

				indicator.setText("Collecting files for analysis...");
				final int[] count = new int[1];
				for (final Map.Entry<Module, VirtualFile> compilerOutputPath : compilerOutputPaths.entrySet()) {
					final FindBugsProject findBugsProject = projects.get(compilerOutputPath.getKey());
					RecurseFileCollector.addFiles(project, indicator, findBugsProject, new File(compilerOutputPath.getValue().getCanonicalPath()), count);
				}
				return true;
			}
		}.start();
	}
}
