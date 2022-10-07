package nl.litpho.mybatis.generator.test.utils

import com.github.javaparser.JavaParser
import com.github.javaparser.ParseResult
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import org.junit.jupiter.api.fail
import java.io.File
import kotlin.io.path.Path

fun File.parseResult(vararg subpaths: String): ParseResult<CompilationUnit> =
    JavaParser().parse(Path(canonicalPath, *subpaths)).also {
        if (!it.isSuccessful) {
            fail(it.problems.joinToString("\n"))
        }
    }

fun ClassOrInterfaceDeclaration.innerClasses(): List<ClassOrInterfaceDeclaration> =
    members.filterIsInstance<ClassOrInterfaceDeclaration>()

fun <T : Node> NodeList<T>.getNull(idx: Int): T? =
    if (size > idx) {
        get(idx)
    } else {
        null
    }

fun ParseResult<CompilationUnit>.getType(idx: Int): TypeDeclaration<*> = result.get().types[idx]

fun TypeDeclaration<*>.fieldNamesAndTypes(): Map<String, String> =
    fields.flatMap { f -> f.variables }.associate { v -> v.nameAsString to v.typeAsString }
