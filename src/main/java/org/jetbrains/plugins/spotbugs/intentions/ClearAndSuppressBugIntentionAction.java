/*
 * Copyright 2020 SpotBugs plugin contributors
 *
 * This file is part of IntelliJ SpotBugs plugin.
 *
 * IntelliJ SpotBugs plugin is free software: you can redistribute it 
 * and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 *
 * IntelliJ SpotBugs plugin is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied 
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with IntelliJ SpotBugs plugin.
 * If not, see <http://www.gnu.org/licenses/>.
 */
package org.jetbrains.plugins.spotbugs.intentions;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.ui.RowIcon;
import com.intellij.util.IconUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.spotbugs.common.ExtendedProblemDescriptor;
import org.jetbrains.plugins.spotbugs.gui.toolwindow.view.ToolWindowPanel;
import org.jetbrains.plugins.spotbugs.resources.ResourcesLoader;

import javax.swing.Icon;
import java.util.List;
import java.util.Map;

public class ClearAndSuppressBugIntentionAction extends SuppressReportBugIntentionAction {

	public ClearAndSuppressBugIntentionAction(final ExtendedProblemDescriptor problemDescriptor) {
		super(problemDescriptor);
	}

	@Override
	public void invoke(@NotNull final Project project, final Editor editor, @NotNull final PsiElement element) throws IncorrectOperationException {
		final ToolWindowPanel toolWindow = ToolWindowPanel.getInstance(project);
		final Map<PsiFile, List<ExtendedProblemDescriptor>> problems = toolWindow.getProblems();
		problems.get(element.getContainingFile()).remove(getProblemDescriptor());
		super.invoke(project, editor, element);
		DaemonCodeAnalyzer.getInstance(project).restart();
	}

	@Override
	@NotNull
	public String getText() {
		return ResourcesLoader.getString("findbugs.inspection.quickfix.clearAndSuppress") + " '" + getBugPatternId() + '\'';
	}

	@SuppressWarnings("HardcodedFileSeparator")
	@Override
	public Icon getIcon(final int flags) {
		return new RowIcon(
				IconUtil.scale(AllIcons.Actions.Cancel, null, 0.5f),
				IconUtil.scale(AllIcons.Ide.HectorOff, null, 0.5f)
		);
	}
}
