/*
 * Scribe
 * Copyright (C) 2021 ParchmentMC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.parchmentmc.scribe.util

import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiParameter

val PsiParameter.jvmIndex: Byte
    get() {
        val containingMethod = this.declarationScope as? PsiMethod ?: return -1
        val thisIndex = containingMethod.getParameterIndexOffset() + containingMethod.parameterList.getParameterIndex(this)
        return containingMethod.iterateJvmIndices { curIndex, curJvmIndex -> if (curIndex == thisIndex) curJvmIndex else null } ?: -1
    }

fun PsiMethod.getParameterByJvmIndex(jvmIndex: Byte): PsiParameter? {
    return iterateJvmIndices { curIndex, curJvmIndex ->
        if (curJvmIndex == jvmIndex) {
            this.parameterList.getParameter(curIndex - this.getParameterIndexOffset())
        } else null
    }
}

private fun <T> PsiMethod.iterateJvmIndices(successFun: (Int, Byte) -> T?): T? {
    val isStatic = this.modifierList.hasModifierProperty(PsiModifier.STATIC)
    val params = this.qualifiedMemberReference.descriptor?.substringAfter('(')?.substringBefore(')') ?: return null

    var i = 0
    var curIndex = 0
    var curJvmIndex: Byte = if (isStatic) 0 else 1
    var isArray = false
    while (i < params.length) {
        successFun(curIndex, curJvmIndex)?.let { return it }
        val c = params[i]
        when (c) {
            'D', 'J' -> if (!isArray) curJvmIndex++
            'L' -> i = params.indexOf(';', startIndex = i) // i++ will add one to this
        }
        if (!isArray) {
            curJvmIndex++
            curIndex++
        }
        isArray = c == '['
        i++
    }

    return null
}

fun PsiMethod.getParameterIndexOffset(): Int = when {
    this.isEnumConstructor() -> 2
    else -> 0
}

fun PsiMethod.isEnumConstructor(): Boolean = this.isConstructor && this.findContainingClass()?.isEnum == true