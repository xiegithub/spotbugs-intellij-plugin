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
package org.jetbrains.plugins.spotbugs.common;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.QuickFix;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.lang.annotation.ProblemGroup;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import edu.umd.cs.findbugs.MethodAnnotation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.spotbugs.common.util.BugInstanceUtil;
import org.jetbrains.plugins.spotbugs.common.util.IdeaUtilImpl;
import org.jetbrains.plugins.spotbugs.core.Bug;

public class ExtendedProblemDescriptor implements ProblemDescriptor, ProblemGroup {

	@NotNull
	private final PsiFile psiFile;

	private PsiElement psiElement;

	/**
	 * This is the line as reported by FindBugs, rather than that computed by IDEA.
	 */
	private final int lineStart;
	private final int lineEnd;

	@NotNull
	private final Bug bug;


	public ExtendedProblemDescriptor(@NotNull final PsiFile psiFile, @NotNull final Bug bug) {
		this.psiFile = psiFile;
		this.bug = bug;
		final int[] lines = BugInstanceUtil.getSourceLines(this.bug.getInstance());
		lineStart = lines[0] - 1;
		lineEnd = lines[1];
	}

	@Override
	public boolean showTooltip() {
		return true;
	}

	@NotNull
	public Bug getBug() {
		return bug;
	}

	@Override
	public void setTextAttributes(final TextAttributesKey key) {
	}

	@Nullable
	@Override
	public ProblemGroup getProblemGroup() {
		return this;
	}

	@Override
	public void setProblemGroup(@Nullable final ProblemGroup problemgroup) {
	}

	@Override
	public String getProblemName() {
		return "SpotBugs";
	}

	@Override
	public PsiElement getEndElement() {
		return getPsiElement();
	}

	@NotNull
	@Override
	public ProblemHighlightType getHighlightType() {
		return ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
	}

	@Override
	public int getLineNumber() {
		return lineStart;
	}

	@Override
	public PsiElement getPsiElement() {
		if (psiElement != null) {
			return psiElement;
		}
		if (lineStart == 0 && lineEnd == 1) {
			psiElement = IdeaUtilImpl.findPsiElement(psiFile, bug.getInstance(), psiFile.getProject());
		} else if (lineStart < 0) {
			psiElement = IdeaUtilImpl.findClassPsiElement(psiFile, bug.getInstance(), psiFile.getProject());
		} else {
			psiElement = IdeaUtilImpl.getElementAtLine(psiFile, lineStart);
		}
		final MethodAnnotation primaryMethod = BugInstanceUtil.getPrimaryMethod(bug.getInstance());
		if (primaryMethod != null && DebuggerUtilsEx.isLambdaName(primaryMethod.getMethodName())) {
			psiElement = IdeaUtilImpl.findOnlyLambdaExpressionOrPsiElement(psiElement);
		}
		return psiElement;
	}

	@Override
	public PsiElement getStartElement() {
		return getPsiElement();
	}

	@Override
	public boolean isAfterEndOfLine() {
		return false;
	}

	@NotNull
	@Override
	public String getDescriptionTemplate() {
		return "<description template>";
	}

	@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({"PZLA_PREFER_ZERO_LENGTH_ARRAYS"})
	@Nullable
	@Override
	public QuickFix<?>[] getFixes() {
		return null;
	}

	@Override
	public TextRange getTextRangeInElement() {
		return null;
	}

	@NotNull
	public PsiFile getPsiFile() {
		return psiFile;
	}
}
