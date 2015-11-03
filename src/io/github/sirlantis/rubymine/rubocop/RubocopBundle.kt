package io.github.sirlantis.rubymine.rubocop

import com.intellij.CommonBundle
import java.lang.ref.Reference
import java.util.ResourceBundle
import java.lang.ref.SoftReference

class RubocopBundle {
    companion object {
        val BUNDLE: String = "io.github.sirlantis.rubymine.rubocop.RubocopBundle"
        val LOG_ID: String = "io.github.sirlantis.rubymine.rubocop"

        fun message(key: String, vararg params: Any?): String {
            return CommonBundle.message(instance, key, *params)
        }

        private var ourBundle: Reference<ResourceBundle>? = null

        val instance: ResourceBundle
            get() {
                var bundle = com.intellij.reference.SoftReference.dereference(ourBundle)

                if (bundle == null) {
                    bundle = ResourceBundle.getBundle(BUNDLE)
                    ourBundle = SoftReference(bundle)
                }

                return bundle!!
            }
    }
}
