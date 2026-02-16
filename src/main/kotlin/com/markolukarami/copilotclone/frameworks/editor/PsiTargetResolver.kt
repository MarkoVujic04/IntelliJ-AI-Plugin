package com.markolukarami.copilotclone.frameworks.editor

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil

object PsiTargetResolver {

    fun findFirstClass(psiFile: PsiFile): PsiClass? {
        return when (psiFile) {
            is PsiJavaFile -> psiFile.classes.firstOrNull()
            else -> psiFile.children.filterIsInstance<PsiClass>().firstOrNull()
        }
    }

    fun findMethodByName(psiFile: PsiFile, name: String): PsiMethod? {
        val cls = findFirstClass(psiFile) ?: return null
        return cls.methods.firstOrNull { it.name == name }
    }
}